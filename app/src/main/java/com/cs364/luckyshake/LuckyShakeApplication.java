package com.cs364.luckyshake;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.ui.settings.ThemeManager;

/**
 * เป็น {@link ViewModelStoreOwner} ด้วย เพื่อให้ ViewModel ที่ต้องใช้ร่วมกันหลาย Activity
 * (เช่น HomeViewModel ของหน้าหลักและหน้าผล) อยู่ได้ตลอดอายุโปรเซส
 */
public class LuckyShakeApplication extends Application implements ViewModelStoreOwner {
    private final ViewModelStore viewModelStore = new ViewModelStore();

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences =
                getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);

        // ตั้งภาษาก็ต่อเมื่อผู้ใช้เคยเลือกไว้ ถ้ายังไม่เคยเลือกให้ตามภาษาของเครื่อง
        String language = preferences.getString(PrefKeys.LANGUAGE, null);
        if (language != null) {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(language));
        }

        ThemeManager.applySavedMode(this);
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
}
