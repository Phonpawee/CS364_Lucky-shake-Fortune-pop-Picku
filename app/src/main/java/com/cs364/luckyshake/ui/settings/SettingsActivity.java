package com.cs364.luckyshake.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.PrefKeys;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        FontSizeHelper.apply(this, findViewById(R.id.settings_root));

        preferences = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);

        RadioGroup languageGroup = findViewById(R.id.languageGroup);
        RadioGroup themeGroup = findViewById(R.id.themeGroup);
        SeekBar fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        Spinner rerollQuotaSpinner = findViewById(R.id.rerollQuotaSpinner);

        // คืนค่าที่เคยเลือกไว้
        String language = preferences.getString(PrefKeys.LANGUAGE, "en");
        languageGroup.check("th".equals(language)
                ? R.id.languageThai : R.id.languageEnglish);

        int themeMode = preferences.getInt(
                PrefKeys.THEME_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            themeGroup.check(R.id.themeLight);
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeGroup.check(R.id.themeDark);
        } else {
            themeGroup.check(R.id.themeSystem);
        }

        int fontSize = Math.max(
                1, Math.min(preferences.getInt(PrefKeys.FONT_SIZE, 5), 10));
        fontSizeSeekBar.setMax(9);
        fontSizeSeekBar.setProgress(fontSize - 1);

        int quota = Math.max(
                1, Math.min(preferences.getInt(PrefKeys.REROLL_QUOTA, 1), 5));
        rerollQuotaSpinner.setSelection(quota - 1);

        // ภาษา
        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String tag;
            if (checkedId == R.id.languageThai) {
                tag = "th";
            } else if (checkedId == R.id.languageEnglish) {
                tag = "en";
            } else {
                return;
            }

            preferences.edit().putString(PrefKeys.LANGUAGE, tag).apply();
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag));
        });

        // ธีม
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.themeLight) {
                ThemeManager.setMode(this, AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.themeDark) {
                ThemeManager.setMode(this, AppCompatDelegate.MODE_NIGHT_YES);
            } else if (checkedId == R.id.themeSystem) {
                ThemeManager.setMode(
                        this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });

        // เก็บค่าขนาดตัวอักษรระดับ 1–10
        fontSizeSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            preferences.edit()
                                    .putInt(PrefKeys.FONT_SIZE, progress + 1)
                                    .apply();

                            FontSizeHelper.apply(
                                    SettingsActivity.this,
                                    findViewById(R.id.settings_root));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        // Spinner มีตัวเลือก 1–5 จาก android:entries ใน layout
        rerollQuotaSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        preferences.edit()
                                .putInt(PrefKeys.REROLL_QUOTA, position + 1)
                                .apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
    }
}