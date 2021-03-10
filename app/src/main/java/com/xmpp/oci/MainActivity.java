package com.xmpp.oci;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.CreateMultipartUploadRequest;
import com.oracle.bmc.objectstorage.responses.CreateMultipartUploadResponse;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            upload();
        } catch (IOException e) {
            Log.e("nt.dung", "Upload " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void upload() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("oci.ini");
        InputStream configStream = fileDescriptor.createInputStream();
        final ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configStream, null);
        final AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

        ObjectStorage client = new ObjectStorageClient(provider);

        CreateMultipartUploadResponse multipartRequest =
                client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucketName("Test")
                        .build());


        Log.d("nt.dung", "Result: " + multipartRequest.getOpcClientRequestId());

        // Put Object

    }
}