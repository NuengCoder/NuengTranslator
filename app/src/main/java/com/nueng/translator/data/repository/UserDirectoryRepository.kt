package com.nueng.translator.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.dao.UserDataDao
import com.nueng.translator.data.local.dao.UserDirectoryDao
import com.nueng.translator.data.local.entity.UserDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDirectoryRepository @Inject constructor(
    private val userDirectoryDao: UserDirectoryDao,
    private val userDataDao: UserDataDao
) {
    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    // ── Local Room reads (unchanged) ──────────────────────────────────────
    fun getDirectoriesByUserId(userId: Long): Flow<List<UserDirectory>> =
        userDirectoryDao.getDirectoriesByUserId(userId)

    fun searchDirectories(userId: Long, query: String): Flow<List<UserDirectory>> =
        userDirectoryDao.searchDirectories(userId, query)

    suspend fun getDirectoryCount(userId: Long): Int =
        userDirectoryDao.getDirectoryCount(userId)

    suspend fun updateSortIndex(id: Long, sortIndex: Int) =
        userDirectoryDao.updateSortIndex(id, sortIndex)

    // ── Add ───────────────────────────────────────────────────────────────
    suspend fun addDirectory(directory: UserDirectory, username: String = ""): Long {
        val localId = userDirectoryDao.insertDirectory(directory)
        if (username.isNotBlank() && username != "NuengAdmin") {
            pushDirectoryToFirebase(directory.copy(id = localId), username)
        }
        return localId
    }

    // ── Update ────────────────────────────────────────────────────────────
    suspend fun updateDirectory(directory: UserDirectory, username: String = "") {
        userDirectoryDao.updateDirectory(directory)
        if (username.isNotBlank() && username != "NuengAdmin") {
            pushDirectoryToFirebase(directory, username)
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────
    suspend fun deleteDirectory(directory: UserDirectory, username: String = "") {
        userDataDao.deleteNotesByDirectoryId(directory.id)
        userDirectoryDao.deleteDirectory(directory)
        if (username.isNotBlank() && username != "NuengAdmin") {
            deleteDirectoryFromFirebase(directory, username)
        }
    }

    // ── Firebase: push one directory ──────────────────────────────────────
    private fun pushDirectoryToFirebase(dir: UserDirectory, username: String) {
        val ref = db.getReference("user_notes").child(username)
            .child("directories").child(dir.id.toString())
        val map = mapOf(
            "localId"   to dir.id,
            "name"      to dir.name,
            "sortIndex" to dir.sortIndex,
            "createdAt" to dir.createdAt
        )
        ref.setValue(map)
            .addOnFailureListener { Log.w("DirSync", "push failed: ${it.message}") }
    }

    private fun deleteDirectoryFromFirebase(dir: UserDirectory, username: String) {
        db.getReference("user_notes").child(username)
            .child("directories").child(dir.id.toString()).removeValue()
    }

    // ── Pull: called once on login ─────────────────────────────────────────
    fun pullFromFirebase(userId: Long, username: String) {
        if (username.isBlank() || username == "NuengAdmin") return
        val ref = db.getReference("user_notes").child(username).child("directories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Get all existing local directory names to avoid duplicates
                    val existingNames = userDirectoryDao.getDirectoriesOnceSuspend(userId)
                        .map { it.name }.toSet()
                    for (child in snapshot.children) {
                        try {
                            val name      = child.child("name").getValue(String::class.java) ?: continue
                            val sortIndex = child.child("sortIndex").getValue(Int::class.java) ?: 0
                            val createdAt = child.child("createdAt").getValue(Long::class.java)
                                ?: System.currentTimeMillis()
                            val localId   = child.child("localId").getValue(Long::class.java) ?: 0L
                            // Skip if already exists locally by name
                            if (name in existingNames) {
                                Log.d("DirSync", "Skipped duplicate dir: $name")
                                continue
                            }
                            val dir = UserDirectory(
                                id        = if (localId > 0) localId else 0,
                                userId    = userId,
                                name      = name,
                                sortIndex = sortIndex,
                                createdAt = createdAt
                            )
                            userDirectoryDao.insertDirectory(dir)
                            Log.d("DirSync", "Pulled dir: $name")
                        } catch (e: Exception) {
                            Log.e("DirSync", "parse error: ${e.message}")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DirSync", "pull cancelled: ${error.message}")
            }
        })
    }

    // ── Push all local dirs to Firebase (call after login to ensure sync) ──
    fun pushAllToFirebase(userId: Long, username: String) {
        if (username.isBlank() || username == "NuengAdmin") return
        CoroutineScope(Dispatchers.IO).launch {
            val dirs = userDirectoryDao.getDirectoriesOnceSuspend(userId)
            for (dir in dirs) {
                pushDirectoryToFirebase(dir, username)
            }
            Log.d("DirSync", "Pushed ${dirs.size} dirs to Firebase")
        }
    }
}
