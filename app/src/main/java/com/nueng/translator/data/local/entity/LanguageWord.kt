package com.nueng.translator.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "language_words",
    indices = [
        Index(value = ["lang_code", "translation_lang_code"]),
        Index(value = ["word"]),
        Index(value = ["translation"]),
        Index(value = ["firebase_key"], unique = true)
    ]
)
data class LanguageWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "firebase_key")
    val firebaseKey: String = "",

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

    @ColumnInfo(name = "added_by")
    val addedBy: Long = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
