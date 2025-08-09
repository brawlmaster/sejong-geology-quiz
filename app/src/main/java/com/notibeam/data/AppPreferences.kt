package com.notibeam.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "notibeam_prefs")

object AppPreferences {
    private val KEY_ENABLED_PACKAGES = stringSetPreferencesKey("enabled_packages")
    private val KEY_CHANNEL_ID = stringPreferencesKey("channel_id")
    private val KEY_ROLE = stringPreferencesKey("role") // "sender" or "receiver"

    fun getPreferencesFlow(context: Context): Flow<androidx.datastore.preferences.core.Preferences> =
        context.dataStore.data

    suspend fun getEnabledPackages(context: Context): Set<String> {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_ENABLED_PACKAGES] ?: emptySet()
    }

    suspend fun setEnabledPackages(context: Context, packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED_PACKAGES] = packages
        }
    }

    suspend fun getChannelId(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_CHANNEL_ID]
    }

    suspend fun setChannelId(context: Context, channelId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHANNEL_ID] = channelId
        }
    }

    suspend fun getRole(context: Context): String {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_ROLE] ?: "sender"
    }

    suspend fun setRole(context: Context, role: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ROLE] = role
        }
    }
}