package org.mariotaku.pass.util.passgen;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.mariotaku.pass.util.passgen.impl.HotpPinImpl;
import org.mariotaku.pass.util.passgen.impl.PasswordComposerImpl;
import org.mariotaku.pass.util.passgen.impl.SuperGenPassImpl;

import java.security.GeneralSecurityException;

/**
 * Created by mariotaku on 15/11/1.
 */
public abstract class PassGen {

    protected abstract String generateChecked(@NonNull String pass, @NonNull String domain, int length) throws PassGenException;

    public String generate(String pass, String domain, int length) throws PassGenException {
        if (TextUtils.isEmpty(pass) || TextUtils.isEmpty(domain)) {
            throw new IllegalArgumentException("Password and domain must not be null or empty");
        }
        return generateChecked(pass, domain, length);
    }

    public static PassGen getInstance(String provider) {
        switch (provider) {
            case "sgp_md5":
                return new SuperGenPassImpl("MD5");
            case "sgp_sha512":
                return new SuperGenPassImpl("SHA512");
            case "pw_composer":
                return new PasswordComposerImpl();
            case "hotp_pin":
                return new HotpPinImpl();
        }
        throw new UnsupportedOperationException("Unsupported provider " + provider);
    }

    public static class PassGenException extends GeneralSecurityException {

        public PassGenException(final String message) {
            super(message);
        }
    }
}
