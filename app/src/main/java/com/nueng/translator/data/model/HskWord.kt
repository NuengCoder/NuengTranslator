package com.nueng.translator.data.model

data class HskWord(
    val id: Int,
    val type: String = "",
    val chinese: String,
    val pinyinMap: Map<String, String> = emptyMap(), // "ch" -> "yi", "en" -> "one", "th" -> "Nueng"
    val translations: Map<String, String> = emptyMap(),
    val exampleChinese: String = "",
    val examplePinyin: String = "",
    val exampleTranslations: Map<String, String> = emptyMap(),

    // Convenience
    val pinyin: String = "",
    val english: String = "",
    val exampleEnglish: String = ""
) {
    fun getTranslation(langCode: String): String = translations[langCode] ?: english
    fun getExampleTranslation(langCode: String): String = exampleTranslations[langCode] ?: exampleEnglish
    fun getPinyin(langCode: String): String = pinyinMap[langCode] ?: pinyinMap["ch"] ?: pinyin
}
