package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.util.concurrent.TimeUnit

/**
 * Background worker for automatic Google Drive Sync.
 *
 * Architecture:
 * - WorkManager with 2-minute initial delay acts as a disk-persisted debounce.
 *   Every new trigger replaces the pending job, restarting the 2-minute timer.
 *   Even if the app is killed, Android will still fire the backup on schedule.
 * - Login guard ensures zero battery waste for non-logged-in users.
 * - Retry logic with exponential backoff handles transient server errors.
 * - SHA-256 hash check in DriveSyncManager skips identical uploads.
 */
class DriveSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Guard: skip if user is not logged in
        if (FirebaseAuth.getInstance().currentUser == null) {
            logcat(LogPriority.INFO) { "DriveSyncWorker: No Firebase user, skipping" }
            return@withContext Result.success()
        }

        val driveSyncManager: DriveSyncManager = Injekt.get()
        val driveApiHelper: DriveApiHelper = Injekt.get()

        val isRestoreCheck = inputData.getBoolean(KEY_IS_RESTORE_CHECK, false)

        val accessToken = driveApiHelper.getAccessToken()
        if (accessToken == null) {
            logcat(LogPriority.INFO) { "DriveSyncWorker: No access token, skipping" }
            return@withContext Result.success()
        }

        var completed = false
        try {
            if (isRestoreCheck) {
                doRestoreCheck(driveSyncManager, driveApiHelper, accessToken)
            } else {
                doBackup(driveSyncManager)
            }
            completed = true
            Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "DriveSyncWorker: Failed or cancelled" }
            Result.failure()
        } finally {
            // If the worker was killed by WorkManager (e.g., 10 minute limit) while a notification was showing,
            // this ensures the notification doesn't get stuck forever on the screen.
            if (!completed) {
                context.cancelNotification(NOTIF_ID_BACKUP)
                context.cancelNotification(NOTIF_ID_RESTORE)
            }
        }
    }

    /**
     * APP STARTUP: Check if cloud backup is newer than local data.
     * If yes, restore it with a progress notification.
     */
    private suspend fun doRestoreCheck(
        driveSyncManager: DriveSyncManager,
        driveApiHelper: DriveApiHelper,
        accessToken: String
    ) {
        logcat(LogPriority.INFO) { "DriveSyncWorker: Checking for newer cloud backup..." }
        val cloudModifiedStr = driveApiHelper.getSyncFileModifiedTime(accessToken)

        if (cloudModifiedStr != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cloudTime = sdf.parse(cloudModifiedStr)?.time ?: 0L
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val localSyncTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)

            if (cloudTime > localSyncTime) {
                logcat(LogPriority.INFO) { "DriveSyncWorker: Cloud file is newer! Restoring..." }

                // Show restore notification with progress bar
                context.notify(NOTIF_ID_RESTORE, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS) {
                    setContentTitle("Google Drive")
                    setContentText("Restoring your library...")
                    setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
                    setOngoing(true)
                    setProgress(0, 0, true)
                }

                val result = driveSyncManager.restore()
                if (result is SyncResult.Success) {
                    prefs.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
                    context.notify(NOTIF_ID_RESTORE, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
                        setContentTitle("Google Drive")
                        setContentText("Library restored!")
                        setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
                        setOngoing(false)
                        setTimeoutAfter(3000)
                    }
                } else {
                    context.cancelNotification(NOTIF_ID_RESTORE)
                }
            } else {
                logcat(LogPriority.INFO) { "DriveSyncWorker: Local data is up to date." }
            }
        }
    }

    /**
     * DATA CHANGED: Create backup and upload to Drive.
     * Shows notification, retries on failure.
     */
    private suspend fun doBackup(driveSyncManager: DriveSyncManager) {
        logcat(LogPriority.INFO) { "DriveSyncWorker: Starting backup with notification..." }

        // Show "Syncing..." notification
        context.notify(NOTIF_ID_BACKUP, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS) {
            setContentTitle("Google Drive")
            setContentText("Syncing...")
            setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
            setOngoing(true)
            setProgress(0, 0, true)
        }

        val success = doBackupWithRetry(driveSyncManager)

        if (success) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .putBoolean(KEY_DATA_DIRTY, false)
                .apply()

            context.notify(NOTIF_ID_BACKUP, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
                setContentTitle("Google Drive")
                setContentText("Sync complete!")
                setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
                setOngoing(false)
                setTimeoutAfter(3000)
            }
        } else {
            context.notify(NOTIF_ID_BACKUP, eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
                setContentTitle("Google Drive")
                setContentText("Sync failed.")
                setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_mihon)
                setOngoing(false)
                setTimeoutAfter(5000)
            }
        }
    }

    /**
     * Retry backup up to 3 times with exponential backoff (2s, 4s).
     */
    private suspend fun doBackupWithRetry(driveSyncManager: DriveSyncManager): Boolean {
        var lastError: Exception? = null
        for (attempt in 1..3) {
            try {
                val result = driveSyncManager.backup()
                if (result is SyncResult.Success) {
                    logcat(LogPriority.INFO) { "DriveSyncWorker: Backup succeeded on attempt $attempt" }
                    return true
                } else if (result is SyncResult.Error) {
                    logcat(LogPriority.WARN) { "DriveSyncWorker: Backup attempt $attempt failed: ${result.message}" }
                }
            } catch (e: Exception) {
                lastError = e
                logcat(LogPriority.WARN, e) { "DriveSyncWorker: Backup attempt $attempt threw exception" }
            }
            if (attempt < 3) {
                delay(2000L * attempt) // 2s, then 4s
            }
        }
        logcat(LogPriority.ERROR, lastError) { "DriveSyncWorker: Backup failed after 3 retries" }
        return false
    }

    companion object {
        private const val WORK_NAME_BACKUP = "DriveSyncBackup"
        private const val WORK_NAME_RESTORE = "DriveSyncRestoreCheck"
        private const val KEY_IS_RESTORE_CHECK = "is_restore_check"

        const val PREFS_NAME = "wammy_sync_prefs"
        const val KEY_LAST_SYNC_TIME = "last_sync_time"
        const val KEY_DATA_DIRTY = "data_dirty"
        const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"

        private const val NOTIF_ID_BACKUP = -510
        private const val NOTIF_ID_RESTORE = -511

        /**
         * Schedule an auto-backup with 2-minute debounce using WorkManager.
         *
         * How the debounce works:
         * - Each call enqueues a WorkManager job with a 2-minute delay using REPLACE policy.
         * - If another call comes in before the 2 minutes are up, the old job is cancelled
         *   and a new 2-minute timer starts.
         * - This means: save 10 manga in 2 minutes = 1 single backup.
         *
         * Why WorkManager instead of GlobalScope:
         * - WorkManager persists its job queue to disk. Even if the user kills the app
         *   or Android kills the process, the backup WILL fire when the timer expires.
         * - GlobalScope dies with the process — data would be lost.
         */
        fun scheduleBackup(context: Context) {
            // Guard: skip if user is not logged in
            if (FirebaseAuth.getInstance().currentUser == null) return

            // Guard: skip if user disabled auto-sync
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)) return

            // Mark data as dirty (persisted to disk)
            prefs.edit().putBoolean(KEY_DATA_DIRTY, true).apply()

            // Enqueue a delayed WorkManager backup.
            // REPLACE policy = debounce. Every new call resets the 2-minute timer.
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DriveSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(2, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_BACKUP,
                ExistingWorkPolicy.REPLACE,
                request
            )

            logcat(LogPriority.INFO) { "DriveSyncWorker: Backup scheduled (2-minute debounce)" }
        }

        /**
         * Schedule a restore check on app startup.
         * Only runs if user is logged in.
         */
        fun scheduleRestoreCheck(context: Context) {
            // Guard: skip if user is not logged in
            if (FirebaseAuth.getInstance().currentUser == null) return

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

        /**
         * Schedule a periodic backup every 12 hours as a fallback.
         * Only runs if user is logged in.
         */
        fun schedulePeriodicBackup(context: Context) {
            // Guard: skip if user is not logged in
            if (FirebaseAuth.getInstance().currentUser == null) return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = androidx.work.PeriodicWorkRequestBuilder<DriveSyncWorker>(
                12, TimeUnit.HOURS
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
