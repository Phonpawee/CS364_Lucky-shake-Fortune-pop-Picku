package com.cs364.luckyshake.reminder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.ui.menu.MenuListActivity;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "meal_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !ReminderScheduler.ACTION_MEAL_REMINDER.equals(intent.getAction())) {
            return;
        }

        int reminderId = intent.getIntExtra(
                ReminderScheduler.EXTRA_REMINDER_ID, -1);
        int hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, -1);
        int minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, -1);
        long menuSetId = intent.getLongExtra(
                ReminderScheduler.EXTRA_MENU_SET_ID, -1L);
        String menuSetName = intent.getStringExtra(
                ReminderScheduler.EXTRA_MENU_SET_NAME);

        if (reminderId < 0 || hour < 0 || minute < 0
                || menuSetId < 0 || menuSetName == null) {
            return;
        }

        createNotificationChannel(context);

        boolean hasPermission = Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        if (hasPermission) {
            Intent openAppIntent = new Intent(context, MenuListActivity.class);
            openAppIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openAppIntent.putExtra(
                    MenuListActivity.EXTRA_SET_ID, menuSetId);

            PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    reminderId,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notification =
                    new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle(
                                    context.getString(
                                            R.string.reminder_title_notification))
                            .setContentText(
                                    context.getString(
                                            R.string.reminder_msg_notification_body,
                                            menuSetName))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .setContentIntent(openAppPendingIntent);

            try {
                NotificationManagerCompat.from(context)
                        .notify(reminderId, notification.build());
            } catch (SecurityException ignored) {
                // Permission may have been revoked while the alarm was firing.
            }
        }

        // ตั้งเตือนรอบถัดไปของมื้อเดิม
        ReminderScheduler.scheduleDaily(
                context, reminderId, hour, minute, menuSetId, menuSetName);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_label_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);

        channel.setDescription(
                context.getString(R.string.reminder_msg_channel_description));
        manager.createNotificationChannel(channel);
    }
}
