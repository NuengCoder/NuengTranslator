package com.nueng.translator.data.repository

import com.nueng.translator.data.local.dao.ChatMessageDao
import com.nueng.translator.data.local.entity.ChatMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    fun getRecentMessages(): Flow<List<ChatMessage>> = chatMessageDao.getRecentMessages()

    suspend fun sendMessage(message: ChatMessage): Long = chatMessageDao.insertMessage(message)

    suspend fun cleanupOldMessages(daysOld: Int = 7): Int {
        val cutoff = System.currentTimeMillis() - (daysOld.toLong() * 24 * 60 * 60 * 1000)
        return chatMessageDao.deleteOldMessages(cutoff)
    }
}
