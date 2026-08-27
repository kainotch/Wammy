package com.example.wammy.util

import android.content.Context

object ReadDurationTracker {
    private const val PREFS_NAME = "statistics_prefs"
    private const val KEY_DURATION = "total_read_duration_ms"

    fun addDuration(context: Context, durationMillis: Long) {
        if (durationMillis <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getLong(KEY_DURATION, 0L)
        prefs.edit().putLong(KEY_DURATION, current + durationMillis).apply()
    }

    fun getTotalDuration(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_DURATION, 0L)
    }

    fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else if (minutes > 0) {
            "${minutes}m"
        } else {
            "0m"
        }
    }
}
