package com.cs364.luckyshake.ui.settings;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.cs364.luckyshake.data.PrefKeys;
import com.cs364.luckyshake.reminder.ReminderScheduler;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/** Binds meal reminder controls declared in activity_settings.xml. */
final class MealReminderSettings {
    private static final int[] IDS = {1, 2, 3};
    private static final int[] DEFAULT_HOURS = {7, 11, 18};
    private static final int[] DEFAULT_MINUTES = {30, 30, 0};
    private static final int[] SWITCH_IDS = {R.id.reminderBreakfastEnabled, R.id.reminderLunchEnabled, R.id.reminderDinnerEnabled};
    private static final int[] TIME_IDS = {R.id.reminderBreakfastTime, R.id.reminderLunchTime, R.id.reminderDinnerTime};
    private static final int[] SET_IDS = {R.id.reminderBreakfastSet, R.id.reminderLunchSet, R.id.reminderDinnerSet};
    private final Activity activity;
    private final SharedPreferences preferences;
    private final ActivityResultLauncher<String> permissionLauncher;
    private int pendingId = -1;

    MealReminderSettings(Activity activity, ActivityResultLauncher<String> permissionLauncher) {
        this.activity = activity;
        this.permissionLauncher = permissionLauncher;
        preferences = activity.getSharedPreferences(PrefKeys.PREFS_NAME, Activity.MODE_PRIVATE);
    }

    void attach(View root) {
        List<MenuSet> sets = new MenuRepository(activity).getSets();
        List<String> names = new ArrayList<>();
        for (MenuSet set : sets) names.add(set.getName(isThai()));
        if (names.isEmpty()) names.add(activity.getString(R.string.reminder_no_sets));
        for (int i = 0; i < IDS.length; i++) bindMeal(root, i, sets, names);
    }

    boolean onPermissionResult(boolean granted) {
        if (pendingId < 0) return false;
        int id = pendingId;
        pendingId = -1;
        if (granted) schedule(id, true);
        else {
            ((SwitchCompat) activity.findViewById(SWITCH_IDS[id - 1])).setChecked(false);
            preferences.edit().putBoolean(key(id) + "enabled", false).apply();
            Toast.makeText(activity, R.string.reminder_msg_permission_denied, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void bindMeal(View root, int index, List<MenuSet> sets, List<String> names) {
        int id = IDS[index];
        String key = key(id);
        SwitchCompat toggle = root.findViewById(SWITCH_IDS[index]);
        MaterialButton time = root.findViewById(TIME_IDS[index]);
        Spinner spinner = root.findViewById(SET_IDS[index]);
        spinner.setAdapter(FontSizeHelper.spinnerAdapter(activity, names));
        int hour = preferences.getInt(key + "hour", DEFAULT_HOURS[index]);
        int minute = preferences.getInt(key + "minute", DEFAULT_MINUTES[index]);
        setTimeLabel(time, hour, minute);
        long savedSetId = preferences.getLong(key + "set", sets.isEmpty() ? -1 : sets.get(0).id);
        for (int j = 0; j < sets.size(); j++) if (sets.get(j).id == savedSetId) spinner.setSelection(j);
        toggle.setChecked(preferences.getBoolean(key + "enabled", false));
        time.setOnClickListener(v -> {
            int currentHour = preferences.getInt(key + "hour", DEFAULT_HOURS[index]);
            int currentMinute = preferences.getInt(key + "minute", DEFAULT_MINUTES[index]);
            new TimePickerDialog(activity, (picker, h, m) -> {
                setTimeLabel(time, h, m);
                preferences.edit().putInt(key + "hour", h).putInt(key + "minute", m).apply();
                if (toggle.isChecked()) schedule(id, true);
            }, currentHour, currentMinute, false).show();
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean initial = true;
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long itemId) {
                if (initial) { initial = false; return; }
                if (position < sets.size()) {
                    preferences.edit().putLong(key + "set", sets.get(position).id).apply();
                    if (toggle.isChecked()) schedule(id, true);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (checked && sets.isEmpty()) { toggle.setChecked(false); Toast.makeText(activity, R.string.reminder_no_sets, Toast.LENGTH_SHORT).show(); return; }
            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                pendingId = id;
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else schedule(id, checked);
        });
    }

    private void schedule(int id, boolean enabled) {
        String key = key(id);
        preferences.edit().putBoolean(key + "enabled", enabled).apply();
        if (!enabled) { ReminderScheduler.cancel(activity, id); return; }
        List<MenuSet> sets = new MenuRepository(activity).getSets();
        if (sets.isEmpty()) { preferences.edit().putBoolean(key + "enabled", false).apply(); return; }
        long setId = preferences.getLong(key + "set", sets.get(0).id);
        MenuSet set = new MenuRepository(activity).getSet(setId);
        if (set == null) set = sets.get(0);
        int hour = preferences.getInt(key + "hour", DEFAULT_HOURS[id - 1]);
        int minute = preferences.getInt(key + "minute", DEFAULT_MINUTES[id - 1]);
        preferences.edit().putLong(key + "set", set.id).apply();
        ReminderScheduler.scheduleDaily(activity, id, hour, minute, set.id, set.getName(isThai()));
    }

    private void setTimeLabel(MaterialButton button, int hour, int minute) {
        String time = activity.getString(R.string.reminder_time_format, hour, minute);
        button.setText(time);
        button.setContentDescription(activity.getString(R.string.reminder_time_description, time));
    }

    private String key(int id) { return PrefKeys.MEAL_REMINDER_PREFIX + id + "_"; }
    private boolean isThai() { return "th".equals(activity.getResources().getConfiguration().getLocales().get(0).getLanguage()); }
}


