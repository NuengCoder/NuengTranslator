package com.nueng.translator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nueng.translator.data.local.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("SELECT * FROM chat_messages ORDER BY created_at DESC LIMIT 100")
    fun getRecentMessages(): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_messages WHERE created_at < :cutoffTime")
    suspend fun deleteOldMessages(cutoffTime: Long): Int
}
