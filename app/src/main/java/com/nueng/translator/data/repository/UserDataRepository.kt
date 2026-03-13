package com.nueng.translator.data.repository

import com.nueng.translator.data.local.dao.UserDataDao
import com.nueng.translator.data.local.entity.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataRepository @Inject constructor(
    private val userDataDao: UserDataDao
) {
    fun getNotesByUserId(userId: Long): Flow<List<UserData>> {
        return userDataDao.getNotesByUserId(userId)
    }

    fun searchNotes(userId: Long, query: String): Flow<List<UserData>> {
        return userDataDao.searchNotes(userId, query)
    }

    suspend fun addNote(note: UserData): Long = userDataDao.insertNote(note)

    suspend fun updateNote(note: UserData) = userDataDao.updateNote(note)

    suspend fun deleteNote(note: UserData) = userDataDao.deleteNote(note)

    suspend fun deleteAllNotes(userId: Long): Int = userDataDao.deleteAllNotesByUserId(userId)

    suspend fun getNoteCount(userId: Long): Int = userDataDao.getNoteCount(userId)
}
