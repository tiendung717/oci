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
import androidx.core.os.EnvironmentCompat;

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
    private TextView tvDownloadProgress;
    private View btnDownload;
    private View btnUpload;

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

        ProgressBar progressBarUpload = findViewById(R.id.progressUpload);
        progressBarUpload.setVisibility(View.GONE);
        tvUploadProgress = findViewById(R.id.tvUploadProgress);
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress);

        btnUpload.setOnClickListener(this::startUpload);
        btnDownload.setOnClickListener(this::startDownload);
        try {
            initOciSdk();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startDownload(View view) {
        String downloadId = UUID.randomUUID().toString();
        Log.d("nt.dung", String.format("Download: (%s) (%s)", downloadId, objectName));
        long timeMs = System.currentTimeMillis();

        File file = new File(uploadFilePath);
        String downloadFileName = "copy_" + file.getName();
        String downloadFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + downloadFileName;
        disposableMap.add(
                ociSdk.download(downloadId, downloadFilePath, objectName)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            long duration = (System.currentTimeMillis() - timeMs);
                            Toast.makeText(this, "Download time: " + TimeUnit.MILLISECONDS.toSeconds(duration) + "s", Toast.LENGTH_LONG).show();
                            tvDownloadProgress.setText("Total download time: " + TimeUnit.MILLISECONDS.toSeconds(duration) + "s");
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Toast.makeText(MainActivity.this, "Failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        })
        );

        disposableMap.add(ociSdk.observableProgress(downloadId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ProgressEvent>() {
                    @Override
                    public void accept(ProgressEvent progressEvent) throws Exception {
                        Log.e("nt.dung", "Download progress: " + progressEvent.toString());
                        tvDownloadProgress.setText(progressEvent.toString());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("nt.dung", "Tracking progress error. " + throwable.getMessage());
                    }
                }));
    }

    private void startUpload(View view) {
        pickFile();
    }

    private void doUpload(String filePath) {
        uploadFilePath = filePath;

        String uploadId = UUID.randomUUID().toString();
        objectName = UUID.randomUUID().toString();

        Log.d("nt.dung", String.format("Upload: (%s) (%s)", uploadId, objectName));

        long timeMs = System.currentTimeMillis();
        disposableMap.add(ociSdk.upload(uploadId, filePath, objectName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    long duration = (System.currentTimeMillis() - timeMs);
                    Toast.makeText(this, "Upload time: " + TimeUnit.MILLISECONDS.toSeconds(duration) + "s", Toast.LENGTH_LONG).show();
                    tvUploadProgress.setText("Total upload time: " + TimeUnit.MILLISECONDS.toSeconds(duration) + "s");
                    btnDownload.setEnabled(true);
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