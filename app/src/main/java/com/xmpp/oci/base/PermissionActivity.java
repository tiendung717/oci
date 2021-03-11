package com.xmpp.oci.base;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;


import com.xmpp.oci.R;

import java.util.Map;

public class PermissionActivity extends BaseActivity {

    private final String[] permissions = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean granted = true;
            for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                Log.d("nt.dung", "Permission: " + entry.getKey() + ", Result: " + entry.getValue());
                if (!entry.getValue()) granted = false;
            }

            if (granted) {
                startActivity(new Intent(PermissionActivity.this, ActivityLauncher.class));
                finish();
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        findViewById(R.id.btnGrantPermission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permissionLauncher.launch(permissions);
            }
        });
    }
}
