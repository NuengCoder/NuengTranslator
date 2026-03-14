package com.nueng.translator.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.model.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor() {
    private val database = FirebaseDatabase.getInstance("https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    fun registerUser(username: String, role: String) {
        val userMap = mapOf(
            "username" to username,
            "role" to role,
            "createdAt" to System.currentTimeMillis(),
            "lastOnline" to System.currentTimeMillis()
        )
        usersRef.child(username).setValue(userMap)
    }

    fun updateLastOnline(username: String) {
        usersRef.child(username).child("lastOnline").setValue(System.currentTimeMillis())
    }

    fun getAllUsers(callback: (List<FirebaseUser>) -> Unit) {
        usersRef.orderByChild("lastOnline")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = mutableListOf<FirebaseUser>()
                    for (child in snapshot.children) {
                        try {
                            val user = FirebaseUser(
                                username = child.child("username").getValue(String::class.java) ?: continue,
                                role = child.child("role").getValue(String::class.java) ?: "user",
                                createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0,
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

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    fun getOnlineUserCount(callback: (Int) -> Unit) {
        val fiveMinAgo = System.currentTimeMillis() - 5 * 60 * 1000
        usersRef.orderByChild("lastOnline").startAt(fiveMinAgo.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.childrenCount.toInt())
                }
                override fun onCancelled(error: DatabaseError) {
                    callback(0)
                }
            })
    }
}
