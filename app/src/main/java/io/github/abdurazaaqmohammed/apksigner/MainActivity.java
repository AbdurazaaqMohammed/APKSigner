package io.github.abdurazaaqmohammed.apksigner;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;
import static io.github.abdurazaaqmohammed.apksigner.LegacyUtils.doesNotSupportInbuiltAndroidFilePicker;
import static io.github.abdurazaaqmohammed.apksigner.LegacyUtils.supportsActionBar;
import static io.github.abdurazaaqmohammed.apksigner.LegacyUtils.supportsArraysCopyOfAndDownloadManager;
import static io.github.abdurazaaqmohammed.apksigner.LegacyUtils.supportsAsyncTask;

import com.aefyr.pseudoapksigner.IOUtils;
import com.aefyr.pseudoapksigner.PseudoApkSigner;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import io.github.abdurazaaqmohammed.ApkSigner.R;

import com.starry.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.github.paul035.LocaleHelper;
import com.starry.FileUtilsWrapper;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import yuku.ambilwarna.AmbilWarnaDialog;

/** @noinspection deprecation*/
public class MainActivity extends Activity {
    private static boolean ask = true;
    private Uri apkUri;
    private ArrayList<Uri> uris;
    private boolean multi;
    public static int textColor;
    public static int bgColor;
    public static boolean errorOccurred;
    public static String lang;
    private String keyPath;
    private String password;
    public FileUtilsWrapper fileUtil;
    private boolean v1;
    private boolean v2;
    private boolean v3;
    private boolean v4;

    public void setButtonBorder(Button button) {
        ShapeDrawable border = new ShapeDrawable(new RectShape());
        Paint paint = border.getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(textColor);
        paint.setStrokeWidth(4);

        button.setTextColor(textColor);
        button.setBackgroundDrawable(border);
    }

