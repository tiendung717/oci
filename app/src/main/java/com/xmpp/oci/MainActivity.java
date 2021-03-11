package com.xmpp.oci;

import android.app.Activity;
import android.content.Context;
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
import com.oracle.bmc.objectstorage.transfer.UploadManager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_FILE = 4;

    private TextView tvUploadProgress;
    private View btnUpload;
    private ProgressBar progressBarUpload;

    private ProgressReporter uploadReporter = new ProgressReporter() {
        @Override
        public void onProgress(long byteSent, long total) {
            int progress = (int) ((byteSent * 100) / total);
            tvUploadProgress.setText(String.format(Locale.getDefault(), "%s/%s (%d%%)", toPrettySize(byteSent), toPrettySize(total), progress));
            progressBarUpload.setProgress(progress);

        }
    };

    private ProgressReporter downloadReporter = new ProgressReporter() {
        @Override
        public void onProgress(long byteRecv, long total) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        progressBarUpload = findViewById(R.id.progressUpload);
        tvUploadProgress = findViewById(R.id.tvUploadProgress);

        btnUpload.setOnClickListener(this::startUpload);
    }

    private void startUpload(View view) {
        pickFile();
    }

    private void doUpload(String filePath) {
        Disposable d = Single.create(emitter -> upload(filePath))
                .subscribeOn(Schedulers.io())
                .subscribe(aBoolean -> Log.d("nt.dung", "Upload OK!!!"));
    }

    private void upload(String filePath) throws IOException {
        InputStream configStream = getAssets().open("oci.txt");
        final ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configStream, null);

        copyFileTo(this, "oci.pem", getCacheDir().getAbsolutePath() + "/oci.pem");

        OciManager ociManager = new OciManager();
        OciCredential credential = ociManager.createOciCredential(
                config.get("tenancy"),
                config.get("user"),
                config.get("fingerprint"),
                Region.UK_LONDON_1,
                getCacheDir().getAbsolutePath() + "/oci.pem"
        );

        InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
        long contentLength = inputStream.available();
        String objectName = "Waafi_Test_" + System.currentTimeMillis();

        progressBarUpload.setMax(100);

        UploadManager.UploadResponse response = ociManager.upload(credential, objectName, null, contentLength, inputStream, uploadReporter);
    }

    static public boolean copyFileTo(Context c, String orifile,
                                     String desfile) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(desfile);
        myInput = c.getAssets().open(orifile);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }

        myOutput.flush();
        myInput.close();
        myOutput.close();

        return true;
    }


    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_FILE);
    }

    public String toPrettySize(long sizeInBytes) {
        double sizeInKb = sizeInBytes / 1024f;

        double sizeInMb = sizeInKb / 1024f;

        DecimalFormat format = new DecimalFormat("##.##");
        if (sizeInMb >= 1) {
            return format.format(sizeInMb) + " MB";
        } else if (sizeInKb >= 1) {
            return format.format(sizeInKb) + " KB";
        } else {
            return sizeInBytes + " B";
        }
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