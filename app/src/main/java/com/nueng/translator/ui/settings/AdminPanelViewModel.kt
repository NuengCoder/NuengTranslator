package com.nueng.translator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.local.entity.User
import com.nueng.translator.data.repository.LanguageWordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val users: List<User> = emptyList(),
    val wordCount: Int = 0,
    val addWordSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    private val userDao: UserDao,
    private val languageWordRepository: LanguageWordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val count = languageWordRepository.getWordCount()
            _uiState.value = _uiState.value.copy(wordCount = count)
        }
    }

    fun addWord(
        word: String,
        pinyin: String,
        langCode: String,
        translation: String,
        translationLangCode: String,
        exampleSentence: String,
        translationExampleSentence: String,
        adminUserId: Long,
        wordType: String = ""
    ) {
        if (word.isBlank() || translation.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Word and translation are required")
            return
        }

        viewModelScope.launch {
            val languageWord = LanguageWord(
                word = word.trim(),
                wordType = wordType.trim(),
                pinyin = pinyin.trim(),
                langCode = langCode,
                translation = translation.trim(),
                translationLangCode = translationLangCode,
                exampleSentence = exampleSentence.trim(),
                translationExampleSentence = translationExampleSentence.trim(),
                addedBy = adminUserId
            )
            languageWordRepository.addWord(languageWord)
            val count = languageWordRepository.getWordCount()
            _uiState.value = _uiState.value.copy(
                wordCount = count,
                addWordSuccess = true
            )
        }
    }

    fun clearAddWordSuccess() {
        _uiState.value = _uiState.value.copy(addWordSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun refreshUsers(users: List<User>) {
        _uiState.value = _uiState.value.copy(users = users)
    }
}
