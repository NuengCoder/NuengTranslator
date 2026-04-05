package com.nueng.translator.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.model.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class FirebaseAccountData(
    val username: String = "",
    val passwordHash: String = "",
    val role: String = "user",
    val createdAt: Long = 0L,
    val lastOnline: Long = 0L
)

@Singleton
class FirebaseUserRepository @Inject constructor() {
    private val database = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private val usersRef    = database.getReference("users")
    private val accountsRef = database.getReference("accounts") // cross-device login source

    // ── Register: write account to Firebase so other devices can log in ──
    fun registerUser(username: String, role: String) {
        val userMap = mapOf(
            "username"   to username,
            "role"       to role,
            "createdAt"  to System.currentTimeMillis(),
            "lastOnline" to System.currentTimeMillis()
        )
        usersRef.child(username).setValue(userMap)
    }

    // Write password hash to accounts/ node (separate from public users/ node)
    fun saveAccountCredentials(username: String, passwordHash: String, role: String) {
        val map = mapOf(
            "username"     to username,
            "passwordHash" to passwordHash,
            "role"         to role,
            "createdAt"    to System.currentTimeMillis(),
            "lastOnline"   to System.currentTimeMillis()
        )
        accountsRef.child(username).setValue(map)
    }

    // ── Check if username already taken on Firebase ──────────────────────
    suspend fun checkUsernameExists(username: String): Boolean =
        suspendCancellableCoroutine { cont ->
            accountsRef.child(username).get()
                .addOnSuccessListener { snapshot -> cont.resume(snapshot.exists()) }
                .addOnFailureListener { cont.resume(false) } // treat failure as "not taken"
        }

    // ── Pull account from Firebase (for login on new device) ─────────────
    suspend fun getAccountFromFirebase(username: String): FirebaseAccountData? =
        suspendCancellableCoroutine { cont ->
            accountsRef.child(username).get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) { cont.resume(null); return@addOnSuccessListener }
                    try {
                        val data = FirebaseAccountData(
                            username     = snapshot.child("username").getValue(String::class.java) ?: username,
                            passwordHash = snapshot.child("passwordHash").getValue(String::class.java) ?: "",
                            role         = snapshot.child("role").getValue(String::class.java) ?: "user",
                            createdAt    = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
                            lastOnline   = snapshot.child("lastOnline").getValue(Long::class.java) ?: 0L
                        )
                        cont.resume(data)
                    } catch (e: Exception) {
                        Log.e("FirebaseUser", "getAccount parse: ${e.message}")
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    Log.e("FirebaseUser", "getAccount failed: ${it.message}")
                    cont.resume(null)
                }
        }

    fun updateLastOnline(username: String) {
        val now = System.currentTimeMillis()
        usersRef.child(username).child("lastOnline").setValue(now)
        accountsRef.child(username).child("lastOnline").setValue(now)
    }

    fun getAllUsers(callback: (List<FirebaseUser>) -> Unit) {
        usersRef.orderByChild("lastOnline")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = mutableListOf<FirebaseUser>()
                    for (child in snapshot.children) {
                        try {
                            val user = FirebaseUser(
                                username   = child.child("username").getValue(String::class.java) ?: continue,
                                role       = child.child("role").getValue(String::class.java) ?: "user",
                                createdAt  = child.child("createdAt").getValue(Long::class.java) ?: 0,
                                lastOnline = child.child("lastOnline").getValue(Long::class.java) ?: 0
                            )
                            users.add(user)
                        } catch (e: Exception) {
                            Log.e("FirebaseUser", "Parse: ${e.message}")
                        }
                    }
                    users.sortByDescending { it.lastOnline }
                    callback(users)
                }
                override fun onCancelled(error: DatabaseError) { callback(emptyList()) }
            })
    }

    fun getOnlineUserCount(callback: (Int) -> Unit) {
        val fiveMinAgo = System.currentTimeMillis() - 5 * 60 * 1000
        usersRef.orderByChild("lastOnline").startAt(fiveMinAgo.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { callback(snapshot.childrenCount.toInt()) }
                override fun onCancelled(error: DatabaseError) { callback(0) }
            })
    }
}
