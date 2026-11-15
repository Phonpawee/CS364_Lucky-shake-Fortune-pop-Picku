package com.cs364.luckyshake.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.ui.nav.BottomNav;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View root = findViewById(R.id.settings_root);
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom);
            return insets;
        });
        BottomNav.attach(this, R.id.nav_settings);

        preferences = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);

        MaterialButtonToggleGroup languageGroup =
                findViewById(R.id.languageGroup);
        MaterialButtonToggleGroup themeGroup =
                findViewById(R.id.themeGroup);
        MaterialButtonToggleGroup fontSizeGroup =
                findViewById(R.id.fontSizeGroup);
        SeekBar shakeSensitivitySeekBar =
                findViewById(R.id.shakeSensitivitySeekBar);
        Spinner rerollQuotaSpinner =
                findViewById(R.id.rerollQuotaSpinner);

        restoreSelections(languageGroup, themeGroup, fontSizeGroup);
        FontSizeHelper.apply(this, root);

        int sensitivity = clamp(
                preferences.getInt(PrefKeys.SHAKE_SENSITIVITY, 5), 1, 10);
        shakeSensitivitySeekBar.setMax(9);
        shakeSensitivitySeekBar.setProgress(sensitivity - 1);

        int quota = clamp(
                preferences.getInt(PrefKeys.REROLL_QUOTA, GameConfig.MAX_REDRAWS), 1, 5);
        rerollQuotaSpinner.setSelection(quota - 1);

        languageGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) return;

                    String tag;
                    if (checkedId == R.id.languageThai) {
                        tag = "th";
                    } else if (checkedId == R.id.languageEnglish) {
                        tag = "en";
                    } else {
                        return;
                    }

                    preferences.edit()
                            .putString(PrefKeys.LANGUAGE, tag)
                            .apply();

                    AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(tag));
                });

        themeGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) return;

                    if (checkedId == R.id.themeLight) {
                        ThemeManager.setMode(
                                this, AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (checkedId == R.id.themeDark) {
                        ThemeManager.setMode(
                                this, AppCompatDelegate.MODE_NIGHT_YES);
                    } else if (checkedId == R.id.themeSystem) {
                        ThemeManager.setMode(
                                this,
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                });

        fontSizeGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) return;

                    int level;
                    if (checkedId == R.id.fontSizeSmall) {
                        level = 1;
                    } else if (checkedId == R.id.fontSizeNormal) {
                        level = 5;
                    } else if (checkedId == R.id.fontSizeLarge) {
                        level = 8;
                    } else if (checkedId == R.id.fontSizeExtraLarge) {
                        level = 10;
                    } else {
                        return;
                    }

                    preferences.edit()
                            .putInt(PrefKeys.FONT_SIZE, level)
                            .apply();

                    FontSizeHelper.apply(this, root);
                });

        shakeSensitivitySeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            preferences.edit()
                                    .putInt(
                                            PrefKeys.SHAKE_SENSITIVITY,
                                            progress + 1)
                                    .apply();
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

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

    private void restoreSelections(
            MaterialButtonToggleGroup languageGroup,
            MaterialButtonToggleGroup themeGroup,
            MaterialButtonToggleGroup fontSizeGroup) {
        // ยังไม่เคยเลือกภาษา: แสดงตามภาษาที่แอปใช้อยู่ (ตามเครื่อง)
        String language = preferences.getString(PrefKeys.LANGUAGE, null);
        if (language == null) {
            language = getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        languageGroup.check("th".equals(language)
                ? R.id.languageThai
                : R.id.languageEnglish);

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

        int fontLevel = clamp(
                preferences.getInt(PrefKeys.FONT_SIZE, 5), 1, 10);

        if (fontLevel <= 2) {
            fontSizeGroup.check(R.id.fontSizeSmall);
        } else if (fontLevel <= 6) {
            fontSizeGroup.check(R.id.fontSizeNormal);
        } else if (fontLevel <= 9) {
            fontSizeGroup.check(R.id.fontSizeLarge);
        } else {
            fontSizeGroup.check(R.id.fontSizeExtraLarge);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}