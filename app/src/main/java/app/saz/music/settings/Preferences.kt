package app.saz.music.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sazPreferences by preferencesDataStore("settings")
data class Settings(val history: Boolean = true, val graphics: String = "Balanced", val reducedMotion: Boolean = false)

@Singleton class Preferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val history = booleanPreferencesKey("history")
    private val graphics = stringPreferencesKey("graphics")
    private val reducedMotion = booleanPreferencesKey("reduced_motion")
    val settings = context.sazPreferences.data.map { Settings(it[history] ?: true, it[graphics] ?: "Balanced", it[reducedMotion] ?: false) }
    suspend fun history(value: Boolean) { context.sazPreferences.edit { it[history] = value } }
    suspend fun graphics(value: String) { context.sazPreferences.edit { it[graphics] = value } }
    suspend fun reducedMotion(value: Boolean) { context.sazPreferences.edit { it[reducedMotion] = value } }
}
