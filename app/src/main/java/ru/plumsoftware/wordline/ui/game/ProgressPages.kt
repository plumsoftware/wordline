package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.BuildConfig
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val backgroundResources=listOf(R.drawable.game_lake_day,R.drawable.game_forest_river,R.drawable.game_lake_evening,R.drawable.game_autumn_valley,R.drawable.game_night_mountains,R.drawable.game_sea_coast,R.drawable.game_canyon,R.drawable.game_flower_meadow,R.drawable.game_lotus_lake,R.drawable.game_snow_valley)
val postcardResources=listOf(R.drawable.postcard_lake_day,R.drawable.postcard_forest_river,R.drawable.postcard_lake_evening,R.drawable.postcard_autumn_valley,R.drawable.postcard_night_mountains,R.drawable.postcard_sea_coast,R.drawable.postcard_canyon,R.drawable.postcard_flower_meadow,R.drawable.postcard_lotus_lake,R.drawable.postcard_snow_valley)

@Composable
fun ProgressPage(title: String,back: ()->Unit,content: @Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeader(title,back=back)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable
fun RewardRow(title: String,progress: Int,target: Int,reward: Long,claimed: Boolean,busy: Boolean,claim: ()->Unit) {
    Paper(Modifier.fillMaxWidth()) {
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Text("${progress.coerceAtMost(target)} из $target · +$reward монет")
        LinearProgressIndicator(progress={(progress.toFloat()/target).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
        Button(onClick=claim,enabled=progress>=target && !claimed && !busy,modifier=Modifier.fillMaxWidth()) {Text(if(claimed) "Получено" else if(progress>=target) "Забрать" else "В процессе")}
    }
}
@Composable
fun TasksScreen(state: AppState,vm: GameViewModel,calendar: ()->Unit,trail: ()->Unit,gift: ()->Unit,streak: ()->Unit,resume: (String)->Unit) {
    val snapshot=requireNotNull(state.snapshot);val w=snapshot.world
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Сегодняшние открытия",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(w.effectiveDate)
        MainButton("Ежедневный кроссворд",!state.busy,calendar)
        snapshot.sessions.values.firstOrNull {it.mode==Mode.DAILY && !it.completed && !it.replay}?.let {s ->
            OutlinedButton(onClick={resume(s.id)},modifier=Modifier.fillMaxWidth()) {Text("Продолжить кроссворд за ${s.dateKey}")}
        }
        tasks(w,w.effectiveDate,snapshot.completions.size==state.campaign?.levels?.size).forEach {task ->
            RewardRow(task.title,task.progress,task.target,task.reward,"task:${w.effectiveDate}:${task.id}" in w.claims,state.busy) {vm.claimTask(task.id)}
        }
        OutlinedButton(onClick=gift,modifier=Modifier.fillMaxWidth()) {Text(if("gift:${w.effectiveDate}" in w.claims) "Подарок получен · следующий завтра" else "Подарок дня")}
        OutlinedButton(onClick=streak,modifier=Modifier.fillMaxWidth()) {Text("Серия посещений: ${w.streak}")}
        OutlinedButton(onClick=trail,modifier=Modifier.fillMaxWidth()) {Text("Личная тропа: ${w.trailEvents.size} из 30")}
        Spacer(Modifier.height(16.dp))
    }
}
@Composable
fun CalendarScreen(state: AppState,back: ()->Unit,play: (String)->Unit) {
    val w=requireNotNull(state.snapshot).world
    val today=LocalDate.parse(w.effectiveDate);val month=YearMonth.from(today)
    ProgressPage("Календарь",back) {
        Paper {
            Text(today.format(DateTimeFormatter.ofPattern("LLLL yyyy",Locale.forLanguageTag("ru"))),style=MaterialTheme.typography.headlineSmall)
            Text("Прошедшие дни этого месяца доступны. Будущие откроются в свою дату.")
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                Row {listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach {Box(Modifier.width(48.dp).padding(vertical=8.dp),contentAlignment=Alignment.Center) {Text(it)}}}
                val offset=month.atDay(1).dayOfWeek.value-1
                repeat((offset+month.lengthOfMonth()+6)/7) {week ->
                    Row {
                        repeat(7) {day ->
                            val number=week*7+day-offset+1
                            if(number !in 1..month.lengthOfMonth()) Spacer(Modifier.size(48.dp)) else {
                                val date=month.atDay(number);val key=date.toString();val done=key in w.dailyCompleted
                                val available=date<=today
                                TextButton(onClick={play(key)},enabled=available && !state.busy,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).width(48.dp).semantics {
                                    contentDescription="$number, ${if(done) "пройден" else if(date==today) "сегодня" else if(available) "доступен" else "будущий день"}"
                                },contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(containerColor=if(done) PaleGreen else if(date==today) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)) {Text(if(done) "$number ✓" else number.toString())}
                            }
                        }
                    }
                }
            }
            val count=w.dailyCompleted.count {it.startsWith(month.toString())}
            Text("Открытка месяца: $count из 20 разных дат")
        }
    }
}
@Composable
fun GiftScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    val w=requireNotNull(state.snapshot).world;val claimed="gift:${w.effectiveDate}" in w.claims
    ProgressPage("Подарок дня",back) {
        Paper {
            Image(painterResource(R.drawable.item_gift),null,Modifier.size(96.dp).align(Alignment.CenterHorizontally))
            Text(if(claimed) "До завтра!" else "Рады тебя видеть",style=MaterialTheme.typography.headlineMedium)
            Text("Пропуск дня не сбрасывает цикл подарков.")
            state.economy.gifts.forEachIndexed {index,amount ->Text("День ${index+1}: +$amount монет${if(index==w.giftStep && !claimed) " · сегодня" else ""}")}
            MainButton(if(claimed) "Подарок уже получен" else "Забрать +${state.economy.gifts[w.giftStep]}",!claimed && !state.busy,vm::claimGift)
        }
    }
}
@Composable
fun StreakScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    val w=requireNotNull(state.snapshot).world;val milestone=w.streak/7*7
    ProgressPage("День за днём",back) {
        Paper {
            Image(painterResource(R.drawable.item_streak_flame),null,Modifier.size(96.dp).align(Alignment.CenterHorizontally))
            Text("${w.streak} дней подряд",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
            Text("Лучшая серия: ${w.bestStreak}. Пропуск начинает новую серию, но уже полученные призы остаются.")
            RewardRow("Семидневный приз",w.streak,(milestone+7).takeIf {milestone==0} ?: milestone,50,milestone>0 && "streak:${w.streakRun}:$milestone" in w.claims,state.busy,vm::claimStreak)
        }
    }
}
@Composable
fun TrailScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    val w=requireNotNull(state.snapshot).world
    ProgressPage("Личная тропа",back) {
        Paper {Text("Без спешки и дедлайна",style=MaterialTheme.typography.headlineSmall);Text("Один лист за первую победу в кампании или ежедневном кроссворде. Повторы и видео не дают листьев.")}
        trailRewards.forEach { (target,reward) ->RewardRow("$target листьев и открытка",w.trailEvents.size,target,reward,"trail:$target" in w.claims,state.busy) {vm.claimTrail(target)}}
    }
}
@Composable
fun AchievementsScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    ProgressPage("Достижения",back) {
        achievements(snapshot.world,snapshot.completions.size).forEach {a ->RewardRow(a.title,a.progress,a.target,a.reward,"achievement:${a.id}" in snapshot.world.claims,state.busy) {vm.claimAchievement(a.id)}}
    }
}
@Composable
fun StatisticsScreen(state: AppState,back: ()->Unit) {
    val s=requireNotNull(state.snapshot);val w=s.world
    ProgressPage("Статистика",back) {
        Paper {
            listOf("Засчитанные слова" to w.totalWords,"Уникальные слова" to w.uniqueWords.size,"Первые победы кампании" to s.completions.size,
                "Дополнительные слова" to w.bonusWords,"Бонусные сундуки" to w.chests,"Подсказки" to w.hints,"Лучшая серия дней" to w.bestStreak).forEach { (label,value) ->Text("$label: $value")}
            Text("Число находок и число уникальных слов могут отличаться. Повторы уровней не увеличивают счётчики.")
        }
        Paper {
            Text("Последние семь дней",fontWeight=FontWeight.Bold)
            val today=LocalDate.parse(w.effectiveDate)
            (6 downTo 0).forEach {offset ->val date=today.minusDays(offset.toLong()).toString();val day=w.days[date] ?: DayStats();Text("$date: ${day.words} слов, ${day.wins+day.daily} побед, ${day.activeMillis/60_000} мин игры")}
        }
    }
}
@Composable
fun CollectionScreen(state: AppState,achievements: ()->Unit,statistics: ()->Unit,appearance: ()->Unit,open: (Int)->Unit,openExtra: (String)->Unit = {}) {
    val w=requireNotNull(state.snapshot).world
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=20.dp)) {
        item {Text("Коллекция путешествий",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
        item {OutlinedButton(onClick=appearance,modifier=Modifier.fillMaxWidth()) {Text("Оформление")}}
        item {OutlinedButton(onClick=achievements,modifier=Modifier.fillMaxWidth()) {Text("Достижения")}}
        item {OutlinedButton(onClick=statistics,modifier=Modifier.fillMaxWidth()) {Text("Статистика")}}
        items((0..9).toList(),key={it}) {index ->
            val owned="location:$index" in w.collectibles
            Surface(onClick={open(index)},enabled=owned,shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
                Column {
                    if(owned) Image(painterResource(postcardResources[index]),locationNames[index],Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.Crop)
                    Column(Modifier.padding(16.dp)) {Text(locationNames[index],fontWeight=FontWeight.Bold);Text(if(owned) "Открытка получена" else "Откроется после уровня ${20+100*index}")}
                }
            }
        }
        item {Paper {Text("Другие открытки",fontWeight=FontWeight.Bold);if(w.collectibles.none {!it.startsWith("location:")}) Text("Здесь появятся открытки личной тропы и календаря.") else w.collectibles.filter {!it.startsWith("location:")}.sorted().forEach {id ->OutlinedButton(onClick={openExtra(id)},modifier=Modifier.fillMaxWidth()) {Text(if(id.startsWith("trail:")) "Тропа: ${id.substringAfter(':')} листьев" else "Календарь: ${id.substringAfter(':')}")}}}}
    }
}
@Composable
fun PostcardScreen(state: AppState,index: Int,vm: GameViewModel,back: ()->Unit) {
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {uri ->vm.exportPostcard(uri,postcardResources[index],locationNames[index])}
    ProgressPage(locationNames[index],back) {
        Paper {
            Image(painterResource(postcardResources[index]),locationNames[index],Modifier.fillMaxWidth(),contentScale=ContentScale.FillWidth)
            Text("Получено за уровень ${20+index*100}")
            MainButton("Сделать фоном",!state.busy,{vm.appearance(index,state.settings.theme)})
            OutlinedButton(onClick={vm.share(postcardResources[index],locationNames[index])},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Поделиться")}
            OutlinedButton(onClick={export.launch("connected-words-postcard.png")},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Сохранить открытку в файл")}
            if(state.feedback.isNotBlank()) Text(state.feedback)
        }
    }
}
@Composable
fun AppearanceScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot);val count=snapshot.completions.size
    ProgressPage("Оформление",back) {
        Paper {
            listOf("day" to "Дневная · доступна сразу","contrast" to "Контрастная · доступна сразу","evening" to "Вечерняя · после уровня 50","forest" to "Лесная · после уровня 100","trail" to "Тропа · 30 листьев").forEach { (id,title) ->
                val unlocked=id in setOf("day","contrast") || id=="evening" && count>=50 || id=="forest" && count>=100 || id=="trail" && snapshot.world.trailEvents.size>=30
                OutlinedButton(onClick={vm.appearance(state.settings.background.coerceAtMost(count/100),id)},enabled=unlocked && !state.busy,modifier=Modifier.fillMaxWidth()) {Text(title+if(state.settings.theme==id) " ✓" else "")}
            }
        }
        locationNames.forEachIndexed {index,name ->
            Paper {
                Text(name,fontWeight=FontWeight.Bold)
                Image(painterResource(backgroundResources[index]),null,Modifier.fillMaxWidth().height(120.dp),contentScale=ContentScale.Crop)
                OutlinedButton(onClick={vm.appearance(index,state.settings.theme)},enabled=count>=index*100 && !state.busy,modifier=Modifier.fillMaxWidth()) {Text(if(count<index*100) "Пройди уровень ${index*100}" else if(state.settings.background==index) "Выбран" else "Выбрать фон")}
            }
        }
    }
}
@Composable
fun SaveScreen(state: AppState,vm: GameViewModel,back: ()->Unit) {
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"),vm::exportTo)
    val importPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(),vm::readSave)
    val phase=state.filePhase
    BackHandler(enabled=phase in setOf("reading","confirm","applying")) {if(phase!="applying") vm.cancelFile()}
    ProgressPage("Сохранение",back) {
        Paper {
            Text("На этом устройстве",style=MaterialTheme.typography.headlineSmall)
            Text("Пройдено ${state.snapshot?.completions?.size ?: 0} уровней. ${state.snapshot?.profile?.coins ?: 0} монет.")
            Text("Облачной синхронизации нет. При удалении приложения данные могут исчезнуть. Сохрани файл, чтобы перенести прогресс.")
            val at=state.snapshot?.world?.lastExportAt ?: 0
            Text(if(at==0L) "Экспортов пока не было" else "Последний экспорт: ${java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
            MainButton("Экспорт в файл",!state.busy && phase!="reading",{vm.prepareExport {export.launch("connected-words-save-${LocalDate.now()}.wordsave")}})
            OutlinedButton(onClick={importPicker.launch(arrayOf("application/json","application/octet-stream","*/*"))},enabled=!state.busy && phase!="reading",modifier=Modifier.fillMaxWidth()) {Text("Импорт из файла")}
            OutlinedButton(onClick=vm::previousSave,enabled=!state.busy && phase!="reading",modifier=Modifier.fillMaxWidth()) {Text("Предыдущая копия")}
            Text("Перед заменой создаётся страховочная копия. Игра хранит три последние внутренние копии; более старые заменяются. Внешние файлы не удаляются. Настройки удобства этого устройства сохраняются.")
        }
        if(phase=="exported" || phase=="imported") Paper {Text(if(phase=="exported") "Файл сохранён" else "Прогресс загружен",fontWeight=FontWeight.Bold);TextButton(onClick=vm::cancelFile) {Text("Готово")}}
    }
    if(phase=="reading" || phase=="applying" || phase=="preparing") AlertDialog(onDismissRequest={if(phase=="reading") vm.cancelFile()},title={Text(if(phase=="reading") "Проверяем файл" else if(phase=="applying") "Загружаем прогресс" else "Готовим сохранение")},text={CircularProgressIndicator()},confirmButton={if(phase=="reading") TextButton(onClick=vm::cancelFile) {Text("Отмена")}})
    if(phase=="confirm") state.candidate?.let {candidate ->AlertDialog(onDismissRequest=vm::cancelFile,title={Text("Заменить текущий прогресс?")},text={Text("Сейчас: ${state.snapshot?.completions?.size ?: 0} уровней, ${state.snapshot?.profile?.coins ?: 0} монет.\nИз файла: ${candidate.completed} уровней, ${candidate.coins} монет.\nЭкспорт: ${candidate.exportedAt}.\nТекущий прогресс будет заменён. Сначала создадим страховочную копию.")},confirmButton={TextButton(onClick=vm::importSave,enabled=!state.busy) {Text("Загрузить")}},dismissButton={TextButton(onClick=vm::cancelFile) {Text("Отмена")}})}
}

@Composable
fun ExtraPostcardScreen(state: AppState,id: String,vm: GameViewModel,back: ()->Unit) {
    val title=if(id.startsWith("trail:")) "Тропа: ${id.substringAfter(':')} листьев" else "Календарь: ${id.substringAfter(':')}"
    val drawable=postcardResources.first()
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {uri ->vm.exportPostcard(uri,drawable,title)}
    ProgressPage(title,back) {
        Paper {
            Image(painterResource(drawable),title,Modifier.fillMaxWidth())
            Text(title,fontWeight=FontWeight.Bold)
            Text("Открытка получена и сохранена в коллекции.")
            MainButton("Сделать фоном",!state.busy,{vm.appearance(0,state.settings.theme)})
            OutlinedButton(onClick={vm.share(drawable,title)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Поделиться")}
            OutlinedButton(onClick={export.launch("connected-words-postcard.png")},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Сохранить открытку в файл")}
            if(state.feedback.isNotBlank()) Text(state.feedback)
        }
    }
}
