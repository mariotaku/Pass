package org.mariotaku.pass.util;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by mariotaku on 15/11/19.
 */
public class DebugModeUtils {

    public static void init(Application application) {
        Stetho.initializeWithDefaults(application);
    }

}
