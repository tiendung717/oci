package com.xmpp.oci.base;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;

import androidx.annotation.IntDef;


import com.xmpp.oci.MainActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LauncherIntentDispatcher {

    private final String[] smsPermissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    @IntDef({Action.ACTION_FINISH, Action.ACTION_CONTINUE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {
        int ACTION_FINISH = 0;
        int ACTION_CONTINUE = 1;
    }

    private Activity fromActivity;
    private Intent fromIntent;

    public LauncherIntentDispatcher(Activity fromActivity, Intent fromIntent) {
        this.fromActivity = fromActivity;
        this.fromIntent = fromIntent;
    }

    public @Action
    int dispatch() {
        if (!PermissionUtils.hasPermissions(fromActivity, smsPermissions)) {
            fromActivity.startActivity(new Intent(fromActivity, PermissionActivity.class));
            return Action.ACTION_FINISH;
        }
        fromActivity.startActivity(new Intent(fromActivity, MainActivity.class));
        return Action.ACTION_FINISH;
    }
}
