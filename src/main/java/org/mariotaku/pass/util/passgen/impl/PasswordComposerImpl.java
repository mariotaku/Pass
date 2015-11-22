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

    public PasswordComposerImpl() {
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
        return toHexString(md5.digest(new String(pass + ":" + domain).getBytes())).substring(0, length);
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
