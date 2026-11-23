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
                    val resultId: String?, val reward: Long, val status: String = "pending",
                    val hintKind: Hint? = null,val hintTarget: String? = null,val hintDefinition: String? = null)
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
data class AchievementSpec(val id: String, val title: String, val progress: Int, val target: Int,
                           val reward: Long, val requirement: String)
fun achievements(world: World, campaignWins: Int): List<AchievementSpec> = listOf(
    AchievementSpec("ACH01","Первые шаги",campaignWins,10,20,"Пройди 10 уровней путешествия."),
    AchievementSpec("ACH02","Сотня открытий",campaignWins,100,50,"Пройди 100 уровней путешествия."),
    AchievementSpec("ACH03","Большое путешествие",campaignWins,500,100,"Пройди 500 уровней путешествия."),
    AchievementSpec("ACH04","Сто слов",world.totalWords,100,30,"Найди 100 слов. Повторное прохождение не учитывается."),
    AchievementSpec("ACH05","Тысяча слов",world.totalWords,1000,75,"Найди 1 000 слов. Повторное прохождение не учитывается."),
    AchievementSpec("ACH06","Разнообразный словарь",world.uniqueWords.size,100,40,"Найди 100 разных слов. Каждое слово считается один раз."),
    AchievementSpec("ACH07","Без подсказок",world.noHintLevels.size,10,30,"Пройди 10 разных уровней путешествия без подсказок."),
    AchievementSpec("ACH08","Искатель бонусов",world.bonusWords,50,40,"Найди 50 дополнительных слов вне сетки кроссворда."),
    AchievementSpec("ACH09","Сундуки открытий",world.chests,10,40,"Открой 10 сундуков за дополнительные слова."),
    AchievementSpec("ACH10","Неделя вместе",world.bestStreak,7,50,"Заходи в игру 7 дней подряд. Учитывается лучшая серия."),
    AchievementSpec("ACH11","Каждый день",world.dailyCompleted.size,20,75,"Заверши ежедневные кроссворды за 20 разных дат."),
    AchievementSpec("ACH12","Новые горизонты",(campaignWins/100+1).coerceAtMost(10),5,100,"Открой 5 локаций путешествия: пройди 400 уровней."),
    AchievementSpec("ACH13","Первое открытие",campaignWins,1,10,"Пройди первый уровень путешествия."),
    AchievementSpec("ACH14","В пути",campaignWins,50,35,"Пройди 50 уровней путешествия."),
    AchievementSpec("ACH15","Все горизонты",campaignWins,1000,150,"Пройди все 1 000 уровней путешествия."),
    AchievementSpec("ACH16","Мастер слов",world.totalWords,5000,120,"Найди 5 000 слов. Повторное прохождение не учитывается."),
    AchievementSpec("ACH17","Словесная сокровищница",world.uniqueWords.size,500,90,"Найди 500 разных слов. Каждое слово считается один раз."),
    AchievementSpec("ACH18","Своя голова",world.noHintLevels.size,50,70,"Пройди 50 разных уровней путешествия без подсказок."),
    AchievementSpec("ACH19","Мастер без подсказок",world.noHintLevels.size,100,100,"Пройди 100 разных уровней путешествия без подсказок."),
    AchievementSpec("ACH20","Первая находка",world.bonusWords,1,10,"Найди дополнительное слово, которого нет в сетке."),
    AchievementSpec("ACH21","Охотник за словами",world.bonusWords,200,90,"Найди 200 дополнительных слов вне сетки кроссворда."),
    AchievementSpec("ACH22","Первый сундук",world.chests,1,15,"Собери 5 дополнительных слов в одном кроссворде и открой сундук."),
    AchievementSpec("ACH23","Коллекционер сундуков",world.chests,50,100,"Открой 50 сундуков за дополнительные слова."),
    AchievementSpec("ACH24","Три дня вместе",world.bestStreak,3,20,"Заходи в игру 3 дня подряд. Учитывается лучшая серия."),
    AchievementSpec("ACH25","Месяц вместе",world.bestStreak,30,120,"Заходи в игру 30 дней подряд. Учитывается лучшая серия."),
    AchievementSpec("ACH26","Кроссворд дня",world.dailyCompleted.size,1,15,"Полностью заверши один ежедневный кроссворд."),
    AchievementSpec("ACH27","Неделя кроссвордов",world.dailyCompleted.size,7,40,"Заверши ежедневные кроссворды за 7 разных дат, необязательно подряд."),
    AchievementSpec("ACH28","Дальняя тропа",world.trailEvents.size,30,80,"Заверши 30 этапов тропы: новые уровни или ежедневные кроссворды."),
    AchievementSpec("ACH29","Коллекция воспоминаний",world.collectibles.size,5,60,"Получи 5 открыток за путешествие, тропу и ежедневные кроссворды."),
    AchievementSpec("ACH30","Десять встреч",world.visits.size,10,35,"Заходи в игру в 10 разных дней, необязательно подряд."))

val giftRewards=listOf(20L,25L,30L,35L,40L,50L,75L)
val trailRewards=mapOf(10 to 50L,20 to 75L,30 to 100L)
val locationNames=listOf("Алтайское озеро","Бамбуковый лес","Пески Сахары","Красная площадь","Северное сияние","Лазурный берег","Красный каньон","Холмы Тосканы","Тропический водопад","Снежные Альпы")
val backgroundNames=listOf("lake_day","forest_river","lake_evening","autumn_valley","night_mountains","sea_coast","canyon","flower_meadow","lotus_lake","snow_valley")
fun dailyIndex(date: LocalDate, size: Int): Int = Math.floorMod(ChronoUnit.DAYS.between(LocalDate.of(2026,1,1),date),size.toLong()).toInt()
fun effectiveDay(previous: String, candidate: LocalDate): LocalDate = maxOf(if(previous.isBlank()) candidate else LocalDate.parse(previous),candidate)
fun World.withDay(date: String, update: (DayStats) -> DayStats): World = copy(days=days+(date to update(days[date] ?: DayStats())))
