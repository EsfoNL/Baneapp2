package com.example.baneapp2.settingstore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class StoreUserSettings(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("bgColor")
        val USER_BG_KEY = stringPreferencesKey("bgcolor")
        private val Context.dataStore2: DataStore<Preferences> by preferencesDataStore("fgColor")
        val USER_FG_KEY = stringPreferencesKey("fgcolor")
        private val Context.dataStore3: DataStore<Preferences> by preferencesDataStore("textColor")
        val USER_TEXT_KEY = stringPreferencesKey("textcolor")
    }

    val getBGColor: Flow<String?> = context.dataStore.data
        .map {preferences ->
            preferences[USER_BG_KEY] ?: ""
        }
    val getFGColor: Flow<String?> = context.dataStore2.data
        .map {preferences ->
            preferences[USER_FG_KEY] ?: ""
        }
    val getTextColor: Flow<String?> = context.dataStore3.data
        .map {preferences ->
            preferences[USER_TEXT_KEY] ?: ""
        }
    suspend fun saveBGColor(name: String) {
        context.dataStore.edit { preferences ->
        preferences[USER_BG_KEY] = name
        }
    }
    suspend fun saveFGColor(name: String) {
        context.dataStore2.edit { preferences ->
            preferences[USER_FG_KEY] = name
        }
    }suspend fun saveTextColor(name: String) {
        context.dataStore3.edit { preferences ->
            preferences[USER_TEXT_KEY] = name
        }
    }


}