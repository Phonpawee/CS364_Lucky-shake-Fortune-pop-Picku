package com.cs364.luckyshake.ui.result;

import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

final class ResultGallery {
    private ResultGallery() { }

    static void save(Context context, File snapshot) throws IOException {
        String name = "LuckyShake-" + UUID.randomUUID() + ".png";
        if (Build.VERSION.SDK_INT < 29) {
            saveLegacy(context, snapshot, name);
            return;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LuckyShake");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Cannot create gallery entry");
        try {
            try (OutputStream output = context.getContentResolver().openOutputStream(uri, "w")) {
                copy(snapshot, output);
            }
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (context.getContentResolver().update(uri, values, null, null) == 0) {
                throw new IOException("Cannot publish image");
            }
        } catch (IOException | RuntimeException error) {
            try { context.getContentResolver().delete(uri, null, null); }
            catch (RuntimeException ignored) { /* Preserve the original error. */ }
            throw error;
        }
    }

    @SuppressWarnings("deprecation")
    private static void saveLegacy(Context context, File snapshot, String name) throws IOException {
        File folder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "LuckyShake");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Cannot create folder");
        File image = new File(folder, name);
        try {
            try (OutputStream output = new FileOutputStream(image)) { copy(snapshot, output); }
            MediaScannerConnection.scanFile(context, new String[]{image.getPath()},
                    new String[]{"image/png"}, null);
        } catch (IOException | RuntimeException error) {
            image.delete();
            throw error;
        }
    }

    private static void copy(File source, OutputStream output) throws IOException {
        if (output == null) throw new IOException("No output stream");
        try (FileInputStream input = new FileInputStream(source)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        output.flush();
    }
}
