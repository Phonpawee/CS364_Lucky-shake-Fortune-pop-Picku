package com.cs364.luckyshake.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.cs364.luckyshake.data.PrefKeys;

public final class ThemeManager {
    private ThemeManager() {
        // Utility class
    }

    // เรียกตอนเริ่มเปิดแอป เพื่อคืนค่าโหมดที่เลือกไว้
    public static void applySavedMode(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PrefKeys.PREFS_NAME, Context.MODE_PRIVATE
        );

        int mode = preferences.getInt(
                PrefKeys.THEME_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );

        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // ให้หน้า Settings เรียกเมื่อผู้ใช้เลือกโหมด
    public static void setMode(Context context, int mode) {
        if (mode != AppCompatDelegate.MODE_NIGHT_NO
                && mode != AppCompatDelegate.MODE_NIGHT_YES
                && mode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            throw new IllegalArgumentException("Unsupported theme mode");
        }

        context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(PrefKeys.THEME_MODE, mode)
                .apply();

        AppCompatDelegate.setDefaultNightMode(mode);
    }
}