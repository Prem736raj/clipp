package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationHelper {
    fun showExportCompleteNotification(context: Context, projectName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "clipp_notifications")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("Export Complete")
            .setContentText("Successfully exported $projectName.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showAutoSaveNotification(context: Context, projectName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "clipp_notifications")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("Auto-saved")
            .setContentText("$projectName has been safely auto-saved.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(projectName.hashCode(), notification)
    }

    fun showCloudSyncNotification(context: Context, projectName: String, isComplete: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "clipp_notifications")
            .setSmallIcon(if (isComplete) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_sys_upload)
            .setContentTitle(if (isComplete) "Sync Complete" else "Syncing...")
            .setContentText(if (isComplete) "$projectName is backed up." else "Backing up $projectName to cloud.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setProgress(0, 0, !isComplete)
            .build()
        notificationManager.notify("sync_${projectName}".hashCode(), notification)
    }

    fun showProgressNotification(context: Context, taskId: Int, title: String, text: String, progress: Int, max: Int = 100) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "clipp_notifications")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, progress, progress == -1) // -1 for indeterminate
            .build()
        notificationManager.notify(taskId, notification)
    }

    fun cancelNotification(context: Context, taskId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId)
    }
}
