package com.bloodpressure.app.reminder

import android.content.Context
import android.content.SharedPreferences

object ReminderPreferences {

    private const val PREFS_NAME = "reminder_prefs"

    // 默认提醒时间
    const val DEFAULT_MORNING_BP_HOUR = 7
    const val DEFAULT_MORNING_BP_MINUTE = 0
    const val DEFAULT_MORNING_MED_HOUR = 7
    const val DEFAULT_MORNING_MED_MINUTE = 30
    const val DEFAULT_EVENING_BP_HOUR = 21
    const val DEFAULT_EVENING_BP_MINUTE = 30

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getMorningBpTime(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        return Pair(
            prefs.getInt("morning_bp_hour", DEFAULT_MORNING_BP_HOUR),
            prefs.getInt("morning_bp_minute", DEFAULT_MORNING_BP_MINUTE)
        )
    }

    fun getMorningMedTime(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        return Pair(
            prefs.getInt("morning_med_hour", DEFAULT_MORNING_MED_HOUR),
            prefs.getInt("morning_med_minute", DEFAULT_MORNING_MED_MINUTE)
        )
    }

    fun getEveningBpTime(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        return Pair(
            prefs.getInt("evening_bp_hour", DEFAULT_EVENING_BP_HOUR),
            prefs.getInt("evening_bp_minute", DEFAULT_EVENING_BP_MINUTE)
        )
    }

    fun setMorningBpTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt("morning_bp_hour", hour)
            .putInt("morning_bp_minute", minute)
            .apply()
    }

    fun setMorningMedTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt("morning_med_hour", hour)
            .putInt("morning_med_minute", minute)
            .apply()
    }

    fun setEveningBpTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt("evening_bp_hour", hour)
            .putInt("evening_bp_minute", minute)
            .apply()
    }
}
