package org.mariotaku.pass.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import org.mariotaku.pass.activity.PassGenDialogActivity;

/**
 * Created by mariotaku on 16/9/1.
 */
@TargetApi(Build.VERSION_CODES.N)
public class QuickSettingsTileService extends TileService {
    @Override
    public void onClick() {
        unlockAndRun(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(QuickSettingsTileService.this, PassGenDialogActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }
}
