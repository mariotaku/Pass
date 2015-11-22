package org.mariotaku.pass.util;

import android.content.Context;
import android.util.Pair;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by mariotaku on 15/10/30.
 */
public class MasterPasswordEncrypter {

    public MasterPasswordEncrypter() {
    }


    public byte[] decrypt(byte[] in, byte[] key, byte[] iv) throws InternalErrorException, WrongPasswordException {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(getRawKey(key), "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(in);
        } catch (BadPaddingException e) {
            throw new WrongPasswordException(e);
        } catch (GeneralSecurityException e) {
            throw new InternalErrorException(e);
        }
    }

    public Pair<byte[], byte[]> encrypt(byte[] in, byte[] key) throws InternalErrorException {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(getRawKey(key), "AES"));
            return Pair.create(cipher.doFinal(in), cipher.getIV());
        } catch (GeneralSecurityException e) {
            throw new InternalErrorException(e);
        }
    }


    /**
     * Get 256-bit encryption key
     * <p/>
     * From http://blog.csdn.net/zhaokaiqiang1992/article/details/41142883
     *
     * @param seed
     * @return
     * @throws Exception
     */
    private static byte[] getRawKey(byte[] seed) throws NoSuchAlgorithmException, NoSuchProviderException {
        final KeyGenerator kgen = KeyGenerator.getInstance("AES");
        final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
        sr.setSeed(seed);
        // 256 bits or 128 bits,192bits
        kgen.init(256, sr);
        final SecretKey key = kgen.generateKey();
        return key.getEncoded();
    }

    public static class InternalErrorException extends GeneralSecurityException {

        public InternalErrorException(final Exception e) {
            super(e);
        }
    }

    public static class WrongPasswordException extends GeneralSecurityException {

        public WrongPasswordException(final Exception e) {
            super(e);
        }
    }
}
