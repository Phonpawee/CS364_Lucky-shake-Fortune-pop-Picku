package com.cs364.luckyshake.reminder;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

public final class ReminderScheduler {
    public static final String ACTION_MEAL_REMINDER =
            "com.cs364.luckyshake.action.MEAL_REMINDER";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_MENU_SET_ID = "menu_set_id";
    public static final String EXTRA_MENU_SET_NAME = "menu_set_name";
    public static final String EXTRA_HOUR = "hour";
    public static final String EXTRA_MINUTE = "minute";
    public static final String EXTRA_GENERATION = "generation";
    /** true = แจ้งเตือนครั้งเดียว (ปุ่มเดโม) ไม่ต้องตั้งรอบถัดไป */
    public static final String EXTRA_ONE_SHOT = "one_shot";

    /** id ของแจ้งเตือนจากปุ่มเดโม แยกจาก id ของมื้อจริงเพื่อไม่ไปยกเลิก/ทับกัน */
    public static final int DEMO_REMINDER_ID = 9_000;

    private static final String PREFERENCES_NAME = "meal_reminder_state";
    private static final String KEY_ENABLED_PREFIX = "enabled_";
    private static final String KEY_GENERATION_PREFIX = "generation_";

    private ReminderScheduler() {
    }

    public static synchronized void scheduleDaily(
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

        SharedPreferences preferences = getPreferences(context);
        String generationKey = generationKey(reminderId);
        long generation = preferences.getLong(generationKey, 0L) + 1L;
        preferences.edit()
                .putBoolean(enabledKey(reminderId), true)
                .putLong(generationKey, generation)
                .apply();

        scheduleAlarm(context, reminderId, hour, minute, menuSetId, menuSetName, generation);
    }

    /** Re-arms a fired alarm only while its saved generation is still enabled. */
    public static synchronized void rescheduleDailyIfCurrent(
            Context context,
            int reminderId,
            int hour,
            int minute,
            long menuSetId,
            String menuSetName,
            long generation
    ) {
        if (!isCurrentGenerationEnabled(context, reminderId, generation)) {
            return;
        }
        scheduleAlarm(context, reminderId, hour, minute, menuSetId, menuSetName, generation);
    }

    /**
     * แจ้งเตือนครั้งเดียวในอีก {@code delaySeconds} วินาที สำหรับซ้อมเดโม (เมนูลับในหน้าตั้งค่า)
     * ใช้ทางเดียวกับการเตือนจริง (AlarmManager → ReminderReceiver) แต่ไม่ตั้งรอบถัดไป
     */
    public static synchronized void scheduleDemo(
            Context context,
            int delaySeconds,
            long menuSetId,
            String menuSetName
    ) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        SharedPreferences preferences = getPreferences(context);
        String generationKey = generationKey(DEMO_REMINDER_ID);
        long generation = preferences.getLong(generationKey, 0L) + 1L;
        preferences.edit()
                .putBoolean(enabledKey(DEMO_REMINDER_ID), true)
                .putLong(generationKey, generation)
                .apply();

        Calendar at = Calendar.getInstance();
        at.add(Calendar.SECOND, Math.max(1, delaySeconds));
        Intent intent = reminderIntent(context, DEMO_REMINDER_ID,
                at.get(Calendar.HOUR_OF_DAY), at.get(Calendar.MINUTE),
                menuSetId, menuSetName, generation);
        intent.putExtra(EXTRA_ONE_SHOT, true);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DEMO_REMINDER_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ใช้ exact เมื่อเครื่องอนุญาต (ต่ำกว่า Android 12 ไม่ต้องขอสิทธิ์) ไม่งั้นใช้แบบเดียวกับ
        // การเตือนจริง ซึ่งตอนแอปเปิดอยู่ระบบมักยิงภายในไม่กี่วินาที
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || alarmManager.canScheduleExactAlarms();
        if (exact) {
            try {
                setExact(alarmManager, at.getTimeInMillis(), pendingIntent);
                return;
            } catch (SecurityException ignored) {
                // สิทธิ์ถูกถอนระหว่างทาง ใช้แบบไม่ exact แทน
            }
        }
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, at.getTimeInMillis(), pendingIntent);
    }

    /** เรียกเฉพาะเมื่อเช็กแล้วว่าตั้ง exact ได้ (ดู {@link #scheduleDemo}) */
    @SuppressLint("MissingPermission")
    private static void setExact(AlarmManager alarmManager, long at, PendingIntent pendingIntent) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent);
    }

    private static Intent reminderIntent(
            Context context,
            int reminderId,
            int hour,
            int minute,
            long menuSetId,
            String menuSetName,
            long generation
    ) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_MEAL_REMINDER);
        intent.putExtra(EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(EXTRA_MENU_SET_ID, menuSetId);
        intent.putExtra(EXTRA_MENU_SET_NAME, menuSetName);
        intent.putExtra(EXTRA_HOUR, hour);
        intent.putExtra(EXTRA_MINUTE, minute);
        intent.putExtra(EXTRA_GENERATION, generation);
        return intent;
    }

    private static void scheduleAlarm(
            Context context,
            int reminderId,
            int hour,
            int minute,
            long menuSetId,
            String menuSetName,
            long generation
    ) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = reminderIntent(context, reminderId, hour, minute,
                menuSetId, menuSetName, generation);

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

    public static synchronized void cancel(Context context, int reminderId) {
        SharedPreferences preferences = getPreferences(context);
        String generationKey = generationKey(reminderId);
        long generation = preferences.getLong(generationKey, 0L) + 1L;
        preferences.edit()
                .putBoolean(enabledKey(reminderId), false)
                .putLong(generationKey, generation)
                .apply();

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

    public static boolean isCurrentGenerationEnabled(
            Context context, int reminderId, long generation) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getBoolean(enabledKey(reminderId), false)
                && preferences.getLong(generationKey(reminderId), -1L) == generation;
    }

    /** Runs a notification action atomically with respect to cancel/reschedule calls. */
    public static synchronized void runIfCurrentGenerationEnabled(
            Context context, int reminderId, long generation, Runnable action) {
        if (isCurrentGenerationEnabled(context, reminderId, generation)) {
            action.run();
        }
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static String enabledKey(int reminderId) {
        return KEY_ENABLED_PREFIX + reminderId;
    }

    private static String generationKey(int reminderId) {
        return KEY_GENERATION_PREFIX + reminderId;
    }
}
