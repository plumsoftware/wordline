package ru.plumsoftware.wordline

import android.app.Application
import androidx.room.Room
import ru.plumsoftware.wordline.data.GameDatabase
import ru.plumsoftware.wordline.data.SettingsRepository

class WordLineApplication : Application() {
    val database by lazy { Room.databaseBuilder(this,GameDatabase::class.java,"word-line.db").addMigrations(GameDatabase.MIGRATION_1_2).build() }
    val ads by lazy { ru.plumsoftware.wordline.ads.AdsController(this) }
    val settings by lazy { SettingsRepository(this) }
}
