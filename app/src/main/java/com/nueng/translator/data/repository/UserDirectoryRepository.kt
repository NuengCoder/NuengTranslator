package com.nueng.translator.data.repository

import com.nueng.translator.data.local.dao.UserDataDao
import com.nueng.translator.data.local.dao.UserDirectoryDao
import com.nueng.translator.data.local.entity.UserDirectory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDirectoryRepository @Inject constructor(
    private val userDirectoryDao: UserDirectoryDao,
    private val userDataDao: UserDataDao
) {
    fun getDirectoriesByUserId(userId: Long): Flow<List<UserDirectory>> =
        userDirectoryDao.getDirectoriesByUserId(userId)

    fun searchDirectories(userId: Long, query: String): Flow<List<UserDirectory>> =
        userDirectoryDao.searchDirectories(userId, query)

    suspend fun addDirectory(directory: UserDirectory): Long =
        userDirectoryDao.insertDirectory(directory)

    suspend fun updateDirectory(directory: UserDirectory) =
        userDirectoryDao.updateDirectory(directory)

    suspend fun deleteDirectory(directory: UserDirectory) {
        // Delete all words inside the directory first, then delete directory
        userDataDao.deleteNotesByDirectoryId(directory.id)
        userDirectoryDao.deleteDirectory(directory)
    }

    suspend fun updateSortIndex(id: Long, sortIndex: Int) =
        userDirectoryDao.updateSortIndex(id, sortIndex)

    suspend fun getDirectoryCount(userId: Long): Int =
        userDirectoryDao.getDirectoryCount(userId)
}
