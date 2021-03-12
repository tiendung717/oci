package com.safarifone.oci;

import android.content.Context;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.transfer.ProgressReporter;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;

public class OciSdk {

    private boolean initialized = false;
    private ObjectStorage service;
    private AuthenticationDetailsProvider provider;
    private String bucketName;
    private String namespace;
    private UploadManager uploadManager;

    private PublishSubject<ProgressEvent> progressEventBus = PublishSubject.create();

    public void initialize(Context context, String tenantId, String userId, String fingerprint, Region region, String bucketName) throws IOException {
        String pemFilePath = OciHelper.copyFileTo(context, "oci.pem", context.getCacheDir().getAbsolutePath() + "/oci.pem");
        Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(pemFilePath);

        this.provider = SimpleAuthenticationDetailsProvider.builder()
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

    public String getNamespace() {
        if (namespace == null) {
            GetNamespaceResponse namespaceResponse = service.getNamespace(GetNamespaceRequest.builder().build());
            this.namespace = namespaceResponse.getValue();
        }
        return namespace;
    }

    public Single<Boolean> upload(String uploadId, String filePath, String objectName) {
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
                UploadManager.UploadResponse response = doUpload(uploadId, objectName, contentType, contentLength, stream);
                emitter.onSuccess(Boolean.TRUE);
            }
        });
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

    public Observable<ProgressEvent> observableProgress(String id) {
        return progressEventBus.filter(progressEvent -> progressEvent.getId().equals(id));
    }
}
