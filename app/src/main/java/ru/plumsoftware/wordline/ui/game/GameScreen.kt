package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*

@Composable
fun GameScreen(state: AppState, session: Session, onBack: () -> Unit, onMap: () -> Unit,
               onSubmit: (String) -> Unit, onShuffle: () -> Unit, onHint: (Hint,String) -> Unit,
               onActive: (Boolean)->Unit = {}, onVideo: ()->Unit = {}, online: Boolean = true) {
    val campaign=requireNotNull(state.campaign)
    val level=campaign.levelFor(session)
    var pause by rememberSaveable(session.id) { mutableStateOf(false) }
    var bonus by rememberSaveable(session.id) { mutableStateOf(false) }
    var hintKind by remember(session.id) { mutableStateOf<Hint?>(null) }
    var inputEpoch by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var tutorialDismissed by rememberSaveable(session.id) { mutableStateOf(false) }
    val owner=LocalLifecycleOwner.current
    val focused=LocalWindowInfo.current.isWindowFocused
    DisposableEffect(owner,session.id) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_STOP) {inputEpoch++;dragging=false}
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(focused) {if(!focused) {inputEpoch++;dragging=false}}
    fun showPause() {inputEpoch++;pause=true}
    BackHandler {
        when {
            hintKind!=null -> hintKind=null
            bonus -> bonus=false
            else -> {inputEpoch++;pause=!pause}
        }
    }
    val enabled=!state.busy && !pause && !bonus && hintKind==null && !session.completed && focused
    SideEffect {onActive(focused && !pause && !bonus && hintKind==null && !session.completed)}
    val visible=visibleCells(level,session)
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeader(if(session.mode==Mode.DAILY) "Ежедневный" else if(session.mode==Mode.TUTORIAL) "Учимся играть" else "Уровень ${level.id}",state.snapshot?.profile?.coins ?: 0,back=::showPause)
        if(!online) Text("Офлайн · игра доступна, видео может быть недоступно",style=MaterialTheme.typography.bodySmall)
        if(session.mode==Mode.DAILY) Text(session.dateKey.orEmpty())
        if(session.replay) Text("Повтор · без наград",fontWeight=FontWeight.Bold,color=Green)
        Text("Найдено ${session.solved.size} из ${level.slots.size} слов",Modifier.padding(vertical=4.dp))
        if(session.mode==Mode.TUTORIAL && !tutorialDismissed) {
            Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),color=PaleGreen) {
                Column(Modifier.padding(12.dp)) {
                    Text(if(session.solved.isEmpty()) "Собери К → О → Т одним жестом или нажми буквы и «Проверить»." else if(session.order==level.letters.indices.toList()) "Общая буква уже открыта. Попробуй бесплатное перемешивание в центре круга." else if(!session.tutorialLetterUsed) "Теперь попробуй первую бесплатную букву. Она не снижает учебные звёзды." else "Осталось закончить последнее слово. Все дальнейшие уровни сохраняются автоматически.")
                    TextButton(onClick={tutorialDismissed=true}) { Text("Понятно") }
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide=maxWidth>=600.dp || (maxHeight<470.dp && maxWidth>420.dp)
            val board: @Composable ColumnScope.() -> Unit = {
                CrosswordBoard(level,session,state.settings.largeLetters,Modifier.padding(vertical=12.dp))
                OutlinedButton(onClick={inputEpoch++;bonus=true},enabled=!dragging && !state.busy,modifier=Modifier.align(Alignment.CenterHorizontally)) {
                    GameIcon(R.drawable.game_ic_bookmark);Spacer(Modifier.width(8.dp))
                    Text("Дополнительные слова: ${session.bonus.size} из ${level.bonusPool.size}")
                }
            }
            val tools: @Composable ColumnScope.() -> Unit = {
                LetterWheel(level,session,state.settings.tapInput,state.settings.largeLetters,enabled,inputEpoch,
                    onSubmit=onSubmit,onShuffle=onShuffle,onDragging={dragging=it})
                Text(state.feedback.ifBlank { "Без штрафов и спешки" },Modifier.fillMaxWidth().padding(vertical=4.dp).semantics { liveRegion=LiveRegionMode.Polite })
                HintButtons(session,state.economy,enabled && !dragging,visible.size<level.cells.size) {inputEpoch++;hintKind=it}
                Spacer(Modifier.height(12.dp))
            }
            if(wide) Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState(),enabled=!dragging),horizontalAlignment=Alignment.CenterHorizontally,content=board)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState(),enabled=!dragging),horizontalAlignment=Alignment.CenterHorizontally,content=tools)
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(),enabled=!dragging),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {board();tools()}
        }
    }
    if(pause) AlertDialog(onDismissRequest={pause=false},title={Text("Передохнём?")},text={Text("Все найденные слова и раскрытые буквы сохранены.")},confirmButton={TextButton(onClick={pause=false}) {Text("Продолжить")}},dismissButton={TextButton(onClick={pause=false;onMap()}) {Text("На карту")}})
    if(bonus) AlertDialog(onDismissRequest={bonus=false},title={Text("Дополнительные слова")},text={
        Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(session.bonus.isEmpty()) Text("Здесь появятся слова, которые можно собрать из букв, но которых нет в сетке.")
            session.bonus.sorted().forEach {key -> Text("${campaign.dictionary.getValue(key).display} — ${campaign.dictionary.getValue(key).definition}")}
            if(level.bonusPool.size>=5) Text("Сундуки: ${session.bonus.size/5}. До следующего: ${session.bonus.size%5} из 5. Награда — ${state.economy.chest} монет${if(session.replay) ", только при первом прохождении" else ""}.")
            else Text("Найдено ${session.bonus.size} из ${level.bonusPool.size} дополнительных слов. На этом уровне нет сундука.")
        }
    },confirmButton={TextButton(onClick={bonus=false}) {Text("К игре")}})
    hintKind?.let { kind -> HintDialog(level,session,kind,state.economy,state.snapshot?.profile?.coins ?: 0,
        onDismiss={hintKind=null},onConfirm={target ->hintKind=null;onHint(kind,target)},onVideo=if(session.replay || session.mode==Mode.TUTORIAL) null else onVideo,videoReason=adUnavailable(state,"hint_coins",session.id)) }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HintButtons(session: Session, economy: Economy, enabled: Boolean, hasCells: Boolean, select: (Hint) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterHorizontally),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        val freeTutorial=session.mode==Mode.TUTORIAL && !session.tutorialLetterUsed
        listOf(Triple(Hint.LETTER,"Буква",if(freeTutorial) 0L else economy.letter),Triple(Hint.WORD,"Слово",economy.word),Triple(Hint.CLUE,"Намёк",economy.clue)).forEach { (kind,label,price) ->
            OutlinedButton(onClick={select(kind)},enabled=enabled && hasCells && (session.mode!=Mode.TUTORIAL || kind==Hint.LETTER),contentPadding=PaddingValues(horizontal=12.dp,vertical=12.dp),modifier=Modifier.semantics {
                contentDescription="$label, ${if(session.replay || price==0L) "бесплатно" else "$price монет"}"
            }) { Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text(label,fontWeight=FontWeight.Bold)
                Text(if(session.replay || price==0L) "Бесплатно" else "$price монет",style=MaterialTheme.typography.labelMedium)
            } }
        }
    }
}
@Composable
private fun HintDialog(level: Level, session: Session, kind: Hint, economy: Economy, coins: Long, onDismiss: () -> Unit, onConfirm: (String) -> Unit, onVideo: (()->Unit)? = null, videoReason: String? = null) {
    var target by remember(kind) {mutableStateOf<String?>(null)}
    var selected by remember(kind) {mutableStateOf<Cell?>(null)}
    val visible=visibleCells(level,session)
    val candidates=if(kind==Hint.LETTER) level.cells.keys.filter { it.key !in visible }.sortedWith(compareBy<Cell> {it.row}.thenBy {it.column}).map { it.key to "Строка ${it.row+1}, столбец ${it.column+1}" }
        else level.slots.filter {it.id !in session.solved && (selected==null || selected in it.cells)}.map { slot ->
            val mask=slot.cells.mapIndexed { i,c ->if(c.key in visible) slot.word[i] else '□'}.joinToString(" ")
            slot.id to "$mask · ${if(slot.direction==Direction.ACROSS) "горизонтально" else "вертикально"}"
        }
    val price=when {session.replay || session.mode==Mode.TUTORIAL && !session.tutorialLetterUsed && kind==Hint.LETTER ->0L;kind==Hint.LETTER ->economy.letter;kind==Hint.WORD ->economy.word;target in session.clues ->0L;else ->economy.clue}
    AlertDialog(onDismissRequest=onDismiss,title={Text(when(kind) {Hint.LETTER -> "Выбери клетку";Hint.WORD -> "Выбери слово";Hint.CLUE -> "Выбери намёк"})},text={
        Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(if(session.replay) "Подсказки бесплатны. Они учитываются в оценке за повтор." else "Баланс: $coins монет. Списание — только после подтверждения.")
            Text("Нажми клетку на поле или выбери из списка ниже.")
            CrosswordBoard(level,session,false,onCell={cell ->
                if(kind==Hint.LETTER) {if(cell.key !in visible) {target=cell.key;selected=cell}}
                else {
                    selected=cell
                    val slots=level.slots.filter {it.id !in session.solved && cell in it.cells}
                    target=slots.singleOrNull()?.id
                }
            },selectedCell=selected?.key)
            if(selected!=null && kind!=Hint.LETTER) TextButton(onClick={selected=null;target=null}) {Text("Показать все направления")}
            candidates.forEach { (id,label) ->
                Surface(onClick={target=id},color=if(target==id) PaleGreen else MaterialTheme.colorScheme.surface,
                    shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics { selected=target==id }) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        RadioButton(selected=target==id,onClick=null)
                        Text(label,Modifier.weight(1f))
                    }
                }
            }
            if(target!=null && coins<price) Text("Нужно ещё ${price-coins} монет. Можно продолжить без подсказки.")
            if(onVideo!=null) {OutlinedButton(onClick=onVideo,enabled=videoReason==null,modifier=Modifier.fillMaxWidth()) {Text("+${economy.rewarded} монет за видео · Реклама")};if(videoReason!=null) Text(videoReason)}
            if(kind==Hint.CLUE) Text("Повторное чтение уже оплаченного намёка бесплатно.")
        }
    },confirmButton={TextButton(onClick={target?.let(onConfirm)},enabled=target!=null && coins>=price) {Text(if(price==0L) "Открыть бесплатно" else "Открыть за $price")}},dismissButton={TextButton(onClick=onDismiss) {Text("Отмена")}})
}
