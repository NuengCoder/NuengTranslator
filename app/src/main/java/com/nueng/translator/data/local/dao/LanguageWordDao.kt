package com.nueng.translator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nueng.translator.data.local.entity.LanguageWord
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: LanguageWord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<LanguageWord>)

    @Update
    suspend fun updateWord(word: LanguageWord)

    @Delete
    suspend fun deleteWord(word: LanguageWord)

    @Query("SELECT * FROM language_words WHERE lang_code = :langCode AND translation_lang_code = :translationLangCode ORDER BY word ASC LIMIT :limit OFFSET :offset")
    suspend fun getWordsByLanguagePairPaged(langCode: String, translationLangCode: String, limit: Int, offset: Int): List<LanguageWord>

    @Query("SELECT * FROM language_words WHERE lang_code = :langCode AND translation_lang_code = :translationLangCode ORDER BY word ASC")
    fun getWordsByLanguagePair(langCode: String, translationLangCode: String): Flow<List<LanguageWord>>

    @Query("SELECT * FROM language_words WHERE (lang_code = :langCode AND translation_lang_code = :translationLangCode) AND (word LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%' OR pinyin LIKE '%' || :query || '%') ORDER BY word ASC LIMIT 50")
    fun searchWords(query: String, langCode: String, translationLangCode: String): Flow<List<LanguageWord>>

    @Query("SELECT * FROM language_words ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(limit: Int = 10): List<LanguageWord>

    @Query("SELECT COUNT(*) FROM language_words")
    suspend fun getWordCount(): Int
}
