package ru.plumsoftware.wordline.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import ru.plumsoftware.wordline.BuildConfig
import ru.plumsoftware.wordline.data.*
import ru.plumsoftware.wordline.game.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map {getJSONObject(it)}
data class SaveCandidate(val root: JSONObject, val payload: JSONObject, val coins: Long, val completed: Int, val exportedAt: String)
class SaveRepository(private val context: Context, private val game: GameRepository) {
    private val backupDir=File(context.filesDir,"save-backups")
    suspend fun exportBytes(settings: GameSettings): ByteArray = game.exclusive {encode(settings)}
    private suspend fun encode(settings: GameSettings): ByteArray {
        val w=game.world()
        require(w.adTickets.values.none {it.status in setOf("pending","shown")}) {"Дождитесь завершения рекламы"}
        val dao=game.dao;val profile=requireNotNull(dao.profile())
        val payload=JSONObject().put("profile",JSONObject().put("coins",profile.coins).put("campaignSessionId",profile.campaignSessionId ?: JSONObject.NULL))
            .put("world",w.copy(adTickets=w.adTickets.filterValues {it.status=="committed"}).toJson())
            .put("completions",JSONArray().apply {dao.completions().forEach {put(JSONObject().put("levelId",it.levelId).put("bestStars",it.bestStars).put("completedAt",it.completedAt))}})
            .put("sessions",JSONArray().apply {dao.sessions().forEach {put(JSONObject().put("id",it.id).put("levelId",it.levelId).put("replay",it.replay).put("state",JSONObject(it.json)))}})
            .put("ledger",JSONArray().apply {dao.ledger().forEach {put(JSONObject().put("id",it.operationId).put("reason",it.reason).put("source",it.sourceId).put("amount",it.amount).put("balanceAfter",it.balanceAfter).put("createdAt",it.createdAt))}})
            .put("settings",JSONObject().put("highContrast",settings.highContrast).put("largeLetters",settings.largeLetters).put("tapInput",settings.tapInput).put("reduceMotion",settings.reduceMotion))
        val root=JSONObject().put("formatId","connected-words-save").put("schemaVersion",1)
            .put("appVersionName",BuildConfig.VERSION_NAME).put("appVersionCode",BuildConfig.VERSION_CODE)
            .put("exportedAtUtc",Instant.now().toString()).put("contentVersion",game.campaign.version)
            .put("dictionaryVersion",game.campaign.version).put("economyVersion",game.economy.version)
            .put("payloadSha256",sha256(canonicalJson(payload).toByteArray())).put("payload",payload)
        return canonicalJson(root).toByteArray(Charsets.UTF_8).also {require(it.size<=MAX_SAVE_BYTES) {"Сохранение больше 10 МиБ"}}
    }
    suspend fun exportTo(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri,"wt").use {out ->requireNotNull(out) {"Файл недоступен"};out.write(bytes);out.flush()}
        game.markExported()
    }
    suspend fun read(uri: Uri): SaveCandidate = withTimeout(30_000) {
        runInterruptible(Dispatchers.IO) {
            val bytes=context.contentResolver.openInputStream(uri).use {input ->
                requireNotNull(input) {"Не удалось открыть файл"};val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val count=input.read(buffer);if(count<0) break;require(out.size()+count<=MAX_SAVE_BYTES) {"Файл больше 10 МиБ"};out.write(buffer,0,count)}
                out.toByteArray()
            };validate(bytes)
        }
    }
    fun validate(bytes: ByteArray): SaveCandidate {
        val root=strictJson(bytes)
        require(root.getString("formatId")=="connected-words-save") {"Это не сохранение игры"}
        require(root.getInt("schemaVersion")==1 && root.getInt("contentVersion")<=game.campaign.version && root.getInt("economyVersion")==game.economy.version) {"Обновите игру, чтобы открыть это сохранение"}
        val payload=root.getJSONObject("payload")
        require(sha256(canonicalJson(payload).toByteArray())==root.getString("payloadSha256")) {"Контрольная сумма не совпадает"}
        val coins=payload.getJSONObject("profile").getLong("coins");require(coins in 0..game.economy.cap)
        val completions=payload.getJSONArray("completions").objects()
        val completed=completions.map {it.getInt("levelId")}
        require(completed.distinct().size==completed.size && completed.sorted()==(1..completed.size).toList()) {"Нарушена последовательность уровней"}
        completions.forEach {require(it.getInt("levelId")<=game.campaign.levels.size && it.getInt("bestStars") in 1..3 && it.getLong("completedAt")>=0)}
        payload.getJSONArray("sessions").objects().forEach {o ->
            val state=o.getJSONObject("state")
            listOf("solved","revealed","bonus","clues").forEach {key ->val a=state.getJSONArray(key);require(a.stringList().distinct().size==a.length()) {"Повтор записи $key"}}
        }
        val sessions=payload.getJSONArray("sessions").objects().map {SessionRow(it.getString("id"),it.getInt("levelId"),it.getBoolean("replay"),it.getJSONObject("state").toString()).toDomain()}
        require(sessions.map {it.id}.distinct().size==sessions.size)
        sessions.forEach {s ->
            val level=game.campaign.levelFor(s)
            require(s.id.length in 1..200 && s.economyVersion==game.economy.version && s.reward in 0..game.economy.cap && s.hintCount in 0..1000 && s.revision>=0)
            require(s.order.sorted()==level.letters.indices.toList()) {"Неверный порядок букв"}
            require(s.solved.all {id ->level.slots.any {it.id==id}} && s.clues.all {id ->level.slots.any {it.id==id}})
            require(s.revealed.all {key ->level.cells.keys.any {it.key==key}} && s.bonus.all {it in level.bonusPool})
            require(settle(level,s).let {it.solved==s.solved && it.completed==s.completed}) {"Противоречивое поле"}
            if(s.mode==Mode.DAILY) require(LocalDate.parse(requireNotNull(s.dateKey)).toString()==s.dateKey)
        }
        require(sessions.count {!it.completed && !it.replay && it.mode==Mode.CAMPAIGN}<=1 && sessions.count {!it.completed && !it.replay && it.mode==Mode.DAILY}<=1)
        val current=payload.getJSONObject("profile").optString("campaignSessionId").takeIf {it!="null" && it.isNotEmpty()}
        require(current==null || sessions.any {it.id==current && it.mode==Mode.CAMPAIGN && !it.replay})
        val ledger=payload.getJSONArray("ledger").objects()
        require(ledger.map {it.getString("id")}.distinct().size==ledger.size)
        require(ledger.sumOf {it.getLong("amount")}==coins) {"Журнал и баланс не совпадают"}
        ledger.forEach {require(it.getLong("amount") in -game.economy.cap..game.economy.cap && it.getLong("balanceAfter") in 0..game.economy.cap && it.getLong("createdAt")>=0)}
        val worldJson=payload.getJSONObject("world")
        listOf("visits","claims","dailyCompleted","uniqueWords","noHintLevels","trailEvents","collectibles","acknowledgedLocations").forEach {key ->val a=worldJson.getJSONArray(key);require((0 until a.length()).map {a.get(it)}.distinct().size==a.length()) {"Повтор записи $key"}}
        val w=worldFromJson(worldJson);validateWorld(w)
        require(sessions.size+completions.size+ledger.size+w.claims.size+w.visits.size+w.days.size+w.dailyAssignments.size+w.adTickets.size+w.collectibles.size<=100_000) {"Слишком много записей"}
        require(completed.all {id ->ledger.any {it.getString("id")=="campaign:first:$id"}}) {"Нет записи о победе"}
        require(w.dailyCompleted.all {date ->ledger.any {it.getString("id")=="daily:$date"}})
        require(w.uniqueWords.size<=w.totalWords && w.bonusWords<=w.totalWords)
        require(w.noHintLevels.all {id ->completions.any {it.getInt("levelId")==id && it.getInt("bestStars")==3}})
        sessions.forEach {session ->
            if(session.mode==Mode.CAMPAIGN) {
                require(!session.replay || session.levelId in completed)
                require(!session.completed || session.levelId in completed)
                require(session.replay || session.completed || session.levelId==completed.size+1)
            }
            if(session.mode==Mode.DAILY) require(w.dailyAssignments[requireNotNull(session.dateKey)]==session.levelId)
        }
        require(w.trailEvents.all {event ->
            if(event.startsWith("campaign:")) event.substringAfter(':').toIntOrNull() in completed
            else event.startsWith("daily:") && event.substringAfter(':') in w.dailyCompleted
        })
        w.adTickets.values.forEach {ticket ->require(ledger.any {it.getString("id")=="ad:${ticket.id}"})}

        Instant.parse(root.getString("exportedAtUtc"))
        return SaveCandidate(root,payload,coins,completed.size,root.getString("exportedAtUtc"))
    }
    private fun validateWorld(w: World) {
        require(w.generation.length in 1..200 && w.streakRun.length in 1..200)
        val date=LocalDate.parse(w.effectiveDate);require(w.giftStep in game.economy.gifts.indices && w.streak in 1..100_000 && w.bestStreak>=w.streak)
        require(w.activeMillis>=0 && w.lastExportAt>=0 && w.trailEvents.size<=30 && w.acknowledgedLocations.all {it in 0..9})
        require(listOf(w.totalWords,w.bonusWords,w.chests,w.hints,w.winsSinceInterstitial).all {it in 0..100_000_000})
        require(w.uniqueWords.all {it in game.campaign.dictionary} && w.noHintLevels.all {id ->game.campaign.levels.any {it.id==id}})
        (w.visits+w.dailyAssignments.keys+w.days.keys+w.dailyCompleted).forEach {require(LocalDate.parse(it)<=date)}
        require(w.dailyCompleted.all {it in w.dailyAssignments} && w.dailyAssignments.values.all {id ->game.campaign.dailyLevels.any {it.id==id}})
        w.days.values.forEach {require(it.words>=0 && it.wins>=0 && it.bonus>=0 && it.daily>=0 && it.activeMillis>=0)}
        require(w.collectibles.all {id ->id.matches(Regex("location:[0-9]|trail:(10|20|30)|month:[0-9]{4}-[0-9]{2}"))})
        require(w.adTickets.values.all {it.status=="committed" && it.reward in 0..game.economy.cap && LocalDate.parse(it.date)<=date})
        w.adShowings.forEach {require(it.wallMillis>=0 && it.activeMillis in 0..w.activeMillis && LocalDate.parse(it.date)<=date)}
    }
    suspend fun apply(candidate: SaveCandidate, settings: GameSettings) {
        game.exclusive {
        val backup=encode(settings)
        withContext(Dispatchers.IO) {
            backupDir.mkdirs()
            backupDir.listFiles()?.filter {it.extension=="pending"}?.forEach {it.delete()}
            val file=File(backupDir,"${System.currentTimeMillis()}-${UUID.randomUUID()}.wordsave")
            val pending=File(backupDir,file.nameWithoutExtension+".pending")
            java.io.FileOutputStream(pending).use {it.write(backup);it.flush();it.fd.sync()}
            validate(pending.readBytes())
            require(pending.renameTo(file)) {"Не удалось завершить страховочную копию"}
            validate(file.readBytes()) // A bad or incomplete safety copy aborts before any database change.
            backupDir.listFiles()?.filter {it.extension=="wordsave"}?.sortedByDescending {it.name}?.drop(3)?.forEach {it.delete()}
        }
            require(game.world().adTickets.values.none {it.status in setOf("pending","shown")}) {"Дождитесь завершения рекламы"}
            val p=candidate.payload;val dao=game.dao
            dao.clearSessions();dao.clearCompletions();dao.clearLedger()
            val profile=p.getJSONObject("profile")
            dao.save(ProfileRow(coins=candidate.coins,campaignSessionId=if(profile.isNull("campaignSessionId")) null else profile.getString("campaignSessionId")))
            p.getJSONArray("sessions").objects().forEach {dao.save(SessionRow(it.getString("id"),it.getInt("levelId"),it.getBoolean("replay"),it.getJSONObject("state").toString()))}
            p.getJSONArray("completions").objects().forEach {dao.save(CompletionRow(it.getInt("levelId"),it.getInt("bestStars"),it.getLong("completedAt")))}
            p.getJSONArray("ledger").objects().forEach {dao.record(LedgerRow(it.getString("id"),it.getString("reason"),it.getString("source"),it.getLong("amount"),it.getLong("balanceAfter"),it.getLong("createdAt")))}
            game.save(worldFromJson(p.getJSONObject("world")).copy(generation=UUID.randomUUID().toString(),clockNotice=false))
        }
        game.resetActivityBaseline(worldFromJson(candidate.payload.getJSONObject("world")).activeMillis)
        // Product retention policy: keep the last three verified private snapshots, never external files.
        withContext(Dispatchers.IO) {backupDir.listFiles()?.filter {it.extension=="wordsave"}?.sortedByDescending {it.name}?.drop(3)?.forEach {it.delete()}}
    }
    suspend fun previous(): SaveCandidate? = withContext(Dispatchers.IO) {
        backupDir.listFiles()?.filter {it.extension=="wordsave"}?.sortedByDescending {it.name}?.firstNotNullOfOrNull {runCatching {validate(it.readBytes())}.getOrNull()}
    }
}
