package org.mariotaku.pass.view.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.eftimoff.patternview.PatternView;
import com.eftimoff.patternview.cells.Cell;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.util.MasterPasswordEncrypter;
import org.mariotaku.pass.view.PasswordContainer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by mariotaku on 15/10/30.
 */
public class PatternPasswordController extends PasswordContainer.PasswordController implements Constants, PatternView.OnPatternDetectedListener {
    private final MasterPasswordEncrypter mEncrypter;
    private PatternView mPatternView;

    public PatternPasswordController(final PasswordContainer container, final Context context, final AttributeSet attributeSet) {
        super(container, context, attributeSet);
        mEncrypter = new MasterPasswordEncrypter();
    }

    @Override
    public void onAttach(final View view) {
        super.onAttach(view);
        mPatternView = (PatternView) view.findViewById(R.id.password_pattern);
        mPatternView.setOnPatternDetectedListener(this);
    }

    @Override
    public void onPatternDetected() {
        final List<Cell> patterns = mPatternView.getPattern();
        final byte[] key = getKeyByPatterns(patterns);
        try {
            final SharedPreferences preferences = getPreferences();
            final String patternPw = preferences.getString(KEY_PATTERN_PASSWORD, null);
            final String patternIv = preferences.getString(KEY_PATTERN_IV, null);
            if (TextUtils.isEmpty(patternPw) || TextUtils.isEmpty(patternIv)) {
                // No password set, go to text password
                useTextPassword();
                return;
            }
            final byte[] iv = Base64.decode(patternIv, Base64.URL_SAFE);
            notifyPasswordEntered(mEncrypter.decrypt(Base64.decode(patternPw, Base64.URL_SAFE), key, iv));
            resetPasswordRetries();
        } catch (MasterPasswordEncrypter.InternalErrorException | IllegalArgumentException e) {
            // TODO Show internal error message
            e.printStackTrace();
        } catch (MasterPasswordEncrypter.WrongPasswordException e) {
            notifyPasswordWrong();
        }
        mPatternView.clearPattern();
    }

    @Override
    protected void onPasswordWrong(final int retries) {
        mPatternView.setDisplayMode(PatternView.DisplayMode.Wrong);
        if (retries >= 4) {
            getPreferences().edit().remove(KEY_PATTERN_PASSWORD).apply();
            Toast.makeText(getContext(), R.string.pattern_password_cleared, Toast.LENGTH_SHORT).show();
            resetPasswordRetries();
            useTextPassword();
        }
    }

    private void useTextPassword() {
        final PasswordContainer container = getContainer();
        final PasswordContainer.OnPasswordEnteredListener listener = getOnPasswordEnteredListener();
        container.removeAllViews();
        View.inflate(getContext(), R.layout.layout_password_text, container);
        container.getPasswordController().setOnPasswordEnteredListener(listener);
    }

    public static byte[] getKeyByPatterns(final List<Cell> patterns) {
        final ByteBuffer bb = ByteBuffer.allocate(patterns.size() * 2 * 4);
        for (final Cell pattern : patterns) {
            bb.putInt(pattern.getRow());
            bb.putInt(pattern.getColumn());
        }
        return bb.array();
    }

}
