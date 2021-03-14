package com.safarifone.filetransfer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.common.base.Supplier;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.RetryOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.PublishSubject;

public class OciFileTransfer implements FileTransfer {

    private boolean initialized = false;
    private ObjectStorageClient service;
    private String bucketName;
    private String namespace;
    private UploadManager uploadManager;
    private AuthenticationDetailsProvider provider;

    private final PublishSubject<ProgressEvent> progressEventBus = PublishSubject.create();

    public OciFileTransfer() {

    }

    public void initialize(Context context, String bucketName) throws IOException {
        InputStream configStream = context.getAssets().open("oci.txt");
        final ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configStream, null);

        initialize(context, config.get("tenancy"), config.get("user"), config.get("fingerprint"), Region.UK_LONDON_1, bucketName);
    }

    public void initialize(Context context, String tenantId, String userId, String fingerprint, Region region, String bucketName) throws IOException {
        String pemFilePath = OciHelper.copyFileTo(context, "pem.txt", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/pem.txt");

        Log.d("nt.dung", "File: " + pemFilePath);
        Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(pemFilePath);

        provider = SimpleAuthenticationDetailsProvider.builder()
                .tenantId("ocid1.tenancy.oc1..aaaaaaaaam4tx4gnec3sq755rcw7y5snrkgxcwnn2gb4efctsm3zfa4lf7ea")
                .userId("ocid1.user.oc1..aaaaaaaaaflvckuurvvagas45heubr6tqptw22jcypozspwqo473ow6fbm3q")
                .fingerprint("01:b8:73:61:ec:58:db:30:48:62:4f:78:2a:33:87:fe")
                .privateKeySupplier(privateKeySupplier)
                .build();

        Log.d("nt.dung", "tenant: " + provider.getTenantId());
        Log.d("nt.dung", "user: " + provider.getUserId());
        Log.d("nt.dung", "fingerprint: " + provider.getFingerprint());
        this.service = new ObjectStorageClient(provider);
        this.service.setRegion(Region.UK_LONDON_1);

        this.bucketName = bucketName;
        this.uploadManager = getUploadManager(service);

        this.initialized = true;
    }

    public void testUpload(Context context, String filePath, String objectName) throws IOException {

        String pemFilePath = OciHelper.copyFileTo(context, "pem.txt", context.getCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".pem");

        Log.d("nt.dung", "File: " + pemFilePath);
        Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(pemFilePath);

        AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                .tenantId("ocid1.tenancy.oc1..aaaaaaaaam4tx4gnec3sq755rcw7y5snrkgxcwnn2gb4efctsm3zfa4lf7ea")
                .userId("ocid1.user.oc1..aaaaaaaaaflvckuurvvagas45heubr6tqptw22jcypozspwqo473ow6fbm3q")
                .fingerprint("01:b8:73:61:ec:58:db:30:48:62:4f:78:2a:33:87:fe")
                .privateKeySupplier(privateKeySupplier)
                .build();

        ClientConfiguration clientConfig
                = ClientConfiguration.builder()
                .retryConfiguration(RetryConfiguration.builder()
                        .build())
                .connectionTimeoutMillis(3000)
                .readTimeoutMillis(60000)
                .build();

        ObjectStorageClient client = new ObjectStorageClient(provider, clientConfig);
        client.setRegion(Region.UK_LONDON_1);

        GetNamespaceResponse namespaceResponse = client.getNamespace(GetNamespaceRequest.builder()
                .compartmentId(provider.getTenantId())
                .build());
        String namespace = namespaceResponse.getValue();
        Log.d("nt.dung", "Namespace: " + namespace);

        Log.d("nt.dung", "end point: " + client.getEndpoint());

        File file = new File(filePath);
        long contentLength = file.length();
        String contentType = "image/png";

        // configure upload settings as desired
        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        UploadManager uploadManager = new UploadManager(client, uploadConfiguration);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucketName("Staging")
                .namespaceName(namespace)
                .objectName(objectName)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        UploadManager.UploadRequest uploadRequest = UploadManager.UploadRequest.builder(file)
                .allowOverwrite(true)
                .parallelUploadExecutorService(Executors.newFixedThreadPool(4))
                .build(request);


        try {
            UploadManager.UploadResponse response = uploadManager
                    .upload(uploadRequest);
            Log.d("nt.dung", ">>>>> result: " + response);
        } catch (BmcException e) {
            String requestId = e.getOpcRequestId();
            Log.d("nt.dung", e.getMessage() + ", requestId: " + requestId);
            e.printStackTrace();
        }


    }

    @Override
    public Single<Boolean> upload(String transId, String filePath, String objectName) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<Boolean> emitter) throws Exception {
                if (!initialized) {
                    emitter.onError(new Throwable("OciSdk.initialize() must be called first."));
                    return;
                }

                File file = new File(filePath);
                long contentLength = file.length();
                String contentType = OciHelper.getMimeTypeFrom(filePath);
                UploadManager.UploadResponse response = doUpload(transId, objectName, contentType, contentLength, file);
                emitter.onSuccess(Boolean.TRUE);
            }
        });
    }

    @Override
    public Single<Boolean> download(String transId, String filePath, String objectName) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<Boolean> emitter) throws Exception {
                if (!initialized) {
                    emitter.onError(new Throwable("OciSdk.initialize() must be called first."));
                    return;
                }
                doDownload(transId, objectName, filePath);
                emitter.onSuccess(Boolean.TRUE);
            }
        });
    }

    @Override
    public Observable<ProgressEvent> observableProgress(String transId) {
        return progressEventBus.filter(progressEvent -> progressEvent.getId().equals(transId));
    }

    @Override
    public Observable<ProgressEvent> observableProgress() {
        return progressEventBus;
    }

    private UploadManager.UploadResponse doUpload(String uploadId, String objectName, String contentType, long contentLength, File file) {
        Log.d("nt.dung", "NS: " + getNamespace());


        // configure upload settings as desired
        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        UploadManager uploadManager = new UploadManager(service, uploadConfiguration);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucketName(bucketName)
                .namespaceName(getNamespace())
                .objectName(objectName)
//                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        UploadManager.UploadRequest uploadRequest = UploadManager.UploadRequest.builder(file)
                .allowOverwrite(true)
                .build(request);


        return uploadManager.upload(uploadRequest);
    }

    private void doDownload(String downloadId, String objectName, String filePath) throws IOException {
        GetObjectResponse getResponse = service.getObject(
                GetObjectRequest.builder()
                        .namespaceName(getNamespace())
                        .bucketName(bucketName)
                        .objectName(objectName)
                        .build());

        InputStream downStream = getResponse.getInputStream();

        OutputStream output = new FileOutputStream(filePath);
        long totalBytes = getResponse.getContentLength();
        long byteRecv = 0;

        byte[] buffer = new byte[4096];
        int recvBytes = downStream.read(buffer);
        while (recvBytes > 0) {
            byteRecv += recvBytes;
            output.write(buffer, 0, recvBytes);
            recvBytes = downStream.read(buffer);
            progressEventBus.onNext(new ProgressEvent(totalBytes, byteRecv, downloadId));
        }

        output.flush();
        output.close();
        downStream.close();
    }

    private UploadManager getUploadManager(ObjectStorage service) {
        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        return new UploadManager(service, uploadConfiguration);
    }

    public String getNamespace() {
//        return "lrl0eb8o6say";
        if (namespace == null) {
            Log.d("nt.dung", "Namespace: +++");
            GetNamespaceResponse namespaceResponse = service.getNamespace(GetNamespaceRequest.builder()
                    .compartmentId(provider.getTenantId())
                    .build());
            this.namespace = namespaceResponse.getValue();
            Log.d("nt.dung", "Namespace: " + namespace);
        }
        Log.d("nt.dung", ">> Namespace: " + namespace);
        return namespace;
    }
}
