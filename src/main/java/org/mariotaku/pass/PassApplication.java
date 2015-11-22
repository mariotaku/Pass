package org.mariotaku.pass;

import android.app.Application;

import org.mariotaku.pass.util.DebugModeUtils;

public class PassApplication extends Application implements Constants {
    public void onCreate() {
        super.onCreate();
        DebugModeUtils.init(this);
    }
}