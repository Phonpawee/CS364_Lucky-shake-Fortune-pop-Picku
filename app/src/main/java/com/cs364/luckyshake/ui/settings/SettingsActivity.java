package com.cs364.luckyshake.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.os.LocaleListCompat;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.DbHelper;
import com.cs364.luckyshake.data.DemoHistory;
import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.data.HistoryRepository;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.logic.GameConfig;
import com.cs364.luckyshake.reminder.ReminderScheduler;
import com.cs364.luckyshake.ui.home.HomeSession;
import com.cs364.luckyshake.ui.home.HomeViewModel;
import com.cs364.luckyshake.ui.nav.BottomNav;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private MealReminderSettings mealReminderSettings;

    private final ActivityResultLauncher<String> reminderPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                boolean notificationsEnabled =
                        NotificationManagerCompat.from(this).areNotificationsEnabled();
                if (mealReminderSettings != null
                        && mealReminderSettings.onPermissionResult(granted, notificationsEnabled)) {
                    if (granted && !notificationsEnabled) showNotificationSettingsPrompt();
                    return;
                }
                if (granted && notificationsEnabled) {
                    Toast.makeText(this, R.string.reminder_msg_permission_granted,
                            Toast.LENGTH_SHORT).show();
                } else if (granted) {
                    showNotificationSettingsPrompt();
                } else {
                    Toast.makeText(this, R.string.reminder_msg_permission_denied,
                            Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View root = findViewById(R.id.settings_root);
        BottomNav.attach(this, R.id.nav_settings, root);

        preferences = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE);
        mealReminderSettings = new MealReminderSettings(this, reminderPermissionLauncher);
        mealReminderSettings.attach(root);
        findViewById(R.id.settingsAllowNotifications).setOnClickListener(v -> requestNotificationPermission());
        findViewById(R.id.settingsClearAll).setOnClickListener(v -> confirmClearAllData());
        findViewById(R.id.settingsMenuWeights).setOnClickListener(v -> openMenuWeights());

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
        setUpDemoTools();
        FontSizeHelper.apply(this, root);

        int sensitivity = clamp(
                preferences.getInt(PrefKeys.SHAKE_SENSITIVITY, GameConfig.DEFAULT_SENSITIVITY),
                GameConfig.MIN_SENSITIVITY, GameConfig.MAX_SENSITIVITY);
        shakeSensitivitySeekBar.setMax(GameConfig.MAX_SENSITIVITY - GameConfig.MIN_SENSITIVITY);
        shakeSensitivitySeekBar.setProgress(sensitivity - GameConfig.MIN_SENSITIVITY);

        int quota = clamp(
                preferences.getInt(PrefKeys.REROLL_QUOTA, GameConfig.MAX_REDRAWS),
                GameConfig.MIN_REDRAW_LIMIT, GameConfig.MAX_REDRAW_LIMIT);
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
                    BottomNav.applyFontSize(this);
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
                                            progress + GameConfig.MIN_SENSITIVITY)
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

    private void confirmClearAllData() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_clear_title)
                .setMessage(R.string.settings_clear_message)
                .setNegativeButton(R.string.common_btn_cancel, null)
                .setPositiveButton(R.string.settings_btn_clear_all,
                        (dialog, which) -> clearAllData())
                .show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            showNotificationSettingsPrompt();
            return;
        }
        Toast.makeText(this, R.string.reminder_msg_permission_granted,
                Toast.LENGTH_SHORT).show();
    }

    private void showNotificationSettingsPrompt() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_notification_disabled_title)
                .setMessage(R.string.settings_notification_disabled_message)
                .setNegativeButton(R.string.common_btn_cancel, null)
                .setPositiveButton(R.string.settings_notification_open,
                        (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                            startActivity(intent);
                        })
                .show();
    }

    private void clearAllData() {
        for (int reminderId : new int[]{1, 2, 3}) {
            ReminderScheduler.cancel(this, reminderId);
        }

        DbHelper.closeInstance();
        deleteDatabase(DbHelper.DB_NAME);
        preferences.edit().clear().apply();

        HomeViewModel homeViewModel = HomeViewModel.get(this);
        homeViewModel.reset();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        Toast.makeText(this, R.string.settings_clear_done, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, SettingsActivity.class));
        finish();
    }

    private void openMenuWeights() {
        MenuSet set = demoSet();
        if (set == null) {
            Toast.makeText(this, R.string.settings_msg_no_menu_set, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(com.cs364.luckyshake.ui.weights.MenuWeightsActivity.newIntent(this, set.id));
    }
    // ------------------------------------------------------------ demo tools

    /** แสดงเลขเวอร์ชัน กดค้างเพื่อเปิดเครื่องมือซ้อมเดโม (ผู้ใช้ทั่วไปจะไม่รู้ว่ามี) */
    private void setUpDemoTools() {
        TextView version = findViewById(R.id.settingsVersion);
        version.setText(getString(R.string.settings_label_version, versionName()));
        version.setOnLongClickListener(v -> {
            showDemoTools();
            return true;
        });
    }

    private void showDemoTools() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.demo_title_tools)
                .setItems(new String[]{getString(R.string.demo_btn_fill_history)},
                        (dialog, which) -> fillSampleHistory())
                .show();
    }

    private void fillSampleHistory() {
        MenuSet set = demoSet();
        List<Menu> menus = set == null
                ? new ArrayList<>()
                : new MenuRepository(this).getMenus(set.id, null, null);
        if (menus.isEmpty()) {
            Toast.makeText(this, R.string.demo_msg_no_menus, Toast.LENGTH_SHORT).show();
            return;
        }
        HistoryRepository history = new HistoryRepository(this);
        int added = 0;
        for (HistoryEntry e : DemoHistory.generate(menus, DemoHistory.DEFAULT_COUNT,
                DemoHistory.DEFAULT_DAYS, System.currentTimeMillis(), new Random())) {
            if (history.add(e) > 0) {
                added++;
            }
        }
        Toast.makeText(this, getString(R.string.demo_msg_history_filled, added),
                Toast.LENGTH_SHORT).show();
    }


    /** ชุดที่เลือกอยู่ที่หน้าหลัก ถ้ายังไม่ได้เลือกใช้ชุดแรก */
    private MenuSet demoSet() {
        MenuRepository repository = new MenuRepository(this);
        long selected = HomeViewModel.get(this).getSelectedSetId();
        MenuSet set = selected == HomeSession.NO_SET ? null : repository.getSet(selected);
        if (set == null) {
            List<MenuSet> sets = repository.getSets();
            set = sets.isEmpty() ? null : sets.get(0);
        }
        return set;
    }

    private String versionName() {
        try {
            String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return name == null ? "" : name;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private boolean isThai() {
        return "th".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
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
