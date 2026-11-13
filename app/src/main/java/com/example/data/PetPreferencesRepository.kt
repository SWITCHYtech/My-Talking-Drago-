package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.petDataStore: DataStore<Preferences> by preferencesDataStore(name = "dragos_pet_prefs")

data class PersistedPetData(
    val happiness: Float = 86f,
    val hunger: Float = 78f,
    val energy: Float = 84f,
    val freshness: Float = 88f,
    val coins: Int = 60,
    val xp: Int = 20,
    val soundEnabled: Boolean = true,
    val highScoreKeepyUppy: Int = 0,
    val lastSavedMs: Long = 0L
)

class PetPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAPPINESS = floatPreferencesKey("happiness")
        val HUNGER = floatPreferencesKey("hunger")
        val ENERGY = floatPreferencesKey("energy")
        val FRESHNESS = floatPreferencesKey("freshness")
        val COINS = intPreferencesKey("coins")
        val XP = intPreferencesKey("xp")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HIGH_SCORE = intPreferencesKey("high_score_keepy_uppy")
        val LAST_SAVED = longPreferencesKey("last_saved_ms")
    }

    val petDataFlow: Flow<PersistedPetData> = context.petDataStore.data.map { prefs ->
        PersistedPetData(
            happiness = prefs[Keys.HAPPINESS] ?: 86f,
            hunger = prefs[Keys.HUNGER] ?: 78f,
            energy = prefs[Keys.ENERGY] ?: 84f,
            freshness = prefs[Keys.FRESHNESS] ?: 88f,
            coins = prefs[Keys.COINS] ?: 60,
            xp = prefs[Keys.XP] ?: 20,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            highScoreKeepyUppy = prefs[Keys.HIGH_SCORE] ?: 0,
            lastSavedMs = prefs[Keys.LAST_SAVED] ?: 0L
        )
    }

    suspend fun savePetData(data: PersistedPetData) {
        context.petDataStore.edit { prefs ->
            prefs[Keys.HAPPINESS] = data.happiness
            prefs[Keys.HUNGER] = data.hunger
            prefs[Keys.ENERGY] = data.energy
            prefs[Keys.FRESHNESS] = data.freshness
            prefs[Keys.COINS] = data.coins
            prefs[Keys.XP] = data.xp
            prefs[Keys.SOUND_ENABLED] = data.soundEnabled
            prefs[Keys.HIGH_SCORE] = data.highScoreKeepyUppy
            prefs[Keys.LAST_SAVED] = System.currentTimeMillis()
        }
    }
}
