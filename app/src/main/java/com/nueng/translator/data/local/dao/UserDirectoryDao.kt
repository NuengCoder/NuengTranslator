package com.nueng.translator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nueng.translator.data.local.entity.UserDirectory
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDirectoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectory(directory: UserDirectory): Long

    @Update
    suspend fun updateDirectory(directory: UserDirectory)

    @Delete
    suspend fun deleteDirectory(directory: UserDirectory)

    @Query("SELECT * FROM user_directories WHERE user_id = :userId ORDER BY sort_index ASC, created_at ASC")
    fun getDirectoriesByUserId(userId: Long): Flow<List<UserDirectory>>

    @Query("SELECT * FROM user_directories WHERE user_id = :userId AND name LIKE '%' || :query || '%' ORDER BY sort_index ASC")
    fun searchDirectories(userId: Long, query: String): Flow<List<UserDirectory>>

    @Query("SELECT COUNT(*) FROM user_directories WHERE user_id = :userId")
    suspend fun getDirectoryCount(userId: Long): Int

    @Query("UPDATE user_directories SET sort_index = :sortIndex WHERE id = :id")
    suspend fun updateSortIndex(id: Long, sortIndex: Int)

    @Query("DELETE FROM user_directories WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: Long)
}
