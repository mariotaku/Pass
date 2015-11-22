package org.mariotaku.pass.view.controller;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import org.mariotaku.pass.R;
import org.mariotaku.pass.view.PasswordContainer;

/**
 * Created by mariotaku on 15/10/31.
 */
public class TextPasswordController extends PasswordContainer.PasswordController implements View.OnClickListener {
    private EditText mEditPassword;
    private View mPasswordSubmit;

    public TextPasswordController(final PasswordContainer container, final Context context, final AttributeSet attributeSet) {
        super(container, context, attributeSet);
    }

    @Override
    protected void onAttach(final View view) {
        super.onAttach(view);
        mEditPassword = (EditText) view.findViewById(R.id.edit_password);
        mPasswordSubmit = view.findViewById(R.id.password_submit);

        mPasswordSubmit.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        final Editable text = mEditPassword.getText();
        if (TextUtils.isEmpty(text)) {
            // Show empty error
            return;
        }
        notifyPasswordEntered(String.valueOf(text).getBytes());
    }
}