    private void setColor(int color, boolean isTextColor, ScrollView settingsMenu) {
        final boolean supportsSwitch = Build.VERSION.SDK_INT > 13;
        boolean fromSettingsMenu = settingsMenu != null;
        //if(fromSettingsMenu) settingsMenu.setBackgroundColor(color);
        if(isTextColor) {
            textColor = color;
            if(fromSettingsMenu) ((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.ask : R.id.askText)).setTextColor(color);
        } else findViewById(R.id.main).setBackgroundColor(bgColor = color);
        Button decodeButton = findViewById(R.id.decodeButton);
        setButtonBorder(decodeButton);
        LightingColorFilter themeColor = new LightingColorFilter(0xFF000000, textColor);
        ImageView settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setColorFilter(themeColor);

        ((ImageView) findViewById(R.id.loadingImage)).setColorFilter(themeColor);
        if(fromSettingsMenu) {
            setButtonBorder(settingsMenu.findViewById(R.id.langPicker));
            setButtonBorder(settingsMenu.findViewById(R.id.changeTextColor));
            setButtonBorder(settingsMenu.findViewById(R.id.changeBgColor));
            setButtonBorder(settingsMenu.findViewById(R.id.keyPicker));

            if(!supportsSwitch) {
                setButtonBorder(settingsMenu.findViewById(R.id.ask));
                setButtonBorder(settingsMenu.findViewById(R.id.v1));
                setButtonBorder(settingsMenu.findViewById(R.id.v2));
                setButtonBorder(settingsMenu.findViewById(R.id.v3));
                setButtonBorder(settingsMenu.findViewById(R.id.v4));
            } else {
                ((TextView) settingsMenu.findViewById(R.id.v1)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(R.id.v2)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(R.id.v3)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(R.id.v4)).setTextColor(color);
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());

        ActionBar ab;
        File externalCacheDir;
        if (LegacyUtils.supportsExternalCacheDir && ((externalCacheDir = getExternalCacheDir()) != null)) deleteDir(externalCacheDir);

        deleteDir(getCacheDir());
        if(supportsActionBar && (ab = getActionBar()) != null) ab.hide();

        setContentView(R.layout.activity_main);

        // Fetch settings from SharedPreferences
        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);

        v1 = settings.getBoolean("v1", true);
        v2 = settings.getBoolean("v2", true);
        v3 = settings.getBoolean("v3", true);
        v4 = settings.getBoolean("v4", false);

        try {
            File debugKey = new File(getFilesDir(), "debug.keystore");
            if(!debugKey.exists()) FileUtils.copyFile(getAssets().open("debug23.keystore"), debugKey);
            String signingPath = settings.getString("signPath", null);
            if(TextUtils.isEmpty(signingPath) || !new File(signingPath).exists()) {
                keyPath = debugKey.getPath();
                password = "android";
            } else {
                keyPath = signingPath;
                password = settings.getString("password", "android");
            }
        } catch (IOException e) {
            showError(e);
        }

        fileUtil = new FileUtilsWrapper(this);

        setColor(settings.getInt("textColor", 0xffffffff), true, null);
        setColor(settings.getInt("backgroundColor", 0xff000000), false, null);

        ask = settings.getBoolean("ask", true);

        lang = settings.getString("lang", "en");
        if(Objects.equals(lang, Locale.getDefault().getLanguage())) rss = getResources();
        else updateLang(LocaleHelper.setLocale(MainActivity.this, lang).getResources(), null);

        ImageView settingsButton = findViewById(R.id.settingsButton);
        Button decodeButton = findViewById(R.id.decodeButton);
        ((LinearLayout) findViewById(R.id.topButtons)).setGravity(Gravity.CENTER_VERTICAL);

        settingsButton.setOnClickListener(v -> {
            ScrollView dialogView = (ScrollView) LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
            dialogView.setBackgroundColor(bgColor);

            setColor(textColor, true, dialogView);

            CompoundButton v1Toggle = dialogView.findViewById(R.id.v1);
            v1Toggle.setChecked(v1);
            v1Toggle.setOnCheckedChangeListener((buttonView, isChecked) -> v1 = isChecked);
            CompoundButton v2Toggle = dialogView.findViewById(R.id.v2);
            v2Toggle.setChecked(v2);
            v2Toggle.setOnCheckedChangeListener((buttonView, isChecked) -> v2 = isChecked);
            CompoundButton v3Toggle = dialogView.findViewById(R.id.v3);
            v3Toggle.setChecked(v3);
            v3Toggle.setOnCheckedChangeListener((buttonView, isChecked) -> v3 = isChecked);
            CompoundButton v4Toggle = dialogView.findViewById(R.id.v4);
            v4Toggle.setChecked(v4);
            v4Toggle.setOnCheckedChangeListener((buttonView, isChecked) -> v4 = isChecked);

            ((TextView) dialogView.findViewById(R.id.langPicker)).setText(rss.getString(R.string.lang));
            final boolean supportsSwitch = Build.VERSION.SDK_INT > 13;
            ((TextView) dialogView.findViewById(supportsSwitch ? R.id.ask : R.id.askText)).setText(rss.getString(R.string.ask));
            ((TextView) dialogView.findViewById(R.id.changeTextColor)).setText(rss.getString(R.string.change_text_color));
            ((TextView) dialogView.findViewById(R.id.changeBgColor)).setText(rss.getString(R.string.change_background_color));
            Button keyPicker = dialogView.findViewById(R.id.keyPicker);
            keyPicker.setText(rss.getString(R.string.select_key, new File(keyPath).getName()));
            keyPicker.setOnClickListener(view -> {
                File[] fs = getFilesDir().listFiles();
                ArrayList<File> keys = new ArrayList<>();
                for(File f : fs) if (!f.getName().endsWith(".txt")) keys.add(f);
                String[] names = new String[keys.size() + 1];
                names[0] = "Select new key";
                String curr = null;
                for(int i = 1; i < names.length; i++) {
                    File currentFile = keys.get(i - 1);
                    names[i] = currentFile.getName();
                    if(Objects.equals(currentFile.getPath(), keyPath)) curr = names[i];
                }
                TextView title = new TextView(this);
                title.setTextColor(textColor);
                title.setText(rss.getString(R.string.select));
                styleAlertDialog(new AlertDialog.Builder(this).setSingleChoiceItems(names, -1, (dialog, which) -> {
                    dialog.dismiss();
                    if(which == 0) {
                        if(doesNotSupportInbuiltAndroidFilePicker) {
                            DialogProperties properties = new DialogProperties();
                            properties.selection_mode = DialogConfigs.SINGLE_MODE;
                            properties.selection_type = DialogConfigs.FILE_SELECT;
                            properties.root = Environment.getExternalStorageDirectory();
                            properties.error_dir = Environment.getExternalStorageDirectory();
                            properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                            properties.extensions = new String[] {"keystore", "jks", "pkcs12", "p12", "bks", "pem", "ks", "key", "der"};
                            FilePickerDialog fp = new FilePickerDialog(this, properties, textColor, bgColor);
                            fp.setTitle(rss.getString(R.string.select));
                            fp.setDialogSelectionListener(files -> {
                                keyPath = files[0];
                                fp.dismiss();
                            });
                            runOnUiThread(fp::show);
                        } else startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .setType("*/*")
                                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                        .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                                                "application/x-java-keystore",
                                                "application/x-pkcs12",
                                                "application/x-pem-file",
                                                "application/x-x509-ca-cert",
                                                "application/pkix-cert",
                                                "application/x-iwork-keynote-sffkey",
                                                "application/x-bks-keystore",
                                                "application/octet-stream" // ehh
                                        })
                                , 111);
                    } else {
                        File selected = keys.get(which - 1);
                        keyPath = selected.getPath();
                        if(Objects.equals(selected.getName(), "debug.keystore")) password = "android";
                        else promptEnterPassword();
                    }
                }).create(), names, TextUtils.isEmpty(curr) ? "debug.keystore" : curr);
            });

