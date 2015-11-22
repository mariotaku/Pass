package org.mariotaku.pass.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.accessibility.AccessibilityEvent;

/**
 * Created by mariotaku on 15/11/18.
 */
public class AccessibilityExtra implements Parcelable {

    @NonNull
    public AccessibilityEvent window, view;

    public AccessibilityExtra(@NonNull final AccessibilityEvent window, @NonNull final AccessibilityEvent view) {
        this.window = window;
        this.view = view;
    }

    protected AccessibilityExtra(Parcel in) {
        window = in.readParcelable(AccessibilityEvent.class.getClassLoader());
        view = in.readParcelable(AccessibilityEvent.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(window, flags);
        dest.writeParcelable(view, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AccessibilityExtra> CREATOR = new Creator<AccessibilityExtra>() {
        @Override
        public AccessibilityExtra createFromParcel(Parcel in) {
            return new AccessibilityExtra(in);
        }

        @Override
        public AccessibilityExtra[] newArray(int size) {
            return new AccessibilityExtra[size];
        }
    };
}
