package org.mariotaku.pass.util.passgen.impl;

import android.support.annotation.NonNull;

import org.mariotaku.pass.util.passgen.PassGen;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by mariotaku on 15/11/1.
 */
public class PasswordComposerImpl extends PassGen {

    private final MessageDigest md5;
    private final boolean specialChar;

    public PasswordComposerImpl(boolean specialChar) {
        this.specialChar = specialChar;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String generateChecked(@NonNull final String pass, @NonNull final String domain, final int length) throws PassGenException {
        if (length < 1 || length > 31) {
            throw new IllegalArgumentException("Password length must between 1 and 31 inclusive.");
        }
        //noinspection RedundantStringConstructorCall
        final String md5str = toHexString(md5.digest(new String(pass + ":" + domain).getBytes()));
        if (specialChar) {
            return md5str.substring(0, length - 1) + ".";
        }
        return md5str.substring(0, length);
    }

    private String toHexString(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
