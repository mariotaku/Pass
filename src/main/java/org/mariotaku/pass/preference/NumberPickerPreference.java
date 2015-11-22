package org.mariotaku.pass.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import org.mariotaku.pass.R;

public class NumberPickerPreference extends DialogPreference {
    private final int mMinValue, mMaxValue;
    private NumberPicker mPicker;
    private Integer mNumber = 0;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreferences);
        mMinValue = a.getInt(R.styleable.NumberPickerPreferences_min, 0);
        mMaxValue = a.getInt(R.styleable.NumberPickerPreferences_max, 10);
        a.recycle();
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {
        mPicker = new NumberPicker(getContext());
        mPicker.setMinValue(mMinValue);
        mPicker.setMaxValue(mMaxValue);
        mPicker.setValue(mNumber);
        return mPicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            // needed when user edits the text field and clicks OK
            mPicker.clearFocus();
            setValue(mPicker.getValue());
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mNumber) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }

        if (value != mNumber) {
            mNumber = value;
            notifyChanged();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }
}