package com.cs364.luckyshake;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.ui.settings.ThemeManager;

public class LuckyShakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences =
                getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);

        String language = preferences.getString(PrefKeys.LANGUAGE, "en");
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language));

        ThemeManager.applySavedMode(this);
    }
}