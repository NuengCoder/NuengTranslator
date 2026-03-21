package com.nueng.translator.ui.mynote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
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
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _userId = MutableStateFlow(-1L)
    val userId: StateFlow<Long> = _userId.asStateFlow()

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
            _userId.value = preferencesManager.loggedInUserId.first()
            _isGuest.value = preferencesManager.isGuest.first()
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun addDirectory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = userDirectoryRepository.getDirectoryCount(_userId.value)
            userDirectoryRepository.addDirectory(
                UserDirectory(
                    userId = _userId.value,
                    name = name.trim(),
                    sortIndex = count
                )
            )
        }
    }

    fun renameDirectory(directory: UserDirectory, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            userDirectoryRepository.updateDirectory(directory.copy(name = newName.trim()))
        }
    }

    fun deleteDirectory(directory: UserDirectory) {
        viewModelScope.launch {
            userDirectoryRepository.deleteDirectory(directory)
        }
    }

    // Called after drag-to-reorder: save new order to DB
    fun reorderDirectories(reordered: List<UserDirectory>) {
        viewModelScope.launch {
            reordered.forEachIndexed { index, dir ->
                if (dir.sortIndex != index) {
                    userDirectoryRepository.updateSortIndex(dir.id, index)
                }
            }
        }
    }
}