package org.mariotaku.pass;

/**
 * Created by mariotaku on 15/10/29.
 */
public interface Constants {

    String LOGTAG = "Pass";

    String SHARED_PREFERENCES_NAME = "preferences";
    String KEY_PATTERN_PASSWORD = "pattern_password";
    String KEY_PATTERN_IV = "pattern_iv";
    String KEY_FINGERPRINT_PASSWORD = "fingerprint_password";
    String KEY_FINGERPRINT_IV = "fingerprint_iv";
    String KEY_PASSWORD_SALT = "password_salt";
    String KEY_PASSWORD_LENGTH = "password_length";
    String KEY_PIN_LENGTH = "pin_length";
    String KEY_PASSWORD_GENERATOR_TYPE = "password_generator_type";
    String KEY_PASSWORD_RETRY_COUNT = "password_retry_count";
    String KEY_CLEAR_COPIED_PASSWORD_AUTOMATICALLY = "clear_copied_password_automatically";
    String KEY_PASSWORD_INCLUDE_SPECIAL_CHARACTERS = "password_include_special_characters";

    String KEY_COPIED_PASSWORD_VALIDITY = "copied_password_validity";
    String ACTION_TAG_DISCOVERED = BuildConfig.APPLICATION_ID + ".TAG_DISCOVERED";

    String ACTION_PASSWORD_CALLBACK = BuildConfig.APPLICATION_ID + ".PASSWORD_CALLBACK";
    String EXTRA_RESID = "resid";

    String EXTRA_ACCESSIBILITY_EXTRA = "accessibility_extra";

    int REQUEST_REQUEST_PERMISSION = 101;
    int NOTIFICATION_ID_CLIPBOARD = 100;
    int NOTIFICATION_ID_ACCESSIBILITY = 101;
}
