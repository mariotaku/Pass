package org.mariotaku.pass.util.passgen.impl;

import android.support.annotation.NonNull;
import android.util.Base64;

import org.mariotaku.pass.util.passgen.PassGen;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * Created by mariotaku on 15/11/1.
 */
public class SuperGenPassImpl extends PassGen {

    /*   from http://supergenpass.com/about/#PasswordComplexity :
            *  Consist of alphanumerics (A-Z, a-z, 0-9)
            * Always start with a lowercase letter of the alphabet
            * Always contain at least one uppercase letter of the alphabet
            * Always contain at least one numeral
            * Can be any length from 4 to 24 characters (default: 10)
     */

    // regex looks for:
    // "lcletter stuff Uppercase stuff Number stuff" or
    // "lcletter stuff Number stuff Uppercase stuff"
    // which should satisfy the above requirements.
    private static final Pattern PATTERN_VALID_PASSWORD =
            Pattern.compile("^[a-z][a-zA-Z0-9]*(?:(?:[A-Z][a-zA-Z0-9]*[0-9])|(?:[0-9][a-zA-Z0-9]*[A-Z]))[a-zA-Z0-9]*$");

    private static final int ROUNDS_REQUIREMENT = 10;
    private final MessageDigest digest;

    public SuperGenPassImpl(String algorithm) {
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

    private boolean validate(final String generated, final int length) {
        return PATTERN_VALID_PASSWORD.matcher(generated.subSequence(0, length)).matches();
    }

    @NonNull
    private String hash(final byte[] bytes) {
        String hashed = Base64.encodeToString(digest.digest(bytes), Base64.NO_WRAP);
        return hashed.replace('=', 'A').replace('/', '8').replace('+', '9');
    }


}
