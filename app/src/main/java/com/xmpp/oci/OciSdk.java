package com.xmpp.oci;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Supplier;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.OCID;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.retrier.RetryConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OciSdk {

    public OciSdk() {

    }

    public void testUpload(Context context, String filePath, String objectName) throws IOException {

        String pemFilePath = copyFileTo(context, context.getCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".pem");

        Log.d("nt.dung", "File: " + pemFilePath);
        Supplier<InputStream> keySupplier = new SimplePrivateKeySupplier(pemFilePath);

        SimpleAuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                .tenantId("ocid1.tenancy.oc1..aaaaaaaaam4tx4gnec3sq755rcw7y5snrkgxcwnn2gb4efctsm3zfa4lf7ea".trim())
                .userId("ocid1.user.oc1..aaaaaaaaaflvckuurvvagas45heubr6tqptw22jcypozspwqo473ow6fbm3q".trim())
                .fingerprint("01:b8:73:61:ec:58:db:30:48:62:4f:78:2a:33:87:fe".trim())
                .region(Region.UK_LONDON_1)
                .privateKeySupplier(keySupplier)
                .build();

        ClientConfiguration clientConfig = ClientConfiguration.builder()
                .retryConfiguration(RetryConfiguration.builder().build())
                .connectionTimeoutMillis(3000)
                .readTimeoutMillis(60000)
                .build();

        ObjectStorageClient client = new ObjectStorageClient(provider, clientConfig);

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .opcClientRequestId(UUID.randomUUID().toString())
                        .namespaceName(getNamespace())
                        .bucketName("Staging")
                        .objectName(UUID.randomUUID().toString())
                        .contentLength(4L)
                        .putObjectBody(
                                new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)))
                        .build();
        client.putObject(putObjectRequest);


    }


    public String getNamespace() {
        return "lrl0eb8o6say";
    }

    public static String copyFileTo(Context c, String destFile) {
        InputStream input;
        try {
            OutputStream output = new FileOutputStream(new File(destFile));
            input = c.getResources().openRawResource(R.raw.pem);
            int size = input.available();
            byte[] buffer = new byte[size];
            int length = input.read(buffer);
            output.write(buffer, 0, length);
            Log.d("nt.dung", "Length of pem file: " + length);

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
