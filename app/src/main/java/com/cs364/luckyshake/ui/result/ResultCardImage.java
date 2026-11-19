package com.cs364.luckyshake.ui.result;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import java.util.LinkedHashMap;
import java.util.Map;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Renders the full result screen, including content outside its scroll viewports. */
final class ResultCardImage {
    private ResultCardImage() { }

    // Bound memory use on high-density tablets and unusually long fortunes.
    static void capture(View page, File target) throws IOException {
        Bitmap bitmap = render(page);
        try {
            writePng(bitmap, target);
        } finally {
            bitmap.recycle();
        }
    }

    static Bitmap render(View page) throws IOException {
        int width = page.getWidth();
        int height = page.getHeight();
        if (width <= 0 || height <= 0) throw new IOException("Result page is not laid out");

        Map<ScrollView, Integer> scrollPositions = new LinkedHashMap<>();
        int overflow = findScrollOverflow(page, scrollPositions);
        try {
            if (overflow > 0) layoutAtHeight(page, width, height + overflow);
            scrollPositions.keySet().forEach(scroll -> scroll.scrollTo(0, 0));
            return drawPage(page);
        } finally {
            if (overflow > 0) layoutAtHeight(page, width, height);
            scrollPositions.forEach((scroll, position) -> scroll.scrollTo(0, position));
        }
    }

    private static int findScrollOverflow(View view, Map<ScrollView, Integer> positions) {
        if (view instanceof ScrollView) {
            ScrollView scroll = (ScrollView) view;
            View child = scroll.getChildAt(0);
            positions.put(scroll, scroll.getScrollY());
            return child == null ? 0 : Math.max(0,
                    child.getBottom() + scroll.getPaddingBottom() - scroll.getHeight());
        }
        int overflow = 0;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    overflow = Math.max(overflow, findScrollOverflow(child, positions));
                }
            }
        }
        return overflow;
    }

    private static void layoutAtHeight(View page, int width, int height) {
        page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        page.layout(page.getLeft(), page.getTop(), page.getLeft() + width, page.getTop() + height);
    }

    private static Bitmap drawPage(View page) {
        int width = page.getWidth();
        int height = page.getHeight();
        float scale = Math.min(1f, Math.min(2048f / width,
                (float) Math.sqrt(4_000_000d / ((double) width * height))));
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, Math.round(width * scale)),
                Math.max(1, Math.round(height * scale)), Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(scale, scale);
            page.draw(canvas);
            return bitmap;
        } catch (RuntimeException | Error failure) {
            bitmap.recycle();
            throw failure;
        }
    }

    static void writePng(Bitmap bitmap, File target) throws IOException {
        try (FileOutputStream output = new FileOutputStream(target)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("PNG encoding failed");
            }
        }
    }
}
