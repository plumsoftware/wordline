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
fun UnlockScreen(index: Int,done: ()->Unit) {
    ProgressPage("Новая локация",done) {
        Paper {
            Image(painterResource(backgroundResources[index.coerceIn(0,9)]),locationNames[index.coerceIn(0,9)],Modifier.fillMaxWidth())
            Text(locationNames[index.coerceIn(0,9)],style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
            Text("Следующая часть пути открыта. Фон можно выбрать в оформлении.")
            MainButton("Отправиться",onClick=done)
            TextButton(onClick=done,modifier=Modifier.fillMaxWidth()) {Text("Позже")}
        }
    }
}
@Composable
fun AccessibilityScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    ProgressPage("Удобство",back) {
        Paper {
            Text("Предпросмотр: А Б В",style=if(state.settings.largeLetters) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall)
            listOf(Triple(Setting.LARGE_LETTERS,"Крупные игровые буквы",state.settings.largeLetters),
                Triple(Setting.TAP_INPUT,"Ввод нажатием",state.settings.tapInput),Triple(Setting.CONTRAST,"Высокий контраст",state.settings.highContrast),
                Triple(Setting.REDUCE_MOTION,"Уменьшить движение",state.settings.reduceMotion)).forEach { (key,label,value) ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {Text(label,Modifier.weight(1f));Switch(value,{vm.setting(key,it)},enabled=!state.busy,modifier=Modifier.semantics {contentDescription=label})}
            }
            Text("Буквы можно выбирать отдельными нажатиями и затем нажать «Проверить». При TalkBack этот режим включается автоматически. При большом системном размере текста круг перестраивается в удобные ряды кнопок.")
        }
    }
}
@Composable
fun WalletScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    ProgressPage("Монеты",back) {
        Paper {
            Balance(state.snapshot?.profile?.coins ?: 0)
            Text("Монеты можно получать за первые победы, задания, подарки и достижения. Покупки за деньги нет.")
            Text("Победа: +${state.economy.victory}. Ежедневный кроссворд: +${state.economy.daily}. Полный сундук: +${state.economy.chest}.")
            if(!state.settings.adsAllowed && BuildConfig.ADS_ENABLED) {
                Text("Для видео нужна загрузка рекламы. Рекламная сеть обрабатывает сетевые данные; персонализация и определение местоположения отключены. Это необязательно для игры.")
                OutlinedButton(onClick={vm.setting(Setting.ADS_ALLOWED,true)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Разрешить загрузку видео")}
            }
            MainButton("+${state.economy.rewarded} монет за видео · Реклама",adUnavailable(state,"wallet_coins")==null && !state.busy,{vm.reward("wallet_coins")})
            adUnavailable(state,"wallet_coins")?.let {Text(it)}
            val world=state.snapshot?.world
            val used=world?.adTickets?.values?.count {it.date==world.effectiveDate && it.status=="committed"} ?: 0
            Text("Получено наград за видео сегодня: $used из ${state.economy.rewardedMax}. После просмотра монеты зачисляются отдельно от покупки подсказки.")
        }
    }
}
@Composable
fun AboutScreen(back: ()->Unit,article: (String)->Unit,help: ()->Unit,debug: ()->Unit = {}) {
    val context=LocalContext.current;val clipboard=LocalClipboardManager.current
    var error by remember {mutableStateOf(false)}
    ProgressPage("Об игре",back) {
        Paper {
            Text("Соедини слова - кроссворды",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            Text("Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            if(BuildConfig.DEBUG) {Text("Тестовая версия · ${BuildConfig.STORE}");TextButton(onClick=debug) {Text("Диагностика видео")}}
            OutlinedButton(onClick=help,modifier=Modifier.fillMaxWidth()) {Text("Как играть")}
            OutlinedButton(onClick={article("privacy")},modifier=Modifier.fillMaxWidth()) {Text("Конфиденциальность")}
            OutlinedButton(onClick={article("licenses")},modifier=Modifier.fillMaxWidth()) {Text("Лицензии")}
            if(BuildConfig.SUPPORT_EMAIL.isNotBlank()) OutlinedButton(onClick={
                try {context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:"+BuildConfig.SUPPORT_EMAIL)))} catch(e: android.content.ActivityNotFoundException) {error=true}
            },modifier=Modifier.fillMaxWidth()) {Text("Поддержка")}
        }
    }
    if(error) AlertDialog(onDismissRequest={error=false},title={Text("Почтовое приложение не найдено")},text={Text(BuildConfig.SUPPORT_EMAIL)},confirmButton={TextButton(onClick={clipboard.setText(AnnotatedString(BuildConfig.SUPPORT_EMAIL));error=false}) {Text("Скопировать адрес")}},dismissButton={TextButton(onClick={error=false}) {Text("Назад")}})
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
