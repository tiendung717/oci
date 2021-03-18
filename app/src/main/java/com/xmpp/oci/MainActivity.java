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

        btnUpload.setOnClickListener(this::startUpload);
        btnGetNamespace.setOnClickListener(this::getNameSpace);

        System.setProperty(
                "java.version",
                "1.8"
        );
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


    private void startUpload(View view) {
        pickFile();
//        Executors.newSingleThreadExecutor().execute(() -> ociSdk.getNamespace());

    }

    private void doUpload(String filePath) {
        uploadFilePath = filePath;
        String objectName = UUID.randomUUID().toString() + "@" + new File(filePath).getName();

        Log.d("nt.dung", String.format("Upload: (%s)", objectName));

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ociSdk = new OciSdk();
                    ociSdk.testUpload(MainActivity.this, filePath, objectName);
                } catch (IOException e) {
                    Log.e("nt.dung", "E: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        disposableMap.dispose();
        super.onDestroy();
    }


    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, "Something is wrong with data", Toast.LENGTH_LONG).show();
                return;
            }
            switch (requestCode) {
                case REQUEST_FILE:
                    String path = FilePathUri.getPath(this, data.getData());
                    Toast.makeText(this, "Start upload file " + path, Toast.LENGTH_LONG).show();
                    doUpload(path);
                    break;
            }
        }
    }
}