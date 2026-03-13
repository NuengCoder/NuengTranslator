package com.nueng.translator.data.repository

import com.nueng.translator.data.local.dao.LanguageWordDao
import com.nueng.translator.data.local.entity.LanguageWord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageWordRepository @Inject constructor(
    private val languageWordDao: LanguageWordDao
) {
    fun getWordsByLanguagePair(lang1: String, lang2: String): Flow<List<LanguageWord>> {
        return languageWordDao.getWordsByLanguagePair(lang1, lang2)
    }

    fun searchWords(query: String, lang1: String, lang2: String): Flow<List<LanguageWord>> {
        return languageWordDao.searchWords(query, lang1, lang2)
    }

    suspend fun getRandomWords(limit: Int = 10): List<LanguageWord> {
        return languageWordDao.getRandomWords(limit)
    }

    suspend fun getWordCount(): Int = languageWordDao.getWordCount()

    // Admin operations
    suspend fun addWord(word: LanguageWord): Long = languageWordDao.insertWord(word)

    suspend fun addWords(words: List<LanguageWord>) = languageWordDao.insertWords(words)

    suspend fun updateWord(word: LanguageWord) = languageWordDao.updateWord(word)

    suspend fun deleteWord(word: LanguageWord) = languageWordDao.deleteWord(word)
}
