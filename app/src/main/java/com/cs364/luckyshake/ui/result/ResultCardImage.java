package com.cs364.luckyshake.ui.result;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Captures the whole measured card, including content outside the scroll viewport. */
final class ResultCardImage {
    private ResultCardImage() { }

    // Bound memory use on high-density tablets and unusually long fortunes.
    static void capture(View card, File target) throws IOException {
        int width = card.getWidth();
        int height = card.getHeight();
        if (width <= 0 || height <= 0) throw new IOException("Card is not laid out");
        float scale = Math.min(1f, Math.min(2048f / width,
                (float) Math.sqrt(4_000_000d / ((double) width * height))));
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, Math.round(width * scale)),
                Math.max(1, Math.round(height * scale)), Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(scale, scale);
            card.draw(canvas);
            try (FileOutputStream output = new FileOutputStream(target)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw new IOException("PNG encoding failed");
                }
            }
        } finally {
            bitmap.recycle();
        }
    }
}
