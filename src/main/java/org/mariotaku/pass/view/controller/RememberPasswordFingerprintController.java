package org.mariotaku.pass.view.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.fragment.RememberMasterPasswordFingerprintFragment;
import org.mariotaku.pass.util.FingerprintCryptoHelper;
import org.mariotaku.pass.util.FingerprintHelper;
import org.mariotaku.pass.view.RememberMasterPasswordContainer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Created by mariotaku on 15/10/31.
 */
public class RememberPasswordFingerprintController extends RememberMasterPasswordContainer.PageController
        implements Constants {
    private final SharedPreferences mPreferences;
    private CancellationSignal cancellationSignal;
    private TextView mFingerprintHintView;

    public RememberPasswordFingerprintController(final RememberMasterPasswordContainer container,
                                                 final Context context, final AttributeSet attributeSet) {
        super(container, context, attributeSet);
        mPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    protected void onAttach(final View view) {
        super.onAttach(view);
        mFingerprintHintView = (TextView) view.findViewById(R.id.fingerprint_hint);
    }

    @Override
    protected boolean onPageNextEnter() {
        startFingerprintAuthentication();
        return true;
    }

    @Override
    protected boolean onPagePreviousEnter() {
        startFingerprintAuthentication();
        return true;
    }

    private void startFingerprintAuthentication() {
        if (cancellationSignal != null && !cancellationSignal.isCanceled()) return;
        regenerateKey();
        final RememberMasterPasswordContainer.PageListener listener = getContainer().getPageListener();
        final RememberPasswordInputController controller = (RememberPasswordInputController) getContainer()
                .findPageControllerById(R.id.remember_input_password);
        if (listener instanceof RememberMasterPasswordFingerprintFragment) {
            FingerprintHelper helper = ((RememberMasterPasswordFingerprintFragment) listener).getFingerprintHelper();
            final KeyStore keyStore = FingerprintCryptoHelper.getKeystore();
            final Cipher cipher = FingerprintCryptoHelper.getCipher();
            final FingerprintManagerCompat.CryptoObject crypto = new FingerprintManagerCompat.CryptoObject(cipher);
            cancellationSignal = new CancellationSignal();
            try {
                keyStore.load(null);
                SecretKey key = (SecretKey) keyStore.getKey(FingerprintCryptoHelper.SECURITY_KEY_NAME, null);
                cipher.init(Cipher.ENCRYPT_MODE, key);
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
            helper.authenticate(crypto, 0, cancellationSignal, new FingerprintManagerCompat.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(final int errMsgId, final CharSequence errString) {
                    mFingerprintHintView.setText(errString);
                }

                @Override
                public void onAuthenticationHelp(final int helpMsgId, final CharSequence helpString) {
                    mFingerprintHintView.setText(helpString);
                }

                @Override
                public void onAuthenticationSucceeded(final FingerprintManagerCompat.AuthenticationResult result) {
                    try {
                        final Cipher cipher = result.getCryptoObject().getCipher();
                        final String encrypted = Base64.encodeToString(cipher.doFinal(controller.getPassword()), Base64.URL_SAFE);
                        mPreferences.edit().putString(KEY_FINGERPRINT_PASSWORD, encrypted).apply();
                        mPreferences.edit().putString(KEY_FINGERPRINT_IV, Base64.encodeToString(cipher.getIV(), Base64.URL_SAFE)).apply();
                        final RememberMasterPasswordContainer container = getContainer();
                        container.showNext();
                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                        showInternalError();
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            }, getView().getHandler());
        }
    }

    private boolean regenerateKey() {
        try {
            final KeyStore keyStore = FingerprintCryptoHelper.getKeystore();
            final KeyGenerator keyGenerator = FingerprintCryptoHelper.getKeyGenerator();
            keyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(FingerprintCryptoHelper.createKeyGenParameterSpec());
            keyGenerator.generateKey();
            return true;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showInternalError() {

    }


}
