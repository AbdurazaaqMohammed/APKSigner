package io.github.abdurazaaqmohammed.apksigner;

import com.android.apksig.ApkSigner;
import com.starry.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Objects;

public class SignWrapper {

    public void setKey(File key) {
        this.key = key;
    }

    File key;
    char[] pw;
    boolean v1;
    boolean v2;
    boolean v3;
    boolean v4;
    public SignWrapper(String signPath, String password, boolean v1Enabled, boolean v2Enabled, boolean v3Enabled, boolean v4Enabled) {
        this.key = new File(signPath);
        this.pw = password.toCharArray();
        this.v1 = v1Enabled;
        this.v2 = v2Enabled;
        this.v3 = v3Enabled;
        this.v4 = v4Enabled;
    }

    public SignWrapper(String signPath, String password) {
        this(signPath, password, true, true, true, false);
    }

    public void signApk(File inputApk, File output, boolean v1, boolean v2, boolean v3, boolean v4) throws Exception {
        KeyStore keystore = null;
        String[] types = {"JKS", "PKCS12", "BKS"};
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            try (InputStream fis = FileUtils.getInputStream(key)) {
                keystore = KeyStore.getInstance(type);
                keystore.load(fis, pw);
            } catch (Exception e) {
                if(i == types.length - 1) throw (e);
            }
        }
        String alias = keystore.aliases().nextElement();

        ApkSigner.Builder b = new ApkSigner.Builder(Collections.singletonList(new ApkSigner.SignerConfig.Builder("CERT",
                ((KeyStore.PrivateKeyEntry) keystore.getEntry(alias, new KeyStore.PasswordProtection(pw))).getPrivateKey(),
                Collections.singletonList((X509Certificate) keystore.getCertificate(alias))).build()))
                .setInputApk(inputApk)
                .setOutputApk(output)
                .setCreatedBy("Android Gradle 8.0.2")
                .setV1SigningEnabled(v1)
                .setV2SigningEnabled(v2)
                .setV3SigningEnabled(v3)
                .setV4SigningEnabled(v4);
                if(v4) {
                    String fileName = inputApk.getName();
                    String formattedName = fileName.replaceFirst("\\.(xapk|aspk|apk[sm]|apk)", ".idsig");
                    int lastDotIndex;
                    b.setV4SignatureOutputFile(new File(output.getParentFile(), Objects.equals(fileName, formattedName) ?  (lastDotIndex = fileName.lastIndexOf('.')) == -1 ?
                            fileName + "_signed" : fileName.substring(0, lastDotIndex) + "_signed." + fileName.substring(lastDotIndex + 1) : formattedName));
                }
                b.build().sign();
    }

    public void signApk(File inputApk, File output) throws Exception {
        signApk(inputApk, output, v1, v2, v3, v4);
    }
}
