package com.nueng.translator.util

data class Language(
    val code: String,
    val nameEn: String,
    val nameNative: String
)

object Languages {
    val SUPPORTED = listOf(
        Language("en", "English", "English"),
        Language("th", "Thai", "ไทย"),
        Language("zh", "Chinese", "中文"),
        Language("lo", "Lao", "ລາວ"),
        Language("vi", "Vietnamese", "Tiếng Việt"),
        Language("id", "Indonesian", "Bahasa Indonesia")
    )

    fun getByCode(code: String): Language? = SUPPORTED.find { it.code == code }

    fun getDisplayName(code: String): String {
        return getByCode(code)?.nameNative ?: code
    }
}
