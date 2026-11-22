package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import java.text.NumberFormat
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn

internal val PlayInk = Color(0xFF071738)
internal val PlayPaper = listOf(Color(0xFFFFFBEA), Color(0xFFFFEEC9))
internal val PlayMint = listOf(Color(0xFFB9FFD1), Color(0xFF86EDB1))

/** Native surfaces: artwork never contains a label, a price, or a touch target. */
@Composable
internal fun PlaySurface(
    modifier: Modifier = Modifier, radius: Dp = 16.dp,
    colors: List<Color> = PlayPaper, enabled: Boolean = true,
    description: String? = null, onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(modifier.shadow(3.dp, shape).clip(shape).background(Brush.verticalGradient(colors))
        .border(1.dp, Color(0xFFFFFCEB), shape)
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
        .then(if (description != null) Modifier.semantics(mergeDescendants = true) { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center, content = content)
}

/** S04/S05 use the original 327 × 686 proportions, with live game data. */
@Composable
internal fun PlayScene(
    state: AppState, level: Level, session: Session, enabled: Boolean, inputEpoch: Int,
    online: Boolean, onBack: () -> Unit, onBonus: () -> Unit,
    onSubmit: (String) -> Unit, onShuffle: () -> Unit,
    onDragging: (Boolean) -> Unit, onHint: (Hint) -> Unit, adVisible: Boolean = false,
) {
    var selection by remember(session.id, inputEpoch, session.order) { mutableStateOf("") }
    var dragging by remember { mutableStateOf(false) }
    var wordFeedback by remember(session.id) { mutableStateOf("") }
    var previousBonus by remember(session.id) {mutableIntStateOf(session.bonus.size)}
    var bonusEvent by remember(session.id) {mutableIntStateOf(0)}
    var bonusToast by remember(session.id) {mutableStateOf("")}
    val allBonus=level.bonusPool.isNotEmpty() && session.bonus.size==level.bonusPool.size
    val almostDone=session.solved.size==level.slots.size-1 && session.bonus.size<level.bonusPool.size
    val bonusPulse=celebrationPulse(bonusToast.isNotEmpty() || almostDone || allBonus)
    LaunchedEffect(session.bonus) {
        if(session.bonus.size>previousBonus) {
            previousBonus=session.bonus.size;bonusEvent++
            bonusToast=if(allBonus) "Все дополнительные слова найдены!" else "Дополнительное слово!"
            delay(2400);bonusToast=""
        }
    }
    LaunchedEffect(state.feedback) {
        wordFeedback = state.feedback
        if (wordFeedback.isNotEmpty()) { delay(2200); wordFeedback = "" }
    }
    val topClearance=gameHeaderClearance()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = (maxWidth.value / 327f).coerceAtMost(1.8f)
        val fontScale = LocalDensity.current.fontScale
        val daily = session.mode == Mode.DAILY
        var offlineLayout by remember { mutableStateOf(!online) }
        LaunchedEffect(online,dragging) { if(!dragging) offlineLayout=!online }
        val compact = offlineLayout || adVisible
        val toolsHeight = ((if(compact) 59 else if(daily) 74 else 86) * scale).dp
        val toolsBottom = ((if(compact) 62 else if(daily) 12 else 16) * scale).dp
        val wheelBottom = toolsBottom + toolsHeight + ((if(compact) 1 else if(daily) 12 else 16) * scale).dp
        val boardStart = topClearance+((if(offlineLayout) 126 else if(daily) 141 else 94) * scale).dp
        val wheelRoom = maxHeight - boardStart - (30*scale).dp - wheelBottom - (level.rows * 28).dp
        val wide = maxWidth > maxHeight || maxWidth >= 650.dp
        val crowded=level.letters.size>=6
        val wheel = minOf(maxWidth * (if(compact) (if(crowded) .66f else .59f) else if(daily) (if(crowded) .77f else .66f) else (if(crowded) .79f else .70f)),
            maxHeight * (if(compact) (if(crowded) .34f else .295f) else if(crowded) .40f else if(daily) .345f else .335f),
            wheelRoom.coerceAtLeast(if(crowded) 210.dp else 180.dp),if(crowded) 380.dp else 350.dp)
        val title = when {
            daily -> LocalDate.parse(requireNotNull(session.dateKey)).format(DateTimeFormatter.ofPattern("d MMMM",Locale.forLanguageTag("ru")))
            session.mode == Mode.TUTORIAL -> "Обучение"
            else -> "Уровень ${level.id}"
        }
        if (!state.settings.highContrast) {
            val selectedBackground = state.settings.background.coerceIn(0, 9)
            val defaultArtwork = if(selectedBackground == 0) {
                if(daily) R.drawable.game_lake_evening else R.drawable.play_lake_reference
            } else backgroundResources[selectedBackground]
            val artwork = selectedSceneArtwork(state, defaultArtwork)
            AnimatedScenery(artwork,Modifier.fillMaxSize())
        } else Box(Modifier.fillMaxSize().background(Color.White))

        if (fontScale > 1.15f) {
            // Larger text leaves fixed-coordinate composition before labels can collide.
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top=topClearance).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolButton(R.drawable.game_ic_back, "Пауза", onClick = onBack)
                    Text(title, Modifier.weight(1f), color = PlayInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Balance(state.snapshot?.profile?.coins ?: 0)
                if(bonusToast.isNotEmpty()) ReferenceText(bonusToast,Modifier.fillMaxWidth(),22f)
                if(!online) OfflineNotice(Modifier.fillMaxWidth(),scale)
                ZoomableCrossword(level, session, selection, state.settings.largeLetters,
                    Modifier.fillMaxWidth().height((level.rows * (26 * fontScale + 16)).dp.coerceAtMost(300.dp)),
                    minimumTile = (26 * fontScale + 16).dp)
                PlaySurface(Modifier.fillMaxWidth().heightIn(min = 48.dp).graphicsLayer {scaleX=bonusPulse.value;scaleY=bonusPulse.value}, onClick = onBonus, enabled = enabled) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(R.drawable.item_leaf_token),null,Modifier.size(28.dp))
                        Text("Дополнительные слова: ${session.bonus.size} из ${level.bonusPool.size}",
                            Modifier.weight(1f),color=PlayInk,textAlign=TextAlign.Center)
                    }
                }
                LetterWheel(level, session, true, state.settings.largeLetters, enabled, inputEpoch,
                    onSubmit, onShuffle, onDragging,
                    Modifier.fillMaxWidth().height((((level.letters.size + 1) / 2) * 110 + 120).dp),
                    onSelectionChange = { selection = it })
                if (wordFeedback.isNotEmpty()) Text(wordFeedback, color = PlayInk,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                PlayHints(session, state.economy, enabled, 1f, onHint, stacked = true)
                if(compact) OfflineVideoFooter(Modifier.fillMaxWidth().height(48.dp),1f)
            }
            return@BoxWithConstraints
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = (11 * scale).dp).padding(top = topClearance+(8 * scale).dp).height((48 * scale).dp)) {
            Box(Modifier.align(Alignment.CenterStart).size((48 * scale).dp).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) {
                PlaySurface(Modifier.size((42 * scale).dp, (37 * scale).dp), (13 * scale).dp) {
                    Icon(painterResource(R.drawable.game_ic_back), "Пауза", Modifier.size((27 * scale).dp), tint = PlayInk)
                }
            }
            val largeBalance = (state.snapshot?.profile?.coins ?: 0) >= 10_000
            PlaySurface(Modifier.align(Alignment.Center).offset(x = ((if (largeBalance) -24 else -3) * scale).dp)
                .widthIn(min = ((if (largeBalance) 108 else 121) * scale).dp).heightIn(min = (32 * scale).dp), (22 * scale).dp) {
                Text(title, Modifier.padding(horizontal = (12 * scale).dp, vertical = (5 * scale).dp),
                    color = PlayInk, fontWeight = FontWeight.Black, fontSize = ((if (largeBalance) 14 else 16) * scale).sp, textAlign = TextAlign.Center)
            }
            PlaySurface(Modifier.align(Alignment.CenterEnd).heightIn(min = (33 * scale).dp), (20 * scale).dp,
                description = "${state.snapshot?.profile?.coins ?: 0} монет") {
                Row(Modifier.padding(horizontal = (6 * scale).dp, vertical = (3 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.item_coin), null, Modifier.size((27 * scale).dp))
                    Text(NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(state.snapshot?.profile?.coins ?: 0),
                        color = PlayInk, fontSize = ((if (largeBalance) 14 else 17) * scale).sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if(!online) OfflineNotice(Modifier.align(Alignment.TopCenter).padding(top=topClearance+(55*scale).dp).width((215*scale).dp).height((57*scale).dp),scale)
        if(compact) OfflineVideoFooter(Modifier.align(Alignment.BottomCenter).padding(bottom=(11*scale).dp).width((264*scale).dp).height((45*scale).dp),scale)
        if(daily && online) PlaySurface(Modifier.align(Alignment.TopCenter).padding(top=topClearance+(66*scale).dp).width((174*scale).dp).height((37*scale).dp),
            24.dp,description="Найдено ${session.solved.size} из ${level.slots.size} слов") {
            Row(Modifier.fillMaxSize().padding(horizontal=(15*scale).dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                ReferenceProgress(session.solved.size.toFloat()/level.slots.size,Modifier.weight(1f).height((15*scale).dp))
                Text("${session.solved.size}/${level.slots.size}",color=PlayInk,fontWeight=FontWeight.Black,fontSize=(17*scale).sp)
            }
        }
        val boardTop = boardStart
        val boardHeight = (maxHeight - boardTop - wheelBottom - wheel - ((if(selection.isNotEmpty()) 48 else 30) * scale).dp).coerceAtLeast(60.dp)
        if (wide) {
            // The two-column fallback keeps both input and tools usable in short windows.
            Row(Modifier.fillMaxSize().padding(top = 80.dp+topClearance, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                ZoomableCrossword(level, session, selection, state.settings.largeLetters, Modifier.weight(1f).fillMaxHeight(),
                    minimumTile = 28.dp)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LetterWheel(level, session, state.settings.tapInput, state.settings.largeLetters, enabled, inputEpoch,
                        onSubmit, onShuffle, onDragging, Modifier.weight(1f), onSelectionChange = { selection = it })
                    PlaySurface(Modifier.fillMaxWidth().heightIn(min=48.dp),onClick=onBonus,enabled=enabled) {
                        Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Image(painterResource(R.drawable.item_leaf_token),null,Modifier.size(26.dp))
                            ReferenceText("${session.bonus.size}/${level.bonusPool.size}",size=18f)
                        }
                    }
                    PlayHints(session, state.economy, enabled, scale.coerceAtMost(1f), onHint)
                }
            }
        } else {
            ZoomableCrossword(level, session, selection, state.settings.largeLetters,
                Modifier.align(Alignment.TopCenter).padding(top = boardTop).fillMaxWidth().height(boardHeight))
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = wheelBottom).size(wheel)) {
                LetterWheel(level, session, state.settings.tapInput, state.settings.largeLetters, enabled, inputEpoch,
                    onSubmit, onShuffle, { dragging = it; onDragging(it) }, Modifier.fillMaxSize(), onSelectionChange = { selection = it })
            }
            val poolSize = level.bonusPool.size
            val bonusText = if(allBonus) "${session.bonus.size}/$poolSize" else if (poolSize >= 5) "${session.bonus.size % 5}/5" else "${session.bonus.size}/$poolSize"
            PlaySurface(Modifier.align(Alignment.BottomStart).padding(start = (12 * scale).dp, bottom = wheelBottom + wheel - (3 * scale).dp)
                .height((35 * scale).dp).widthIn(min = (74 * scale).dp).graphicsLayer {scaleX=bonusPulse.value;scaleY=bonusPulse.value}
                .then(if(allBonus) Modifier.border(2.dp,Color(0xFFFFD43B),RoundedCornerShape(14.dp)) else Modifier), (14 * scale).dp,
                enabled = enabled && !dragging, description = "Дополнительные слова: $bonusText", onClick = onBonus) {
                Row(Modifier.padding(horizontal = (6 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.item_leaf_token), null, Modifier.size((28 * scale).dp))
                    Text(bonusText, color = PlayInk, fontSize = (17 * scale).sp, fontWeight = FontWeight.Black)
                }
            }
            if (selection.isNotEmpty()) {
                PlaySurface(Modifier.align(Alignment.BottomCenter).padding(bottom = wheelBottom + wheel + ((if(compact) 1 else 5) * scale).dp)
                    .widthIn(min = (122 * scale).dp).height(((if(compact) 34 else 37) * scale).dp), (24 * scale).dp,
                    colors = listOf(Color(0xFF20C66D), Color(0xFF008B43))) {
                    Text(selection, Modifier.padding(horizontal = 16.dp), color = Color.White,
                        fontSize = (26 * scale).sp, fontWeight = FontWeight.Black)
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = ((if(compact) 21 else 9) * scale).dp).padding(bottom = toolsBottom)) {
                PlayHints(session, state.economy, enabled && !dragging, if(daily && !compact) scale*.86f else scale, onHint, compact=compact)
            }
        }
        // Feedback remains accessible without adding permanent copy over the scenery.
        val feedback = when {
            bonusToast.isNotEmpty() -> ""
            session.replay -> "Повтор · без наград"
            wordFeedback.isNotBlank() -> wordFeedback
            else -> ""
        }
        if (feedback.isNotEmpty() && selection.isEmpty()) {
            PlaySurface(Modifier.align(Alignment.TopCenter).padding(top = topClearance+((if(!online) 95 else if(daily) 109 else 58) * scale).dp), 12.dp) {
                Text(feedback, Modifier.padding(horizontal = 12.dp, vertical = 3.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    color = PlayInk, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        CelebrationConfetti(Modifier.fillMaxSize(),seed=bonusEvent,active=bonusEvent>0)
        AnimatedVisibility(bonusToast.isNotEmpty(),Modifier.align(Alignment.TopCenter).padding(top=topClearance+((if(!online) 96 else 58)*scale).dp),
            enter=if(LocalGameMotion.current) fadeIn()+scaleIn(initialScale=.7f) else EnterTransition.None,
            exit=if(LocalGameMotion.current) fadeOut() else ExitTransition.None) {
            PlaySurface(Modifier.padding(horizontal=12.dp),22.dp,colors=PlayMint) {
                Column(Modifier.padding(horizontal=16.dp,vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    ReferenceText(bonusToast,size=20*scale)
                    if(allBonus && !session.replay && session.mode!=Mode.TUTORIAL) ReferenceText("Бонус добавлен к награде",size=14*scale)
                }
            }
        }
    }
}

@Composable
internal fun ReferenceBoard(level: Level, session: Session, selection: String, large: Boolean, modifier: Modifier,
                            minimumTile: Dp = 0.dp) {
    val visible = remember(level, session.solved, session.revealed) { visibleCells(level, session) }
    val history=remember(session.id) {BoardRevealHistory(visible)}
    val revealRanks=remember(session.id,visible) {
        val fresh=visible-history.cells
        history.cells=visible
        level.slots.flatMap {it.cells}.map {it.key}.distinct().filter {it in fresh}.withIndex().associate {it.value to it.index}
    }
    val cells = remember(level) { level.cells }
    val fontScale = LocalDensity.current.fontScale
    val highlighted = remember(level, selection) { level.slots.filter { it.word == selection }.flatMap { it.cells }.toSet() }
    val description = level.slots.joinToString(". ") { slot ->
        slot.cells.mapIndexed { i, cell -> if (cell.key in visible) slot.word[i].toString() else "пропуск" }.joinToString(" ")
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        val tile = minOf((maxWidth - 24.dp) / level.columns, maxHeight / level.rows, maxWidth * (44f / 327f)).coerceAtLeast(minimumTile)
        Box(if (minimumTile > 0.dp) Modifier.horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState()) else Modifier) {
        Column(Modifier.clearAndSetSemantics { contentDescription = description }) {
            repeat(level.rows) { row ->
                Row {
                    repeat(level.columns) { column ->
                        val cell = Cell(row, column)
                        Box(Modifier.size(tile), contentAlignment = Alignment.Center) {
                            cells[cell]?.let { letter ->
                                PlaySurface(Modifier.fillMaxSize().padding(1.dp), tile * .23f,
                                    colors = if (cell in highlighted) PlayMint else PlayPaper) {
                                    if (cell.key in visible || cell in highlighted) AnimatedCrosswordLetter(letter,revealRanks[cell.key],Modifier.fillMaxSize(),
                                        if (fontScale > 1.15f) (if (large) 30f else 26f) else tile.value * if (large) .86f else .75f)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private class BoardRevealHistory(var cells: Set<String>)

@Composable
private fun PlayHints(session: Session, economy: Economy, enabled: Boolean, scale: Float, select: (Hint) -> Unit, stacked: Boolean = false, compact: Boolean = false) {
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Hint.LETTER to "Буква", Hint.WORD to "Слово", Hint.CLUE to "Подсказка").forEach { (kind, label) ->
                val price = when (kind) { Hint.LETTER -> economy.letter; Hint.WORD -> economy.word; Hint.CLUE -> economy.clue }
                val free = session.replay || session.mode == Mode.TUTORIAL && kind == Hint.LETTER && !session.tutorialLetterUsed
                PlaySurface(Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    enabled = enabled && (session.mode != Mode.TUTORIAL || kind == Hint.LETTER),
                    description = "$label, ${if (free) "бесплатно" else "$price монет"}", onClick = { select(kind) }) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(label, color = PlayInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(if (free) "Бесплатно" else "$price монет", color = PlayInk, fontSize = 16.sp)
                    }
                }
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
        listOf(Triple(Hint.LETTER, "Буква", R.drawable.play_ic_letter), Triple(Hint.WORD, "Слово", R.drawable.play_ic_word),
            Triple(Hint.CLUE, "Подсказка", R.drawable.play_ic_clue)).forEach { (kind, label, icon) ->
            val price = when (kind) { Hint.LETTER -> economy.letter; Hint.WORD -> economy.word; Hint.CLUE -> economy.clue }
            val free = session.replay || session.mode == Mode.TUTORIAL && kind == Hint.LETTER && !session.tutorialLetterUsed
            val allowed = enabled && (session.mode != Mode.TUTORIAL || kind == Hint.LETTER)
            PlaySurface(Modifier.weight(1f).heightIn(min = ((if(compact) 59 else 86) * scale).dp), (21 * scale).dp,
                enabled = allowed, description = "$label, ${if (free) "бесплатно" else "$price монет"}", onClick = { select(kind) }) {
                Column(Modifier.padding(vertical = (6 * scale).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(icon), null, Modifier.size((26 * scale).dp), tint = if (allowed) PlayInk else Color(0xFF788477))
                    Text(label, color = PlayInk, fontWeight = FontWeight.Black, fontSize = (15 * scale).sp,
                        lineHeight = (18 * scale).sp, textAlign = TextAlign.Center)
                    if(!compact) Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!free) Image(painterResource(R.drawable.item_coin), null, Modifier.size((23 * scale).dp))
                        Text(if (free) "Бесплатно" else "$price", color = PlayInk, fontWeight = FontWeight.Black,
                            fontSize = ((if (free) 12 else 18) * scale).sp)
                    }
                }
            }
        }
    }
}
