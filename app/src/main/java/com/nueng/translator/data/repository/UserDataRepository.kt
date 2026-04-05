package com.nueng.translator.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.dao.UserDataDao
import com.nueng.translator.data.local.entity.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataRepository @Inject constructor(
    private val userDataDao: UserDataDao
) {
    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    fun getNotesByDirectory(userId: Long, directoryId: Long): Flow<List<UserData>> =
        userDataDao.getNotesByDirectory(userId, directoryId)

    fun searchNotesInDirectory(userId: Long, directoryId: Long, query: String): Flow<List<UserData>> =
        userDataDao.searchNotesInDirectory(userId, directoryId, query)

    suspend fun deleteAllNotes(userId: Long): Int = userDataDao.deleteAllNotesByUserId(userId)

    // ── Add ───────────────────────────────────────────────────────────────
    suspend fun addNote(note: UserData, username: String = ""): Long {
        val localId = userDataDao.insertNote(note)
        if (username.isNotBlank() && username != "NuengAdmin") {
            pushNoteToFirebase(note.copy(id = localId), username)
        }
        return localId
    }

    // ── Update ────────────────────────────────────────────────────────────
    suspend fun updateNote(note: UserData, username: String = "") {
        userDataDao.updateNote(note)
        if (username.isNotBlank() && username != "NuengAdmin") {
            pushNoteToFirebase(note, username)
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────
    suspend fun deleteNote(note: UserData, username: String = "") {
        userDataDao.deleteNote(note)
        if (username.isNotBlank() && username != "NuengAdmin") {
            deleteNoteFromFirebase(note, username)
        }
    }

    // ── Firebase helpers ──────────────────────────────────────────────────
    private fun pushNoteToFirebase(note: UserData, username: String) {
        val ref = db.getReference("user_notes").child(username)
            .child("words").child(note.id.toString())
        val map = mapOf(
            "localId"                     to note.id,
            "directoryId"                 to note.directoryId,
            "word"                        to note.word,
            "wordType"                    to note.wordType,
            "pinyin"                      to note.pinyin,
            "langCode"                    to note.langCode,
            "translation"                 to note.translation,
            "translationLangCode"         to note.translationLangCode,
            "exampleSentence"             to note.exampleSentence,
            "translationExampleSentence"  to note.translationExampleSentence,
            "createdAt"                   to note.createdAt
        )
        ref.setValue(map)
            .addOnFailureListener { Log.w("NoteSync", "push failed: ${it.message}") }
    }

    private fun deleteNoteFromFirebase(note: UserData, username: String) {
        db.getReference("user_notes").child(username)
            .child("words").child(note.id.toString()).removeValue()
    }

    // ── Pull: called once on login ─────────────────────────────────────────
    fun pullFromFirebase(userId: Long, username: String) {
        if (username.isBlank() || username == "NuengAdmin") return
        val ref = db.getReference("user_notes").child(username).child("words")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    for (child in snapshot.children) {
                        try {
                            val localId   = child.child("localId").getValue(Long::class.java) ?: 0L
                            val word      = child.child("word").getValue(String::class.java) ?: continue
                            val langCode  = child.child("langCode").getValue(String::class.java) ?: continue
                            val trans     = child.child("translation").getValue(String::class.java) ?: continue
                            val transLang = child.child("translationLangCode").getValue(String::class.java) ?: continue
                            val note = UserData(
                                id                         = if (localId > 0) localId else 0,
                                userId                     = userId,
                                directoryId                = child.child("directoryId").getValue(Long::class.java) ?: 0L,
                                word                       = word,
                                wordType                   = child.child("wordType").getValue(String::class.java) ?: "",
                                pinyin                     = child.child("pinyin").getValue(String::class.java) ?: "",
                                langCode                   = langCode,
                                translation                = trans,
                                translationLangCode        = transLang,
                                exampleSentence            = child.child("exampleSentence").getValue(String::class.java) ?: "",
                                translationExampleSentence = child.child("translationExampleSentence").getValue(String::class.java) ?: "",
                                createdAt                  = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                            )
                            userDataDao.insertNote(note)
                            Log.d("NoteSync", "Pulled note: $word")
                        } catch (e: Exception) {
                            Log.e("NoteSync", "parse error: ${e.message}")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("NoteSync", "pull cancelled: ${error.message}")
            }
        })
    }
}
