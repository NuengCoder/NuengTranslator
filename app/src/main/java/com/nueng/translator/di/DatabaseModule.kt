package com.nueng.translator.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nueng.translator.data.local.NuengTranslatorDatabase
import com.nueng.translator.data.local.dao.ChatMessageDao
import com.nueng.translator.data.local.dao.LanguageWordDao
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.data.local.dao.UserDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        userDaoProvider: Provider<UserDao>
    ): NuengTranslatorDatabase {
        return Room.databaseBuilder(
            context,
            NuengTranslatorDatabase::class.java,
            "NuengTranslator.db"
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val userDao = userDaoProvider.get()
                    val hash = hashPassword("362@Admin621DotNueng")
                    userDao.insertUser(
                        com.nueng.translator.data.local.entity.User(
                            username = "NuengAdmin",
                            passwordHash = hash,
                            role = "admin"
                        )
                    )
                }
            }
        })
        .build()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Provides
    @Singleton
    fun provideUserDao(database: NuengTranslatorDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideLanguageWordDao(database: NuengTranslatorDatabase): LanguageWordDao = database.languageWordDao()

    @Provides
    @Singleton
    fun provideUserDataDao(database: NuengTranslatorDatabase): UserDataDao = database.userDataDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(database: NuengTranslatorDatabase): ChatMessageDao = database.chatMessageDao()
}
