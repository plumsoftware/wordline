package ru.plumsoftware.wordline.ui.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.BuildConfig
import ru.plumsoftware.wordline.data.Setting
import ru.plumsoftware.wordline.game.*

@Composable
fun WalletScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    val received=rememberRewardReceived(state,"wallet_coins")
    ProgressPage("Монеты",back) {
        Paper {
            Balance(state.snapshot?.profile?.coins ?: 0)
            Text("Монеты можно получать за первые победы, задания, подарки и достижения. Покупки за деньги нет.")
            Text("Победа: +${state.economy.victory}. Ежедневный кроссворд: +${state.economy.daily}. Полный сундук: +${state.economy.chest}.")
            if(!state.settings.adsAllowed && BuildConfig.ADS_ENABLED) {
                Text("Для видео нужна загрузка рекламы. Рекламная сеть обрабатывает сетевые данные; персонализация и определение местоположения отключены. Это необязательно для игры.")
                OutlinedButton(onClick={vm.setting(Setting.ADS_ALLOWED,true)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Разрешить загрузку видео")}
            }
            VictoryButton(if(received) "Награда получена" else "+${state.economy.rewarded} монет за видео",!received,
                Modifier.fillMaxWidth().height(66.dp),1f,adUnavailable(state,"wallet_coins")==null && !state.busy && !received,{vm.reward("wallet_coins")})
            adUnavailable(state,"wallet_coins")?.let {Text(it)}
            val world=state.snapshot?.world
            val used=world?.adTickets?.values?.count {it.date==world.effectiveDate && it.status=="committed"} ?: 0
            Text("Получено наград за видео сегодня: $used из ${state.economy.rewardedMax}. После просмотра монеты зачисляются отдельно от покупки подсказки.")
        }
    }
}
@Composable
fun ArticleScreen(kind: String,back: ()->Unit) {
    val context=LocalContext.current
    var unavailable by remember {mutableStateOf(false)}
    ProgressPage(if(kind=="licenses") "Лицензии" else "Конфиденциальность",back) {
        Paper {
            if(kind=="licenses") {
                Text("Игровые определения и синтезированный звук созданы для проекта. Графика поставлена владельцем в комплекте ТЗ. Системный шрифт используется средствами Android.")
                Text("AndroidX и Kotlin: Apache License2.0. Yandex Mobile Ads распространяется по условиям Яндекса. Открытые библиотеки сохраняют собственные лицензии и уведомления.")
                Text("Условия рекламного SDK: https://legal.yandex.com/partner_ch/")
            } else {
                Text("Игровой прогресс, монеты, настройки и статистика хранятся на устройстве. Аккаунта и собственного сервера игры нет. Перенос выполняется файлом, выбранным вами.")
                Text("После разрешения загрузки рекламы объявления показывает Yandex Mobile Ads. Рекламная сеть может получать сетевые и технические данные устройства. Прогресс и содержимое сохранения в рекламные запросы не передаются. Персонализация и отслеживание местоположения отключены.")
                Text("Вы можете запретить загрузку рекламы в настройках и продолжать основную игру без интернета. Доступ к контактам, фотографиям и всему хранилищу игра не запрашивает.")
                if(BuildConfig.PRIVACY_URL.startsWith("https://")) OutlinedButton(onClick={try {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(BuildConfig.PRIVACY_URL)))} catch(e: android.content.ActivityNotFoundException) {unavailable=true}},modifier=Modifier.fillMaxWidth()) {Text("Полная политика издателя")}
            }
        }
    }
    if(unavailable) AlertDialog(onDismissRequest={unavailable=false},text={Text("Приложение для открытия ссылки не найдено")},confirmButton={TextButton(onClick={unavailable=false}) {Text("Понятно")}})
}

@Composable
fun DebugAdsScreen(state: AppState,ads: ru.plumsoftware.wordline.ads.AdsState,back: ()->Unit) {
    if(!BuildConfig.DEBUG) return
    val w=state.snapshot?.world
    ProgressPage("Диагностика видео",back) {
        Paper {
            Text("Магазин: ${BuildConfig.STORE}; источник ID: demo")
            Text("demo-rewarded-yandex\ndemo-interstitial-yandex")
            Text("Состояние: ${ads.phase}")
            Text("Наград сегодня: ${w?.adTickets?.values?.count {it.date==w.effectiveDate && it.status=="committed"} ?: 0}")
            Text("Побед после interstitial: ${w?.winsSinceInterstitial ?: 0}")
            Text("Активная игра: ${(w?.activeMillis ?: 0)/1000} с")
            ads.log.forEach {Text(it,style=MaterialTheme.typography.bodySmall)}
        }
    }
}
