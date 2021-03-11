package com.xmpp.oci;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.transfer.ProgressReporter;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class OciManager {

    public OciCredential createOciCredential(String tenantId, String userId, String fingerprint, Region region, String pemFilePath) {
        Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(pemFilePath);

        AuthenticationDetailsProvider provider
                = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(tenantId)
                .userId(userId)
                .fingerprint(fingerprint)
                .region(region)
                .privateKeySupplier(privateKeySupplier)
                .build();

        return new OciCredential(new ObjectStorageClient(provider), provider);
    }

    public OciBucket createUploadBucket(OciCredential credential,
                                        String objectName,
                                        String contentType,
                                        long contentLength) {
        String namespaceName = credential.getNamespace();
        String bucketName = UUID.randomUUID().toString();

        CreateBucketDetails createSourceBucketDetails = CreateBucketDetails.builder()
                .compartmentId(credential.getTenantId())
                .name(bucketName)
                .build();

        CreateBucketRequest createSourceBucketRequest = CreateBucketRequest.builder()
                .namespaceName(namespaceName)
                .createBucketDetails(createSourceBucketDetails)
                .build();

        CreateBucketResponse response = credential.getService().createBucket(createSourceBucketRequest);

        OciBucket ociBucket = new OciBucket();
        ociBucket.setBucketName(response.getBucket().getName());
        ociBucket.setObjectName(objectName);
        ociBucket.setContentLength(contentLength);
        ociBucket.setContentType(contentType);
        return ociBucket;
    }

    private UploadManager getUploadManager(ObjectStorage service) {
        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        return new UploadManager(service, uploadConfiguration);
    }

    public UploadManager.UploadResponse upload(OciCredential credential, OciBucket bucket, String uploadFilePath, ProgressReporter progressReporter) {
        String namespaceName = credential.getNamespace();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucketName(bucket.getBucketName())
                .namespaceName(namespaceName)
                .objectName(bucket.getObjectName())
                .contentType(bucket.getContentType())
                .contentLength(bucket.getContentLength())
                .build();

        UploadManager.UploadRequest uploadRequest = UploadManager.UploadRequest.builder(new File(uploadFilePath))
                .progressReporter(progressReporter)
                .allowOverwrite(true)
                .build(request);

        return getUploadManager(credential.getService()).upload(uploadRequest);
    }

    public void download(OciCredential credential, OciBucket bucket, String downloadFilePath, ProgressReporter progressReporter) throws IOException {
        String namespaceName = credential.getNamespace();

        GetObjectResponse getResponse = credential.getService().getObject(GetObjectRequest.builder()
                .namespaceName(namespaceName)
                .bucketName(bucket.getBucketName())
                .objectName(bucket.getObjectName())
                .build());

        OutputStream outputStream = new FileOutputStream(downloadFilePath);
        InputStream fileStream = getResponse.getInputStream();

        byte[] buffer = new byte[2048];
        long byteRead = 0;
        int length = fileStream.read(buffer);
        while (length > 0) {
            byteRead += length;
            outputStream.write(buffer, 0, length);
            length = fileStream.read(buffer);
            progressReporter.onProgress(byteRead, fileStream.available());
        }

        outputStream.flush();
        outputStream.close();
        fileStream.close();
    }
}
