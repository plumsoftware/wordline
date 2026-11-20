package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.data.*
import ru.plumsoftware.wordline.game.Session
import ru.plumsoftware.wordline.game.Economy
import ru.plumsoftware.wordline.game.Mode
import ru.plumsoftware.wordline.game.levelFor
import ru.plumsoftware.wordline.game.locationNames

@Composable
fun WelcomeScreen(busy: Boolean, onStart: () -> Unit, onSkip: () -> Unit, onPrivacy: ()->Unit = {}) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(36.dp))
        Paper(Modifier.widthIn(max=480.dp)) {
            Text("Соедини слова — кроссворды",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
            Text("Соединяй буквы и открывай пейзажи. Играй в своём темпе: без жизней и таймера.",fontSize=18.sp)
            Text("Проведи по К → О → Т или нажимай на буквы и выбирай «Проверить».")
            Text("Прогресс хранится на этом устройстве. Для игры не нужны аккаунт и интернет.")
            MainButton("Начать",!busy,onStart)
            TextButton(onClick=onSkip,enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("Пропустить обучение") }
            Text("Прогресс хранится на устройстве. Видео с наградой необязательно; рекламная сеть обрабатывает данные при загрузке объявлений.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick=onPrivacy,modifier=Modifier.fillMaxWidth()) {Text("Конфиденциальность")}
        }
    }
}
@Composable
fun MapScreen(state: AppState, back: () -> Unit, play: (Int,Boolean) -> Unit) {
    val campaign=requireNotNull(state.campaign);val snapshot=requireNotNull(state.snapshot)
    val current=campaign.levels.firstOrNull { it.id !in snapshot.completions }?.id
    var replay by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeader("Карта пути",back=back)
        val scroll=androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex=((current ?: campaign.levels.size)-2).coerceAtLeast(0))
        LazyColumn(state=scroll,contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            item { Paper { Text("Путь открытий",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${campaign.levels.size} уровней, 10 локаций. Каждые 100 побед открывают следующую.") } }
            items(campaign.levels,key={it.id}) { level ->
                val done=snapshot.completions[level.id]; val open=level.id==current || done!=null
                Surface(onClick={if(done!=null) replay=level.id else play(level.id,false)},enabled=open && !state.busy,
                    shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),color=if(level.id==current) PaleGreen else MaterialTheme.colorScheme.surface,
                    modifier=Modifier.fillMaxWidth().semantics { contentDescription=if(!open) "Уровень ${level.id}. Пройдите уровень ${level.id-1}" else "Уровень ${level.id}" }) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        GameIcon(if(done!=null) R.drawable.game_ic_check else if(open) R.drawable.game_ic_play else R.drawable.game_ic_lock)
                        Column(Modifier.weight(1f)) {
                            Text("Уровень ${level.id}",fontWeight=FontWeight.Bold,fontSize=18.sp)
                            Text(if(done!=null) "Повтор · без наград" else if(open) "Продолжить путь" else "Пройди уровень ${level.id-1}",style=MaterialTheme.typography.bodySmall)
                        }
                        if(done!=null) StarRating(done.bestStars)
                    }
                }
            }
        }
    }
    replay?.let { id -> AlertDialog(onDismissRequest={replay=null},title={Text("Повторить уровень $id?")},text={Text("Монеты повторно не выдаются. Подсказки бесплатны; можно улучшить звёзды.")},confirmButton={TextButton(onClick={replay=null;play(id,true)}) {Text("Играть")}},dismissButton={TextButton(onClick={replay=null}) {Text("Отмена")}}) }
}
@Composable
fun ResultScreen(state: AppState, session: Session, onNext: () -> Unit, onHome: () -> Unit, reward: () -> Unit = {}, daily: ()->Unit = {}) {
    val campaign=requireNotNull(state.campaign)
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(36.dp))
        Paper(Modifier.widthIn(max=480.dp)) {
            GameIcon(R.drawable.game_ic_trophy,modifier=Modifier.align(Alignment.CenterHorizontally).size(64.dp),tint=Green)
            Text("Отличная работа!",Modifier.fillMaxWidth().semantics { heading() },textAlign=TextAlign.Center,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
            Text(if(session.mode==Mode.DAILY) "Кроссворд за ${session.dateKey} пройден" else if(session.mode==Mode.TUTORIAL) "Ты готов к путешествию" else "Уровень ${session.levelId} пройден",Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
            StarRating(session.stars,Modifier.align(Alignment.CenterHorizontally))
            Text(if(session.mode==Mode.TUTORIAL) "Обучение завершено" else if(session.replay) "Повтор · без наград" else "+${session.reward} монет уже начислено",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontWeight=FontWeight.Bold,color=Green)
            Balance(state.snapshot?.profile?.coins ?: 0)
            if(!session.replay && session.mode!=Mode.TUTORIAL) {
                val claimed=state.snapshot?.world?.adTickets?.values?.any {it.resultId==session.id && it.status=="committed"}==true
                val reason=adUnavailable(state,"win_extra",session.id)
                OutlinedButton(onClick=reward,enabled=!claimed && !state.busy && reason==null,modifier=Modifier.fillMaxWidth()) {Text(if(claimed) "Бонус получен" else "Ещё +${state.economy.winExtra} за видео · Реклама")}
                if(reason!=null && !claimed) Text(reason,style=MaterialTheme.typography.bodySmall)
            }
            if(session.mode==Mode.CAMPAIGN && session.levelId==campaign.levels.size && !session.replay) Text("Все ${campaign.levels.size} уровней пройдены. Можно вернуться к любому из них на карте.")
            if(session.mode==Mode.CAMPAIGN && session.levelId==campaign.levels.size) OutlinedButton(onClick=daily,modifier=Modifier.fillMaxWidth()) {Text("Ежедневный кроссворд")}
            MainButton(if(session.mode==Mode.DAILY) "К календарю" else if(session.replay || session.levelId==campaign.levels.size && session.mode==Mode.CAMPAIGN) "На главную" else "Дальше",!state.busy,onNext)
            TextButton(onClick=onHome,modifier=Modifier.fillMaxWidth()) { Text("На главную") }
        }
        Paper(Modifier.widthIn(max=480.dp).fillMaxWidth()) {
            Text("Открытые слова",fontWeight=FontWeight.Bold)
            campaign.levelFor(session).slots.forEach { slot ->
                val entry=campaign.dictionary.getValue(slot.word)
                Text("${entry.display} — ${entry.definition}")
            }
        }
    }
}
@Composable
fun SettingsScreen(state: AppState, back: () -> Unit, change: (Setting,Boolean) -> Unit, help: () -> Unit, save: ()->Unit = {}, about: ()->Unit = {}, accessibility: ()->Unit = {}) {
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeader("Настройки",back=back)
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Paper {
                SettingToggle("Музыка","Спокойный фон",state.settings.music,!state.busy) {change(Setting.MUSIC,it)}
                SettingToggle("Звуки","Выбор букв и подтверждение действий",state.settings.effects,!state.busy) {change(Setting.EFFECTS,it)}
                SettingToggle("Вибрация","Тактильное подтверждение",state.settings.vibration,!state.busy) {change(Setting.VIBRATION,it)}
                SettingToggle("Видеореклама","Разрешить загрузку рекламы. Рекламная сеть обрабатывает сетевые данные; игра доступна и без видео.",state.settings.adsAllowed,!state.busy) {change(Setting.ADS_ALLOWED,it)}
                Text("Удобно именно тебе",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                SettingToggle("Контрастная тема","Непрозрачный фон и чёткие границы",state.settings.highContrast,!state.busy) {change(Setting.CONTRAST,it)}
                SettingToggle("Крупные буквы","Увеличить буквы поля и круга",state.settings.largeLetters,!state.busy) {change(Setting.LARGE_LETTERS,it)}
                SettingToggle("Ввод нажатием","Набери слово и нажми «Проверить»",state.settings.tapInput,!state.busy) {change(Setting.TAP_INPUT,it)}
                SettingToggle("Уменьшить движение","Статичные переходы без полётов и вспышек",state.settings.reduceMotion,!state.busy) {change(Setting.REDUCE_MOTION,it)}
                OutlinedButton(onClick=help,modifier=Modifier.fillMaxWidth()) {Text("Как играть")}
                OutlinedButton(onClick=save,modifier=Modifier.fillMaxWidth()) {Text("Сохранение")}
                OutlinedButton(onClick=about,modifier=Modifier.fillMaxWidth()) {Text("Об игре")}
                OutlinedButton(onClick=accessibility,modifier=Modifier.fillMaxWidth()) {Text("Удобство")}
            }
            Paper {
                Text("Об игре",fontWeight=FontWeight.Bold)
                Text("Соедини слова - кроссворды\nВерсия ${ru.plumsoftware.wordline.BuildConfig.VERSION_NAME} · ${state.campaign?.levels?.size ?: 0} уровней")
                Text("Прогресс сохраняется автоматически на этом устройстве. Аккаунт и интернет не нужны.")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title,fontWeight=FontWeight.Bold);Text(description,style=MaterialTheme.typography.bodySmall) }
        Switch(checked,onCheckedChange=onChange,enabled=enabled,modifier=Modifier.semantics { contentDescription=title })
    }
}
@Composable
fun HelpScreen(economy: Economy, back: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeader("Как играть",back=back)
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            listOf("1. Соединяй" to "Проведи по буквам, чтобы собрать слово. Вернись к предыдущей букве, чтобы исправить жест. Отпусти палец — слово проверится.",
                "2. Или нажимай" to "Нажимай на буквы и затем «Проверить». Одну позицию можно выбрать один раз. Последняя выбранная буква снимается повторным нажатием. Этот способ работает и с TalkBack.",
                "3. Открывай пересечения" to "Найденное слово открывает общие буквы. Полностью раскрытое пересечениями слово засчитывается автоматически. Ё и Е в игре считаются одной буквой.",
                "4. Помощь рядом" to "Перемешивание бесплатно. Буква стоит ${economy.letter} монет, слово — ${economy.word}, намёк — ${economy.clue}. Сначала выбери клетку или слово, затем подтверди покупку. Неверные ответы не штрафуются.",
                "5. Собирай звёзды" to "Без подсказок — три звезды, одна подсказка — две, две и больше — одна. Повтор уровня не выдаёт монеты, но позволяет улучшить звёзды. Подсказки на повторе бесплатны и всё равно учитываются в оценке.").forEach { (title,body) ->
                Paper {Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(body)}
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
