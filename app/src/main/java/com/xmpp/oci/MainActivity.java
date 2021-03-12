package com.xmpp.oci;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.objectstorage.transfer.ProgressReporter;
import com.safarifone.oci.OciHelper;
import com.safarifone.oci.OciSdk;
import com.safarifone.oci.ProgressEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.activation.MimetypesFileTypeMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_FILE = 4;

    private TextView tvUploadProgress;

    private final CompositeDisposable disposableMap = new CompositeDisposable();
    private OciSdk ociSdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View btnUpload = findViewById(R.id.btnUpload);
        ProgressBar progressBarUpload = findViewById(R.id.progressUpload);
        progressBarUpload.setVisibility(View.GONE);
        tvUploadProgress = findViewById(R.id.tvUploadProgress);

        btnUpload.setOnClickListener(this::startUpload);

        try {
            initOciSdk();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startUpload(View view) {
        pickFile();
    }

    private void doUpload(String filePath) {

        String uploadId = UUID.randomUUID().toString();
        String objectName = UUID.randomUUID().toString();

        Log.d("nt.dung", String.format("Upload: (%s) (%s)", uploadId, objectName));

        long timeMs = System.currentTimeMillis();
        disposableMap.add(ociSdk.upload(uploadId, filePath, objectName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    Log.d("nt.dung", "Upload OK!!!");
                    long duration = (System.currentTimeMillis() - timeMs);
                    Toast.makeText(this, "Upload time: " + TimeUnit.MILLISECONDS.toSeconds(duration) + "s", Toast.LENGTH_LONG).show();
                    tvUploadProgress.setText("Total upload time: " + TimeUnit.MILLISECONDS.toSeconds(duration) + "s");
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(MainActivity.this, "Failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
        );

        disposableMap.add(ociSdk.observableProgress(uploadId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ProgressEvent>() {
                    @Override
                    public void accept(ProgressEvent progressEvent) throws Exception {
                        tvUploadProgress.setText(progressEvent.toString());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("nt.dung", "Tracking progress error");
                    }
                }));

    }

    @Override
    protected void onDestroy() {
        disposableMap.dispose();
        super.onDestroy();
    }

    private void initOciSdk() throws IOException {
        InputStream configStream = getAssets().open("oci.txt");
        final ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configStream, null);

        ociSdk = new OciSdk();
        ociSdk.initialize(this, config.get("tenancy"), config.get("user"), config.get("fingerprint"), Region.UK_LONDON_1, "Staging");


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