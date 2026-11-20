package ru.plumsoftware.wordline.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ru.plumsoftware.wordline.game.*
import java.security.MessageDigest

private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
fun loadCampaign(context: Context): Campaign {
    val bytes = context.assets.open("content/campaign-v1.json").use { it.readBytes() }
    val manifest = context.assets.open("content/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
    return parseCampaign(bytes, manifest.getString("sha256"))
}

fun parseCampaign(bytes: ByteArray, expectedSha256: String): Campaign {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    require(digest == expectedSha256) { "Повреждён пакет уровней" }
    val root = JSONObject(bytes.toString(Charsets.UTF_8))
    val dictionary = root.getJSONArray("dictionary").let { array ->
        (0 until array.length()).map { i -> array.getJSONObject(i).let { Entry(it.getString("key"),it.getString("display"),it.getString("definition")) } }.associateBy { it.key }
    }
    fun parseLevels(array: JSONArray): List<Level> = array.let {
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val slots = obj.getJSONArray("slots").let { rows -> (0 until rows.length()).map { index ->
                val s = rows.getJSONObject(index)
                Slot(s.getString("id"),s.getString("word"),s.getInt("row"),s.getInt("column"),
                    if(s.getString("direction")=="across") Direction.ACROSS else Direction.DOWN)
            } }
            Level(obj.getInt("id"),obj.getJSONArray("letters").strings().map { it.single() },obj.getInt("rows"),obj.getInt("columns"),slots,obj.getJSONArray("bonusPool").strings().toSet())
        }
    }
    val levels=parseLevels(root.getJSONArray("levels"))
    val daily=root.optJSONArray("dailyLevels")?.let(::parseLevels) ?: levels
    return Campaign(root.getInt("contentVersion"),levels,dictionary,daily).also {
        validateCampaign(it)
        validateCampaign(it.copy(levels=daily))
    }
}

fun loadEconomy(context: Context): Economy {
    val obj=context.assets.open("content/economy-v1.json").bufferedReader().use { JSONObject(it.readText()) }
    require(obj.getString("economyVersion")=="1.0") { "Неизвестная версия экономики" }
    return Economy(initial=obj.getLong("initialCoins"),victory=obj.getLong("campaignWin"),
        chest=obj.getLong("bonusChestCoins"),letter=obj.getLong("hintLetterCost"),
        word=obj.getLong("hintWordCost"),clue=obj.getLong("hintClueCost"),cap=obj.getLong("walletCap"),
        daily=obj.getLong("dailyWin"),rewarded=obj.getLong("rewardedCoins"),winExtra=obj.getLong("winExtraCoins"),
        rewardedMax=obj.getInt("rewardedDailyMax"),gifts=obj.getJSONArray("giftCycle").let {a ->(0 until a.length()).map {a.getLong(it)}},
        ads=obj.getJSONObject("interstitial").let {a ->AdRules(a.getInt("noAdsFirstCampaignLevels"),a.getLong("noAdsFirstActiveSeconds"),a.getInt("minWins"),a.getLong("minActiveSeconds"),a.getLong("afterRewardedCooldownSeconds"),a.getInt("rollingHourMax"),a.getInt("dailyMax"))})
        .also { e -> require(e.initial in 0..e.cap && listOf(e.victory,e.chest,e.letter,e.word,e.clue).all { it in 0..e.cap }) }
}

fun Session.toRow(): SessionRow {
    val json = JSONObject().put("solved",JSONArray(solved.toList())).put("revealed",JSONArray(revealed.toList()))
        .put("bonus",JSONArray(bonus.toList())).put("clues",JSONArray(clues.toList())).put("order",JSONArray(order))
        .put("hintCount",hintCount).put("completed",completed).put("reward",reward).put("economyVersion",economyVersion)
        .put("mode",mode.name).put("dateKey",dateKey ?: JSONObject.NULL).put("revision",revision).put("tutorialLetterUsed",tutorialLetterUsed)
    return SessionRow(id,levelId,replay,json.toString())
}
fun SessionRow.toDomain(): Session {
    val o=JSONObject(json)
    return Session(id,levelId,replay,o.getJSONArray("solved").strings().toSet(),o.getJSONArray("revealed").strings().toSet(),
        o.getJSONArray("bonus").strings().toSet(),o.getJSONArray("clues").strings().toSet(),
        o.getJSONArray("order").let { a -> (0 until a.length()).map { a.getInt(it) } },
        o.getInt("hintCount"),o.getBoolean("completed"),o.getLong("reward"),o.getInt("economyVersion"),
        Mode.valueOf(o.optString("mode","CAMPAIGN")),if(o.isNull("dateKey")) null else o.optString("dateKey").takeIf {it.isNotBlank()},o.optLong("revision"),o.optBoolean("tutorialLetterUsed"))
}
