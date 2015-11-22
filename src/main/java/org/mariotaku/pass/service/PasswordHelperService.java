package org.mariotaku.pass.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import org.mariotaku.pass.BuildConfig;
import org.mariotaku.pass.Constants;
import org.mariotaku.pass.R;
import org.mariotaku.pass.activity.PassGenDialogActivity;
import org.mariotaku.pass.model.AccessibilityExtra;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mariotaku on 15/11/18.
 */
public class PasswordHelperService extends AccessibilityService implements Constants {

    private BroadcastReceiver mCallbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final AccessibilityExtra extra = intent.getParcelableExtra(EXTRA_ACCESSIBILITY_EXTRA);
            final CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (extra == null || text == null) return;
            mCallbackData.set(new PasswordCallbackData(extra, text));

        }
    };

    private final AtomicReference<AccessibilityEvent> mCurrentWindow = new AtomicReference<>();
    private final AtomicReference<AccessibilityEvent> mCurrentView = new AtomicReference<>();

    private final AtomicReference<AccessibilityEvent> mPasswordWindow = new AtomicReference<>();
    private final AtomicReference<AccessibilityEvent> mPasswordView = new AtomicReference<>();

    private final AtomicReference<PasswordCallbackData> mCallbackData = new AtomicReference<>();

    private AtomicReference<BrowserState> mCurrentViewingLink = new AtomicReference<>();


    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(mCallbackReceiver, new IntentFilter(ACTION_PASSWORD_CALLBACK));
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallbackReceiver);
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        // Ignore changes inside myself
        if (TextUtils.equals(getPackageName(), event.getPackageName())) return;
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: {
                // Not an activity window, skip
                if (event.getCurrentItemIndex() >= 0 || event.getItemCount() >= 0) {
                    if (BuildConfig.DEBUG) {
                        Log.d(LOGTAG, "Ignoring window state " + event);
                    }
                    break;
                }
                final AccessibilityEvent passwordWindowEvent = mPasswordWindow.get();
                final AccessibilityEvent passwordViewEvent = mPasswordView.get();
                // Went back to password input window
                if (passwordWindowEvent != null && passwordViewEvent != null && isSameWindow(event, passwordWindowEvent)) {
                    final AccessibilityNodeInfo inputField = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                    final PasswordCallbackData data = mCallbackData.getAndSet(null);
                    // Current focusing view is password view
                    if (isSameView(inputField, passwordViewEvent.getSource())) {
                        // Not finishing from PassGen, just switch back
                        if (data == null) {
                            onFocusedToPasswordView(passwordViewEvent, passwordWindowEvent, mCurrentViewingLink.get());
                        } else if (pasteText(inputField, data.password)) {
                            // Do something after copied successfully
                        } else {
                            Toast.makeText(this, R.string.past_manually_hint, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Clear notification
                        final NotificationManager nm = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
                        nm.cancel(NOTIFICATION_ID_ACCESSIBILITY);
                    }
                } else {
                    // Clear notification
                    final NotificationManager nm = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
                    nm.cancel(NOTIFICATION_ID_ACCESSIBILITY);
                }
                final AccessibilityNodeInfo webViewNode = findWebViewNode(getRootInActiveWindow());
                if (webViewNode != null) {
                    handleBrowserLink(event, webViewNode);
                }
                mCurrentWindow.set(AccessibilityEvent.obtain(event));
                mCurrentViewingLink.set(null);
                break;
            }
            case AccessibilityEvent.TYPE_VIEW_FOCUSED: {
                // Handle password fields
                mCurrentView.getAndSet(AccessibilityEvent.obtain(event));
                final AccessibilityEvent windowEvent = mCurrentWindow.get();
                AccessibilityEvent passwordView = mPasswordView.get();
                if (event.isPassword() && windowEvent != null
                        && TextUtils.equals(event.getPackageName(), windowEvent.getPackageName())) {
                    final BrowserState browserState = mCurrentViewingLink.get();
                    if (browserState != null && TextUtils.equals(event.getPackageName(), browserState.packageName)) {
                        onFocusedToPasswordView(event, windowEvent, browserState);
                    } else {
                        onFocusedToPasswordView(event, windowEvent, null);
                    }
                } else if (passwordView == null || !isSameView(passwordView.getSource(), event.getSource())) {
                    // Clear notification
                    final NotificationManager nm = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
                    nm.cancel(NOTIFICATION_ID_ACCESSIBILITY);
                }
                break;
            }
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED: {
                final AccessibilityNodeInfo source = event.getSource();
                if (source != null && TextUtils.equals(source.getClassName(), WebView.class.getName())) {
                    handleBrowserLink(event, source);
                }
                break;
            }
        }
    }

    @Nullable
    private AccessibilityNodeInfo findWebViewNode(@Nullable final AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (TextUtils.equals(node.getClassName(), WebView.class.getName())) return node;
        AccessibilityNodeInfo webViewNode = null;
        for (int i = 0, j = node.getChildCount(); i < j; i++) {
            webViewNode = findWebViewNode(node.getChild(i));
            if (webViewNode != null) break;
        }
        return webViewNode;
    }

    private void handleBrowserLink(final AccessibilityEvent event, final AccessibilityNodeInfo source) {
        final List<CharSequence> editTextValues = new ArrayList<>();
        findEditTextValues(getRootInActiveWindow(), source, editTextValues);
        CharSequence desiredUri = null;
        for (final CharSequence value : editTextValues) {
            if (Patterns.WEB_URL.matcher(value).matches()) {
                desiredUri = value;
                break;
            }
        }
        mCurrentViewingLink.set(new BrowserState(event.getPackageName(), desiredUri));
    }

    private void onFocusedToPasswordView(final AccessibilityEvent viewEvent,
                                         final AccessibilityEvent windowEvent,
                                         final BrowserState browserState) {
        mPasswordWindow.set(AccessibilityEvent.obtain(windowEvent));
        mPasswordView.set(AccessibilityEvent.obtain(viewEvent));

        final NotificationManager nm = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        final Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_password);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setVibrate(new long[0]);
        builder.setAutoCancel(true);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.accessibility_password_hint));
        final Intent passGenIntent = new Intent(this, PassGenDialogActivity.class);
        passGenIntent.putExtra(EXTRA_ACCESSIBILITY_EXTRA, new AccessibilityExtra(windowEvent, viewEvent));
        if (browserState != null && !TextUtils.isEmpty(browserState.link)) {
            passGenIntent.putExtra(Intent.EXTRA_TEXT, browserState.link);
        }
        builder.setContentIntent(PendingIntent.getActivity(this, 0, passGenIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        //noinspection deprecation
        //builder.addAction(new Notification.Action(R.drawable.ic_action_clear, "Never for this screen", null));
        nm.notify(NOTIFICATION_ID_ACCESSIBILITY, builder.build());
    }

    private boolean pasteText(AccessibilityNodeInfo source, CharSequence text) {
        if (source.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)) {
            final Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        } else {
            final ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            final ClipData clipBackup = cm.getPrimaryClip();
            cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.copied_password), text));
            final boolean result = source.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            if (clipBackup != null) {
                cm.setPrimaryClip(clipBackup);
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""));
            }
            return result;
        }
    }

    private void findEditTextValues(@Nullable final AccessibilityNodeInfo root, final AccessibilityNodeInfo exclude, List<CharSequence> out) {
        if (root == null) return;
        if (TextUtils.equals(root.getClassName(), EditText.class.getName())) {
            out.add(root.getText());
        }
        for (int i = 0, j = root.getChildCount(); i < j; i++) {
            final AccessibilityNodeInfo info = root.getChild(i);
            if (info != null && !info.equals(exclude)) {
                findEditTextValues(info, exclude, out);
            }
        }
    }

    private boolean isSameView(final AccessibilityNodeInfo left, final AccessibilityNodeInfo right) {
        if (left == null || right == null) return false;
        return left.equals(right);
    }

    private boolean isSameWindow(final AccessibilityEvent left, final AccessibilityEvent right) {
        if (left == null || right == null) return false;
        return TextUtils.equals(left.getClassName(), right.getClassName())
                && TextUtils.equals(left.getPackageName(), right.getPackageName());
    }

    static class PasswordCallbackData {
        @NonNull
        AccessibilityExtra extra;
        @NonNull
        CharSequence password;

        public PasswordCallbackData(@NonNull final AccessibilityExtra extra, @NonNull final CharSequence password) {
            this.extra = extra;
            this.password = password;
        }
    }

    static class BrowserState {
        @NonNull
        CharSequence packageName;
        @Nullable
        CharSequence link;

        public BrowserState(@NonNull final CharSequence packageName, @Nullable final CharSequence link) {
            this.packageName = packageName;
            this.link = link;
        }
    }
}
