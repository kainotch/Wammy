package eu.kanade.tachiyomi.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Background worker for automatic Google Drive Sync.
 * Handles both silently uploading (backing up) when data changes,
 * and checking for newer files (restoring) on app launch.
 */
class DriveSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val driveSyncManager: DriveSyncManager = Injekt.get()
        val driveApiHelper: DriveApiHelper = Injekt.get()
        
        val isRestoreCheck = inputData.getBoolean(KEY_IS_RESTORE_CHECK, false)
        
        val accessToken = driveApiHelper.getAccessToken()
        if (accessToken == null) {
            logcat(LogPriority.INFO) { "DriveSyncWorker: No access token, skipping" }
            return@withContext Result.success()
        }

        try {
            if (isRestoreCheck) {
                // APP STARTUP: Check if cloud is newer than local
                logcat(LogPriority.INFO) { "DriveSyncWorker: Checking for newer cloud backup..." }
                val cloudModifiedStr = driveApiHelper.getSyncFileModifiedTime(accessToken)
                
                if (cloudModifiedStr != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val cloudTime = sdf.parse(cloudModifiedStr)?.time ?: 0L
                    val prefs = context.getSharedPreferences("wammy_sync_prefs", Context.MODE_PRIVATE)
                    val localSyncTime = prefs.getLong("last_sync_time", 0L)

                    if (cloudTime > localSyncTime) {
                        logcat(LogPriority.INFO) { "DriveSyncWorker: Cloud file is newer! Restoring..." }
                        val result = driveSyncManager.restore()
                        if (result is SyncResult.Success) {
                            // Update local sync time to avoid re-restoring
                            prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
                        }
                    } else {
                        logcat(LogPriority.INFO) { "DriveSyncWorker: Local data is up to date." }
                    }
                }
            } else {
                // DATA CHANGED: Silently backup
                logcat(LogPriority.INFO) { "DriveSyncWorker: Triggering silent backup..." }
                val result = driveSyncManager.backup()
                if (result is SyncResult.Success) {
                    val prefs = context.getSharedPreferences("wammy_sync_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "DriveSyncWorker: Failed" }
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME_BACKUP = "DriveSyncBackup"
        private const val WORK_NAME_RESTORE = "DriveSyncRestoreCheck"
        private const val KEY_IS_RESTORE_CHECK = "is_restore_check"

        fun scheduleBackup(context: Context) {
            // Android's WorkManager can arbitrarily delay background tasks by 10+ minutes to save battery.
            // Since we want this to happen instantly when the user saves a manga, we launch it directly.
            
            val NOTIF_ID = -510
            
            context.notify(NOTIF_ID, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS) {
                setContentTitle("Google Drive")
                setContentText("Syncing...")
                setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
                setOngoing(true)
                setProgress(0, 0, true)
            }
            
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                try {
                    val driveSyncManager: DriveSyncManager = Injekt.get()
                    val result = driveSyncManager.backup()
                    if (result is SyncResult.Success) {
                        val prefs = context.getSharedPreferences("wammy_sync_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
                        
                        context.notify(NOTIF_ID, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
                            setContentTitle("Google Drive Sync")
                            setContentText("Auto-sync complete!")
                            setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
                            setOngoing(false)
                            setTimeoutAfter(3000) // Auto-dismiss after 3 seconds
                        }
                    } else {
                        context.notify(NOTIF_ID, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
                            setContentTitle("Google Drive Sync")
                            setContentText("Auto-sync failed.")
                            setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
                            setOngoing(false)
                            setTimeoutAfter(5000)
                        }
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Immediate auto-backup failed" }
                    context.cancelNotification(NOTIF_ID)
                }
            }
        }

        fun scheduleRestoreCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DriveSyncWorker>()
                .setConstraints(constraints)
                .setInputData(androidx.work.Data.Builder().putBoolean(KEY_IS_RESTORE_CHECK, true).build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_RESTORE,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun schedulePeriodicBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = androidx.work.PeriodicWorkRequestBuilder<DriveSyncWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DriveSyncPeriodic",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
