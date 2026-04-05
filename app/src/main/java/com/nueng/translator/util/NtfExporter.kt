package com.nueng.translator.util

import com.nueng.translator.data.local.entity.UserData
import com.nueng.translator.data.local.entity.UserDirectory
import org.json.JSONArray
import org.json.JSONObject

object NtfExporter {

    // Serialize a directory + its words to a JSON string (.ntf format)
    fun export(directory: UserDirectory, words: List<UserData>): String {
        val root = JSONObject()
        root.put("version",   1)
        root.put("format",    "nueng_translator_file")
        root.put("dirName",   directory.name)
        root.put("exportedAt", System.currentTimeMillis())

        val arr = JSONArray()
        for (w in words) {
            val obj = JSONObject()
            obj.put("word",                       w.word)
            obj.put("wordType",                   w.wordType)
            obj.put("pinyin",                     w.pinyin)
            obj.put("langCode",                   w.langCode)
            obj.put("translation",                w.translation)
            obj.put("translationLangCode",        w.translationLangCode)
            obj.put("exampleSentence",            w.exampleSentence)
            obj.put("translationExampleSentence", w.translationExampleSentence)
            obj.put("createdAt",                  w.createdAt)
            arr.put(obj)
        }
        root.put("words", arr)
        return root.toString(2)
    }
}
