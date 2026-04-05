package com.nueng.translator.util

import com.nueng.translator.data.local.entity.UserData
import com.nueng.translator.data.local.entity.UserDirectory
import com.nueng.translator.data.repository.UserDataRepository
import com.nueng.translator.data.repository.UserDirectoryRepository
import org.json.JSONObject

object NtfImporter {

    data class ImportResult(
        val directoryName: String,
        val wordCount: Int,
        val success: Boolean,
        val error: String = ""
    )

    // Parse the JSON and insert directory + words into Room for the given userId
    suspend fun import(
        json: String,
        userId: Long,
        directoryRepo: UserDirectoryRepository,
        userDataRepo: UserDataRepository
    ): ImportResult {
        return try {
            val root     = JSONObject(json)
            val dirName  = root.getString("dirName")
            val wordsArr = root.getJSONArray("words")

            // Create directory
            val dir = UserDirectory(
                userId    = userId,
                name      = dirName,
                sortIndex = 0
            )
            val dirId = directoryRepo.addDirectory(dir)

            // Insert words
            var count = 0
            for (i in 0 until wordsArr.length()) {
                val obj = wordsArr.getJSONObject(i)
                val word = UserData(
                    userId                      = userId,
                    directoryId                 = dirId,
                    word                        = obj.optString("word", ""),
                    wordType                    = obj.optString("wordType", ""),
                    pinyin                      = obj.optString("pinyin", ""),
                    langCode                    = obj.optString("langCode", "zh"),
                    translation                 = obj.optString("translation", ""),
                    translationLangCode         = obj.optString("translationLangCode", "en"),
                    exampleSentence             = obj.optString("exampleSentence", ""),
                    translationExampleSentence  = obj.optString("translationExampleSentence", ""),
                    createdAt                   = obj.optLong("createdAt", System.currentTimeMillis())
                )
                if (word.word.isNotBlank()) {
                    userDataRepo.addNote(word)
                    count++
                }
            }

            ImportResult(dirName, count, true)
        } catch (e: Exception) {
            ImportResult("", 0, false, e.message ?: "Parse error")
        }
    }
}
