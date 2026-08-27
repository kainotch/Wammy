// Created by Notch
package com.example.wammy.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationUtils {
    const val LIBRARY_UPDATE_CHANNEL = "library_update_channel"
    const val LIBRARY_UPDATE_NOTIFICATION_ID = 101

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Library Updates"
            val descriptionText = "Notifications for background library updates"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(LIBRARY_UPDATE_CHANNEL, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getProgressNotificationBuilder(context: Context, max: Int, progress: Int, title: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, LIBRARY_UPDATE_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Updating Library")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, progress, false)
    }

    fun showSummaryNotification(context: Context, newChaptersCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val builder = NotificationCompat.Builder(context, LIBRARY_UPDATE_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("Library Updated")
            .setContentText(if (newChaptersCount > 0) "Found $newChaptersCount new chapters." else "No new chapters found.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(LIBRARY_UPDATE_NOTIFICATION_ID, builder.build())
    }
}
