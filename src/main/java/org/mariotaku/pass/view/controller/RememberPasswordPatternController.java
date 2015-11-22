package org.mariotaku.pass.view.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.eftimoff.patternview.PatternView;
import com.eftimoff.patternview.cells.Cell;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.util.MasterPasswordEncrypter;
import org.mariotaku.pass.view.RememberMasterPasswordContainer;
import org.mariotaku.pass.view.RememberMasterPasswordContainer.PageController;

import java.util.List;

/**
 * Created by mariotaku on 15/10/31.
 */
public class RememberPasswordPatternController extends PageController implements Constants,
        PatternView.OnPatternDetectedListener {
    private final MasterPasswordEncrypter mEncrypter;
    private final SharedPreferences mPreferences;
    private PatternView mPatternView;
    private List<Cell> mPattern;
    private TextView mPatternHint;

    public RememberPasswordPatternController(final RememberMasterPasswordContainer container,
                                             final Context context, final AttributeSet attributeSet) {
        super(container, context, attributeSet);
        mEncrypter = new MasterPasswordEncrypter();
        mPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    protected void onAttach(final View view) {
        super.onAttach(view);
        mPatternView = ((PatternView) view.findViewById(R.id.pattern_view));
        mPatternHint = (TextView) view.findViewById(R.id.pattern_hint);

        mPatternView.setOnPatternDetectedListener(this);
    }

    @Override
    public void onPatternDetected() {
        final List<Cell> pattern = mPatternView.getPattern();
        mPatternView.clearPattern();
        if (mPattern != null) {
            // Check second pattern matches
            if (!mPattern.equals(pattern)) {
                mPatternView.setDisplayMode(PatternView.DisplayMode.Wrong);
                mPatternHint.setText(R.string.pattern_not_match_hint);
            } else {
                final RememberMasterPasswordContainer container = getContainer();
                final byte[] key = PatternPasswordController.getKeyByPatterns(pattern);
                final RememberPasswordInputController controller = (RememberPasswordInputController)
                        container.findPageControllerById(R.id.remember_input_password);
                try {
                    final Pair<byte[], byte[]> encryptedAndIv = mEncrypter.encrypt(controller.getPassword(), key);
                    final SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putString(KEY_PATTERN_PASSWORD, Base64.encodeToString(encryptedAndIv.first, Base64.URL_SAFE));
                    editor.putString(KEY_PATTERN_IV, Base64.encodeToString(encryptedAndIv.second, Base64.URL_SAFE));
                    editor.apply();
                    container.showNext();
                } catch (MasterPasswordEncrypter.InternalErrorException e) {
                    mPatternHint.setText(R.string.internal_error);
                }
            }
        } else {
            mPatternHint.setText(R.string.pattern_confirm_hint);
        }
        mPattern = pattern;
    }

    @Override
    protected boolean onPagePreviousEnter() {
        mPatternView.clearPattern();
        return true;
    }

    @Override
    protected boolean onPageNextEnter() {
        mPatternView.clearPattern();
        return true;
    }

    @Override
    protected boolean onPageNextExit() {
        return true;
    }
}
