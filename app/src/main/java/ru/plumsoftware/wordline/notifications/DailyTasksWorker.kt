package ru.plumsoftware.wordline.notifications

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import ru.plumsoftware.wordline.WordLineApplication
import ru.plumsoftware.wordline.data.worldFromJson
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Read saved progress only: background checks must not count as visits or claim rewards. */
class DailyTasksWorker(context: Context, parameters: WorkerParameters): CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WordLineApplication
        if(!app.settings.settings.first().taskNotifications) return Result.success()
        val row = app.database.gameDao().world() ?: return Result.success()
        val world = worldFromJson(JSONObject(row.json))
        val today = LocalDate.now().toString()
        val preferences = app.getSharedPreferences("task-notifications", Context.MODE_PRIVATE)
        if(!shouldNotifyNewTasks(world.effectiveDate, today, preferences.getString("notified-day", null))) return Result.success()
        if(GameNotifications.show(app, "Новые задания ждут!",
                "Найди слова, пройди кроссворды и получи монеты. Сегодня доступен новый набор заданий.",
                tasks = true, id = GameNotifications.TASKS_ID)) {
            // Commit before returning so a restarted worker cannot alert again for this date.
            if(!preferences.edit().putString("notified-day", today).commit()) return Result.retry()
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if(enabled) manager.enqueueUniquePeriodicWork("new-daily-tasks", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyTasksWorker>(1, TimeUnit.HOURS).build())
            else {
                manager.cancelUniqueWork("new-daily-tasks")
                androidx.core.app.NotificationManagerCompat.from(context).cancel(GameNotifications.TASKS_ID)
            }
        }
    }
}

internal fun shouldNotifyNewTasks(effectiveDate: String, today: String, notifiedDate: String?): Boolean =
    effectiveDate.isNotBlank() && today > effectiveDate && (notifiedDate == null || today > notifiedDate)
