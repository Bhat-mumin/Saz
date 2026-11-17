package app.saz.music.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sazPreferences by preferencesDataStore("settings")
data class Settings(val history: Boolean = true, val graphics: String = "Balanced", val reducedMotion: Boolean = false, val region: String = "")

@Singleton class Preferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val history = booleanPreferencesKey("history")
    private val graphics = stringPreferencesKey("graphics")
    private val reducedMotion = booleanPreferencesKey("reduced_motion")
    private val region = stringPreferencesKey("discovery_region")
    val settings = context.sazPreferences.data.map { Settings(it[history] ?: true, it[graphics] ?: "Balanced", it[reducedMotion] ?: false, it[region] ?: "") }
    suspend fun region(value: String) {
        val code = value.trim().uppercase(java.util.Locale.ROOT)
        require(code.isBlank() || code in java.util.Locale.getISOCountries()) { "Use a two-letter country code, or leave it blank." }
        context.sazPreferences.edit { it[region] = code }
    }
    suspend fun history(value: Boolean) { context.sazPreferences.edit { it[history] = value } }
    suspend fun graphics(value: String) { context.sazPreferences.edit { it[graphics] = value } }
    suspend fun reducedMotion(value: Boolean) { context.sazPreferences.edit { it[reducedMotion] = value } }
}
