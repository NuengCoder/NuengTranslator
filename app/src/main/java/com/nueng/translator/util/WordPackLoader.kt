package com.nueng.translator.util

import android.content.Context
import com.nueng.translator.data.model.HskWord
import org.json.JSONObject

object WordPackLoader {

    fun loadFromAssets(context: Context, fileName: String): List<HskWord> {
        return try {
            val actualFile = if (fileName.endsWith(".json")) fileName else "$fileName.json"
            val jsonString = context.assets.open(actualFile).bufferedReader().use { it.readText() }
            parseJson(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseJson(jsonString: String): List<HskWord> {
        val words = mutableListOf<HskWord>()
        val json = JSONObject(jsonString)
        val wordsArray = json.getJSONArray("words")

        for (i in 0 until wordsArray.length()) {
            val w = wordsArray.getJSONObject(i)

            // Parse pinyin - can be string or object
            val pinyinMap = mutableMapOf<String, String>()
            var pinyinStr = ""
            val pinyinRaw = w.get("pinyin")
            if (pinyinRaw is JSONObject) {
                pinyinRaw.keys().forEach { key -> pinyinMap[key] = pinyinRaw.getString(key) }
                pinyinStr = pinyinMap["ch"] ?: ""
            } else {
                pinyinStr = pinyinRaw.toString()
                pinyinMap["ch"] = pinyinStr
            }

            // Parse translations
            val translations = mutableMapOf<String, String>()
            if (w.has("translations")) {
                val transObj = w.getJSONObject("translations")
                transObj.keys().forEach { key -> translations[key] = transObj.getString(key) }
            }

            // Parse example
            val exObj = if (w.has("example")) w.getJSONObject("example") else null
            val exampleTranslations = mutableMapOf<String, String>()
            exObj?.keys()?.forEach { key ->
                if (key != "zh" && key != "pinyin") {
                    exampleTranslations[key] = exObj.getString(key)
                }
            }

            words.add(HskWord(
                id = w.getInt("id"),
                type = w.optString("type", ""),
                chinese = w.getString("word"),
                pinyinMap = pinyinMap,
                pinyin = pinyinStr,
                translations = translations,
                exampleChinese = exObj?.optString("zh", "") ?: "",
                examplePinyin = exObj?.optString("pinyin", "") ?: "",
                exampleTranslations = exampleTranslations,
                english = translations["en"] ?: "",
                exampleEnglish = exampleTranslations["en"] ?: ""
            ))
        }
        return words
    }
}
