package com.nueng.translator.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["created_at"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "message_type")
    val messageType: String = "text", // "text", "shared_word"

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
