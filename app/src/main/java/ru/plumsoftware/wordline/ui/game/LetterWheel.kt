package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
                onShuffle: () -> Unit, onDragging: (Boolean) -> Unit,
                modifier: Modifier = Modifier, onSelectionChange: (String) -> Unit = {}, guide: List<Int> = emptyList()) {
    var selected by remember(session.id,session.order,inputEpoch) { mutableStateOf(emptyList<Int>()) }
    var dragging by remember(session.id,session.order,inputEpoch) { mutableStateOf(false) }
    var pointerPosition by remember(session.id,session.order,inputEpoch) { mutableStateOf<Offset?>(null) }
    val currentSubmit by rememberUpdatedState(onSubmit)
    val currentDragging by rememberUpdatedState(onDragging)
    val currentEnabled by rememberUpdatedState(enabled)
    val density=LocalDensity.current
    val accessible=rememberTouchExplorationEnabled()
    val bigLayout=density.fontScale>1.15f
    val order=session.order
    fun tap(token: Int) {
        if(!enabled || dragging) return
        selected=if(selected.lastOrNull()==token) selected.dropLast(1) else if(token in selected) selected else selected+token
    }
    fun clear() { selected=emptyList();pointerPosition=null }
    val text=selected.joinToString("") { level.letters[it].toString() }
    LaunchedEffect(text) { onSelectionChange(text) }
    DisposableEffect(session.id,inputEpoch,session.order) {
        onDispose { currentDragging(false) }
    }
    BoxWithConstraints(modifier, contentAlignment=Alignment.Center) {
        val gridLayout=bigLayout || maxWidth<180.dp || maxHeight<180.dp
        val tapOnly=tapInput || accessible || gridLayout
        if(gridLayout) {
            val controlSpace=(with(density) { 48.sp.toDp() }+48.dp).coerceAtLeast(150.dp)
            Column(Modifier.fillMaxSize().padding(bottom=controlSpace).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally) {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                order.forEach { token ->
                    Token(level.letters[token],token,level.letters.size,token in selected,enabled,
                        Modifier.padding(4.dp).size(with(density) { (if(largeLetters) 51.sp else 44.sp).toDp() }+20.dp),largeLetters,onClick={tap(token)})
                }
            }
            OutlinedButton(onClick=onShuffle,enabled=enabled && selected.isEmpty()) { Text("Перемешать") }
            }
        } else BoxWithConstraints(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            val wheelSize=minOf(maxWidth,maxHeight,if(level.letters.size>=6) 380.dp else 350.dp)
            val sizePx=with(density) { wheelSize.toPx() }
            val scale=wheelSize.value/230f
            val tokenSize=(58*scale).dp.coerceAtLeast(48.dp)
            val tokenPx=with(density) { tokenSize.toPx() }
            val radius=sizePx/3f
            val centers=remember(order,sizePx) { order.indices.map { i ->
                val angle=2*PI*i/order.size-PI/2
                Point(sizePx/2+radius*cos(angle).toFloat(),sizePx/2+radius*sin(angle).toFloat())
            } }
            val hitRadius=minOf(with(density) { (29*scale).dp.coerceAtLeast(24.dp).toPx() },sizePx * sin(PI / order.size).toFloat()/3f)
            Box(Modifier.size(wheelSize).shadow(3.dp,CircleShape).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFFCED),Color(0xFFFFF4D9))))
                .border(1.5.dp,Color(0xFFFFFCEB),CircleShape)
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
                                    pointerPosition=change.position
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
                            pointerPosition=null
                            dragging=false;currentDragging(false)
                        }
                    } finally { selected=emptyList();pointerPosition=null;dragging=false;currentDragging(false) }
                }) {
                Canvas(Modifier.matchParentSize()) {
                    val tokens=if(selected.isEmpty()) guide else selected
                    val connection=Path()
                    var hasStart=false
                    tokens.forEach { token ->
                        val index=order.indexOf(token)
                        if(index>=0) {
                            val p=centers[index]
                            if(!hasStart) {connection.moveTo(p.x,p.y);hasStart=true}
                            else connection.lineTo(p.x,p.y)
                        }
                    }
                    // The unfinished segment follows every pointer event, before another letter is hit.
                    if(dragging && hasStart) pointerPosition?.let { connection.lineTo(it.x,it.y) }
                    if(hasStart) {
                        drawPath(connection,Color(0xFF009B50),style=Stroke(
                            width=(6*scale).dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round,
                            pathEffect=PathEffect.dashPathEffect(floatArrayOf((2*scale).dp.toPx(),(10*scale).dp.toPx())),
                        ))
                    }
                }
                order.forEachIndexed { index,token ->
                    val p=centers[index]
                    Token(level.letters[token],token,level.letters.size,token in selected || selected.isEmpty() && token in guide,enabled && !dragging,
                        Modifier.offset { IntOffset((p.x-tokenPx/2).roundToInt(),(p.y-tokenPx/2).roundToInt()) }.size(tokenSize),largeLetters,scale,onClick={tap(token)})
                }
                Box(Modifier.align(Alignment.Center)) {
                    PlaySurface(Modifier.size((53*scale).dp), (27*scale).dp,
                        enabled=enabled && !dragging && selected.isEmpty(),onClick=onShuffle) {
                        Icon(androidx.compose.ui.res.painterResource(R.drawable.game_ic_shuffle),"Перемешать бесплатно",
                            Modifier.size((32*scale).dp),tint=Color(0xFF547758))
                    }
                }
            }
        }
        if(selected.isNotEmpty() && !dragging) {
            // Tap input has explicit submission; it never fires a word after each tap.
            if(gridLayout) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                PlaySurface(Modifier.fillMaxWidth().heightIn(min=48.dp),onClick={selected=selected.dropLast(1)},enabled=enabled) {
                    Text("Стереть",Modifier.padding(8.dp).semantics {customActions=listOf(CustomAccessibilityAction("Очистить") {clear();true})},color=PlayInk)
                }
                PlaySurface(Modifier.fillMaxWidth().heightIn(min=48.dp),onClick={if(text.length>=2) currentSubmit(text);clear()},enabled=enabled && text.length>=2) {
                    Text("Проверить",Modifier.padding(8.dp),color=PlayInk,fontWeight=FontWeight.Bold)
                }
            } else {
            Row(Modifier.align(Alignment.BottomCenter).offset(y=12.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                PlaySurface(Modifier.heightIn(min=48.dp),onClick={selected=selected.dropLast(1)},enabled=enabled) {
                    Text("Стереть",Modifier.padding(8.dp).semantics {customActions=listOf(CustomAccessibilityAction("Очистить") {clear();true})},color=PlayInk)
                }
                PlaySurface(Modifier.heightIn(min=48.dp),onClick={if(text.length>=2) currentSubmit(text);clear()},enabled=enabled && text.length>=2) {
                    Text("Проверить",Modifier.padding(8.dp),color=PlayInk,fontWeight=FontWeight.Bold)
                }
            }
            }
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
                  modifier: Modifier, large: Boolean, scale: Float = 1f, onClick: () -> Unit) {
    Surface(onClick=onClick,enabled=enabled,shape=CircleShape,color=Color.Transparent,
        contentColor=PlayInk,modifier=modifier.then(if(selected) Modifier.shadow(2.dp,CircleShape).background(Brush.verticalGradient(PlayMint),CircleShape) else Modifier).semantics {
            contentDescription="Буква $letter, позиция ${token+1} из $total"
            this.selected=selected
        }) {
        Box(contentAlignment=Alignment.Center) { Text(letter.toString(),fontSize=((if(large) 51 else 44)*scale).sp,fontWeight=FontWeight.Black) }
    }
}
