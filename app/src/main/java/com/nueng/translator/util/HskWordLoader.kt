package com.nueng.translator.util

import android.content.Context
import android.util.Log
import com.nueng.translator.data.local.dao.LanguageWordDao
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.model.Hsk1Words
import com.nueng.translator.data.model.HskWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HskWordLoader {

    private val HSK_FILES = listOf("hsk1.json", "hsk2.json", "hsk3.json", "hsk4.json", "hsk5.json", "hsk6.json")
    private val IELTS_FILES = listOf("ielts1.json", "ielts2.json", "ielts3.json", "ielts4.json", "ielts5.json", "ielts6.json")

    suspend fun loadAll(context: Context, languageWordDao: LanguageWordDao) {
        withContext(Dispatchers.IO) {
            for (fileName in HSK_FILES) {
                loadBidirectional(context, languageWordDao, fileName, isHsk = true)
            }
            for (fileName in IELTS_FILES) {
                loadBidirectional(context, languageWordDao, fileName, isHsk = false)
            }
            // Set HSK1 in memory
            val hsk1 = WordPackLoader.loadFromAssets(context, "hsk1.json")
            if (hsk1.isNotEmpty()) Hsk1Words.words = hsk1
        }
    }

    private suspend fun loadBidirectional(
        context: Context, dao: LanguageWordDao, fileName: String, isHsk: Boolean
    ) {
        val words = WordPackLoader.loadFromAssets(context, fileName)
        if (words.isEmpty()) return

        val packPrefix = fileName.removeSuffix(".json") + "_local_"
        val dbWords = mutableListOf<LanguageWord>()

        for (hskWord in words) {
            if (isHsk) {
                // HSK: source is Chinese
                val targetLangs = listOf("en", "th", "lo", "vi", "id")
                for (lang in targetLangs) {
                    val translation = hskWord.translations[lang] ?: continue
                    if (translation.isBlank()) continue

                    // FORWARD: zh -> lang
                    val fwdKey = "${packPrefix}${hskWord.id}_zh_$lang"
                    if (dao.getByFirebaseKey(fwdKey) == null) {
                        dbWords.add(LanguageWord(
                            firebaseKey = fwdKey,
                            word = hskWord.chinese,
                            wordType = hskWord.type,
                            pinyin = hskWord.getPinyin("ch"),
                            langCode = "zh",
                            translation = translation,
                            translationLangCode = lang,
                            exampleSentence = hskWord.exampleChinese,
                            translationExampleSentence = hskWord.getExampleTranslation(lang)
                        ))
                    }

                    // REVERSE: lang -> zh
                    val revKey = "${packPrefix}${hskWord.id}_${lang}_zh"
                    if (dao.getByFirebaseKey(revKey) == null) {
                        dbWords.add(LanguageWord(
                            firebaseKey = revKey,
                            word = translation,
                            wordType = hskWord.type,
                            pinyin = hskWord.getPinyin(lang),
                            langCode = lang,
                            translation = hskWord.chinese,
                            translationLangCode = "zh",
                            exampleSentence = hskWord.getExampleTranslation(lang),
                            translationExampleSentence = hskWord.exampleChinese
                        ))
                    }
                }
            } else {
                // IELTS: source is English
                val sourceWord = hskWord.english.ifBlank { hskWord.chinese }
                if (sourceWord.isBlank()) continue
                val targetLangs = listOf("zh", "th", "lo", "vi", "id")

                for (lang in targetLangs) {
                    val translation = hskWord.translations[lang] ?: continue
                    if (translation.isBlank()) continue

                    // FORWARD: en -> lang
                    val fwdKey = "${packPrefix}${hskWord.id}_en_$lang"
                    if (dao.getByFirebaseKey(fwdKey) == null) {
                        dbWords.add(LanguageWord(
                            firebaseKey = fwdKey,
                            word = sourceWord,
                            wordType = hskWord.type,
                            pinyin = hskWord.getPinyin("en"),
                            langCode = "en",
                            translation = translation,
                            translationLangCode = lang,
                            exampleSentence = hskWord.getExampleTranslation("en"),
                            translationExampleSentence = hskWord.getExampleTranslation(lang)
                        ))
                    }

                    // REVERSE: lang -> en
                    val revKey = "${packPrefix}${hskWord.id}_${lang}_en"
                    if (dao.getByFirebaseKey(revKey) == null) {
                        dbWords.add(LanguageWord(
                            firebaseKey = revKey,
                            word = translation,
                            wordType = hskWord.type,
                            pinyin = hskWord.getPinyin(lang),
                            langCode = lang,
                            translation = sourceWord,
                            translationLangCode = "en",
                            exampleSentence = hskWord.getExampleTranslation(lang),
                            translationExampleSentence = hskWord.getExampleTranslation("en")
                        ))
                    }
                }
            }
        }

        if (dbWords.isNotEmpty()) {
            dao.insertWords(dbWords)
            Log.d("HskLoader", "$fileName: ${dbWords.size} bidirectional entries")
        }
    }

    suspend fun loadHsk1(context: Context, languageWordDao: LanguageWordDao) {
        loadAll(context, languageWordDao)
    }
}
