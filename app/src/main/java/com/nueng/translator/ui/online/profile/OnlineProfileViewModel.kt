package com.nueng.translator.ui.online.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyProfileUiState(
    val username: String = "",
    val nickname: String = "",
    val bio: String = "",
    val rank: String = "Normal",
    val isLoading: Boolean = true
)

@HiltViewModel
class OnlineProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val user    = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null
            val username  = user?.username ?: if (isGuest) "Guest" else "User"
            val localRank = if (user?.role == "admin" && username == "NuengAdmin") "DevAdmin" else "Normal"
            _uiState.value = _uiState.value.copy(username = username, rank = localRank)
            if (userId > 0 && !isGuest) loadOnlineProfile(username, localRank)
            else _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadOnlineProfile(username: String, fallbackRank: String) {
        db.getReference("online_profiles").child(username).get()
            .addOnSuccessListener { snapshot ->
                val nickname = snapshot.child("nickname").getValue(String::class.java) ?: ""
                val bio      = snapshot.child("bio").getValue(String::class.java) ?: ""
                val fbRank   = snapshot.child("rank").getValue(String::class.java) ?: "Normal"
                val rank     = if (fallbackRank == "DevAdmin") "DevAdmin" else fbRank
                _uiState.value = _uiState.value.copy(
                    nickname  = nickname,
                    bio       = bio,
                    rank      = rank,
                    isLoading = false
                )
            }
            .addOnFailureListener { _uiState.value = _uiState.value.copy(isLoading = false) }
    }
}
