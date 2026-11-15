package com.cs364.luckyshake.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.os.Build;

import com.cs364.luckyshake.data.PrefKeys;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class FontSizeHelper {
    private static final Map<View, Float> ORIGINAL_SP = new WeakHashMap<>();

    private FontSizeHelper() {
    }

    /**
     * Adapter for a Spinner whose selected row and drop-down rows follow the font size setting.
     * A Spinner creates its rows later, during layout, so applying the helper to the Spinner
     * once is not enough.
     */
    public static ArrayAdapter<String> spinnerAdapter(Context context, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                FontSizeHelper.apply(getContext(), view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                FontSizeHelper.apply(getContext(), view);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    public static void apply(Context context, View root) {
        SharedPreferences preferences = context.getSharedPreferences(
                PrefKeys.PREFS_NAME, Context.MODE_PRIVATE);

        int level = preferences.getInt(PrefKeys.FONT_SIZE, 5);
        float scale = 0.8f + (level - 1) * 0.05f;
        float scaledDensity = context.getResources()
                .getConfiguration().fontScale
                * context.getResources().getDisplayMetrics().density;

        applyToView(context, root, scale, scaledDensity);
    }

    private static void applyToView(
            Context context, View view, float scale, float scaledDensity) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            float originalSp;

            synchronized (ORIGINAL_SP) {
                Float savedSize = ORIGINAL_SP.get(view);
                if (savedSize == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        savedSize = TypedValue.deriveDimension(
                                TypedValue.COMPLEX_UNIT_SP,
                                textView.getTextSize(),
                                context.getResources().getDisplayMetrics());
                    } else {
                        savedSize = textView.getTextSize() / scaledDensity;
                    }
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
                applyToView(context, group.getChildAt(i), scale, scaledDensity);
            }
        }
    }
}