package com.nueng.translator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nueng.translator.data.local.entity.UserData
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: UserData): Long

    @Update
    suspend fun updateNote(note: UserData)

    @Delete
    suspend fun deleteNote(note: UserData)

    // All notes for user (legacy / orphan check)
    @Query("SELECT * FROM user_data WHERE user_id = :userId ORDER BY created_at DESC")
    fun getNotesByUserId(userId: Long): Flow<List<UserData>>

    // Notes inside a specific directory
    @Query("SELECT * FROM user_data WHERE user_id = :userId AND directory_id = :directoryId ORDER BY created_at DESC")
    fun getNotesByDirectory(userId: Long, directoryId: Long): Flow<List<UserData>>

    // Search inside a specific directory
    @Query("SELECT * FROM user_data WHERE user_id = :userId AND directory_id = :directoryId AND (word LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%') ORDER BY created_at DESC")
    fun searchNotesInDirectory(userId: Long, directoryId: Long, query: String): Flow<List<UserData>>

    // Search across all notes
    @Query("SELECT * FROM user_data WHERE user_id = :userId AND (word LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%') ORDER BY created_at DESC")
    fun searchNotes(userId: Long, query: String): Flow<List<UserData>>

    @Query("DELETE FROM user_data WHERE user_id = :userId")
    suspend fun deleteAllNotesByUserId(userId: Long): Int

    @Query("DELETE FROM user_data WHERE directory_id = :directoryId")
    suspend fun deleteNotesByDirectoryId(directoryId: Long): Int

    @Query("SELECT COUNT(*) FROM user_data WHERE user_id = :userId")
    suspend fun getNoteCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM user_data WHERE directory_id = :directoryId")
    suspend fun getNoteCountByDirectory(directoryId: Long): Int
}