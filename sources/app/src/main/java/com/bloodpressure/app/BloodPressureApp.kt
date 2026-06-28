package com.bloodpressure.app

import android.app.Application
import com.bloodpressure.app.data.AppDatabase
import com.bloodpressure.app.reminder.ReminderScheduler

class BloodPressureApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // 启动每日提醒
        ReminderScheduler.scheduleAll(this)
    }
}
