package com.xmpp.oci;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.common.base.Supplier;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.retrier.RetryConfiguration;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class OciSdk {

    private static final String USER = "ocid1.user.oc1..aaaaaaaa6xeqlugwqp6oreyyz6argwmjo77oah6n52xgz3fjzzz5ccyof5eq";
    private static final String TENANCY = "ocid1.tenancy.oc1..aaaaaaaagz7lkgw2p42gjrlkd4xec2qkdsayghdeq3gbjghoqenfm6twugkq";
    private static final String FINGERPRINT = "85:f5:98:d4:0c:15:08:b4:d2:3e:2c:93:ef:33:d9:9e";
    private static final Region REGION = Region.UK_CARDIFF_1;
    private static final String PEM_FILE_NAME = "pem2.pem";

    public OciSdk() {

    }

    public void testUpload(Context context, String filePath, String objectName) throws IOException {

//        String pemFilePath = copyFileTo(context, context.getCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis() + "_oci_key.pem", PEM_FILE_NAME);

        Supplier<InputStream> keySupplier = new SimplePrivateKeySupplier(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + PEM_FILE_NAME);
        SimpleAuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(TENANCY)
                .userId(USER)
                .fingerprint(FINGERPRINT)
                .region(REGION)
                .privateKeySupplier(keySupplier)
                .build();

        Log.d("nt.dung", "Provider: " + provider.toString());

        ClientConfiguration clientConfig = ClientConfiguration.builder()
                .retryConfiguration(RetryConfiguration.builder().build())
                .connectionTimeoutMillis(3000)
                .readTimeoutMillis(60000)
                .build();

        ObjectStorage client = new ObjectStorageClient(provider, clientConfig);

        File file = new File(filePath);

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .opcClientRequestId(UUID.randomUUID().toString())
                        .namespaceName(getNamespace(client, provider))
                        .bucketName("Staging")
                        .objectName(UUID.randomUUID().toString())
                        .contentLength(file.length())
                        .build();

        UploadManager.UploadRequest uploadRequest = UploadManager.UploadRequest.builder(file)
                .allowOverwrite(true)
                .build(putObjectRequest);

        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        UploadManager uploadManager = new UploadManager(client, uploadConfiguration);
        uploadManager.upload(uploadRequest);

        Log.d("nt.dung", "Uploaded!!!");
    }


    public String getNamespace(ObjectStorage client, AuthenticationDetailsProvider provider) {
        GetNamespaceResponse namespaceResponse = client.getNamespace(GetNamespaceRequest.builder()
                .compartmentId(provider.getTenantId())
                .build());
        return namespaceResponse.getValue();
    }

    public static String copyFileTo(Context c, String destFile, String pemFileName) {
        String pemFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + pemFileName;

        try {
            File pemFile = new File(pemFilePath);
            OutputStream output = new FileOutputStream(new File(destFile));
            DataInputStream input = new DataInputStream(new FileInputStream(pemFile));
            long size = pemFile.length();

            byte[] buffer = new byte[(int) size];
            input.readFully(buffer);
            output.write(buffer, 0, (int) size);
            Log.d("nt.dung", "Length of pem1.pem file: " + size);

            output.flush();
            input.close();
            output.close();
        } catch (Exception e) {
            Log.e("nt.dung", "Can't copy asset file. " + e.getMessage());
            return null;
        }
        return destFile;
    }
}
