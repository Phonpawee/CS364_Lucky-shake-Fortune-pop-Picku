package com.cs364.luckyshake.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cs364.luckyshake.data.PrefKeys;

import java.util.Map;
import java.util.WeakHashMap;

public final class FontSizeHelper {
    private static final Map<View, Float> ORIGINAL_SP = new WeakHashMap<>();

    private FontSizeHelper() {
    }

    public static void apply(Context context, View root) {
        SharedPreferences preferences = context.getSharedPreferences(
                PrefKeys.PREFS_NAME, Context.MODE_PRIVATE);

        int level = preferences.getInt(PrefKeys.FONT_SIZE, 5);
        level = Math.max(1, Math.min(level, 10));

        float scale = 0.8f + (level - 1) * 0.05f;
        float scaledDensity =
                context.getResources().getDisplayMetrics().scaledDensity;

        applyToView(root, scale, scaledDensity);
    }

    private static void applyToView(
            View view, float scale, float scaledDensity) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            float originalSp;

            synchronized (ORIGINAL_SP) {
                Float savedSize = ORIGINAL_SP.get(view);
                if (savedSize == null) {
                    savedSize = textView.getTextSize() / scaledDensity;
                    ORIGINAL_SP.put(view, savedSize);
                }
                originalSp = savedSize;
            }

            textView.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP, originalSp * scale);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToView(group.getChildAt(i), scale, scaledDensity);
            }
        }
    }
}