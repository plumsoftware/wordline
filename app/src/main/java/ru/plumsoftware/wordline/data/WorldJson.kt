package ru.plumsoftware.wordline.data

import org.json.JSONArray
import org.json.JSONObject
import ru.plumsoftware.wordline.game.*

internal fun JSONArray.stringList(): List<String> = (0 until length()).map { getString(it) }
internal fun JSONObject.stringSet(key: String): Set<String> = optJSONArray(key)?.stringList()?.toSet() ?: emptySet()
internal fun JSONObject.objectMap(key: String): Map<String,JSONObject> = optJSONObject(key)?.let { o -> o.keys().asSequence().associateWith { o.getJSONObject(it) } } ?: emptyMap()
fun World.toJson(): JSONObject = JSONObject().apply {
    put("generation",generation);put("effectiveDate",effectiveDate);put("visits",JSONArray(visits.sorted()))
    put("streakRun",streakRun);put("streak",streak);put("bestStreak",bestStreak);put("giftStep",giftStep)
    put("claims",JSONArray(claims.sorted()));put("dailyAssignments",JSONObject(dailyAssignments));put("dailyCompleted",JSONArray(dailyCompleted.sorted()))
    put("days",JSONObject().apply { days.forEach { (key,d) -> put(key,JSONObject().put("words",d.words).put("wins",d.wins).put("bonus",d.bonus).put("daily",d.daily).put("activeMillis",d.activeMillis)) } })
    put("uniqueWords",JSONArray(uniqueWords.sorted()));put("totalWords",totalWords);put("bonusWords",bonusWords);put("chests",chests);put("hints",hints)
    put("noHintLevels",JSONArray(noHintLevels.sorted()));put("trailEvents",JSONArray(trailEvents.sorted()));put("collectibles",JSONArray(collectibles.sorted()))
    put("acknowledgedLocations",JSONArray(acknowledgedLocations.sorted()));put("activeMillis",activeMillis);put("winsSinceInterstitial",winsSinceInterstitial)
    put("adTickets",JSONObject().apply {adTickets.forEach { (key,a) ->put(key,JSONObject().put("generation",a.generation).put("placement",a.placement).put("date",a.date).put("resultId",a.resultId ?: JSONObject.NULL).put("reward",a.reward).put("status",a.status))}})
    put("adShowings",JSONArray().apply {adShowings.forEach { a ->put(JSONObject().put("date",a.date).put("wallMillis",a.wallMillis).put("activeMillis",a.activeMillis).put("rewarded",a.rewarded).put("ticketId",a.ticketId)) }})
    put("lastExportAt",lastExportAt);put("clockNotice",clockNotice);put("dailyReport",JSONArray(dailyReport))
}
fun worldFromJson(o: JSONObject): World = World(
    generation=o.getString("generation"),effectiveDate=o.getString("effectiveDate"),visits=o.stringSet("visits"),
    streakRun=o.getString("streakRun"),streak=o.getInt("streak"),bestStreak=o.getInt("bestStreak"),giftStep=o.getInt("giftStep"),claims=o.stringSet("claims"),
    dailyAssignments=o.optJSONObject("dailyAssignments")?.let {a ->a.keys().asSequence().associateWith {a.getInt(it)}} ?: emptyMap(),dailyCompleted=o.stringSet("dailyCompleted"),
    days=o.objectMap("days").mapValues { (_,d) ->DayStats(d.getInt("words"),d.getInt("wins"),d.getInt("bonus"),d.getInt("daily"),d.getLong("activeMillis")) },
    uniqueWords=o.stringSet("uniqueWords"),totalWords=o.getInt("totalWords"),bonusWords=o.getInt("bonusWords"),chests=o.getInt("chests"),hints=o.getInt("hints"),
    noHintLevels=o.optJSONArray("noHintLevels")?.let {a ->(0 until a.length()).map {a.getInt(it)}.toSet()} ?: emptySet(),trailEvents=o.stringSet("trailEvents"),collectibles=o.stringSet("collectibles"),
    acknowledgedLocations=o.optJSONArray("acknowledgedLocations")?.let {a ->(0 until a.length()).map {a.getInt(it)}.toSet()} ?: setOf(0),activeMillis=o.getLong("activeMillis"),winsSinceInterstitial=o.getInt("winsSinceInterstitial"),
    adTickets=o.objectMap("adTickets").mapValues { (key,a) ->AdTicket(key,a.getString("generation"),a.getString("placement"),a.getString("date"),if(a.isNull("resultId")) null else a.getString("resultId"),a.getLong("reward"),a.getString("status")) },
    adShowings=o.optJSONArray("adShowings")?.let {a ->(0 until a.length()).map {i ->a.getJSONObject(i).let {AdShowing(it.getString("date"),it.getLong("wallMillis"),it.getLong("activeMillis"),it.getBoolean("rewarded"),it.optString("ticketId"))}} ?: emptyList(),
    lastExportAt=o.optLong("lastExportAt"),clockNotice=o.optBoolean("clockNotice"),dailyReport=o.optJSONArray("dailyReport")?.stringList() ?: emptyList())
