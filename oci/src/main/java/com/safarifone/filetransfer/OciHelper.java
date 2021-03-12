package com.safarifone.filetransfer;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import javax.annotation.Nullable;

public class OciHelper {
    public static String copyFileTo(Context c, String assetFileName, String destFile) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(destFile);
        myInput = c.getAssets().open(assetFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }

        myOutput.flush();
        myInput.close();
        myOutput.close();

        return destFile;
    }

    public static String toPrettySize(long sizeInBytes) {
        double sizeInKb = sizeInBytes / 1024f;

        double sizeInMb = sizeInKb / 1024f;

        DecimalFormat format = new DecimalFormat("##.##");
        if (sizeInMb >= 1) {
            return format.format(sizeInMb) + " MB";
        } else if (sizeInKb >= 1) {
            return format.format(sizeInKb) + " KB";
        } else {
            return sizeInBytes + " B";
        }
    }

    public static @Nullable
    String getMimeTypeFrom(String filePath) {
        String ext = getFileExtension(filePath);
        if (ext == null) {
            return null;
        }
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(ext.toLowerCase());
    }

    public static String getFileExtension(String sourcePath) {
        String fileExtension = null;
        if (!TextUtils.isEmpty(sourcePath)) {
            String trim = sourcePath.trim();
            fileExtension = trim.substring(trim.lastIndexOf(".") + 1);
        }
        return fileExtension;
    }
}
