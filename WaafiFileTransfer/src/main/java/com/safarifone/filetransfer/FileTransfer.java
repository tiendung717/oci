package com.safarifone.filetransfer;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface FileTransfer {
    Single<Boolean> upload(String transId, String filePath, String objectName);
    Single<Boolean> download(String transId, String filePath, String objectName);
    Observable<ProgressEvent> observableProgress(String transId);
    Observable<ProgressEvent> observableProgress();
}
