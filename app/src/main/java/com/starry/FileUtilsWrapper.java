package com.starry;

import static io.github.abdurazaaqmohammed.apksigner.MainActivity.doesNotHaveStoragePerm;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import io.github.abdurazaaqmohammed.apksigner.LegacyUtils;
import io.github.abdurazaaqmohammed.apksigner.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class FileUtilsWrapper {

    private final Context context;

    public FileUtilsWrapper(Context context) {
        this.context = context;
    }

    public OutputStream getOutputStream(String filepath) throws IOException {
        return getOutputStream(new File(filepath));
    }

    public OutputStream getOutputStream(File file) throws IOException {
        return LegacyUtils.supportsFileChannel ?
                Files.newOutputStream(file.toPath(), java.nio.file.StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                : new FileOutputStream(file);
    }

    public void copyFile(File sourceFile, File destinationFile) throws IOException {
        try (InputStream is = getInputStream(sourceFile);
             OutputStream os = getOutputStream(destinationFile)) {
            copyFile(is, os);
        }
    }

    public void copyFile(File in, OutputStream os) throws IOException {
        try(InputStream is = getInputStream(in)) {
            copyFile(is, os);
        }
    }

    public void copyFile(InputStream is, File destinationFile) throws IOException {
        try (OutputStream os = getOutputStream(destinationFile)) {
            copyFile(is, os);
        }
    }

    public void copyFile(InputStream is, OutputStream os) throws IOException {
        if(LegacyUtils.supportsWriteExternalStorage) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
        } else android.os.FileUtils.copy(is, os);
    }

    public OutputStream getOutputStream(Uri uri) throws IOException {
        if(doesNotHaveStoragePerm(context))
            return context.getContentResolver().openOutputStream(uri);
        String filePath = getPath(uri);
        File file = filePath == null ? null : new File(filePath);
        return file != null && file.canWrite() ? getOutputStream(file) : context.getContentResolver().openOutputStream(uri);
    }

    public InputStream getInputStream(File file) throws IOException {
        return LegacyUtils.supportsFileChannel ?
                Files.newInputStream(file.toPath(), StandardOpenOption.READ)
                : new FileInputStream(file);
    }

    public InputStream getInputStream(String filePath) throws IOException {
        return getInputStream(new File(filePath));
    }

    public InputStream getInputStream(Uri uri) throws IOException {
        if(doesNotHaveStoragePerm(context)) return context.getContentResolver().openInputStream(uri);
        String filePath = getPath(uri);
        File file = filePath == null ? null : new File(filePath);
        return file != null && file.canRead() ? getInputStream(file) : context.getContentResolver().openInputStream(uri);
    }

    private boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    private String getPathFromExtSD(String[] pathData) {
        String type = pathData[0];
        String relativePath = File.separator + pathData[1];
        String fullPath;

        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory().toString() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        if ("home".equalsIgnoreCase(type)) {
            fullPath = "/storage/emulated/0/Documents" + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
        return fileExists(fullPath) ? fullPath : null;
    }

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @SuppressLint("NewApi")
    public String getPath(Uri uri) throws IOException {
        String selection;
        String[] selectionArgs;

        if (isExternalStorageDocument(uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String fullPath = getPathFromExtSD(split);
            if (fullPath == null || !fileExists(fullPath)) {
                fullPath = copyFileToInternalStorageAndGetPath(uri);
            }
            return TextUtils.isEmpty(fullPath) ? null : fullPath;
        }

        if (isDownloadsDocument(uri)) {
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String fileName = cursor.getString(0);
                    String path = Environment.getExternalStorageDirectory() + "/Download/" + fileName;
                    if (!TextUtils.isEmpty(path)) {
                        return path;
                    }
                }
            }

            String id = DocumentsContract.getDocumentId(uri);
            if (!TextUtils.isEmpty(id)) {
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                }

                String[] contentUriPrefixesToTry = {
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                };

                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    try {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));
                        return getDataColumn(contentUri, null, null);
                    } catch (NumberFormatException e) {
                        return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                    }
                }
            }
        }

        if (isMediaDocument(uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            } else if ("document".equals(type)) {
                contentUri = MediaStore.Files.getContentUri(MediaStore.getVolumeName(uri));
            }

            selection = "_id=?";
            selectionArgs = new String[]{split[1]};
            return getDataColumn(contentUri, selection, selectionArgs);
        }

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return isGooglePhotosUri(uri) ?
                    uri.getLastPathSegment() :
                    doesNotHaveStoragePerm(context) ?
                            copyFileToInternalStorageAndGetPath(uri) :
                            getDataColumn(uri, null, null);
        }

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return copyFileToInternalStorageAndGetPath(uri);
    }

    public String copyFileToInternalStorageAndGetPath(Uri uri) throws IOException {
        return copyFileToInternalStorage(uri).getPath();
    }

    public File copyFileToInternalStorage(Uri uri) throws IOException {
        File output = new File(context.getCacheDir(), MainActivity.getOriginalFileName(context, uri));
        if(output.exists() && output.length() > 999) return output;
        try (OutputStream outputStream = getOutputStream(output); InputStream cursor = context.getContentResolver().openInputStream(uri)) {
            int read;
            byte[] buffers = new byte[1024];
            while ((read = cursor.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
        }
        return output;
    }

    private String getDataColumn(Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
}
