package ru.plumsoftware.wordline

import android.app.Application
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.room.Room
import ru.plumsoftware.wordline.data.GameDatabase
import ru.plumsoftware.wordline.data.SettingsRepository

class WordLineApplication : Application() {
    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        ru.plumsoftware.wordline.notifications.GameNotifications.createChannels(this)
        applicationScope.launch {
            settings.settings.map { it.taskNotifications }.distinctUntilChanged().collect { enabled ->
                ru.plumsoftware.wordline.notifications.DailyTasksWorker.schedule(this@WordLineApplication, enabled)
            }
        }
    }
    val database by lazy { Room.databaseBuilder(this,GameDatabase::class.java,"word-line.db").addMigrations(GameDatabase.MIGRATION_1_2).build() }
    val ads by lazy { ru.plumsoftware.wordline.ads.AdsController(this) }
    val settings by lazy { SettingsRepository(this) }
}
