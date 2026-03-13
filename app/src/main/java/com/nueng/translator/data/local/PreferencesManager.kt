package com.nueng.translator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nueng_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_UI_LANGUAGE = stringPreferencesKey("ui_language")
        val KEY_LOGGED_IN_USER_ID = longPreferencesKey("logged_in_user_id")
        val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
        val KEY_LANG1 = stringPreferencesKey("lang1")
        val KEY_LANG2 = stringPreferencesKey("lang2")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: true // DEFAULT: dark mode ON
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DARK_MODE] = enabled }
    }

    val uiLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_UI_LANGUAGE] ?: "en"
    }

    suspend fun setUiLanguage(langCode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_UI_LANGUAGE] = langCode }
    }

    val loggedInUserId: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN_USER_ID] ?: -1L
    }

    val isGuest: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_GUEST] ?: false
    }

    suspend fun setLoggedInUser(userId: Long, isGuest: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN_USER_ID] = userId
            prefs[KEY_IS_GUEST] = isGuest
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LOGGED_IN_USER_ID)
            prefs.remove(KEY_IS_GUEST)
        }
    }

    val lang1: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANG1] ?: "en"
    }

    val lang2: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANG2] ?: "zh"
    }

    suspend fun setLanguagePair(lang1: String, lang2: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANG1] = lang1
            prefs[KEY_LANG2] = lang2
        }
    }
}
