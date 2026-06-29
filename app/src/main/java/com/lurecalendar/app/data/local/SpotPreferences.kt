package com.lurecalendar.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.spotDataStore by preferencesDataStore(name = "spot_preferences")

class SpotPreferences(private val context: Context) {

    companion object {
        private val SELECTED_SPOT_ID = stringPreferencesKey("selected_spot_id")
        private val SELECTED_SPOT_NAME = stringPreferencesKey("selected_spot_name")
        private val USER_TARGET_SPECIES = stringPreferencesKey("user_target_species")
    }

    val selectedSpotId: Flow<String?> = context.spotDataStore.data.map { prefs ->
        prefs[SELECTED_SPOT_ID]
    }

    val selectedSpotName: Flow<String?> = context.spotDataStore.data.map { prefs ->
        prefs[SELECTED_SPOT_NAME]
    }

    val userTargetSpecies: Flow<String> = context.spotDataStore.data
        .map { preferences -> preferences[USER_TARGET_SPECIES] ?: "翘嘴" }

    suspend fun saveSelectedSpot(spotId: String, spotName: String) {
        context.spotDataStore.edit { prefs ->
            prefs[SELECTED_SPOT_ID] = spotId
            prefs[SELECTED_SPOT_NAME] = spotName
        }
    }

    suspend fun setUserTargetSpecies(species: String) {
        context.spotDataStore.edit { preferences ->
            preferences[USER_TARGET_SPECIES] = species
        }
    }
}
