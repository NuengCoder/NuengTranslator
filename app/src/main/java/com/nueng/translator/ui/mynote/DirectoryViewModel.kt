package com.nueng.translator.ui.mynote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.entity.UserData
import com.nueng.translator.data.repository.UserDataRepository
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
class DirectoryViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _directoryId = MutableStateFlow(0L)
    private val _userId = MutableStateFlow(-1L)

    val notes: StateFlow<List<UserData>> = combine(
        _searchQuery.debounce(300),
        _userId,
        _directoryId
    ) { query, uid, dirId -> Triple(query, uid, dirId) }
    .flatMapLatest { (query, uid, dirId) ->
        if (uid <= 0 || dirId <= 0) flowOf(emptyList())
        else if (query.isBlank()) userDataRepository.getNotesByDirectory(uid, dirId)
        else userDataRepository.searchNotesInDirectory(uid, dirId, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _userId.value = preferencesManager.loggedInUserId.first()
        }
    }

    fun setDirectoryId(id: Long) { _directoryId.value = id }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun addNote(
        word: String,
        pinyin: String,
        langCode: String,
        translation: String,
        translationLangCode: String,
        exampleSentence: String,
        translationExampleSentence: String,
        wordType: String = ""
    ) {
        viewModelScope.launch {
            userDataRepository.addNote(
                UserData(
                    userId = _userId.value,
                    directoryId = _directoryId.value,
                    word = word.trim(),
                    wordType = wordType.trim(),
                    pinyin = pinyin.trim(),
                    langCode = langCode,
                    translation = translation.trim(),
                    translationLangCode = translationLangCode,
                    exampleSentence = exampleSentence.trim(),
                    translationExampleSentence = translationExampleSentence.trim()
                )
            )
        }
    }

    fun updateNote(note: UserData) {
        viewModelScope.launch { userDataRepository.updateNote(note) }
    }

    fun deleteNote(note: UserData) {
        viewModelScope.launch { userDataRepository.deleteNote(note) }
    }
}
