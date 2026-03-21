package com.nueng.translator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nueng.translator.data.local.dao.ChatMessageDao
import com.nueng.translator.data.local.dao.LanguageWordDao
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.data.local.dao.UserDataDao
import com.nueng.translator.data.local.dao.UserDirectoryDao
import com.nueng.translator.data.local.entity.ChatMessage
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.local.entity.User
import com.nueng.translator.data.local.entity.UserData
import com.nueng.translator.data.local.entity.UserDirectory

@Database(
    entities = [
        User::class,
        LanguageWord::class,
        UserData::class,
        ChatMessage::class,
        UserDirectory::class
    ],
    version = 6,
    exportSchema = false
)
abstract class NuengTranslatorDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun languageWordDao(): LanguageWordDao
    abstract fun userDataDao(): UserDataDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userDirectoryDao(): UserDirectoryDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add directory_id column to user_data (default 0 = no directory)
                db.execSQL("ALTER TABLE user_data ADD COLUMN directory_id INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_data_directory_id ON user_data(directory_id)")

                // Create user_directories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_directories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        sort_index INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_directories_user_id ON user_directories(user_id)")
            }
        }
    }
}