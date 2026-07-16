package com.bloodpressure.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bloodpressure.app.MainActivity
import java.util.Calendar

object ReminderScheduler {

    enum class ReminderType(val id: Int, val title: String, val message: String) {
        MORNING_MEDICINE(1001, "该吃药了", "别忘了早上的药！按时服药很重要。"),
        MORNING_BP(1002, "该测血压了", "早上起床后记得测一下血压。"),
        EVENING_BP(1003, "该测血压了", "睡前记得测一下血压。")
    }

    fun scheduleAll(context: Context) {
        val morningBp = ReminderPreferences.getMorningBpTime(context)
        val morningMed = ReminderPreferences.getMorningMedTime(context)
        val eveningBp = ReminderPreferences.getEveningBpTime(context)

        scheduleDailyAlarm(context, ReminderType.MORNING_BP, morningBp.first, morningBp.second)
        scheduleDailyAlarm(context, ReminderType.MORNING_MEDICINE, morningMed.first, morningMed.second)
        scheduleDailyAlarm(context, ReminderType.EVENING_BP, eveningBp.first, eveningBp.second)
    }

    fun scheduleDailyAlarm(context: Context, type: ReminderType, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", type.name)
            putExtra("title", type.title)
            putExtra("message", type.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 如果时间已过，设为明天
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val showIntent = PendingIntent.getActivity(
            context,
            type.id + 10_000,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 闹钟级调度不会被 Doze 或厂商的后台限流延后。
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, showIntent),
            pendingIntent
        )
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ReminderType.entries.forEach { type ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                type.id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }
}
