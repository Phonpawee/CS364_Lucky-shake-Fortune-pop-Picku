package com.cs364.luckyshake.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public final class ReminderScheduler {
    public static final String ACTION_MEAL_REMINDER =
            "com.cs364.luckyshake.action.MEAL_REMINDER";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_MENU_SET_ID = "menu_set_id";
    public static final String EXTRA_MENU_SET_NAME = "menu_set_name";
    public static final String EXTRA_HOUR = "hour";
    public static final String EXTRA_MINUTE = "minute";

    private ReminderScheduler() {
    }

    public static void scheduleDaily(
            Context context,
            int reminderId,
            int hour,
            int minute,
            long menuSetId,
            String menuSetName
    ) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Invalid reminder time");
        }

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_MEAL_REMINDER);
        intent.putExtra(EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(EXTRA_MENU_SET_ID, menuSetId);
        intent.putExtra(EXTRA_MENU_SET_NAME, menuSetName);
        intent.putExtra(EXTRA_HOUR, hour);
        intent.putExtra(EXTRA_MINUTE, minute);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar now = Calendar.getInstance();
        Calendar nextAlarm = (Calendar) now.clone();
        nextAlarm.set(Calendar.HOUR_OF_DAY, hour);
        nextAlarm.set(Calendar.MINUTE, minute);
        nextAlarm.set(Calendar.SECOND, 0);
        nextAlarm.set(Calendar.MILLISECOND, 0);

        if (!nextAlarm.after(now)) {
            nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextAlarm.getTimeInMillis(),
                pendingIntent
        );
    }

    public static void cancel(Context context, int reminderId) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_MEAL_REMINDER);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}