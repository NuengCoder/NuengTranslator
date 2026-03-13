package com.nueng.translator.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_data",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["user_id", "word"])
    ]
)
data class UserData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "word_type")
    val wordType: String = "",

    @ColumnInfo(name = "pinyin")
    val pinyin: String = "",

    @ColumnInfo(name = "lang_code")
    val langCode: String,

    @ColumnInfo(name = "translation")
    val translation: String,

    @ColumnInfo(name = "translation_lang_code")
    val translationLangCode: String,

    @ColumnInfo(name = "example_sentence")
    val exampleSentence: String = "",

    @ColumnInfo(name = "translation_example_sentence")
    val translationExampleSentence: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
