package com.bloodpressure.app.data

import android.content.Context

data class TrainingPlan(
    val name: String,
    val groupCount: Int,
    val groupDuration: Int,
    val restDuration: Int
) {
    val summary: String
        get() = "${groupCount} groups / ${groupDuration / 60} min each / ${restDuration / 60} min rest"
}

object TrainingPlanPrefs {
    private const val PREFS_NAME = "training_plan_prefs"
    private const val KEY_NAME = "name"
    private const val KEY_GROUP_COUNT = "group_count"
    private const val KEY_GROUP_DURATION = "group_duration"
    private const val KEY_REST_DURATION = "rest_duration"

    val defaultPlan = TrainingPlan(
        name = "AHA recommended",
        groupCount = 4,
        groupDuration = 120,
        restDuration = 60
    )

    val presetPlans = listOf(
        defaultPlan,
        TrainingPlan("Light practice", groupCount = 3, groupDuration = 60, restDuration = 60),
        TrainingPlan("Steady practice", groupCount = 4, groupDuration = 90, restDuration = 60)
    )

    fun loadPlan(context: Context): TrainingPlan {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return TrainingPlan(
            name = prefs.getString(KEY_NAME, defaultPlan.name) ?: defaultPlan.name,
            groupCount = prefs.getInt(KEY_GROUP_COUNT, defaultPlan.groupCount),
            groupDuration = prefs.getInt(KEY_GROUP_DURATION, defaultPlan.groupDuration),
            restDuration = prefs.getInt(KEY_REST_DURATION, defaultPlan.restDuration)
        )
    }

    fun savePlan(context: Context, plan: TrainingPlan) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, plan.name)
            .putInt(KEY_GROUP_COUNT, plan.groupCount)
            .putInt(KEY_GROUP_DURATION, plan.groupDuration)
            .putInt(KEY_REST_DURATION, plan.restDuration)
            .apply()
    }
}
