package org.mariotaku.pass.util.passgen.impl;

import android.support.annotation.NonNull;

import org.mariotaku.pass.util.passgen.PassGen;
import org.openauthentication.otp.OneTimePasswordAlgorithm;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * <p>
 * This generates strong Personal Identification Numbers (PINs).
 * </p>
 * <p/>
 * <p>
 * PINs generated with this can be used for bank accounts, phone lock screens, ATMs, etc. The
 * generator avoids common bad PINs ("1234", "0000", "0007", etc.) detected using a variety of
 * techniques.
 * </p>
 * <p>
 * The generation algorithm is a modified version of <a
 * href="http://tools.ietf.org/html/rfc4226">HOTP</a> which uses the master password for the HMAC
 * secret and the domain instead of the moving factor. If a bad PIN is detected, the text " 1" is
 * added to the end of the domain and it's recomputed. If a bad PIN is still generated, it suffixes
 * " 2" instead and will continue in this way until a good PIN comes out.
 * </p>
 *
 * @author <a href="mailto:steve@staticfree.info">Steve Pomeroy</a>
 * @see OneTimePasswordAlgorithm#generateOTPFromText(byte[], byte[], int, boolean, int)
 */
public class HotpPinImpl extends PassGen {

    @Override
    public String generateChecked(@NonNull String pass, @NonNull String domain, int length)
            throws PassGenException {

        if (length < 3 || length > 8) {
            throw new IllegalArgumentException("length must be >= 3 and <= 8");
        }

        try {
            String pin = OneTimePasswordAlgorithm.generateOTPFromText(pass.getBytes(),
                    domain.getBytes(), length, false, -1);

            if (pin.length() != length) {
                throw new PassGenException("PIN generator error; requested length "
                        + length + ", but got " + pin.length());
            }

            int suffix = 0;
            int loopOverrun = 0;

            while (isBadPin(pin)) {
                final String suffixedDomain = domain + " " + suffix;
                pin = OneTimePasswordAlgorithm.generateOTPFromText(pass.getBytes(),
                        suffixedDomain.getBytes(), length, false, -1);

                loopOverrun++;
                suffix++;
                if (loopOverrun > 100) {
                    throw new PassGenException("PIN generator programming error: looped too many times");
                }
            }
            return pin;
        } catch (final InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests the string to see if it contains a numeric run. For example, "123456", "0000", "9876",
     * and "2468" would all match.
     *
     * @param pin
     * @return true if the string is a numeric run
     */
    public boolean isNumericalRun(String pin) {
        final int len = pin.length();
        // int[] diff = new int[len - 1];
        int prevDigit = Character.digit(pin.charAt(0), 10);
        int prevDiff = Integer.MAX_VALUE;
        boolean isRun = true; // assume it's true...

        for (int i = 1; isRun && i < len; i++) {
            final int digit = Character.digit(pin.charAt(i), 10);

            final int diff = digit - prevDigit;
            if (prevDiff != Integer.MAX_VALUE && diff != prevDiff) {
                isRun = false; // ... and prove it's false
            }

            prevDiff = diff;
            prevDigit = digit;
        }

        return isRun;
    }

    /**
     * Tests the string to see if it contains a partial numeric run. Eg. 3000, 5553
     *
     * @param pin
     * @return
     */
    public boolean isIncompleteNumericalRun(String pin) {
        final int len = pin.length();
        int consecutive = 0;
        char last = pin.charAt(0);
        for (int i = 1; i < len; i++) {
            final char c = pin.charAt(i);
            if (last == c) {
                consecutive++;
            } else {
                consecutive = 0;
            }
            last = c;
            if (consecutive >= 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is a hard-coded list of specific PINs that have cultural meaning. While they may be
     * improbable, none the less they won't output from the generation.
     */
    private static final String[] BLACKLISTED_PINS = new String[]{"90210",
            "8675309" /* Jenny */,
            "1004" /* 10-4 */,
            // in this document http://www.datagenetics.com/blog/september32012/index.html
            // these were shown to be the least commonly used. Now they won't be used at all.
            "8068", "8093", "9629", "6835", "7637", "0738", "8398", "6793", "9480", "8957", "0859",
            "7394", "6827", "6093", "7063", "8196", "9539", "0439", "8438", "9047", "8557"};

    /**
     * Tests to see if the PIN is a "bad" pin. That is, one that is easily guessable. Essentially,
     * this is a blacklist of the most commonly used PINs like "1234", "0000" and "1984".
     *
     * @param pin
     * @return true if the PIN matches the bad PIN criteria
     */
    public boolean isBadPin(String pin) {
        final int len = pin.length();

        // special cases for 4-digit PINs (which are quite common)
        if (len == 4) {
            final int start = Integer.parseInt(pin.subSequence(0, 2).toString());
            final int end = Integer.parseInt(pin.subSequence(2, 4).toString());

            // 19xx pins look like years, so might as well ditch them.
            if (start == 19 || (start == 20 && end < 30)) {
                return true;
            }

            // 1515
            if (start == end) {
                return true;
            }
        }

        // find case where all digits are in pairs
        // eg 1122 3300447722

        if (len % 2 == 0) {
            boolean paired = true;
            for (int i = 0; i < len - 1; i += 2) {
                if (pin.charAt(i) != pin.charAt(i + 1)) {
                    paired = false;
                }
            }
            if (paired) {
                return true;
            }
        }

        if (isNumericalRun(pin)) {
            return true;
        }

        if (isIncompleteNumericalRun(pin)) {
            return true;
        }

        // filter out special numbers
        for (final String blacklisted : BLACKLISTED_PINS) {
            if (blacklisted.equals(pin)) {
                return true;
            }
        }

        return false;
    }
}
