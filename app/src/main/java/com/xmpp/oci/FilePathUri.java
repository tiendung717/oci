package com.xmpp.oci;

/**
 * Created by Umer on 10/10/2017.
 */

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.yalantis.ucrop.util.FileUtils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// refer from https://gist.github.com/jaisoni/4d4e20409849dd3cfa12ee15895124a6
public class FilePathUri {


    public static final String DOCUMENTS_DIR = "documents";

    /***
     * get actual path from provided file uri
     *
     * @param context application context
     * @param uri     file uri
     */
    public static String getPath(Context context, Uri uri) {

        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isVirtualFile(context, uri)) {
                return getVirtualFile(context, uri);
            } else if (isLocalStorageDocument(uri)) {
                // LocalStorageProvider
                return DocumentsContract.getDocumentId(uri);
            } else if (FileUtils.isExternalStorageDocument(uri)) {
                // ExternalStorageProvider
                return getPathFromExternalStorage(context, uri);
            } else if (FileUtils.isDownloadsDocument(uri)) {
                // DownloadsProvider
                return getPathFromDownloads(context, uri);
            } else if (FileUtils.isMediaDocument(uri)) {
                // MediaProvider
                return getPathFromMedia(context, uri);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (FileUtils.isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            String path = getDataColumn(context, uri, null, null);

            if (TextUtils.isEmpty(path)) {
                return getVirtualFile(context, uri);
            }
            return path;
        }

        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static boolean isVirtualFile(Context context, Uri uri) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        if (!DocumentsContract.isDocumentUri(context, uri)) {
            return false;
        }

        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{DocumentsContract.Document.COLUMN_FLAGS},
                null, null, null);

        int flags = 0;
        if (cursor.moveToFirst()) {
            flags = cursor.getInt(0);
        }
        cursor.close();

        return (flags & DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is
     */
    public static boolean isLocalStorageDocument(Uri uri) {
        return uri.getAuthority().equals(BuildConfig.APPLICATION_ID + ".provider");
    }

    private static String getVirtualFile(Context context, Uri uri) {
        String fileName = getFileName(context, uri);
        File cacheDir = getDocumentCacheDir(context);
        File file = generateFileName(fileName, cacheDir);
        ContentResolver resolver = context.getContentResolver();

        InputStream inputStream;
        try {
            inputStream = resolver.openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }

            inputStream.close();
            outputStream.close();
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    /**
     * get file path external storage
     *
     * @param context application context
     * @param uri     file uri
     **/
    public static String getPathFromExternalStorage(Context context, Uri uri) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
            return Environment.getExternalStorageDirectory() + "/" + split[1];
        } else {
            String filePath = "";
            //getExternalMediaDirs() added in API 21
            File[] external = context.getExternalMediaDirs();
            if (external.length > 1) {
                filePath = external[1].getAbsolutePath();
                filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1];
            }
            return filePath;
        }
    }

    /**
     * get file path in download database
     *
     * @param context application context
     * @param uri     file uri
     **/
    public static String getPathFromDownloads(Context context, Uri uri) {
        final String id = DocumentsContract.getDocumentId(uri);
        if (id != null && id.startsWith("raw:")) {
            return id.substring(4);
        }

        String[] contentUriPrefixesToTry = new String[]{
                "content://downloads/public_downloads",
                "content://downloads/my_downloads",
                "content://downloads/all_downloads"
        };

        for (String contentUriPrefix : contentUriPrefixesToTry) {
            Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));
            try {
                String path = getDataColumn(context, contentUri, null, null);
                if (path != null) {
                    return path;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String fileName = getFileName(context, uri);
        File cacheDir = getDocumentCacheDir(context);
        File file = generateFileName(fileName, cacheDir);
        String destinationPath = null;
        if (file != null) {
            destinationPath = file.getAbsolutePath();
            saveFileFromUri(context, uri, destinationPath);
        }

        return destinationPath;
    }

    public static String getFileName(@NonNull Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename = null;

        if (mimeType == null) {
            String path = getPath(context, uri);
            if (path == null) {
                filename = getName(uri.toString());
            } else {
                File file = new File(path);
                filename = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null,
                    null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }

        return filename;
    }

    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('/');
        return filename.substring(index + 1);
    }

    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            is.read(buf);
            do {
                bos.write(buf);
            } while (is.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    public static File generateFileName(@Nullable String name, File directory) {
        if (name == null) {
            return null;
        }

        File file = new File(directory, name);

        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }

            int index = 0;

            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }

        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }

        //logDir(directory);

        return file;
    }

    public static File getDocumentCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), DOCUMENTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    /**
     * get file path in media database
     *
     * @param context application context
     * @param uri     file uri
     **/
    public static String getPathFromMedia(Context context, Uri uri) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];
        Uri contentUri = null;
        switch (type) {
            case "image":
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                break;
            case "video":
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
            case "audio":
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                }
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[]{split[1]};

        return getDataColumn(context, contentUri, selection, selectionArgs);
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection,
                    selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            // fallback for Android 11.
            try {
                return fromPathFromUri(context, uri);
            } catch (IOException ioException) {
                return null;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static String fromPathFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        InputStream initialStream = new FileInputStream(descriptor.getFileDescriptor());

        File targetFile = File.createTempFile("waafi", "_share");
        OutputStream outStream = new FileOutputStream(targetFile);

        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = initialStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }
        IOUtils.closeQuietly(initialStream);
        IOUtils.closeQuietly(outStream);

        return targetFile.getPath();
    }
}