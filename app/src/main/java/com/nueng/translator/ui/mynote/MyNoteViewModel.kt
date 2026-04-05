package com.nueng.translator.ui.mynote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.data.local.entity.UserDirectory
import com.nueng.translator.data.repository.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyNoteViewModel @Inject constructor(
    private val userDirectoryRepository: UserDirectoryRepository,
    private val preferencesManager: PreferencesManager,
    private val userDao: UserDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _userId = MutableStateFlow(-1L)
    val userId: StateFlow<Long> = _userId.asStateFlow()

    private val _username = MutableStateFlow("")

    val directories: StateFlow<List<UserDirectory>> = combine(
        _searchQuery.debounce(300),
        _userId
    ) { query, uid -> Pair(query, uid) }
        .flatMapLatest { (query, uid) ->
            if (uid <= 0) flowOf(emptyList())
            else if (query.isBlank()) userDirectoryRepository.getDirectoriesByUserId(uid)
            else userDirectoryRepository.searchDirectories(uid, query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val uid = preferencesManager.loggedInUserId.first()
            _userId.value = uid
            _isGuest.value = preferencesManager.isGuest.first()
            if (uid > 0) {
                _username.value = userDao.getUserById(uid)?.username ?: ""
            }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun addDirectory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            // Wait for username to be loaded (avoids race condition)
            val username = _username.value.ifBlank { _username.first { it.isNotBlank() } }
            val count = userDirectoryRepository.getDirectoryCount(_userId.value)
            userDirectoryRepository.addDirectory(
                UserDirectory(
                    userId    = _userId.value,
                    name      = name.trim(),
                    sortIndex = count
                ),
                username = username
            )
        }
    }

    fun renameDirectory(directory: UserDirectory, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val username = _username.value.ifBlank { _username.first { it.isNotBlank() } }
            userDirectoryRepository.updateDirectory(
                directory.copy(name = newName.trim()),
                username = username
            )
        }
    }

    fun deleteDirectory(directory: UserDirectory) {
        viewModelScope.launch {
            val username = _username.value.ifBlank { _username.first { it.isNotBlank() } }
            userDirectoryRepository.deleteDirectory(directory, username = username)
        }
    }

    fun reorderDirectories(reordered: List<UserDirectory>) {
        viewModelScope.launch {
            val username = _username.value.ifBlank { _username.first { it.isNotBlank() } }
            reordered.forEachIndexed { index, dir ->
                if (dir.sortIndex != index) {
                    userDirectoryRepository.updateSortIndex(dir.id, index)
                    // Sync reorder to Firebase too
                    userDirectoryRepository.updateDirectory(
                        dir.copy(sortIndex = index), username = username
                    )
                }
            }
        }
    }
}
