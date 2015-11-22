package org.mariotaku.pass.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

/**
 * Created by mariotaku on 15/11/1.
 */
public abstract class FingerprintHelper {

    final Context context;
    final FingerprintManagerCompat delegate;

    public FingerprintHelper(final Context context) {
        this.context = context;
        this.delegate = FingerprintManagerCompat.from(context);
    }

    public static FingerprintHelper getInstance(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return new NoOp(activity);
        return new ImplM(activity);
    }

    public void authenticate(@Nullable final FingerprintManagerCompat.CryptoObject crypto, final int flags,
                             @Nullable final CancellationSignal cancel, @NonNull final FingerprintManagerCompat.AuthenticationCallback callback, @Nullable final Handler handler) throws SecurityException {
        delegate.authenticate(crypto, flags, cancel, callback, handler);
    }

    public boolean isHardwareDetected() throws SecurityException {
        return delegate.isHardwareDetected();
    }

    public boolean hasEnrolledFingerprints() throws SecurityException {
        return delegate.hasEnrolledFingerprints();
    }

    public abstract boolean requestPermission(Activity activity, final int requestCode);

    public abstract boolean requestPermission(Fragment fragment, final int requestCode);

    public abstract boolean hasPermission();


    @TargetApi(Build.VERSION_CODES.M)
    private static class ImplM extends FingerprintHelper {

        public ImplM(final Activity activity) {
            super(activity);
        }

        @Override
        public boolean requestPermission(Activity activity, final int requestCode) {
            if (hasPermission()) return true;
            activity.requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT}, requestCode);
            return false;
        }

        @Override
        public boolean requestPermission(final Fragment fragment, final int requestCode) {
            if (hasPermission()) return true;
            fragment.requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT}, requestCode);
            return false;
        }

        @Override
        public boolean hasPermission() {
            return context.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private static class NoOp extends FingerprintHelper {
        public NoOp(final Activity activity) {
            super(activity);
        }

        @Override
        public boolean requestPermission(Activity activity, final int requestCode) {
            return true;
        }

        @Override
        public boolean requestPermission(final Fragment fragment, final int requestCode) {
            return true;
        }

        @Override
        public boolean hasPermission() {
            return false;
        }
    }
}
