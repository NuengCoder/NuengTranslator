package com.nueng.translator.data.repository

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.dao.LanguageWordDao
import com.nueng.translator.data.local.entity.LanguageWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseWordRepository @Inject constructor(
    private val languageWordDao: LanguageWordDao
) {
    private val database = FirebaseDatabase.getInstance("https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val wordsRef = database.getReference("language_words")
    private val hasStartedListener = AtomicBoolean(false)
    private var childListener: ChildEventListener? = null

    fun pushWordToFirebase(word: LanguageWord): String {
        val key = wordsRef.push().key ?: return ""
        wordsRef.child(key).setValue(mapOf(
            "word" to word.word, "wordType" to word.wordType,
            "pinyin" to word.pinyin, "langCode" to word.langCode,
            "translation" to word.translation,
            "translationLangCode" to word.translationLangCode,
            "exampleSentence" to word.exampleSentence,
            "translationExampleSentence" to word.translationExampleSentence,
            "createdAt" to word.createdAt
        ))
        return key
    }

    fun updateWordOnFirebase(word: LanguageWord) {
        if (word.firebaseKey.isBlank()) return
        wordsRef.child(word.firebaseKey).setValue(mapOf(
            "word" to word.word, "wordType" to word.wordType,
            "pinyin" to word.pinyin, "langCode" to word.langCode,
            "translation" to word.translation,
            "translationLangCode" to word.translationLangCode,
            "exampleSentence" to word.exampleSentence,
            "translationExampleSentence" to word.translationExampleSentence,
            "createdAt" to word.createdAt
        ))
    }

    fun deleteWordFromFirebase(word: LanguageWord) {
        if (word.firebaseKey.isBlank()) return
        wordsRef.child(word.firebaseKey).removeValue()
    }

    // REAL-TIME sync: listens for changes continuously
    fun startRealtimeSync() {
        if (!hasStartedListener.compareAndSet(false, true)) return

        childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                upsertFromSnapshot(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                upsertFromSnapshot(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val key = snapshot.key ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    languageWordDao.deleteByFirebaseKey(key)
                    Log.d("FirebaseWord", "Deleted: $key")
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseWord", "Listener cancelled: ${error.message}")
                hasStartedListener.set(false)
            }
        }

        wordsRef.addChildEventListener(childListener!!)
        Log.d("FirebaseWord", "Real-time word sync started")
    }

    private fun upsertFromSnapshot(snapshot: DataSnapshot) {
        val key = snapshot.key ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = languageWordDao.getByFirebaseKey(key)
                val word = LanguageWord(
                    id = existing?.id ?: 0,
                    firebaseKey = key,
                    word = snapshot.child("word").getValue(String::class.java) ?: return@launch,
                    wordType = snapshot.child("wordType").getValue(String::class.java) ?: "",
                    pinyin = snapshot.child("pinyin").getValue(String::class.java) ?: "",
                    langCode = snapshot.child("langCode").getValue(String::class.java) ?: return@launch,
                    translation = snapshot.child("translation").getValue(String::class.java) ?: return@launch,
                    translationLangCode = snapshot.child("translationLangCode").getValue(String::class.java) ?: return@launch,
                    exampleSentence = snapshot.child("exampleSentence").getValue(String::class.java) ?: "",
                    translationExampleSentence = snapshot.child("translationExampleSentence").getValue(String::class.java) ?: "",
                    createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                )
                languageWordDao.insertWord(word)
            } catch (e: Exception) {
                Log.e("FirebaseWord", "Upsert error: ${e.message}")
            }
        }
    }

    fun stopListener() {
        childListener?.let { wordsRef.removeEventListener(it) }
        hasStartedListener.set(false)
    }
}
