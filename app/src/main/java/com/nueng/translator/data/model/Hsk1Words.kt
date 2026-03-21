package com.nueng.translator.data.model

object Hsk1Words {
    // Words loaded dynamically from assets/hsk1.json
    var words: List<HskWord> = emptyList()
        internal set

    fun isLoaded(): Boolean = words.isNotEmpty()
}
