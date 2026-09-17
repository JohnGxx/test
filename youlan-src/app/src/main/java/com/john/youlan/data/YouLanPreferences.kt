package com.john.youlan.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.youLanDataStore by preferencesDataStore(name = "youlan_preferences")

object YouLanPreferences {
    private val selectedPackagesKey = stringSetPreferencesKey("selected_packages")
    private val guardUnknownPackagesKey = booleanPreferencesKey("guard_unknown_packages")

    fun selectedPackages(context: Context): Flow<Set<String>> =
        context.youLanDataStore.data.map { prefs -> prefs[selectedPackagesKey] ?: emptySet() }

    fun guardUnknownPackages(context: Context): Flow<Boolean> =
        context.youLanDataStore.data.map { prefs -> prefs[guardUnknownPackagesKey] ?: false }

    suspend fun setSelectedPackages(context: Context, packages: Set<String>) {
        context.youLanDataStore.edit { prefs -> prefs[selectedPackagesKey] = packages }
    }

    suspend fun setGuardUnknownPackages(context: Context, enabled: Boolean) {
        context.youLanDataStore.edit { prefs -> prefs[guardUnknownPackagesKey] = enabled }
    }
}
