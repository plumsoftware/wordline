package ru.plumsoftware.wordline.ui.game

import ru.plumsoftware.wordline.BuildConfig

fun adUnavailable(state: AppState,placement: String,resultId: String?=null): String? {
    if(!BuildConfig.ADS_ENABLED) return "Видео недоступно в этой версии"
    if(!state.settings.adsAllowed) return "Разреши загрузку видео в настройках"
    val w=state.snapshot?.world ?: return "Подготовка игры"
    if(w.adTickets.values.any {it.status in setOf("pending","shown")}) return "Другое видео ещё открыто"
    if(w.adTickets.values.count {it.date==w.effectiveDate && it.status in setOf("pending","shown","committed")}>=state.economy.rewardedMax) return "Бонусные видео на сегодня закончились"
    val last=w.adShowings.lastOrNull {it.rewarded}
    if(last!=null && w.activeMillis-last.activeMillis<60_000) return "Следующее видео — после минуты активной игры"
    if(placement=="win_extra" && w.adTickets.values.any {it.placement==placement && it.resultId==resultId && it.status=="committed"}) return "Бонус получен"
    return null
}
