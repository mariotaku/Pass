package org.mariotaku.pass.util.passgen.impl;

import android.support.annotation.NonNull;
import android.util.Base64;

import org.mariotaku.pass.util.passgen.PassGen;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by mariotaku on 15/11/1.
 */
public class SuperGenPassImpl extends PassGen {

    private static final int ROUNDS_REQUIREMENT = 10;
    private final MessageDigest digest;
    private final boolean specialChar;

    public SuperGenPassImpl(String algorithm, boolean specialChar) {
        this.specialChar = specialChar;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String generateChecked(@NonNull final String pass, @NonNull final String domain, final int length) throws PassGenException {
        if (length < 4 || length > 24)
            throw new IllegalArgumentException("Password length must between 4 and 24 inclusive");

        String generated = pass + ":" + domain;

        for (int i = 0; i < ROUNDS_REQUIREMENT; i++) {
            generated = hash(generated.getBytes());
        }
        while (!validate(generated, length)) {
            generated = hash(generated.getBytes());
        }
        return generated.substring(0, length);
    }

    /* From http://supergenpass.com/about/#PasswordComplexity :
     * * Consist of alphanumerics (A-Z, a-z, 0-9)
     * * Always start with a lowercase letter of the alphabet
     * * Always contain at least one uppercase letter of the alphabet
     * * Always contain at least one numeral
     * * Can be any length from 4 to 24 characters (default: 10)
     */
    private boolean validate(final String generated, final int length) {
        final CharSequence pw = generated.subSequence(0, length);
        final char firstCh = pw.charAt(0);
        if (firstCh < 'a' || firstCh > 'z') return false;
        boolean hasUppercase = false, hasNumeral = false, hasSpecial = false;
        for (int i = 1; i < length; i++) {
            final char ch = pw.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                hasUppercase = true;
            } else if (ch >= '0' && ch <= '9') {
                hasNumeral = true;
            } else if (ch == '=' || ch == '/' || ch == '+') {
                hasSpecial = true;
            }
        }
        return hasUppercase && hasNumeral && (!specialChar || hasSpecial);
    }

    @NonNull
    private String hash(final byte[] bytes) {
        String hashed = Base64.encodeToString(digest.digest(bytes), Base64.NO_WRAP);
        if (specialChar) return hashed;
        return hashed.replace('=', 'A').replace('/', '8').replace('+', '9');
    }


}
