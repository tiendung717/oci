package com.safarifone.oci;

import java.util.Locale;

public class ProgressEvent {
    private final long totalBytes;
    private final long bytesTransfer;
    private final String id;

    public ProgressEvent(long totalBytes, long bytesTransfer, String id) {
        this.totalBytes = totalBytes;
        this.bytesTransfer = bytesTransfer;
        this.id = id;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getBytesTransfer() {
        return bytesTransfer;
    }

    public String getId() {
        return id;
    }

    public int getPercentProgress() {
        return (int) (bytesTransfer * 100 / totalBytes);
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s/%s (%d%%)", OciHelper.toPrettySize(bytesTransfer), OciHelper.toPrettySize(totalBytes), getPercentProgress());
    }
}
