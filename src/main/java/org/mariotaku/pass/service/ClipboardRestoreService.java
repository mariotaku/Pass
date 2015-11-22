package org.mariotaku.pass.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;


public class ClipboardRestoreService extends IntentService implements Constants {

    public static void startCopy(Context context, ClipData clipData) {
        Intent intent = new Intent(context, ClipboardRestoreService.class);
        intent.setClipData(clipData);
        context.startService(intent);
    }

    public ClipboardRestoreService() {
        super("ClipboardRestoreService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final SharedPreferences pref = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        if (!pref.getBoolean(KEY_CLEAR_COPIED_PASSWORD_AUTOMATICALLY, true)) {
            cm.setPrimaryClip(intent.getClipData());
            return;
        }
        final ClipboardChangedListener listener = new ClipboardChangedListener(cm);
        // Get primary clip as backup
        listener.current = cm.getPrimaryClip();

        final ClipData newData = intent.getClipData();
        cm.setPrimaryClip(newData);
        cm.addPrimaryClipChangedListener(listener);
        // Wait for time up
        final Notification.Builder nb = new Notification.Builder(this);
        nb.setSmallIcon(R.drawable.ic_stat_password);
        nb.setContentTitle(getString(R.string.app_name));
        nb.setContentText(getString(R.string.password_copied));
        nb.setOngoing(true);
        final int duration = Integer.parseInt(pref.getString(KEY_COPIED_PASSWORD_VALIDITY, "15000"));
        long start = SystemClock.uptimeMillis(), end = start + duration, curr = start;
        while (curr < end) {
            final int progress = (int) ((end - curr) / (double) duration * 100);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            nb.setProgress(100, progress, false);
            nm.notify(NOTIFICATION_ID_CLIPBOARD, nb.build());
            curr = SystemClock.uptimeMillis();
        }
        // Clipboard may change during password waiting time, now restore them
        if (listener.current != null) {
            cm.setPrimaryClip(listener.current);
        } else {
            cm.setPrimaryClip(ClipData.newPlainText("", ""));
        }
        cm.removePrimaryClipChangedListener(listener);
        nm.cancel(NOTIFICATION_ID_CLIPBOARD);
    }

    private static class ClipboardChangedListener implements ClipboardManager.OnPrimaryClipChangedListener {

        private final ClipboardManager cm;
        private ClipData current;

        ClipboardChangedListener(ClipboardManager cm) {
            this.cm = cm;
        }

        @Override
        public void onPrimaryClipChanged() {
            current = cm.getPrimaryClip();
        }
    }

}
