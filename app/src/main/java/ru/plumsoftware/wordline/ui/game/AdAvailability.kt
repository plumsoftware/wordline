package ru.plumsoftware.wordline.ui.game

import ru.plumsoftware.wordline.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Only a newly committed SDK reward turns this particular offer green. Failures never do. */
@Composable
internal fun rememberRewardReceived(state: AppState,placement: String,resultId: String?=null,offerKey: Any?=null): Boolean {
    val world=state.snapshot?.world
    val committed=world?.adTickets?.values.orEmpty().filter {
        it.placement==placement && it.resultId==resultId && it.status=="committed"
    }.map {it.id}.toSet()
    val initial=remember(world?.generation,placement,resultId,offerKey) {committed}
    return (committed-initial).isNotEmpty()
}

fun adUnavailable(state: AppState,placement: String,resultId: String?=null): String? {
    if(!BuildConfig.ADS_ENABLED) return "Видео недоступно в этой версии"
    if(!state.settings.adsAllowed) return "Разреши загрузку видео в настройках"
    val w=state.snapshot?.world ?: return "Подготовка игры"
    if(w.adTickets.values.any {it.status in setOf("pending","shown")}) return "Другое видео ещё открыто"
    if(w.adTickets.values.count {it.date==w.effectiveDate && it.status in setOf("pending","shown","committed")}>=state.economy.rewardedMax) return "Бонусные видео на сегодня закончились"
    if(placement=="win_extra" && w.adTickets.values.any {it.placement==placement && it.resultId==resultId && it.status=="committed"}) return "Бонус получен"
    return null
}
