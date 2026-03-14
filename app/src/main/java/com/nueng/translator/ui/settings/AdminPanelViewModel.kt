package com.nueng.translator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            val count = languageWordRepository.getWordCount()
            _uiState.value = _uiState.value.copy(wordCount = count)
        }
        firebaseUserRepository.getAllUsers { users ->
            _uiState.value = _uiState.value.copy(users = users, totalUsers = users.size)
        }
        firebaseUserRepository.getOnlineUserCount { count ->
            _uiState.value = _uiState.value.copy(onlineUsers = count)
        }
    }

    // For Words tab - returns flow of words based on search and lang pair
    fun getWordsFlow(query: String, lang1: String, lang2: String): Flow<List<LanguageWord>> {
        return if (query.isBlank()) {
            languageWordRepository.getWordsByLanguagePair(lang1, lang2)
        } else {
            languageWordRepository.searchWords(query, lang1, lang2)
        }
    }

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
    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
}
