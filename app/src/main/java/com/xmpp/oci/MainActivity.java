package com.xmpp.oci;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Supplier;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest;
import com.oracle.bmc.objectstorage.requests.CreateMultipartUploadRequest;
import com.oracle.bmc.objectstorage.responses.CreateMultipartUploadResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            upload();
        } catch (IOException e) {
            Log.e("nt.dung", "Error -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void upload() throws IOException {
        InputStream configStream = getAssets().open("oci.txt");
        final ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configStream, null);

        boolean copyPem = copyFileTo(this, "oci.pem", getCacheDir().getAbsolutePath() + "/oci.pem");

        Supplier<InputStream> privateKeySupplier
                = new SimplePrivateKeySupplier(getCacheDir().getAbsolutePath() + "/oci.pem");

        AuthenticationDetailsProvider provider
                = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(config.get("tenancy"))
                .userId(config.get("user"))
                .fingerprint(config.get("fingerprint"))
                .region(Region.UK_LONDON_1)
                .privateKeySupplier(privateKeySupplier)
                .build();

        ObjectStorage client = new ObjectStorageClient(provider);

        CreateMultipartUploadResponse multipartRequest =
                client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucketName("Test")
                        .build());


        Log.d("nt.dung", "Result: " + multipartRequest.getOpcClientRequestId());

        // Put Object

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
}