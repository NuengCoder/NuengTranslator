package com.nueng.translator.data.model

data class FirebaseChatMessage(
    val id: String = "",
    val senderName: String = "",
    val message: String = "",
    val messageType: String = "text",
    val timestamp: Long = 0
) {
    // No-arg constructor required by Firebase
    constructor() : this("", "", "", "text", 0)
}
