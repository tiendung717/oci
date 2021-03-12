package com.safarifone.filetransfer;

import android.content.Context;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.transfer.ProgressReporter;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.PublishSubject;

public class OciFileTransfer implements FileTransfer {

    private boolean initialized = false;
    private ObjectStorage service;
    private String bucketName;
    private String namespace;
    private UploadManager uploadManager;

    private final PublishSubject<ProgressEvent> progressEventBus = PublishSubject.create();

    public void initialize(Context context, String tenantId, String userId, String fingerprint, Region region, String bucketName) throws IOException {
        String pemFilePath = OciHelper.copyFileTo(context, "oci.pem", context.getCacheDir().getAbsolutePath() + "/oci.pem");
        Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(pemFilePath);

        AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(tenantId)
                .userId(userId)
                .fingerprint(fingerprint)
                .region(region)
                .privateKeySupplier(privateKeySupplier)
                .build();

        this.service = new ObjectStorageClient(provider);
        this.bucketName = bucketName;
        this.uploadManager = getUploadManager(service);

        this.initialized = true;
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

                InputStream stream = new BufferedInputStream(new FileInputStream(filePath));
                long contentLength = stream.available();
                String contentType = OciHelper.getMimeTypeFrom(filePath);
                UploadManager.UploadResponse response = doUpload(transId, objectName, contentType, contentLength, stream);
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

    private UploadManager.UploadResponse doUpload(String uploadId, String objectName, String contentType, long contentLength, InputStream inputStream) {
        ProgressReporter progressReporter = null;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucketName(bucketName)
                .namespaceName(getNamespace())
                .objectName(objectName)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        UploadManager.UploadRequest uploadRequest = UploadManager.UploadRequest.builder(inputStream, contentLength)
                .progressReporter((completed, total) -> progressEventBus.onNext(new ProgressEvent(total, completed, uploadId)))
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
                        .minimumLengthForMultipartUpload(50)
                        .lengthPerUploadPart(20)
                        .build();

        return new UploadManager(service, uploadConfiguration);
    }

    private String getNamespace() {
        if (namespace == null) {
            GetNamespaceResponse namespaceResponse = service.getNamespace(GetNamespaceRequest.builder().build());
            this.namespace = namespaceResponse.getValue();
        }
        return namespace;
    }
}
