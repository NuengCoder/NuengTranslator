package com.nueng.translator.ui.online.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.entity.UserDirectory
import com.nueng.translator.data.repository.UserDirectoryRepository
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineSettingsUiState(
    val userId: String = "",
    val username: String = "",
    val nickname: String = "",
    val bio: String = "",
    val nicknameLastChanged: Long = 0L,
    val directories: List<UserDirectory> = emptyList(),
    val blockedUsers: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveMessage: String = "",
    val avatarBase64: String = "",
    val colorFg: Long = 0L,
    val colorBg: Long = 0L,
    val colorText: Long = 0L,
    val colorAppText: Long = 0L,
    val canChangeNickname: Boolean = true,
    val daysUntilNicknameChange: Int = 0,
    val isAdmin: Boolean = false
)

@HiltViewModel
class OnlineSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository,
    private val userDirectoryRepository: UserDirectoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSettingsUiState())
    val uiState: StateFlow<OnlineSettingsUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    // Use username as the universal profile key (not local Room id)
    private var currentUsername = ""
    private var currentUserId   = -1L

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            currentUserId = userId

            val user     = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null
            val username = user?.username ?: if (isGuest) "Guest" else "User"
            val isAdmin  = user?.role == "admin"
            val rank     = if (isAdmin && username == "NuengAdmin") "DevAdmin" else "Normal"
            currentUsername = username

            val dirs = if (userId > 0 && !isGuest)
                userDirectoryRepository.getDirectoriesByUserId(userId).first()
            else emptyList()

            _uiState.value = _uiState.value.copy(
                userId      = username.ifBlank { "-" },
                username    = username,
                directories = dirs,
                isAdmin     = isAdmin
            )

            if (userId > 0 && !isGuest) {
                ensureProfileExists(username, rank)
                loadOnlineProfile(username, isAdmin)
                loadBlockList(username)
            }
        }
    }

    private fun ensureProfileExists(username: String, rank: String) {
        val ref = db.getReference("online_profiles").child(username)
        ref.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any>(
                "username" to username,
                "rank"     to rank
            )
            if (!snapshot.hasChild("nickname"))              updates["nickname"]             = ""
            if (!snapshot.hasChild("bio"))                   updates["bio"]                  = ""
            if (!snapshot.hasChild("nickname_last_changed")) updates["nickname_last_changed"] = 0L
            ref.updateChildren(updates)
        }
    }

    private fun loadOnlineProfile(username: String, isAdmin: Boolean) {
        db.getReference("online_profiles").child(username).get()
            .addOnSuccessListener { snapshot ->
                val nickname            = snapshot.child("nickname").getValue(String::class.java) ?: ""
                val bio                 = snapshot.child("bio").getValue(String::class.java) ?: ""
                val avatarB64           = snapshot.child("avatarBase64").getValue(String::class.java) ?: ""
                val nicknameLastChanged = snapshot.child("nickname_last_changed").getValue(Long::class.java) ?: 0L
                val fourteenDaysMs      = 14L * 24 * 60 * 60 * 1000
                val canChange = isAdmin || (System.currentTimeMillis() - nicknameLastChanged) >= fourteenDaysMs
                val daysUntil = if (canChange) 0 else
                    ((fourteenDaysMs - (System.currentTimeMillis() - nicknameLastChanged)) / (24 * 60 * 60 * 1000)).toInt() + 1
                _uiState.value = _uiState.value.copy(
                    nickname                = nickname,
                    bio                     = bio,
                    avatarBase64            = avatarB64,
                    nicknameLastChanged     = nicknameLastChanged,
                    canChangeNickname       = canChange,
                    daysUntilNicknameChange = daysUntil
                )
            }
    }

    private fun loadBlockList(username: String) {
        db.getReference("block_list").child(username).get()
            .addOnSuccessListener { snapshot ->
                val blocked = snapshot.children.mapNotNull {
                    it.child("username").getValue(String::class.java)
                }
                _uiState.value = _uiState.value.copy(blockedUsers = blocked)
            }
    }

    fun saveProfile(nickname: String, bio: String) {
        if (currentUsername.isBlank()) return
        val state           = _uiState.value
        val trimmedNickname = nickname.trim()
        val trimmedBio      = bio.trim()

        if (trimmedNickname != state.nickname && !state.canChangeNickname) {
            _uiState.value = _uiState.value.copy(
                saveMessage = "Nickname can be changed in ${state.daysUntilNicknameChange} day(s)"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true)
        val now             = System.currentTimeMillis()
        val nicknameChanged = trimmedNickname != state.nickname
        val lastChanged     = when {
            state.isAdmin   -> state.nicknameLastChanged
            nicknameChanged -> now
            else            -> state.nicknameLastChanged
        }
        val rank    = if (state.isAdmin && state.username == "NuengAdmin") "DevAdmin" else "Normal"
        val updates = mutableMapOf<String, Any>(
            "bio"                   to trimmedBio,
            "nickname_last_changed" to lastChanged,
            "username"              to state.username,
            "rank"                  to rank
        )
        if (trimmedNickname.isNotBlank()) updates["nickname"] = trimmedNickname

        db.getReference("online_profiles").child(currentUsername)
            .updateChildren(updates)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(
                    nickname            = trimmedNickname,
                    bio                 = trimmedBio,
                    nicknameLastChanged = lastChanged,
                    isSaving            = false,
                    saveMessage         = "Saved!"
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving    = false,
                    saveMessage = "Failed: ${e.message ?: "unknown error"}"
                )
            }
    }

    fun unblockUser(blockedUsername: String) {
        if (currentUsername.isBlank()) return
        db.getReference("block_list").child(currentUsername).child(blockedUsername).removeValue()
            .addOnSuccessListener {
                val updated = _uiState.value.blockedUsers.filter { it != blockedUsername }
                _uiState.value = _uiState.value.copy(blockedUsers = updated)
            }
    }

    fun saveColors(fg: Long, bg: Long, text: Long, appText: Long) {
        viewModelScope.launch {
            preferencesManager.setColors(fg, bg, text, appText)
            _uiState.value = _uiState.value.copy(saveMessage = "Colors saved!")
        }
    }

    fun loadColors() {
        viewModelScope.launch {
            val fg      = preferencesManager.colorFg.first()
            val bg      = preferencesManager.colorBg.first()
            val text    = preferencesManager.colorText.first()
            val appText = preferencesManager.colorAppText.first()
            _uiState.value = _uiState.value.copy(colorFg=fg, colorBg=bg, colorText=text, colorAppText=appText)
        }
    }

    fun logout() {
        viewModelScope.launch { preferencesManager.clearSession() }
    }

    fun uploadAvatar(base64Jpeg: String) {
        val username = _uiState.value.username
        if (username.isBlank()) return
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            val imgUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val apiKey   = com.nueng.translator.BuildConfig.IMGBB_API_KEY
                    val url      = java.net.URL("https://api.imgbb.com/1/upload")
                    val boundary = "----FormBoundary" + System.currentTimeMillis().toString()
                    val conn     = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput     = true
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
                    conn.connectTimeout = 30000
                    conn.readTimeout    = 30000
                    val os = conn.outputStream
                    val nl = "\r\n".toByteArray(Charsets.UTF_8)
                    val dd = "--".toByteArray(Charsets.UTF_8)
                    val b  = boundary.toByteArray(Charsets.UTF_8)
                    os.write(dd); os.write(b); os.write(nl)
                    os.write("Content-Disposition: form-data; name=\"key\"".toByteArray(Charsets.UTF_8))
                    os.write(nl); os.write(nl)
                    os.write(apiKey.toByteArray(Charsets.UTF_8)); os.write(nl)
                    os.write(dd); os.write(b); os.write(nl)
                    os.write("Content-Disposition: form-data; name=\"image\"".toByteArray(Charsets.UTF_8))
                    os.write(nl); os.write(nl)
                    os.write(base64Jpeg.toByteArray(Charsets.UTF_8)); os.write(nl)
                    os.write(dd); os.write(b); os.write(dd); os.write(nl)
                    os.flush(); os.close()
                    val resp      = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val unescaped = resp.replace("\\/", "/")
                    val match     = Regex("\"display_url\":\"([^\"]+)\"").find(unescaped)
                    match?.groupValues?.get(1)
                } catch (_: Exception) { null }
            }
            if (imgUrl != null) {
                db.getReference("online_profiles").child(username)
                    .updateChildren(mapOf("avatarUrl" to imgUrl))
                    .addOnSuccessListener {
                        _uiState.value = _uiState.value.copy(
                            isSaving     = false,
                            avatarBase64 = base64Jpeg,
                            saveMessage  = "Profile photo updated!"
                        )
                    }
            } else {
                db.getReference("online_profiles").child(username)
                    .updateChildren(mapOf("avatarBase64" to base64Jpeg))
                    .addOnSuccessListener {
                        _uiState.value = _uiState.value.copy(
                            isSaving     = false,
                            avatarBase64 = base64Jpeg,
                            saveMessage  = "Profile photo updated!"
                        )
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            isSaving    = false,
                            saveMessage = "Upload failed: " + (e.message ?: "error")
                        )
                    }
            }
        }
    }

    fun clearSaveMessage() { _uiState.value = _uiState.value.copy(saveMessage = "") }

}