            CompoundButton askSwitch = dialogView.findViewById(R.id.ask);
            if(doesNotSupportInbuiltAndroidFilePicker) {
                ask = false;
                askSwitch.setVisibility(View.GONE);
            }
            else {
                askSwitch.setChecked(ask);
                askSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    ask = isChecked;
                    if(!isChecked) checkStoragePerm();
                });
            }

            Button langPicker = dialogView.findViewById(R.id.langPicker);
            setButtonBorder(langPicker);
            langPicker.setOnClickListener(v2 -> {
                String[] langs = rss.getStringArray(R.array.langs);

                String[] display = rss.getStringArray(R.array.langs_display);

                String curr = null;
                for (int i = 0; i < display.length; i++) if (Objects.equals(langs[i], lang)) curr = display[i];

                styleAlertDialog(new AlertDialog.Builder(this).setSingleChoiceItems(display, -1, (dialog, which) -> {
                    updateLang(LocaleHelper.setLocale(MainActivity.this, lang = langs[which]).getResources(), dialogView);
                    dialog.dismiss();
                }).create(), display, TextUtils.isEmpty(curr) ? Locale.getDefault().getLanguage() : curr);
            });

            dialogView.findViewById(R.id.changeBgColor).setOnClickListener(v3 -> showColorPickerDialog(false, bgColor, dialogView));
            dialogView.findViewById(R.id.changeTextColor).setOnClickListener(v4 -> showColorPickerDialog(true, textColor, dialogView));
            TextView title = new TextView(this);
            title.setText(rss.getString(R.string.settings));
            title.setTextColor(textColor);
            title.setTextSize(25);
            styleAlertDialog(
                    new AlertDialog.Builder(this).setCustomTitle(title).setView(dialogView)
                            .setPositiveButton(rss.getString(R.string.close), (dialog, which) -> dialog.dismiss()).create(), null, null);
        });

        decodeButton.setOnClickListener(v -> {
            if(doesNotSupportInbuiltAndroidFilePicker) {
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.MULTI_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = Environment.getExternalStorageDirectory();
                properties.error_dir = Environment.getExternalStorageDirectory();
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = new String[] {"apk", "zip", "apks", "aspk", "apks", "xapk", "apkm"};
                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties, textColor, bgColor);
                dialog.setTitle(rss.getString(R.string.select));
                dialog.setDialogSelectionListener(files -> {
                    uris = new ArrayList<>();
                    multi = files.length > 1;
                    if(multi) for(String file : files) {
                        Uri uri = Uri.fromFile(new File(file));
                        uris.add(uri);
                    } else apkUri = Uri.fromFile(new File(files[0]));
                    dialog.dismiss();
                });
                runOnUiThread(dialog::show);
            }

            else MainActivity.this.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*")
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/vnd.android.package-archive", "application/octet-stream"})
                    , 1);
        } // XAPK is octet-stream
        );
        decodeButton.post(() -> {
            int buttonHeight = decodeButton.getHeight();
            int size = (int) (buttonHeight * 0.75);
            ViewGroup.LayoutParams params = settingsButton.getLayoutParams();
            params.height = size;
            params.width = size;
            settingsButton.setLayoutParams(params);
        });


        // Check if user shared or opened file with the app.
        final Intent openIntent = getIntent();
        final String action = openIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            apkUri = openIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            apkUri = openIntent.getData();
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && openIntent.hasExtra(Intent.EXTRA_STREAM)) {
            uris = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class) : openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                multi = true;
                apkUri = uris.get(0);
            }
        }
        if (apkUri != null) openFilePickerToSaveOrSaveNow();
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    private void sign(Uri inputUri, Uri outputUri) {
        if(signWrapper == null || !Objects.equals(signWrapper.key.getPath(), keyPath) || !Arrays.equals(signWrapper.pw, password.toCharArray())) signWrapper = new SignWrapper(keyPath, password);

        try {
            boolean cantWrite = doesNotHaveStoragePerm(this);
            File cacheDir = getCacheDir();
            File output = new File(cantWrite ? cacheDir + File.separator + "sign.apk" : fileUtil.getPath(outputUri));
            File input = new File(cantWrite ? cacheDir + getOriginalFileName(this, inputUri) : fileUtil.getPath(inputUri));
            if(cantWrite) fileUtil.copyFile(fileUtil.getInputStream(inputUri), input);
            try(ZipFile zf = new ZipFile(input)) {
                PackageInfo p;
                boolean isSplitApk = zf.getEntry("base.apk") != null
                        || ((p = getPackageManager().getPackageArchiveInfo(input.getPath(), 0)) != null && zf.getEntries(p.packageName).iterator().hasNext())
                        || zf.getEntries("dpi.apk").iterator().hasNext()
                        || zf.getEntries("x86.apk").iterator().hasNext()
                        || zf.getEntries("v8a.apk").iterator().hasNext()
                        || zf.getEntries("v7a.apk").iterator().hasNext();
                if(isSplitApk) {
                    Enumeration<ZipArchiveEntry> entries = zf.getEntries();
                    while(entries.hasMoreElements()) {
                        ZipArchiveEntry zae = entries.nextElement();
                        String name = zae.getName();
                        if (name.endsWith(".apk")) try (OutputStream os = fileUtil.getOutputStream(new File(cacheDir, name));
                             InputStream is = zf.getInputStream(zae)) {
                            fileUtil.copyFile(is, os);
                        }
                    }
                    for(File f : cacheDir.listFiles()) {
                        String fileName = f.getName();
                        if(fileName.endsWith(".apk") && !Objects.equals(f, input)) {
                            signWrapper.signApk(f, new File(cacheDir, fileName.replace(".apk", "_signed.apk")), v1, v2, v3, false);
                            f.delete();
                        }
                    }
                    try(ZipOutputStream zos = new ZipOutputStream(fileUtil.getOutputStream(outputUri))) {
                        for(File apk : cacheDir.listFiles()) {
                            String fileName = apk.getName();
                            if(fileName.endsWith(".apk")) {
                                zos.putNextEntry(new ZipEntry(fileName.replace("_signed.apk", ".apk")));
                                fileUtil.copyFile(apk, zos);
                            }
                        }
                    }
                } else {
                    zf.close();
                    signWrapper.signApk(input, output, v1, v2, v3, v4);

                    if(cantWrite) {
                        fileUtil.copyFile(output, fileUtil.getOutputStream(outputUri));
                        // fileUtil.copyFile(new File(output.getParentFile(), input.getName().replaceFirst("\\.(xapk|aspk|apk[sm]|apk)", ".idsig")), fileUtil.getOutputStream(outputUri));
                        // TODO: support v4
                        input.delete();
                        output.delete();
                    }
                }
            }

            showSuccess();
        } catch (Exception e) {
            if(Build.VERSION.SDK_INT > 30) showError(e);
            // When I tried signing with apksig in AVD with sdk 10 java.security is throwing some error saying something not found
            // Apparently 11 is the last version that supports v1 signing alone.
            boolean cantWrite = doesNotHaveStoragePerm(this);
            try (InputStream fis = FileUtils.getInputStream(new File(cantWrite ? getCacheDir() + getOriginalFileName(this, inputUri) : fileUtil.getPath(inputUri)))) {
                final String FILE_NAME_PAST = "testkey.past";
                final String FILE_NAME_PRIVATE_KEY = "testkey.pk8";
                File signingEnvironment = new File(getFilesDir(), "signing");
                File pastFile = new File(signingEnvironment, FILE_NAME_PAST);
                File privateKeyFile = new File(signingEnvironment, FILE_NAME_PRIVATE_KEY);

                if (!pastFile.exists() || !privateKeyFile.exists()) {
                    signingEnvironment.mkdir();
                    IOUtils.copyFileFromAssets(this, FILE_NAME_PAST, pastFile);
                    IOUtils.copyFileFromAssets(this, FILE_NAME_PRIVATE_KEY, privateKeyFile);
                }

                PseudoApkSigner.sign(fis, fileUtil.getOutputStream(outputUri), pastFile, privateKeyFile);
            } catch (Exception e2) {
                showError(e2);
            }
        }
    }

    public void styleAlertDialog(AlertDialog ad, String[] display, String highlight) {
        GradientDrawable border = new GradientDrawable();
        border.setColor(bgColor); // Background color
        border.setStroke(5, textColor); // Border width and color
        border.setCornerRadius(16);

        runOnUiThread(() -> {
            ad.show();

            if(supportsAsyncTask) {
                if(display != null) ad.getListView().setAdapter(new CustomArrayAdapter(this, display, textColor, highlight));
                Button positiveButton = ad.getButton(AlertDialog.BUTTON_POSITIVE);
                if(positiveButton != null) positiveButton.setTextColor(textColor);

                Button negativeButton = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
                if(negativeButton != null) negativeButton.setTextColor(textColor);

                Button neutralButton = ad.getButton(AlertDialog.BUTTON_NEUTRAL);
                if(neutralButton != null) neutralButton.setTextColor(textColor);
            }

            Window w = ad.getWindow();
            if (w != null) {
                View dv = w.getDecorView();
                dv.getBackground().setColorFilter(new LightingColorFilter(0xFF000000, bgColor));
                w.setBackgroundDrawable(border);

                int padding = 16;
                dv.setPadding(padding, padding, padding, padding);
            }
        });
    }

    public static Resources rss;

    private void updateLang(Resources res, ScrollView settingsDialog) {
        rss = res;
        Button decodeButton = findViewById(R.id.decodeButton);
        decodeButton.setText(res.getString(R.string.merge));
        setButtonBorder(decodeButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            settingsButton.setContentDescription(res.getString(R.string.settings));
        }
        decodeButton.post(() -> {
            int buttonHeight = decodeButton.getHeight();
            int size = (int) (buttonHeight * 0.75);
            ViewGroup.LayoutParams params = settingsButton.getLayoutParams();
            params.height = size;
            params.width = size;
            settingsButton.setLayoutParams(params);
        });
        ((LinearLayout) findViewById(R.id.topButtons)).setGravity(Gravity.CENTER_VERTICAL);

        if(settingsDialog != null) {
            ((TextView) settingsDialog.findViewById(R.id.langPicker)).setText(res.getString(R.string.lang));
            final boolean supportsSwitch = Build.VERSION.SDK_INT > 13;
            ((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.ask : R.id.askText)).setText(res.getString(R.string.ask));
            ((TextView) settingsDialog.findViewById(R.id.changeTextColor)).setText(res.getString(R.string.change_text_color));
            ((TextView) settingsDialog.findViewById(R.id.changeBgColor)).setText(res.getString(R.string.change_background_color));
            ((TextView) settingsDialog.findViewById(R.id.changeBgColor)).setText(res.getString(R.string.change_background_color));
        }
    }

    private void showColorPickerDialog(boolean isTextColor, int currentColor, ScrollView from) {
        new AmbilWarnaDialog(this, currentColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog1, int color) {
                setColor(color, isTextColor, from);
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog1) {
                // cancel was selected by the user
            }
        }).show();
    }

    final File getAppFolder() {
        final File appFolder = new File(Environment.getExternalStorageDirectory(), "APK Signer");
        return appFolder.exists() || appFolder.mkdir() ? appFolder : new File(Environment.getExternalStorageDirectory(), "Download");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkStoragePerm() {
        if(doesNotHaveStoragePerm(this)) {
            Toast.makeText(this, rss.getString(R.string.grant_storage), Toast.LENGTH_LONG).show();
            if(LegacyUtils.supportsWriteExternalStorage) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                fileUtil = new FileUtilsWrapper(this);
            }
            else startActivityForResult(new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    public static boolean doesNotHaveStoragePerm(Context context) {
        if (Build.VERSION.SDK_INT < 23) return false;
        return LegacyUtils.supportsWriteExternalStorage ?
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED :
            !Environment.isExternalStorageManager();
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor e = getSharedPreferences("set", Context.MODE_PRIVATE).edit()
                .putBoolean("ask", ask)
                .putBoolean("v1", v1)
                .putBoolean("v2", v2)
                .putBoolean("v3", v3)
                .putBoolean("v4", v4)
                .putInt("textColor", textColor)
                .putInt("backgroundColor", bgColor)
                .putString("lang", lang)
                .putString("password", password)
                .putString("signPath", keyPath);
        if (supportsArraysCopyOfAndDownloadManager) e.apply();
        else e.commit();

        super.onPause();
    }

    private Handler handler;

    /** @noinspection ResultOfMethodCallIgnored, DataFlowIssue */
    public static void deleteDir(File dir) {
        // There should never be folders in here.
        for (String child : dir.list()) new File(dir, child).delete();
    }

    @Override
    protected void onDestroy() {
        File dir = getCacheDir();
        deleteDir(dir);
        if (LegacyUtils.supportsExternalCacheDir && (dir = getExternalCacheDir()) != null) deleteDir(dir);
        super.onDestroy();
    }

    public static void toggleAnimation(MainActivity context, boolean on) {
        ImageView loadingImage = context.findViewById(R.id.loadingImage);
        context.runOnUiThread(() -> {
            if(on) {
                ((LinearLayout) context.findViewById(R.id.wrapImg)).setGravity(Gravity.CENTER);
                loadingImage.setVisibility(View.VISIBLE);
                loadingImage.startAnimation(AnimationUtils.loadAnimation(context, R.anim.loading));
            }
            else {
                loadingImage.setVisibility(View.GONE);
                loadingImage.clearAnimation();
            }
        });
    }

    private void processOneApkUri(Uri uri) {
        apkUri = uri;
        openFilePickerToSaveOrSaveNow();
    }

    private void promptEnterPassword() {
        String originalKeyPath = keyPath;
        EditText input = new EditText(this);
        input.setTextColor(textColor);
        String titleText = "Enter keystore password";
        input.setHint(titleText);
        TextView title = new TextView(this);
        title.setTextColor(textColor);
        title.setText(titleText);
        styleAlertDialog(new AlertDialog.Builder(this).setCustomTitle(title).setView(input).setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            String inputString = input.getText().toString();
            if(TextUtils.isEmpty(inputString)) showError("No password input");
            else password = inputString;
        }).setNegativeButton(rss.getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
            keyPath = originalKeyPath;
            Toast.makeText(this, "Restored last signature " + new File(keyPath).getName(), Toast.LENGTH_LONG).show();
        }).create(), null, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) switch (requestCode) {
            case 111:
                Uri keyUri = data.getData();
                try {
                    fileUtil.copyFile(fileUtil.getInputStream(keyUri), new File(keyPath = getFilesDir() + File.separator + getOriginalFileName(this, keyUri)));
                    promptEnterPassword();
                } catch (IOException e) {
                    showError(e);
                }
                break;
            case 0:
                checkStoragePerm();
                fileUtil = new FileUtilsWrapper(this);
                break;
            case 1:
                // opened through button in the app
                ClipData clipData = data.getClipData();
                if (clipData == null) processOneApkUri(data.getData());
                else {
                    //multiple files selected
                    multi = true;
                    uris = new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        uris.add(uri);
                    }
                    processOneApkUri(uris.get(0));
                }
                break;
            case 2:
                // going to process and save a file now
                new SignTask(this).execute(apkUri, data.getData());
            break;
        }
    }


    private void showSuccess() {
        if(!errorOccurred) runOnUiThread(() -> Toast.makeText(this, rss.getString(R.string.success_saved), Toast.LENGTH_SHORT).show());
    }

    private void copyText(CharSequence text) {
        if(supportsActionBar) ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("log", text));
        else ((android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(text);
        Toast.makeText(this, rss.getString(R.string.copied_log), Toast.LENGTH_SHORT).show();
    }

    private void showError(Exception e) {
        if(!(e instanceof ClosedByInterruptException)) {
            final String mainErr = e.toString();
            errorOccurred = !mainErr.equals(rss.getString(R.string.sign_failed));
            toggleAnimation(this, false);
            StringBuilder stackTrace = new StringBuilder().append(mainErr).append('\n');
            for(StackTraceElement line : e.getStackTrace()) stackTrace.append(line).append('\n');
            AlertDialog.Builder b = new AlertDialog.Builder(this)
                    .setNegativeButton(rss.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(rss.getString(R.string.create_issue), (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AbdurazaaqMohammed/APK-Signer/issues/new?title=Crash%20Report&body=" + stackTrace))))
                    .setNeutralButton(rss.getString(R.string.copy_log), (dialog, which) -> copyText(stackTrace));
            runOnUiThread(() -> {
                TextView title = new TextView( this);
                title.setText(mainErr);
                title.setTextColor(textColor);
                title.setTextSize(20);

                TextView msg = new TextView(this);
                msg.setText(stackTrace);
                msg.setTextColor(textColor);
                ScrollView sv = new ScrollView(this);
                sv.setBackgroundColor(bgColor);
                msg.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (rss.getDisplayMetrics().heightPixels * 0.6)));
                sv.addView(msg);
                styleAlertDialog(b.setCustomTitle(title).setView(sv).create(), null, null);
            });
        }
    }

    private void showError(String err) {
        toggleAnimation(this, false);
        runOnUiThread(() -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
    }

    public static String getOriginalFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (Objects.equals(uri.getScheme(), "content")) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = Objects.requireNonNull(result).lastIndexOf('/'); // Ensure it throw the NullPointerException here to be caught
                if (cut != -1) result = result.substring(cut + 1);
            }
            return result.replaceFirst("\\.(xapk|aspk|apk[sm]|apk)", "_signed.$1");
        } catch (Exception ignored) {
            return "filename_not_found";
        }
    }

    @SuppressLint("InlinedApi")
    private void openFilePickerToSaveOrSaveNow() {
        if (ask) startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(getContentResolver().getType(apkUri))
                .putExtra(Intent.EXTRA_TITLE, getOriginalFileName(this, apkUri)), 2);
        else {
            checkStoragePerm();
            try {
                String originalFilePath;
                originalFilePath = fileUtil.getPath(apkUri);
                File f;
                String newFilePath = TextUtils.isEmpty(originalFilePath) ?
                        getAppFolder() + File.separator + getOriginalFileName(this, apkUri) : originalFilePath.replaceFirst("\\.(xapk|aspk|apk[sm]|apk)", "_signed.$1");
                if(TextUtils.isEmpty(newFilePath) ||
                        newFilePath.startsWith("/data/")
                       // || !(f = new File(newFilePath)).createNewFile() || f.canWrite()
                        ) {
                    f = new File(getAppFolder(), newFilePath.substring(newFilePath.lastIndexOf(File.separator) + 1));
                    showError(rss.getString(R.string.no_filepath) + newFilePath);
                } else f = new File(newFilePath);
                Uri outputUri = Uri.fromFile(f);
                if(supportsAsyncTask) new SignTask(this).execute(apkUri, outputUri);
                else sign(apkUri, outputUri);
            } catch (IOException e) {
                showError(e);
            }
        }
    }

    private static SignWrapper signWrapper = null;

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private static class SignTask extends AsyncTask<Uri, Void, Void> {

        WeakReference<MainActivity> activityReference;
        public SignTask(MainActivity context) {
            this.activityReference = new WeakReference<>(context);
            toggleAnimation(context, true);
        }
        @Override
        protected Void doInBackground(Uri... uris) {
            MainActivity activity = activityReference.get();
            activity.sign(uris[0], uris[1]);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            MainActivity activity = activityReference.get();
            toggleAnimation(activity, false);
            if(activity.multi) activity.getHandler().post(() -> {
                try {
                    activity.uris.remove(0);
                    activity.apkUri = activity.uris.get(0);
                    activity.openFilePickerToSaveOrSaveNow();
                } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                    // End of list, I don't know why but isEmpty is not working
                    activity.showSuccess();
                }
            });
            else activity.showSuccess();
        }
    }
}