package org.mariotaku.pass.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by mariotaku on 15/10/30.
 */
public class PasswordContainer extends FrameLayout {
    public PasswordContainer(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PasswordContainer(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public PasswordContainer(final Context context) {
        super(context);
    }

    @Override
    public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (getChildCount() > 1) {
            throw new InflateException("PasswordContainer must have one exact child");
        }
        final View view = getChildAt(0);
        final PasswordController controller = getPasswordController();
        controller.onAttach(view);
    }

    public PasswordController getPasswordController() {
        return ((LayoutParams) getChildAt(0).getLayoutParams()).getPasswordController();
    }

    @Override
    protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public LayoutParams generateLayoutParams(final AttributeSet attrs) {
        return new LayoutParams(this, getContext(), attrs);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        private final PasswordController passwordController;

        public LayoutParams(final PasswordContainer container, final Context c, final AttributeSet attrs) {
            super(c, attrs);
            final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.PasswordContainer);
            passwordController = parsePasswordController(a.getString(R.styleable.PasswordContainer_layout_passwordController),
                    container, c, attrs);
            a.recycle();
        }

        private static PasswordController parsePasswordController(final String className,
                                                                  final PasswordContainer container,
                                                                  final Context c, final AttributeSet attrs) {
            if (TextUtils.isEmpty(className)) {
                throw new InflateException("You must give PasswordContainer's child a PasswordController class");
            }
            try {
                return (PasswordController) Class.forName(className).getConstructor(PasswordContainer.class, Context.class, AttributeSet.class).newInstance(container, c, attrs);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException | ClassCastException e) {
                throw new InflateException(e);
            }
        }

        public PasswordController getPasswordController() {
            return passwordController;
        }
    }

    public static class PasswordController {
        private final PasswordContainer container;
        private final Context context;
        private final SharedPreferences preferences;
        private View view;
        private OnPasswordEnteredListener listener;

        protected PasswordController(PasswordContainer container, Context context, AttributeSet attributeSet) {
            this.container = container;
            this.context = context;
            this.preferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        public PasswordContainer getContainer() {
            return container;
        }

        public Context getContext() {
            return context;
        }

        public SharedPreferences getPreferences() {
            return preferences;
        }

        public View getView() {
            return view;
        }

        protected void onAttach(final View view) {
            this.view = view;
        }

        protected void notifyPasswordEntered(byte[] key) {
            if (listener != null) {
                listener.onPasswordEntered(key);
            }
        }

        public void setOnPasswordEnteredListener(final OnPasswordEnteredListener listener) {
            this.listener = listener;
        }

        public OnPasswordEnteredListener getOnPasswordEnteredListener() {
            return listener;
        }

        protected final void notifyPasswordWrong() {
            final int retryCount = preferences.getInt(Constants.KEY_PASSWORD_RETRY_COUNT, 0);
            onPasswordWrong(retryCount);
            preferences.edit().putInt(Constants.KEY_PASSWORD_RETRY_COUNT, retryCount + 1).apply();
        }

        protected final void resetPasswordRetries() {
            preferences.edit().remove(Constants.KEY_PASSWORD_RETRY_COUNT).apply();
        }

        protected void onPasswordWrong(int retries) {

        }
    }

    public interface OnPasswordEnteredListener {
        boolean onPasswordEntered(byte[] key);
    }
}
