package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import kotlin.math.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterWheel(level: Level, session: Session, tapInput: Boolean, largeLetters: Boolean,
                enabled: Boolean, inputEpoch: Int, onSubmit: (String) -> Unit,
                onShuffle: () -> Unit, onDragging: (Boolean) -> Unit) {
    var selected by remember(session.id,session.order,inputEpoch) { mutableStateOf(emptyList<Int>()) }
    var dragging by remember(session.id,session.order,inputEpoch) { mutableStateOf(false) }
    val currentSubmit by rememberUpdatedState(onSubmit)
    val currentDragging by rememberUpdatedState(onDragging)
    val currentEnabled by rememberUpdatedState(enabled)
    val density=LocalDensity.current
    val accessible=rememberTouchExplorationEnabled()
    val bigLayout=density.fontScale>1.5f
    val tapOnly=tapInput || accessible || bigLayout
    val order=session.order
    fun tap(token: Int) {
        if(!enabled || dragging) return
        selected=if(selected.lastOrNull()==token) selected.dropLast(1) else if(token in selected) selected else selected+token
    }
    fun clear() { selected=emptyList() }
    val text=selected.joinToString("") { level.letters[it].toString() }
    DisposableEffect(session.id,inputEpoch,session.order) {
        onDispose { currentDragging(false) }
    }
    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface) {
            Text(text.ifEmpty { if(tapOnly) "Нажимай на буквы" else "Соединяй буквы" },
                Modifier.padding(horizontal=20.dp,vertical=8.dp).semantics { liveRegion=LiveRegionMode.Polite },
                fontSize=if(text.isEmpty()) 16.sp else 24.sp,fontWeight=FontWeight.Bold)
        }
        if(bigLayout) {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                order.forEach { token ->
                    Token(level.letters[token],token,level.letters.size,token in selected,enabled,
                        Modifier.padding(4.dp).size(with(density) { (if(largeLetters) 40.sp else 34.sp).toDp() }+24.dp),largeLetters,onClick={tap(token)})
                }
            }
            OutlinedButton(onClick=onShuffle,enabled=enabled && selected.isEmpty()) { Text("Перемешать") }
        } else BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
            val wheelSize=maxWidth.coerceAtMost(288.dp)
            val sizePx=with(density) { wheelSize.toPx() }
            val tokenSize=56.dp
            val tokenPx=with(density) { tokenSize.toPx() }
            val radius=(sizePx-tokenPx)/2f-4f
            val centers=remember(order,sizePx) { order.indices.map { i ->
                val angle=2*PI*i/order.size-PI/2
                Point(sizePx/2+radius*cos(angle).toFloat(),sizePx/2+radius*sin(angle).toFloat())
            } }
            val hitRadius=with(density) { 28.dp.toPx() }
            Box(Modifier.size(wheelSize).clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha=.96f))
                .border(1.dp,Color(0xFF8A9E95),CircleShape)
                .pointerInput(session.id,order,sizePx,tapOnly,inputEpoch,enabled) {
                    if(tapOnly || !enabled) return@pointerInput
                    try {
                        awaitEachGesture {
                            val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
                            val start=centers.indices.minByOrNull { i ->
                                val p=centers[i];(p.x-down.position.x).pow(2)+(p.y-down.position.y).pow(2)
                            } ?: return@awaitEachGesture
                            val center=centers[start]
                            if(hypot(center.x-down.position.x,center.y-down.position.y)>hitRadius) return@awaitEachGesture
                            val pointer=down.id
                            var previous=Point(down.position.x,down.position.y)
                            var path=listOf(order[start])
                            var active=false
                            var cancelled=false
                            while(true) {
                                val event=awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.filter { it.id!=pointer }.forEach { it.consume() }
                                val change=event.changes.firstOrNull { it.id==pointer } ?: break
                                if(change.isConsumed || !currentEnabled) {cancelled=true;break}
                                if(!active && (change.position-down.position).getDistance()>viewConfiguration.touchSlop) {
                                    active=true;dragging=true;currentDragging(true)
                                    selected=path
                                }
                                if(active) {
                                    val position=Point(change.position.x,change.position.y)
                                    crossedTokens(previous,position,centers,hitRadius).forEach { index -> path=appendToken(path,order[index]) }
                                    selected=path
                                    previous=position
                                    change.consume()
                                }
                                if(!change.pressed) {
                                    if(active && !cancelled && currentEnabled && path.size>=2)
                                        currentSubmit(path.joinToString("") { level.letters[it].toString() })
                                    break
                                }
                            }
                            if(active) selected=emptyList()
                            dragging=false;currentDragging(false)
                        }
                    } finally { selected=emptyList();dragging=false;currentDragging(false) }
                }) {
                Canvas(Modifier.matchParentSize()) {
                    selected.zipWithNext().forEach { (a,b) ->
                        val p=centers[order.indexOf(a)]; val q=centers[order.indexOf(b)]
                        drawLine(Green,Offset(p.x,p.y),Offset(q.x,q.y),strokeWidth=10.dp.toPx())
                    }
                }
                order.forEachIndexed { index,token ->
                    val p=centers[index]
                    Token(level.letters[token],token,level.letters.size,token in selected,enabled && !dragging,
                        Modifier.offset { IntOffset((p.x-tokenPx/2).roundToInt(),(p.y-tokenPx/2).roundToInt()) }.size(tokenSize),largeLetters,onClick={tap(token)})
                }
                Box(Modifier.align(Alignment.Center)) {
                    ToolButton(R.drawable.game_ic_shuffle,"Перемешать бесплатно",enabled=enabled && !dragging && selected.isEmpty(),onClick=onShuffle)
                }
            }
        }
        if(selected.isNotEmpty() && !dragging) {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={selected=selected.dropLast(1)},enabled=enabled) { Text("Стереть") }
                Button(onClick={if(text.length>=2) currentSubmit(text);clear()},enabled=enabled && text.length>=2) { Text("Проверить") }
            }
            TextButton(onClick=::clear,enabled=enabled) { Text("Очистить") }
        }
    }
}

@Composable
private fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    var enabled by remember(manager) { mutableStateOf(manager.isTouchExplorationEnabled) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { value ->
            enabled = value
        }
        manager.addTouchExplorationStateChangeListener(listener)
        enabled = manager.isTouchExplorationEnabled
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}
@Composable
private fun Token(letter: Char, token: Int, total: Int, selected: Boolean, enabled: Boolean,
                  modifier: Modifier, large: Boolean, onClick: () -> Unit) {
    Surface(onClick=onClick,enabled=enabled,shape=CircleShape,color=if(selected) Green else Cream,
        contentColor=if(selected) Color.White else Ink,modifier=modifier.semantics {
            contentDescription="Буква $letter, позиция ${token+1} из $total"
            this.selected=selected
        },border=androidx.compose.foundation.BorderStroke(if(selected) 3.dp else 1.dp,if(selected) Ink else Color(0xFF7D9186))) {
        Box(contentAlignment=Alignment.Center) { Text(letter.toString(),fontSize=if(large) 36.sp else 30.sp,fontWeight=FontWeight.Bold) }
    }
}
