package com.nueng.translator.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.model.FirebaseUser
import com.nueng.translator.data.repository.FirebaseUserRepository
import com.nueng.translator.data.repository.FirebaseWordRepository
import com.nueng.translator.data.repository.LanguageWordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val users: List<FirebaseUser> = emptyList(),
    val totalUsers: Int = 0,
    val onlineUsers: Int = 0,
    val wordCount: Int = 0,
    val addWordSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    private val languageWordRepository: LanguageWordRepository,
    private val firebaseWordRepository: FirebaseWordRepository,
    private val firebaseUserRepository: FirebaseUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    init { loadData() }

    fun loadData() {
        // Word count from local Room
        viewModelScope.launch {
            val count = languageWordRepository.getWordCount()
            _uiState.value = _uiState.value.copy(wordCount = count)
        }

        // ── Read users from accounts/ node (source of truth after fix_003) ─
        // accounts/{username}/ has { username, passwordHash, role, createdAt, lastOnline }
        db.getReference("accounts")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = mutableListOf<FirebaseUser>()
                    for (child in snapshot.children) {
                        try {
                            val username   = child.child("username").getValue(String::class.java)
                                ?: child.key ?: continue
                            val role       = child.child("role").getValue(String::class.java) ?: "user"
                            val createdAt  = child.child("createdAt").getValue(Long::class.java) ?: 0L
                            val lastOnline = child.child("lastOnline").getValue(Long::class.java) ?: 0L
                            users.add(FirebaseUser(username, role, createdAt, lastOnline))
                        } catch (e: Exception) {
                            Log.e("AdminPanel", "parse: ${e.message}")
                        }
                    }
                    // Also merge any users still in legacy users/ node that aren't in accounts/ yet
                    mergeWithLegacyUsers(users)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminPanel", "accounts read cancelled: ${error.message}")
                    // Fallback to legacy users/ node
                    firebaseUserRepository.getAllUsers { legacyUsers ->
                        _uiState.value = _uiState.value.copy(
                            users      = legacyUsers,
                            totalUsers = legacyUsers.size
                        )
                    }
                }
            })

        // Online count still from users/ (last online based)
        firebaseUserRepository.getOnlineUserCount { count ->
            _uiState.value = _uiState.value.copy(onlineUsers = count)
        }
    }

    private fun mergeWithLegacyUsers(accountsUsers: MutableList<FirebaseUser>) {
        db.getReference("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val existingUsernames = accountsUsers.map { it.username }.toSet()
                    for (child in snapshot.children) {
                        try {
                            val username = child.child("username").getValue(String::class.java)
                                ?: child.key ?: continue
                            // Skip if already in accounts list
                            if (username in existingUsernames) continue
                            val role       = child.child("role").getValue(String::class.java) ?: "user"
                            val createdAt  = child.child("createdAt").getValue(Long::class.java) ?: 0L
                            val lastOnline = child.child("lastOnline").getValue(Long::class.java) ?: 0L
                            accountsUsers.add(FirebaseUser(username, role, createdAt, lastOnline))
                        } catch (_: Exception) { /* skip */ }
                    }
                    val sorted = accountsUsers.sortedByDescending { it.lastOnline }
                    _uiState.value = _uiState.value.copy(
                        users      = sorted,
                        totalUsers = sorted.size
                    )
                }
                override fun onCancelled(error: DatabaseError) {
                    val sorted = accountsUsers.sortedByDescending { it.lastOnline }
                    _uiState.value = _uiState.value.copy(users = sorted, totalUsers = sorted.size)
                }
            })
    }

    fun getWordsFlow(query: String, lang1: String, lang2: String): Flow<List<LanguageWord>> =
        if (query.isBlank()) languageWordRepository.getWordsByLanguagePair(lang1, lang2)
        else languageWordRepository.searchWords(query, lang1, lang2)

    fun addWord(
        word: String, pinyin: String, langCode: String,
        translation: String, translationLangCode: String,
        exampleSentence: String, translationExampleSentence: String,
        adminUserId: Long, wordType: String = ""
    ) {
        if (word.isBlank() || translation.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Word and translation are required")
            return
        }
        viewModelScope.launch {
            val languageWord = LanguageWord(
                word = word.trim(), wordType = wordType.trim(), pinyin = pinyin.trim(),
                langCode = langCode, translation = translation.trim(),
                translationLangCode = translationLangCode,
                exampleSentence = exampleSentence.trim(),
                translationExampleSentence = translationExampleSentence.trim(),
                addedBy = adminUserId
            )
            val firebaseKey = firebaseWordRepository.pushWordToFirebase(languageWord)
            languageWordRepository.addWord(languageWord.copy(firebaseKey = firebaseKey))
            val count = languageWordRepository.getWordCount()
            _uiState.value = _uiState.value.copy(wordCount = count, addWordSuccess = true)
        }
    }

    fun updateWord(word: LanguageWord) {
        viewModelScope.launch {
            languageWordRepository.updateWord(word)
            firebaseWordRepository.updateWordOnFirebase(word)
        }
    }

    fun deleteWord(word: LanguageWord) {
        viewModelScope.launch {
            languageWordRepository.deleteWord(word)
            firebaseWordRepository.deleteWordFromFirebase(word)
            val count = languageWordRepository.getWordCount()
            _uiState.value = _uiState.value.copy(wordCount = count)
        }
    }

    fun clearAddWordSuccess() { _uiState.value = _uiState.value.copy(addWordSuccess = false) }
    fun clearError()          { _uiState.value = _uiState.value.copy(errorMessage = null) }
}
