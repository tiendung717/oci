package com.safarifone.filetransfer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.safarifone.oci.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import javax.annotation.Nullable;

public class OciHelper {
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
            input.close();
            output.close();
        } catch (Exception e) {
            Log.e("nt.dung", "Can't copy asset file. " + e.getMessage());
            return null;
        }
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
