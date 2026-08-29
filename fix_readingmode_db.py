import re

with open('app/src/main/java/com/example/wammy/data/local/WammyDatabase.kt', 'r') as f:
    text = f.read()
text = text.replace('version = 9,', 'version = 10,')
with open('app/src/main/java/com/example/wammy/data/local/WammyDatabase.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'r') as f:
    text = f.read()

migration_code = """val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE manga ADD COLUMN readingMode INTEGER NOT NULL DEFAULT 0")
    }
}"""

if "MIGRATION_9_10" not in text:
    text = text.replace("val MIGRATION_8_9", migration_code + "\n\nval MIGRATION_8_9")
    text = text.replace(".addMigrations(MIGRATION_8_9)", ".addMigrations(MIGRATION_8_9, MIGRATION_9_10)")

with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'w') as f:
    f.write(text)

