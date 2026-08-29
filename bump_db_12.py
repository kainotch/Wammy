import re

with open('app/src/main/java/com/example/wammy/data/local/WammyDatabase.kt', 'r') as f:
    text = f.read()

text = text.replace("version = 11,", "version = 12,")

with open('app/src/main/java/com/example/wammy/data/local/WammyDatabase.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'r') as f:
    text = f.read()

mig_11_12 = """val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add orientation column
                database.execSQL("ALTER TABLE MangaEntity ADD COLUMN orientation INTEGER NOT NULL DEFAULT 0")
                
                // Migrate existing readingMode values:
                // Old: 0=RTL, 1=LTR, 2=WEBTOON
                // New: 0=DEFAULT, 1=LTR, 2=RTL, 3=VERTICAL, 4=WEBTOON, 5=CONTINUOUS_VERTICAL
                database.execSQL("UPDATE MangaEntity SET readingMode = 2 WHERE readingMode = 0") // RTL -> RTL
                database.execSQL("UPDATE MangaEntity SET readingMode = 1 WHERE readingMode = 1") // LTR -> LTR (no-op, but for clarity)
                database.execSQL("UPDATE MangaEntity SET readingMode = 4 WHERE readingMode = 2") // WEBTOON -> WEBTOON
            }
        }
        database = Room.databaseBuilder"""

text = text.replace("database = Room.databaseBuilder", mig_11_12)
text = text.replace(".addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)", ".addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)")

with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'w') as f:
    f.write(text)

