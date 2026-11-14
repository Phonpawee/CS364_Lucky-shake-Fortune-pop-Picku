package com.cs364.luckyshake;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

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
        ThemeManager.applySavedMode(this);
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
}
