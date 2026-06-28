package com.bloodpressure.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "blood_pressure_reminder"
        private const val CHANNEL_NAME = "降压吧提醒"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "提醒"
        val message = intent.getStringExtra("message") ?: "该打卡了"

        createNotificationChannel(context)
        showNotification(context, title, message)

        // 重新调度下一天的闹钟（读取自定义时间）
        val typeName = intent.getStringExtra("type")
        if (typeName != null) {
            val type = ReminderScheduler.ReminderType.valueOf(typeName)
            val (hour, minute) = when (type) {
                ReminderScheduler.ReminderType.MORNING_BP -> ReminderPreferences.getMorningBpTime(context)
                ReminderScheduler.ReminderType.MORNING_MEDICINE -> ReminderPreferences.getMorningMedTime(context)
                ReminderScheduler.ReminderType.EVENING_BP -> ReminderPreferences.getEveningBpTime(context)
            }
            ReminderScheduler.scheduleDailyAlarm(context, type, hour, minute)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "血压测量和用药提醒"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
