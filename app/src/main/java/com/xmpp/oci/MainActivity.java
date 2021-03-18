package com.xmpp.oci;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_FILE = 4;

    private TextView tvUploadProgress;
    private TextView tvDownloadProgress;
    private View btnDownload;
    private View btnUpload;
    private View btnGetNamespace;

    private final CompositeDisposable disposableMap = new CompositeDisposable();
    private OciSdk ociSdk;
    private String objectName;
    private String uploadFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        btnDownload = findViewById(R.id.btnDownload);
        btnGetNamespace = findViewById(R.id.btnGetNamespace);

        ProgressBar progressBarUpload = findViewById(R.id.progressUpload);
        progressBarUpload.setVisibility(View.GONE);
        tvUploadProgress = findViewById(R.id.tvUploadProgress);
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress);

        btnGetNamespace.setOnClickListener(this::getNameSpace);

    }

    private void getNameSpace(View view) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ociSdk = new OciSdk();
                String namespace = ociSdk.testGetNamespace();
                Log.d("nt.dung", "NS: " + namespace);
            }
        });
    }


    @Override
    protected void onDestroy() {
        disposableMap.dispose();
        super.onDestroy();
    }
}