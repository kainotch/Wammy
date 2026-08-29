// Created by Notch
package com.example.wammy

import android.content.Context
import androidx.room.Room
import com.example.wammy.data.local.WammyDatabase
import com.example.wammy.data.remote.extensions.ExtensionApi
import com.example.wammy.extension.ExtensionManager
import com.example.wammy.data.remote.mangadex.MangaDexApi
import com.example.wammy.source.MangaDexSource
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppContainer {
    lateinit var appContext: Context
    lateinit var database: WammyDatabase
    lateinit var readerPreferences: com.example.wammy.data.prefs.ReaderPreferences
    lateinit var extensionApi: ExtensionApi
    lateinit var extensionManager: ExtensionManager
    lateinit var trackManager: com.example.wammy.track.TrackManager
    lateinit var backupManager: com.example.wammy.data.backup.BackupManager
    lateinit var restoreManager: com.example.wammy.data.backup.RestoreManager
    lateinit var cacheManager: com.example.wammy.data.backup.CacheManager
    lateinit var mangaDexSource: MangaDexSource
    lateinit var themePreferences: com.example.wammy.theme.ThemePreferences
    lateinit var storagePreferences: com.example.wammy.theme.StoragePreferences

    fun init(context: Context) {
        appContext = context.applicationContext
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE manga ADD COLUMN readingMode INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE manga ADD COLUMN isNovel INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE manga ADD COLUMN novelPkgName TEXT")
                database.execSQL("ALTER TABLE manga ADD COLUMN novelApkFile TEXT")
            }
        }
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chapter ADD COLUMN scanlator TEXT")
            }
        }
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add orientation column (try-catch in case column already exists from partial migration)
                try {
                    database.execSQL("ALTER TABLE manga ADD COLUMN orientation INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) { /* column already exists */ }
                
                // Migrate existing readingMode values:
                // Old: 0=RTL, 1=LTR, 2=WEBTOON
                // New: 0=DEFAULT, 1=LTR, 2=RTL, 3=VERTICAL, 4=WEBTOON, 5=CONTINUOUS_VERTICAL
                database.execSQL("UPDATE manga SET readingMode = 2 WHERE readingMode = 0") // RTL -> RTL
                database.execSQL("UPDATE manga SET readingMode = 4 WHERE readingMode = 2") // WEBTOON -> WEBTOON
            }
        }
        database = Room.databaseBuilder(
            context.applicationContext,
            WammyDatabase::class.java,
            "wammy.db"
        )
        .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
        .fallbackToDestructiveMigration()
        .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(MangaDexApi::class.java)
        mangaDexSource = MangaDexSource(api)
        extensionManager = ExtensionManager(context)
        trackManager = com.example.wammy.track.TrackManager(context)
        backupManager = com.example.wammy.data.backup.BackupManager(context, database)
        restoreManager = com.example.wammy.data.backup.RestoreManager(context, database)
        cacheManager = com.example.wammy.data.backup.CacheManager(context)
        themePreferences = com.example.wammy.theme.ThemePreferences(context)
        storagePreferences = com.example.wammy.theme.StoragePreferences(context)

        val extRetrofit = Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        extensionApi = extRetrofit.create(ExtensionApi::class.java)

    }
}
