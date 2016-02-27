package org.mariotaku.pass.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.mariotaku.pass.BuildConfig;
import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.model.AccessibilityExtra;
import org.mariotaku.pass.provider.PassProvider.PassDataStore.AppWindows;
import org.mariotaku.pass.provider.PassProvider.PassDataStore.NfcTags;
import org.mariotaku.pass.provider.PassProvider.PassDataStore.RecentDomains;
import org.mariotaku.pass.service.ClipboardRestoreService;
import org.mariotaku.pass.util.FingerprintCryptoHelper;
import org.mariotaku.pass.util.FingerprintHelper;
import org.mariotaku.pass.util.Utils;
import org.mariotaku.pass.util.passgen.PassGen;
import org.mariotaku.pass.view.PasswordContainer;
import org.mariotaku.sqliteqb.library.Columns;
import org.mariotaku.sqliteqb.library.Expression;
import org.mariotaku.sqliteqb.library.OrderBy;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;

import okio.ByteString;

/**
 * Created by mariotaku on 15/10/29.
 */
public class PassGenDialogActivity extends Activity implements Constants, View.OnClickListener {

    private PassGen mPassGen, mPinGen;
    private FingerprintHelper mFingerprintHelper;
    private Handler mHandler;
    private SharedPreferences mPreferences;
    private PasswordContainer mPasswordContainer;
    private Spinner mDomainSelector;
    private AutoCompleteTextView mDomainInput;
    private BroadcastReceiver mNfcReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            onNfcTagDiscovered(tag);
        }
    };
    private ImageView mPasswordToggle;
    private View mInputContainer, mResultContainer;
    private TextView mPasswordView, mPinView;
    private View mMethodSelect;
    private View mCopyPassword, mCopyPIN;
    private TextView mPasswordHint;

    private boolean mCanUseFingerprint;
    private CancellationSignal mCancelFingerprint;
    private ArrayAdapter<String> mDomainSelectorAdapter;
    private View mBackToInput, mClose;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final Window window = getWindow();
        boolean customTitleSupported;
        final Intent intent = getIntent();
        if (!BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            customTitleSupported = window.requestFeature(Window.FEATURE_CUSTOM_TITLE);
        } else {
            customTitleSupported = false;
            window.requestFeature(Window.FEATURE_NO_TITLE);
        }
        super.onCreate(savedInstanceState);

        mHandler = new Handler(Looper.getMainLooper());
        mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mPassGen = PassGen.getInstance(mPreferences.getString(KEY_PASSWORD_GENERATOR_TYPE, "sgp_md5"));
        mPinGen = PassGen.getInstance("hotp_pin");
        mFingerprintHelper = FingerprintHelper.getInstance(this);
        setContentView(R.layout.activity_pass_gen_dialog);
        if (customTitleSupported) {
            window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.layout_dialog_title_pass_gen);
            window.findViewById(R.id.settings).setOnClickListener(this);
        }

        mPasswordToggle.setOnClickListener(this);

        mCopyPassword.setOnClickListener(this);
        mCopyPIN.setOnClickListener(this);

        mBackToInput.setOnClickListener(this);
        mClose.setOnClickListener(this);

        mDomainSelectorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mDomainSelectorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mDomainInput.setAdapter(new DomainAutoCompleteAdapter(this));
        mDomainSelector.setAdapter(mDomainSelectorAdapter);

        if (mFingerprintHelper.requestPermission(this, REQUEST_REQUEST_PERMISSION)) {
            finishLayout();
        }

        handleIntent(intent);
    }

    private void handleIntent(final Intent intent) {
        mDomainSelectorAdapter.clear();
        for (String uriString : Utils.extractLinks(intent.getCharSequenceExtra(Intent.EXTRA_TEXT))) {
            for (String host : Utils.getHosts(this, Uri.parse(uriString), 2)) {
                mDomainSelectorAdapter.add(host);
            }
        }
        if (mDomainSelectorAdapter.isEmpty()) {
            final AccessibilityExtra extra = getAccessibilityExtra();
            final ContentResolver cr = getContentResolver();
            if (extra != null) {
                final String windowPackageName = String.valueOf(extra.window.getPackageName());
                final String windowClassName = String.valueOf(extra.window.getClassName());
                final String[] projection = {AppWindows.DOMAIN};
                final String where = Expression.equalsArgs(AppWindows.PACKAGE_NAME).getSQL();
                final String[] whereArgs = {windowPackageName, windowClassName};
                final Expression comparison = Expression.equalsArgs(AppWindows.WINDOW_NAME);

                // A hack to make best match, if a window matches, comparison result will be 1,
                // 0 otherwise. so we sort results by this method.
                final String orderBy = new OrderBy(comparison.getSQL(), false).getSQL();
                try (Cursor c = cr.query(AppWindows.CONTENT_URI, projection, where, whereArgs, orderBy)) {
                    if (c != null && c.moveToFirst()) {
                        final String host = c.getString(0);
                        mDomainInput.setText(host);
                        mDomainInput.setSelection(0, mDomainInput.length());
                    }
                }
            }

            mDomainSelector.setVisibility(View.GONE);
            mDomainInput.setVisibility(View.VISIBLE);
        } else {
            mDomainSelector.setVisibility(View.VISIBLE);
            mDomainInput.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        finishLayout();
    }

    private void finishLayout() {
        mCanUseFingerprint = mFingerprintHelper.hasPermission() && mFingerprintHelper.isHardwareDetected()
                && mFingerprintHelper.hasEnrolledFingerprints() && hasFingerprintData();
        if (mCanUseFingerprint) {
            mMethodSelect.setVisibility(View.VISIBLE);
            setPasswordHint(getString(R.string.input_password_or_fingerprint_hint));
            startFingerprintAuthentication();
        } else {
            mMethodSelect.setVisibility(View.GONE);
            setPasswordHint(getString(R.string.input_password_hint));
            inflateAndInitPasswordView();
        }
    }

    private void setPasswordHint(final CharSequence s) {
        mPasswordHint.setText(s);
    }

    private boolean hasFingerprintData() {
        try {
            final String ivEncoded = mPreferences.getString(KEY_FINGERPRINT_IV, null);
            final String passwordEncoded = mPreferences.getString(KEY_FINGERPRINT_PASSWORD, null);
            if (TextUtils.isEmpty(ivEncoded) || TextUtils.isEmpty(passwordEncoded)) {
                return false;
            }
            Base64.decode(ivEncoded, Base64.URL_SAFE);
            Base64.decode(passwordEncoded, Base64.URL_SAFE);
            return true;
        } catch (IllegalArgumentException e) {
            // Thrown when Base64 encoded IV is incorrect
            return false;
        }
    }

    private boolean startFingerprintAuthentication() {
        setPasswordHint(getString(R.string.input_password_or_fingerprint_hint));
        if (mCancelFingerprint != null && !mCancelFingerprint.isCanceled()) return false;
        final CancellationSignal signal = new CancellationSignal();
        final Cipher cipher = FingerprintCryptoHelper.getCipher();
        try {
            final String ivEncoded = mPreferences.getString(KEY_FINGERPRINT_IV, null);
            if (TextUtils.isEmpty(ivEncoded)) {
                return false;
            }
            final KeyStore keystore = FingerprintCryptoHelper.getKeystore();
            keystore.load(null);
            final Key key = keystore.getKey(FingerprintCryptoHelper.SECURITY_KEY_NAME, null);
            final IvParameterSpec ivParam = new IvParameterSpec(Base64.decode(ivEncoded, Base64.URL_SAFE));
            cipher.init(Cipher.DECRYPT_MODE, key, ivParam);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            return false;
        }
        final FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
        mFingerprintHelper.authenticate(cryptoObject, 0, signal, new FingerprintManagerCompat.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(final FingerprintManagerCompat.AuthenticationResult result) {
                mCancelFingerprint = null;
                final String host = getHost();
                if (TextUtils.isEmpty(host)) {
                    showEmptyHostError();
                    startFingerprintAuthentication();
                    return;
                }
                final String encoded = mPreferences.getString(KEY_FINGERPRINT_PASSWORD, null);
                if (TextUtils.isEmpty(encoded)) return;
                try {
                    final Cipher cipher = result.getCryptoObject().getCipher();
                    final String masterPassword = new String(cipher.doFinal(Base64.decode(encoded, Base64.URL_SAFE)));
                    showGeneratedPassword(host, masterPassword);
                } catch (BadPaddingException | IllegalBlockSizeException e) {
                    // TODO show error message
                }
            }

            @Override
            public void onAuthenticationError(final int errMsgId, final CharSequence errString) {
                if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    setPasswordHint(errString);
                }
            }

            @Override
            public void onAuthenticationHelp(final int helpMsgId, final CharSequence helpString) {
                setPasswordHint(helpString);
            }

            @Override
            public void onAuthenticationFailed() {
                setPasswordHint(getString(R.string.fingerprint_not_recognized));
            }
        }, mHandler);
        mCancelFingerprint = signal;
        return true;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mInputContainer = findViewById(R.id.input_container);
        mResultContainer = findViewById(R.id.result_container);
        mDomainSelector = (Spinner) findViewById(R.id.domain_selector);
        mDomainInput = (AutoCompleteTextView) findViewById(R.id.domain_input);
        mMethodSelect = findViewById(R.id.method_select);
        mPasswordToggle = (ImageView) findViewById(R.id.password_toggle);
        mPasswordContainer = ((PasswordContainer) findViewById(R.id.password_container));
        mPasswordView = (TextView) findViewById(R.id.generated_password_text);
        mPinView = (TextView) findViewById(R.id.generated_pin_text);
        mCopyPassword = findViewById(R.id.copy_password);
        mCopyPIN = findViewById(R.id.copy_pin);
        mBackToInput = findViewById(R.id.back_to_input);
        mClose = findViewById(R.id.close);
        mPasswordHint = (TextView) findViewById(R.id.password_hint);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.fingerprint_icon: {
                hidePasswordView();
                break;
            }
            case R.id.password_toggle: {
                togglePasswordView();
                break;
            }
            case R.id.settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            }
            case R.id.copy_password: {
                final CharSequence text = mPasswordView.getText();
                final ClipData clipData = ClipData.newPlainText(getString(R.string.password), text);
                ClipboardRestoreService.startCopy(this, clipData);
                finishAndRemoveTask();
                send(text);
                break;
            }
            case R.id.copy_pin: {
                final CharSequence text = mPinView.getText();
                final ClipData clipData = ClipData.newPlainText(getString(R.string.pin), text);
                ClipboardRestoreService.startCopy(this, clipData);
                finishAndRemoveTask();
                send(text);
                break;
            }
            case R.id.back_to_input: {
                mInputContainer.setVisibility(View.VISIBLE);
                mResultContainer.setVisibility(View.GONE);
                mPasswordView.setText(null);
                mPinView.setText(null);
                mCanUseFingerprint = mFingerprintHelper.hasPermission() && mFingerprintHelper.isHardwareDetected()
                        && mFingerprintHelper.hasEnrolledFingerprints() && hasFingerprintData();
                if (mCanUseFingerprint) {
                    mMethodSelect.setVisibility(View.VISIBLE);
                    setPasswordHint(getString(R.string.input_password_or_fingerprint_hint));
                    startFingerprintAuthentication();
                } else {
                    mMethodSelect.setVisibility(View.GONE);
                    setPasswordHint(getString(R.string.input_password_hint));
                }
                break;
            }
            case R.id.close: {
                finish();
                break;
            }
        }
    }

    private void send(final CharSequence text) {
        final AccessibilityExtra extra = getAccessibilityExtra();
        final Intent intent = new Intent(ACTION_PASSWORD_CALLBACK);
        intent.putExtra(EXTRA_ACCESSIBILITY_EXTRA, extra);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void togglePasswordView() {
        if (mPasswordContainer.getChildCount() > 0) {
            hidePasswordView();
        } else {
            inflateAndInitPasswordView();
        }
    }

    private void hidePasswordView() {
        mPasswordContainer.setVisibility(View.GONE);
        mPasswordContainer.removeAllViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCanUseFingerprint) {
            startFingerprintAuthentication();
        }
        registerReceiver(mNfcReceiver, new IntentFilter(ACTION_TAG_DISCOVERED));
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            final Intent intent = new Intent(ACTION_TAG_DISCOVERED);
            intent.setPackage(BuildConfig.APPLICATION_ID);
            nfcAdapter.enableForegroundDispatch(this, PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT), null, null);
        }
    }

    @Override
    protected void onPause() {
        if (mCancelFingerprint != null && !mCancelFingerprint.isCanceled()) {
            mCancelFingerprint.cancel();
        }
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        unregisterReceiver(mNfcReceiver);
        super.onPause();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private boolean showGeneratedPassword(String host, String masterPassword) {
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(masterPassword)) return false;
        saveRecentHost(host);
        if (mCancelFingerprint != null && !mCancelFingerprint.isCanceled()) {
            mCancelFingerprint.cancel();
        }
        final String salt = mPreferences.getString(KEY_PASSWORD_SALT, null);
        if (!TextUtils.isEmpty(salt)) {
            masterPassword += salt;
        }
        final int passwordLength = mPreferences.getInt(KEY_PASSWORD_LENGTH, 10);
        final int pinLength = mPreferences.getInt(KEY_PIN_LENGTH, 4);
        try {
            mInputContainer.setVisibility(View.GONE);
            mResultContainer.setVisibility(View.VISIBLE);
            mPasswordView.setText(mPassGen.generate(masterPassword, host, passwordLength));
            mPinView.setText(mPinGen.generate(masterPassword, host, pinLength));
            return true;
        } catch (PassGen.PassGenException e) {
            return false;
        }
    }

    @Nullable
    private AccessibilityExtra getAccessibilityExtra() {
        final Intent intent = getIntent();
        return intent.getParcelableExtra(EXTRA_ACCESSIBILITY_EXTRA);
    }

    private void saveRecentHost(final String host) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
                final ContentResolver cr = getContentResolver();
                final AccessibilityExtra extra = getAccessibilityExtra();
                if (extra != null) {
                    final String packageName = String.valueOf(extra.window.getPackageName());
                    if (!Utils.isWebBrowser(getApplicationContext(), packageName)) {
                        final ContentValues appWindowsValues = new ContentValues();
                        appWindowsValues.put(AppWindows.DOMAIN, host);
                        appWindowsValues.put(AppWindows.PACKAGE_NAME, packageName);
                        appWindowsValues.put(AppWindows.WINDOW_NAME, String.valueOf(extra.window.getClassName()));
                        cr.insert(AppWindows.CONTENT_URI, appWindowsValues);
                    }
                }
                final ContentValues values = new ContentValues();
                values.put(RecentDomains.DOMAIN, host);
                values.put(RecentDomains.RECENT, System.currentTimeMillis());
                cr.insert(RecentDomains.CONTENT_URI, values);
            }
        });
    }

    private void showEmptyHostError() {
        if (mDomainInput.getVisibility() == View.VISIBLE) {
            mDomainInput.setError(getString(R.string.no_input_domain_hint));
        } else if (mDomainSelector.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, R.string.no_input_domain_hint, Toast.LENGTH_SHORT).show();
        }
    }

    private String getHost() {
        if (mDomainSelector.getVisibility() == View.VISIBLE) {
            return (String) mDomainSelector.getSelectedItem();
        } else {
            return mDomainInput.getText().toString();
        }
    }

    private void inflateAndInitPasswordView() {
        if (TextUtils.isEmpty(mPreferences.getString(KEY_PATTERN_PASSWORD, null))) {
            getLayoutInflater().inflate(R.layout.layout_password_text, mPasswordContainer);
        } else {
            getLayoutInflater().inflate(R.layout.layout_password_pattern, mPasswordContainer);
        }
        mPasswordContainer.setVisibility(View.VISIBLE);
        mPasswordContainer.getPasswordController().setOnPasswordEnteredListener(new PasswordContainer.OnPasswordEnteredListener() {
            @Override
            public boolean onPasswordEntered(final byte[] key) {
                // Get final passwords here
                return showGeneratedPassword(getHost(), new String(key));
            }
        });
    }

    private void onNfcTagDiscovered(final Tag tag) {
        // Fill tag
        final byte[] id = tag.getId();
        if (id.length > 0) {
            final ByteString bs = ByteString.of(id);
            final String idHex = bs.hex();
            final String where = Expression.equalsArgs(NfcTags.TAG_ID).getSQL();
            final String[] whereArgs = {idHex};
            try (Cursor cursor = getContentResolver().query(NfcTags.CONTENT_URI,
                    NfcTags.COLUMNS, where, whereArgs, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    mDomainInput.setText(cursor.getString(cursor.getColumnIndex(NfcTags.DOMAIN)));
                    mDomainInput.setSelection(0, mDomainInput.length());
                } else {
                    //TODO show unregistered tag confirm
                    Toast.makeText(this, R.string.unregistered_tag, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, R.string.unsupported_tag, Toast.LENGTH_SHORT).show();
        }
    }

    private static class DomainAutoCompleteAdapter extends SimpleCursorAdapter {
        private final Context mContext;

        public DomainAutoCompleteAdapter(final Context context) {
            super(context, android.R.layout.simple_dropdown_item_1line, null,
                    new String[]{RecentDomains.DOMAIN},
                    new int[]{android.R.id.text1}, 0);
            mContext = context;
        }

        @Override
        public Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
            if (constraint == null) return null;
            final ContentResolver cr = mContext.getContentResolver();
            final Uri uri = RecentDomains.CONTENT_URI;
            final String where = Expression.likeRaw(new Columns.Column(RecentDomains.DOMAIN), "?||'%'", "^").getSQL();
            final String[] whereArgs = {constraint.toString().replace("_", "^_")};
            final String sortOrder = new OrderBy(RecentDomains.RECENT, false).getSQL();
            return cr.query(uri, RecentDomains.COLUMNS, where, whereArgs, sortOrder);
        }

        @Override
        public CharSequence convertToString(final Cursor cursor) {
            return cursor.getString(cursor.getColumnIndex(RecentDomains.DOMAIN));
        }
    }
}
