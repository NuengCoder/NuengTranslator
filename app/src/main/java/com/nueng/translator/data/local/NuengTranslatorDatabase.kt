package com.nueng.translator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nueng.translator.data.local.dao.ChatMessageDao
import com.nueng.translator.data.local.dao.LanguageWordDao
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.data.local.dao.UserDataDao
import com.nueng.translator.data.local.entity.ChatMessage
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.local.entity.User
import com.nueng.translator.data.local.entity.UserData

@Database(
    entities = [
        User::class,
        LanguageWord::class,
        UserData::class,
        ChatMessage::class
    ],
    version = 4,
    exportSchema = false
)
abstract class NuengTranslatorDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun languageWordDao(): LanguageWordDao
    abstract fun userDataDao(): UserDataDao
    abstract fun chatMessageDao(): ChatMessageDao
}
