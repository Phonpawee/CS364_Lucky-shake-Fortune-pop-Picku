package com.cs364.luckyshake;

import android.app.Application;

import com.cs364.luckyshake.ui.settings.ThemeManager;

public class LuckyShakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedMode(this);
    }
}