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
    fun getNotesByUserId(userId: Long): Flow<List<UserData>> =
        userDataDao.getNotesByUserId(userId)

    fun getNotesByDirectory(userId: Long, directoryId: Long): Flow<List<UserData>> =
        userDataDao.getNotesByDirectory(userId, directoryId)

    fun searchNotes(userId: Long, query: String): Flow<List<UserData>> =
        userDataDao.searchNotes(userId, query)

    fun searchNotesInDirectory(userId: Long, directoryId: Long, query: String): Flow<List<UserData>> =
        userDataDao.searchNotesInDirectory(userId, directoryId, query)

    suspend fun addNote(note: UserData): Long = userDataDao.insertNote(note)

    suspend fun updateNote(note: UserData) = userDataDao.updateNote(note)

    suspend fun deleteNote(note: UserData) = userDataDao.deleteNote(note)

    suspend fun deleteAllNotes(userId: Long): Int = userDataDao.deleteAllNotesByUserId(userId)

    suspend fun getNoteCount(userId: Long): Int = userDataDao.getNoteCount(userId)

    suspend fun getNoteCountByDirectory(directoryId: Long): Int =
        userDataDao.getNoteCountByDirectory(directoryId)
}