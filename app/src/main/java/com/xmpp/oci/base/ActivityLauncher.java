package com.xmpp.oci.base;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class ActivityLauncher extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LauncherIntentDispatcher dispatcher = new LauncherIntentDispatcher(this, getIntent());

        @LauncherIntentDispatcher.Action int dispatchAction = dispatcher.dispatch();
        switch (dispatchAction) {
            case LauncherIntentDispatcher.Action.ACTION_FINISH:
                finish();
                break;
            case LauncherIntentDispatcher.Action.ACTION_CONTINUE:
                break;
            default:
                finish();
                break;
        }
    }
}
