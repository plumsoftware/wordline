package ru.plumsoftware.wordline.game

import java.text.Normalizer
import java.util.Locale
import kotlin.math.sqrt

fun wordKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
    .uppercase(Locale.ROOT).replace('Ё', 'Е')
fun canSpell(word: String, letters: List<Char>): Boolean {
    val remaining = letters.groupingBy { it }.eachCount().toMutableMap()
    return word.all { ch ->
        val count = remaining[ch] ?: 0
        if (count == 0) false else { remaining[ch] = count - 1; true }
    }
}

data class Cell(val row: Int, val column: Int) { val key: String get() = "$row:$column" }
enum class Direction { ACROSS, DOWN }
data class Slot(val id: String, val word: String, val row: Int, val column: Int, val direction: Direction) {
    val cells: List<Cell> get() = word.indices.map {
        Cell(row + if (direction == Direction.DOWN) it else 0, column + if (direction == Direction.ACROSS) it else 0)
    }
}
data class Entry(val key: String, val display: String, val definition: String)
data class Level(val id: Int, val letters: List<Char>, val rows: Int, val columns: Int,
                 val slots: List<Slot>, val bonusPool: Set<String>) {
    val cells: Map<Cell, Char> get() = buildMap {
        slots.forEach { slot -> slot.cells.zip(slot.word.toList()).forEach { (cell, ch) -> put(cell, ch) } }
    }
}
data class Campaign(val version: Int, val levels: List<Level>, val dictionary: Map<String, Entry>, val dailyLevels: List<Level> = levels)
data class Economy(val version: Int = 1, val initial: Long = 200, val victory: Long = 20,
                   val chest: Long = 10, val letter: Long = 50, val word: Long = 100,
                   val clue: Long = 150, val cap: Long = 1_000_000,
                   val daily: Long = 30, val rewarded: Long = 30, val winExtra: Long = 20,
                   val rewardedMax: Int = 10, val gifts: List<Long> = giftRewards,
                   val ads: AdRules = AdRules())
data class AdRules(val firstLevels: Int = 10, val firstSeconds: Long = 600, val minWins: Int = 4,
                   val minSeconds: Long = 180, val rewardedCooldown: Long = 120, val hourMax: Int = 3, val dayMax: Int = 12)
enum class Mode { CAMPAIGN, DAILY, TUTORIAL }

data class Session(
    val id: String, val levelId: Int, val replay: Boolean = false,
    val solved: Set<String> = emptySet(), val revealed: Set<String> = emptySet(),
    val bonus: Set<String> = emptySet(), val clues: Set<String> = emptySet(),
    val order: List<Int> = emptyList(), val hintCount: Int = 0,
    val completed: Boolean = false, val reward: Long = 0,
    val economyVersion: Int = 1,
    val mode: Mode = Mode.CAMPAIGN, val dateKey: String? = null,
    val revision: Long = 0, val tutorialLetterUsed: Boolean = false,
) {
    val stars: Int get() = (3 - hintCount.coerceAtMost(2))
}
fun Campaign.levelFor(session: Session): Level = (if(session.mode==Mode.DAILY) dailyLevels else levels).first {it.id==session.levelId}

fun visibleCells(level: Level, session: Session): Set<String> = session.revealed +
    level.slots.filter { it.id in session.solved }.flatMap { it.cells.map(Cell::key) }

/** Canonical slots plus purchased cells; intersections may finish additional slots. */
fun settle(level: Level, session: Session): Session {
    var state = session
    while (true) {
        val visible = visibleCells(level, state)
        val solved = state.solved + level.slots.filter { it.cells.all { cell -> cell.key in visible } }.map { it.id }
        if (solved == state.solved) return state.copy(completed = solved.size == level.slots.size)
        state = state.copy(solved = solved)
    }
}

enum class Answer { TARGET, BONUS, ALREADY_FOUND, OUTSIDE_LEVEL, UNKNOWN, IMPOSSIBLE, FINISHED }
data class Submission(val session: Session, val answer: Answer)
fun submit(level: Level, session: Session, raw: String, dictionary: Set<String>): Submission {
    if (session.completed) return Submission(session, Answer.FINISHED)
    val key = wordKey(raw)
    if (!key.matches(Regex("[А-Я]{2,7}")) || !canSpell(key, level.letters)) return Submission(session, Answer.IMPOSSIBLE)
    val slot = level.slots.firstOrNull { it.word == key }
    if (slot != null) {
        if (slot.id in session.solved) return Submission(session, Answer.ALREADY_FOUND)
        return Submission(settle(level, session.copy(solved = session.solved + slot.id)), Answer.TARGET)
    }
    if (key in session.bonus) return Submission(session, Answer.ALREADY_FOUND)
    if (key in level.bonusPool) return Submission(session.copy(bonus = session.bonus + key), Answer.BONUS)
    return Submission(session, if (key in dictionary) Answer.OUTSIDE_LEVEL else Answer.UNKNOWN)
}

