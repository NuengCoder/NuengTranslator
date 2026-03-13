package com.nueng.translator.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.local.entity.User
import com.nueng.translator.data.repository.LanguageWordRepository
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val isGuest: Boolean = false,
    val randomWords: List<LanguageWord> = emptyList(),
    val greeting: String = "Welcome"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val languageWordRepository: LanguageWordRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserAndWords()
    }

    private fun loadUserAndWords() {
        viewModelScope.launch {
            val userId = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()

            val user = if (userId > 0 && !isGuest) {
                userRepository.getUserById(userId)
            } else null

            if (userId > 0) {
                userRepository.updateLastOnline(userId)
            }

            val greeting = getTimeGreeting()
            val words = languageWordRepository.getRandomWords(10)

            _uiState.value = HomeUiState(
                user = user,
                isGuest = isGuest,
                randomWords = words,
                greeting = greeting
            )
        }
    }

    fun refreshWords() {
        viewModelScope.launch {
            val words = languageWordRepository.getRandomWords(10)
            _uiState.value = _uiState.value.copy(randomWords = words)
        }
    }

    private fun getTimeGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
}
