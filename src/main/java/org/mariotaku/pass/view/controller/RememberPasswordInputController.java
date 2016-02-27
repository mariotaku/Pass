package org.mariotaku.pass.view.controller;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.mariotaku.pass.R;
import org.mariotaku.pass.view.RememberMasterPasswordContainer;
import org.mariotaku.pass.view.RememberMasterPasswordContainer.PageController;

/**
 * Created by mariotaku on 15/10/31.
 */
public class RememberPasswordInputController extends PageController {
    private EditText mEditPassword;
    private EditText mPasswordConfirm;

    public RememberPasswordInputController(final RememberMasterPasswordContainer container,
                                           final Context context, final AttributeSet attributeSet) {
        super(container, context, attributeSet);
    }

    @Override
    protected void onAttach(final View view) {
        super.onAttach(view);
        mEditPassword = (EditText) view.findViewById(R.id.edit_password);
        mPasswordConfirm = (EditText) view.findViewById(R.id.password_confirm);
    }

    @Override
    protected boolean onPagePreviousExit() {
        hideIME();
        return true;
    }

    @Override
    protected boolean onPagePreviousEnter() {
        showIME();
        return true;
    }

    @Override
    protected boolean onPageNextEnter() {
        showIME();
        return true;
    }

    @Override
    protected boolean onPageNextExit() {
        if (TextUtils.isEmpty(mEditPassword.getText())) {
            mEditPassword.setError(getContext().getString(R.string.empty_password));
            return false;
        }
        if (!TextUtils.equals(mPasswordConfirm.getText(), mEditPassword.getText())) {
            mPasswordConfirm.setSelection(0, mPasswordConfirm.length());
            mPasswordConfirm.setError(getContext().getString(R.string.password_not_match));
            return false;
        }
        hideIME();
        return true;
    }

    private void showIME() {
        if (mEditPassword.requestFocus()) {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mEditPassword, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideIME() {
        mEditPassword.clearFocus();
        final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditPassword.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public byte[] getPassword() {
        return mEditPassword.getText().toString().getBytes();
    }
}
