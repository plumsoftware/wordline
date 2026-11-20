package ru.plumsoftware.wordline.game

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

interface GameClock {
    fun today(): LocalDate
    fun millis(): Long
}
object DeviceClock : GameClock {
    override fun today(): LocalDate = LocalDate.now()
    override fun millis(): Long = System.currentTimeMillis()
}
data class DayStats(val words: Int = 0, val wins: Int = 0, val bonus: Int = 0, val daily: Int = 0, val activeMillis: Long = 0)
data class AdTicket(val id: String, val generation: String, val placement: String, val date: String,
                    val resultId: String?, val reward: Long, val status: String = "pending")
data class AdShowing(val date: String, val wallMillis: Long, val activeMillis: Long, val rewarded: Boolean, val ticketId: String = "")
data class World(
    val generation: String = UUID.randomUUID().toString(),
    val effectiveDate: String = "", val visits: Set<String> = emptySet(),
    val streakRun: String = UUID.randomUUID().toString(), val streak: Int = 0, val bestStreak: Int = 0,
    val giftStep: Int = 0, val claims: Set<String> = emptySet(),
    val dailyAssignments: Map<String,Int> = emptyMap(), val dailyCompleted: Set<String> = emptySet(),
    val days: Map<String,DayStats> = emptyMap(), val uniqueWords: Set<String> = emptySet(),
    val totalWords: Int = 0, val bonusWords: Int = 0, val chests: Int = 0, val hints: Int = 0,
    val noHintLevels: Set<Int> = emptySet(), val trailEvents: Set<String> = emptySet(),
    val collectibles: Set<String> = emptySet(), val acknowledgedLocations: Set<Int> = setOf(0),
    val activeMillis: Long = 0, val winsSinceInterstitial: Int = 0,
    val adTickets: Map<String,AdTicket> = emptyMap(), val adShowings: List<AdShowing> = emptyList(),
    val lastExportAt: Long = 0, val clockNotice: Boolean = false, val dailyReport: List<String> = emptyList(),
)
data class TaskSpec(val id: String, val title: String, val progress: Int, val target: Int, val reward: Long)
fun tasks(world: World, date: String, campaignFinished: Boolean): List<TaskSpec> {
    val day=world.days[date] ?: DayStats()
    return listOf(TaskSpec("words","Найди 10 новых слов",day.words,10,15),
        if(campaignFinished && day.wins<3) TaskSpec("levels","Пройди ежедневный кроссворд",day.daily,1,20)
        else TaskSpec("levels","Пройди 3 новых уровня",day.wins,3,20),
        TaskSpec("bonus","Найди дополнительное слово",day.bonus,1,10))
}
data class AchievementSpec(val id: String, val title: String, val progress: Int, val target: Int, val reward: Long)
fun achievements(world: World, campaignWins: Int): List<AchievementSpec> = listOf(
    AchievementSpec("ACH01","Первые шаги",campaignWins,10,20),
    AchievementSpec("ACH02","Сотня открытий",campaignWins,100,50),
    AchievementSpec("ACH03","Большое путешествие",campaignWins,500,100),
    AchievementSpec("ACH04","Сто слов",world.totalWords,100,30),
    AchievementSpec("ACH05","Тысяча слов",world.totalWords,1000,75),
    AchievementSpec("ACH06","Разнообразный словарь",world.uniqueWords.size,100,40),
    AchievementSpec("ACH07","Без подсказок",world.noHintLevels.size,10,30),
    AchievementSpec("ACH08","Искатель бонусов",world.bonusWords,50,40),
    AchievementSpec("ACH09","Сундуки открытий",world.chests,10,40),
    AchievementSpec("ACH10","Неделя вместе",world.bestStreak,7,50),
    AchievementSpec("ACH11","Каждый день",world.dailyCompleted.size,20,75),
    AchievementSpec("ACH12","Новые горизонты",(campaignWins/100+1).coerceAtMost(10),5,100))

val giftRewards=listOf(20L,25L,30L,35L,40L,50L,75L)
val trailRewards=mapOf(10 to 50L,20 to 75L,30 to 100L)
val locationNames=listOf("Озёрный край","Лесная река","Вечернее озеро","Осенняя долина","Ночные горы","Морское побережье","Каньон","Цветочная поляна","Озеро лотосов","Снежная долина")
val backgroundNames=listOf("lake_day","forest_river","lake_evening","autumn_valley","night_mountains","sea_coast","canyon","flower_meadow","lotus_lake","snow_valley")
fun dailyIndex(date: LocalDate, size: Int): Int = Math.floorMod(ChronoUnit.DAYS.between(LocalDate.of(2026,1,1),date),size.toLong()).toInt()
fun effectiveDay(previous: String, candidate: LocalDate): LocalDate = maxOf(if(previous.isBlank()) candidate else LocalDate.parse(previous),candidate)
fun World.withDay(date: String, update: (DayStats) -> DayStats): World = copy(days=days+(date to update(days[date] ?: DayStats())))
