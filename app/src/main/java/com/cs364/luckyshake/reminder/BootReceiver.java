package com.cs364.luckyshake.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.cs364.luckyshake.data.PrefKeys;

import java.util.List;

/** Restores enabled meal reminders after the device finishes booting. */
public final class BootReceiver extends BroadcastReceiver {
    private static final int[] REMINDER_IDS = {1, 2, 3};
    private static final int[] DEFAULT_HOURS = {7, 11, 18};
    private static final int[] DEFAULT_MINUTES = {30, 30, 0};

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = appContext.getSharedPreferences(
                PrefKeys.PREFS_NAME, Context.MODE_PRIVATE);
        MenuRepository repository = new MenuRepository(appContext);
        List<MenuSet> menuSets = null;
        boolean thai = isThai(appContext, preferences);

        for (int index = 0; index < REMINDER_IDS.length; index++) {
            int reminderId = REMINDER_IDS[index];
            String key = PrefKeys.MEAL_REMINDER_PREFIX + reminderId + "_";
            if (!preferences.getBoolean(key + "enabled", false)) {
                continue;
            }

            int hour = preferences.getInt(key + "hour", DEFAULT_HOURS[index]);
            int minute = preferences.getInt(key + "minute", DEFAULT_MINUTES[index]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                continue;
            }

            long savedSetId = preferences.getLong(key + "set", -1L);
            MenuSet menuSet = repository.getSet(savedSetId);
            if (menuSet == null) {
                if (menuSets == null) {
                    menuSets = repository.getSets();
                }
                if (menuSets.isEmpty()) {
                    continue;
                }
                menuSet = menuSets.get(0);
            }

            ReminderScheduler.scheduleDaily(appContext, reminderId, hour, minute,
                    menuSet.id, menuSet.getName(thai));
        }
    }

    private static boolean isThai(Context context, SharedPreferences preferences) {
        String language = preferences.getString(PrefKeys.LANGUAGE, null);
        if (language != null) {
            return language.toLowerCase().startsWith("th");
        }
        return "th".equals(context.getResources().getConfiguration().getLocales()
                .get(0).getLanguage());
    }
}
