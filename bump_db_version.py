import re

with open('app/src/main/java/com/example/wammy/data/local/WammyDatabase.kt', 'r') as f:
    text = f.read()

text = text.replace("version = 10,", "version = 11,")

with open('app/src/main/java/com/example/wammy/data/local/WammyDatabase.kt', 'w') as f:
    f.write(text)


with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'r') as f:
    text = f.read()

mig_10_11 = """val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chapter ADD COLUMN scanlator TEXT")
            }
        }
        database = Room.databaseBuilder"""

text = text.replace("database = Room.databaseBuilder", mig_10_11)
text = text.replace(".addMigrations(MIGRATION_8_9, MIGRATION_9_10)", ".addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)")

with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'w') as f:
    f.write(text)