enum class Hint { LETTER, WORD, CLUE }
data class HintResult(val session: Session, val cost: Long, val definition: String? = null)
fun hint(level: Level, session: Session, kind: Hint, target: String, economy: Economy,
         dictionary: Map<String, Entry>): HintResult? {
    if (session.completed) return null
    val next: Session
    val price: Long
    var definition: String? = null
    when (kind) {
        Hint.LETTER -> {
            if (level.cells.keys.none { it.key == target } || target in visibleCells(level, session)) return null
            val free=session.mode==Mode.TUTORIAL && !session.tutorialLetterUsed
            next = settle(level, session.copy(revealed = session.revealed + target, hintCount = session.hintCount + if(free) 0 else 1,tutorialLetterUsed=session.tutorialLetterUsed || free))
            price = if(free) 0 else economy.letter
        }
        Hint.WORD, Hint.CLUE -> {
            val slot = level.slots.firstOrNull { it.id == target && it.id !in session.solved } ?: return null
            if (kind == Hint.WORD) {
                next = settle(level, session.copy(solved = session.solved + target, hintCount = session.hintCount + 1))
                price = economy.word
            } else {
                definition = dictionary[slot.word]?.definition?.takeIf { it.isNotBlank() } ?: return null
                val paid = target in session.clues
                next = session.copy(clues = session.clues + target, hintCount = session.hintCount + if (paid) 0 else 1)
                price = if (paid) 0 else economy.clue
            }
        }
    }
    return HintResult(next, if (session.replay) 0 else price, definition)
}

/** Static content constraints are also checked when the package is loaded. */
fun validateCampaign(campaign: Campaign) {
    require(campaign.levels.isNotEmpty()) { "Нет уровней" }
    require(campaign.levels.map { it.id } == (1..campaign.levels.size).toList()) { "Неверная последовательность уровней" }
    campaign.levels.forEach { level ->
        fun check(value: Boolean, message: String) = require(value) { "Уровень ${level.id}: $message" }
        check(level.rows in 1..9 && level.columns in 1..9 && level.letters.size in 3..7, "размеры")
        check(level.slots.size >= 2 && level.slots.map { it.id }.distinct().size == level.slots.size, "слоты")
        check(level.slots.map { it.word }.distinct().size == level.slots.size, "повтор слова")
        val occupied = mutableMapOf<Cell, MutableList<Pair<Char, Direction>>>()
        level.slots.forEach { slot ->
            check(slot.word.matches(Regex("[А-Я]{3,7}")) && canSpell(slot.word, level.letters), "буквы ${slot.id}")
            check(!campaign.dictionary[slot.word]?.definition.isNullOrBlank(), "нет определения ${slot.id}")
            slot.cells.zip(slot.word.toList()).forEach { (cell, ch) ->
                check(cell.row in 0 until level.rows && cell.column in 0 until level.columns, "координаты")
                occupied.getOrPut(cell) { mutableListOf() }.add(ch to slot.direction)
            }
        }
        occupied.values.forEach { items ->
            check(items.map { it.first }.distinct().size == 1 && items.map { it.second }.distinct().size == items.size, "наложение")
        }
        // Every horizontal/vertical run of at least two cells must be an authored slot.
        for (direction in Direction.entries) {
            val expected = level.slots.filter { it.direction == direction }.map { it.cells }.toSet()
            occupied.keys.forEach { start ->
                val previous = if (direction == Direction.ACROSS) Cell(start.row,start.column-1) else Cell(start.row-1,start.column)
                if (previous !in occupied) {
                    val run = mutableListOf<Cell>(); var p = start
                    while (p in occupied) {
                        run.add(p)
                        p = if (direction == Direction.ACROSS) Cell(p.row,p.column+1) else Cell(p.row+1,p.column)
                    }
                    check(run.size < 2 || run in expected, "незаданный ряд")
                }
            }
        }
        val reached = mutableSetOf(level.slots.first().id)
        do {
            val old = reached.size
            val linked = level.slots.filter { it.id in reached }.flatMap { it.cells }.toSet()
            reached += level.slots.filter { it.cells.any { cell -> cell in linked } }.map { it.id }
        } while (reached.size != old)
        check(reached.size == level.slots.size, "несвязная сетка")
        level.bonusPool.forEach { check(it in campaign.dictionary && canSpell(it,level.letters) && level.slots.none { s -> s.word==it },"бонус") }
    }
}

data class Point(val x: Float, val y: Float)
/** Segment hit testing keeps fast swipes from skipping a letter between frames. */
fun crossedTokens(from: Point, to: Point, centers: List<Point>, radius: Float): List<Int> {
    val dx = to.x-from.x; val dy = to.y-from.y; val length = dx*dx+dy*dy
    return centers.mapIndexedNotNull { index, center ->
        val ox=from.x-center.x; val oy=from.y-center.y
        val c=ox*ox+oy*oy-radius*radius
        val t = if (c<=0f) 0f else if (length==0f) return@mapIndexedNotNull null else {
            val b=2f*(ox*dx+oy*dy); val disc=b*b-4f*length*c
            if (disc<0) return@mapIndexedNotNull null
            (-b-sqrt(disc))/(2f*length)
        }
        if(t in 0f..1f) index to t else null
    }.sortedWith(compareBy<Pair<Int,Float>> { it.second }.thenBy { it.first }).map { it.first }
}
fun appendToken(selected: List<Int>, token: Int): List<Int> = when {
    selected.size>=2 && token==selected[selected.lastIndex-1] -> selected.dropLast(1)
    token in selected -> selected
    else -> selected+token
}
