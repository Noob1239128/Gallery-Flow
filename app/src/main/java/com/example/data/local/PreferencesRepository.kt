package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GridLayoutMode
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumina_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val gridLayoutMode: GridLayoutMode = GridLayoutMode.MASONRY,
    val gridColumns: Int = 2,
    val slideshowIntervalSec: Int = 4,
    val isVaultPinSet: Boolean = false,
    val hasSeededInitialData: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GRID_LAYOUT_MODE = stringPreferencesKey("grid_layout_mode")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val HAS_SEEDED = booleanPreferencesKey("has_seeded")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()
        } ?: ThemeMode.DARK
        val gridMode = prefs[Keys.GRID_LAYOUT_MODE]?.let { name ->
            runCatching { GridLayoutMode.valueOf(name) }.getOrNull()
        } ?: GridLayoutMode.MASONRY
        val cols = (prefs[Keys.GRID_COLUMNS] ?: 2).coerceIn(2, 4)
        val interval = (prefs[Keys.SLIDESHOW_INTERVAL] ?: 4).coerceIn(2, 8)
        val pinHash = prefs[Keys.VAULT_PIN_HASH] ?: ""
        val seeded = prefs[Keys.HAS_SEEDED] ?: false

        UserPreferences(
            themeMode = theme,
            gridLayoutMode = gridMode,
            gridColumns = cols,
            slideshowIntervalSec = interval,
            isVaultPinSet = pinHash.isNotEmpty(),
            hasSeededInitialData = seeded
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setGridLayoutMode(mode: GridLayoutMode) {
        context.dataStore.edit { it[Keys.GRID_LAYOUT_MODE] = mode.name }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(2, 4) }
    }

    suspend fun setSlideshowInterval(seconds: Int) {
        context.dataStore.edit { it[Keys.SLIDESHOW_INTERVAL] = seconds.coerceIn(2, 8) }
    }

    suspend fun markSeeded(seeded: Boolean = true) {
        context.dataStore.edit { it[Keys.HAS_SEEDED] = seeded }
    }

    suspend fun setVaultPin(pin: String) {
        context.dataStore.edit { it[Keys.VAULT_PIN_HASH] = hashPin(pin) }
    }

    suspend fun verifyVaultPin(pin: String): Boolean {
        val stored = context.dataStore.data.first()[Keys.VAULT_PIN_HASH] ?: return false
        return stored == hashPin(pin)
    }

    suspend fun clearVaultPin() {
        context.dataStore.edit { it.remove(Keys.VAULT_PIN_HASH) }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("lumina_salt_$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
