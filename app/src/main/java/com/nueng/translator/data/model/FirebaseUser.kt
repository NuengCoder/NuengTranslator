package com.nueng.translator.data.model

data class FirebaseUser(
    val username: String = "",
    val role: String = "user",
    val createdAt: Long = 0,
    val lastOnline: Long = 0
) {
    constructor() : this("", "user", 0, 0)
}
