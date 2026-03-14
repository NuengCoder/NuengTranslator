package com.nueng.translator.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.repository.FirebaseWordRepository
import com.nueng.translator.data.repository.LanguageWordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TranslateViewModel @Inject constructor(
    private val languageWordRepository: LanguageWordRepository,
    private val firebaseWordRepository: FirebaseWordRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lang1 = MutableStateFlow("en")
    val lang1: StateFlow<String> = _lang1.asStateFlow()

    private val _lang2 = MutableStateFlow("zh")
    val lang2: StateFlow<String> = _lang2.asStateFlow()

    val searchResults: StateFlow<List<LanguageWord>> = combine(
        _searchQuery.debounce(300), _lang1, _lang2
    ) { query, l1, l2 -> Triple(query, l1, l2) }
    .flatMapLatest { (query, l1, l2) ->
        if (query.isBlank()) languageWordRepository.getWordsByLanguagePair(l1, l2)
        else languageWordRepository.searchWords(query, l1, l2)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        firebaseWordRepository.startRealtimeSync()
        viewModelScope.launch { preferencesManager.lang1.collect { _lang1.value = it } }
        viewModelScope.launch { preferencesManager.lang2.collect { _lang2.value = it } }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun setLang1(code: String) {
        _lang1.value = code
        viewModelScope.launch { preferencesManager.setLanguagePair(code, _lang2.value) }
    }

    fun setLang2(code: String) {
        _lang2.value = code
        viewModelScope.launch { preferencesManager.setLanguagePair(_lang1.value, code) }
    }

    fun swapLanguages() {
        val temp = _lang1.value; _lang1.value = _lang2.value; _lang2.value = temp
        viewModelScope.launch { preferencesManager.setLanguagePair(_lang1.value, _lang2.value) }
    }
}
