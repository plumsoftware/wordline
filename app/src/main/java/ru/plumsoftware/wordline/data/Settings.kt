package ru.plumsoftware.wordline.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.gameSettings by preferencesDataStore("game-settings")
data class GameSettings(val onboardingCompleted: Boolean = false, val highContrast: Boolean = false,
    val largeLetters: Boolean = false, val tapInput: Boolean = false, val reduceMotion: Boolean = false,
    val tutorialEnabled: Boolean = false, val music: Boolean = true, val effects: Boolean = true,
    val vibration: Boolean = true, val adsAllowed: Boolean = true,
    val background: Int = 0, val theme: String = "day", val seenContent: Set<String> = emptySet())
enum class Setting { ONBOARDING, CONTRAST, LARGE_LETTERS, TAP_INPUT, REDUCE_MOTION, TUTORIAL, MUSIC, EFFECTS, VIBRATION, ADS_ALLOWED }
class SettingsRepository(context: Context) {
    private val store=context.applicationContext.gameSettings
    private fun key(setting: Setting)=booleanPreferencesKey(setting.name)
    private val backgroundKey=intPreferencesKey("background")
    private val themeKey=stringPreferencesKey("theme")
    private val seenKey=stringSetPreferencesKey("seen-content")
    val settings=store.data.map {p ->GameSettings(p[key(Setting.ONBOARDING)] ?: false,
        p[key(Setting.CONTRAST)] ?: false,p[key(Setting.LARGE_LETTERS)] ?: false,
        p[key(Setting.TAP_INPUT)] ?: false,p[key(Setting.REDUCE_MOTION)] ?: false,
        p[key(Setting.TUTORIAL)] ?: false,p[key(Setting.MUSIC)] ?: true,p[key(Setting.EFFECTS)] ?: true,
        p[key(Setting.VIBRATION)] ?: true,p[key(Setting.ADS_ALLOWED)] ?: true,
        p[backgroundKey] ?: 0,p[themeKey] ?: "day",p[seenKey] ?: emptySet())}
    suspend fun markSeen(keys: Set<String>) {if(keys.isNotEmpty()) store.edit {it[seenKey]=(it[seenKey] ?: emptySet())+keys}}
    suspend fun set(setting: Setting,value: Boolean) {store.edit {it[key(setting)]=value}}
    suspend fun appearance(background: Int,theme: String) {store.edit {it[backgroundKey]=background;it[themeKey]=theme}}
}
