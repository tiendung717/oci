package com.xmpp.oci;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Supplier;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetBucketResponse;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Disposable disposable = Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<Boolean> emitter) throws Exception {
                upload();
            }
        }).subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        Log.d("nt.dung", "Success -> ");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("nt.dung", "Error -> " + throwable.getMessage());
                    }
                });
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

        final String compartmentId = provider.getTenantId();
        final String bucket = UUID.randomUUID().toString();
        final String object = "File";

        Log.d("nt.dung","Getting the namespace.");
        GetNamespaceResponse namespaceResponse = client.getNamespace(GetNamespaceRequest.builder().build());
        String namespaceName = namespaceResponse.getValue();

        Log.d("nt.dung","Creating the source bucket.");
        CreateBucketDetails createSourceBucketDetails =
                CreateBucketDetails.builder()
                        .compartmentId(compartmentId)
                        .name(bucket).build();
        CreateBucketRequest createSourceBucketRequest =
                CreateBucketRequest.builder()
                        .namespaceName(namespaceName)
                        .createBucketDetails(createSourceBucketDetails)
                        .build();
        client.createBucket(createSourceBucketRequest);

        Log.d("nt.dung","Creating the source object");
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .namespaceName(namespaceName)
                        .bucketName(bucket)
                        .objectName(object)
                        .contentLength(4L)
                        .putObjectBody(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)))
                        .build();
        client.putObject(putObjectRequest);

        Log.d("nt.dung","Creating Get bucket request");
        List<GetBucketRequest.Fields> fieldsList = new ArrayList<>(2);
        fieldsList.add(GetBucketRequest.Fields.ApproximateCount);
        fieldsList.add(GetBucketRequest.Fields.ApproximateSize);
        GetBucketRequest request =
                GetBucketRequest.builder()
                        .namespaceName(namespaceName)
                        .bucketName(bucket)
                        .fields(fieldsList)
                        .build();

        Log.d("nt.dung","Fetching bucket details");
        GetBucketResponse response = client.getBucket(request);

        Log.d("nt.dung","Bucket Name : " + response.getBucket().getName());
        Log.d("nt.dung","Bucket Compartment : " + response.getBucket().getCompartmentId());
        Log.d("nt.dung",
                "The Approximate total number of objects within this bucket : "
                        + response.getBucket().getApproximateCount());
        Log.d("nt.dung",
                "The Approximate total size of objects within this bucket : "
                        + response.getBucket().getApproximateSize());

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