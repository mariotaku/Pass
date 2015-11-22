package org.mariotaku.pass.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.mariotaku.pass.BuildConfig;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by mariotaku on 15/11/2.
 */
@TargetApi(Build.VERSION_CODES.M)
public class FingerprintCryptoHelper {

    public static final String KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    public static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    public static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;

    public static final String SECURITY_KEY_NAME = BuildConfig.APPLICATION_ID + ".security_key";

    public static KeyStore getKeystore() {
        try {
            return KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cipher getCipher() {
        try {
            return Cipher.getInstance(KEY_ALGORITHM + "/" + BLOCK_MODE + "/" + ENCRYPTION_PADDING);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyGenerator getKeyGenerator() {
        try {
            return KeyGenerator.getInstance(KEY_ALGORITHM, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyGenParameterSpec createKeyGenParameterSpec() {
        return new KeyGenParameterSpec.Builder(SECURITY_KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                        // Require the user to authenticate with a fingerprint to authorize every use
                        // of the key
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .build();
    }

}
