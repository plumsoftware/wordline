package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.locationNames

@Composable
fun MapScreen(state: AppState, back: () -> Unit, play: (Int, Boolean) -> Unit) {
    val campaign = requireNotNull(state.campaign)
    val snapshot = requireNotNull(state.snapshot)
    val current = campaign.levels.firstOrNull { it.id !in snapshot.completions }?.id
    val total = campaign.levels.size
    var first by rememberSaveable { mutableIntStateOf(((current ?: total) - 3).coerceIn(1, (total - 5).coerceAtLeast(1))) }
    var replay by rememberSaveable { mutableStateOf<Int?>(null) }
    var locked by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(current) {
        if (current != null && current !in first..first + 5) {
            first = (current - 3).coerceIn(1, (total - 5).coerceAtLeast(1))
        }
    }
    val representativeLevel=if(current!=null && current in first..first+5) current else first+2
    val location = ((representativeLevel-1) / 100).coerceIn(0, 9)
    val artwork=mapArtworks[location]
    val centers=artwork.stops.map {(x,y)->x*327f to y*627f}
    val topClearance=gameHeaderClearance()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = (maxWidth.value / 327f).coerceAtMost(1.8f)
        fun frame(x: Float, y: Float, w: Float, h: Float) = Modifier
            .offset(maxWidth * (x / 327f), maxHeight * (y / 627f))
            .size(maxWidth * (w / 327f), maxHeight * (h / 627f))
        if (!state.settings.highContrast) {
            Crossfade(targetState=location,modifier=Modifier.fillMaxSize(),
                animationSpec=tween(if(state.settings.reduceMotion) 0 else 650),label="Пейзаж локации карты") { index ->
                AnimatedScenery(mapArtworks[index].resource,Modifier.fillMaxSize())
            }
        } else Box(Modifier.fillMaxSize().background(Cream))

        // Normal mode uses the illustrated trail. A simple road remains only in high contrast mode.
        if(state.settings.highContrast) Canvas(Modifier.fillMaxSize()) {
            val sx=size.width/327f;val sy=size.height/627f
            val anchors=listOf(145f to 590f)+centers+listOf(271f to 96f)
            val road=Path().apply {
                moveTo(anchors.first().first*sx,anchors.first().second*sy)
                anchors.zipWithNext().forEach {(from,to) ->
                    val middle=(from.second+to.second)/2f*sy
                    cubicTo(from.first*sx,middle,to.first*sx,middle,to.first*sx,to.second*sy)
                }
            }
            drawPath(road,Color(0x55524827),style=Stroke(18f*sx,cap=StrokeCap.Round))
            drawPath(road,Color(0xFFFFF4CF),style=Stroke(14f*sx,cap=StrokeCap.Round))
            drawPath(road,Color(0xFFFFFBE9),style=Stroke(7f*sx,cap=StrokeCap.Round))
        }

        ReferenceBack(frame(12f,14f,42f,38f).offset(y=topClearance),back)
        LocationRibbon(locationNames[location],frame(58f,5f,264f,76f).offset(y=topClearance))
        // Every location has its own measured anchors on the painted trail, ascending from the bottom.
        centers.forEachIndexed { index, (x, y) ->
            val id = first + index
            if (id <= total) {
                val done = snapshot.completions[id]
                val active = id == current
                val future = done == null && !active
                val nodeSize = (if (active) 58 else 51) * scale
                Box(Modifier.offset(maxWidth * (x / 327f) - (46 * scale).dp, maxHeight * (y / 627f) - (46 * scale).dp)
                    .size((92 * scale).dp).semantics { isTraversalGroup = true; traversalIndex = index.toFloat() },
                    contentAlignment = Alignment.Center) {
                    if(active) CurrentLevelGlow(Modifier.fillMaxSize())
                    Box(Modifier.size(nodeSize.dp).shadow((3 * scale).dp, CircleShape).clip(CircleShape)
                        .background(Brush.verticalGradient(when { active -> listOf(Color(0xFF80ED7B), Color(0xFF00964A)); future -> listOf(Color(0xFFF3F5F1), Color(0xFFD9DFDF)); else -> PlayPaper }))
                        .border((if (active) 2.5f else 1f).dp, if (active) Color(0xFFFFFFB7) else Color(0xFFFFFCEB), CircleShape)
                        .clickable(enabled = !state.busy, role = Role.Button) { when { done != null -> replay = id; active -> play(id, false); else -> locked = id } }
                        .semantics(mergeDescendants = true) {
                            contentDescription = when { active -> "Уровень $id. Продолжить"; done != null -> "Уровень $id. Пройден, ${done.bestStars} звезды. Повтор без наград"; else -> "Уровень $id. Закрыт. Пройди уровень ${id - 1}" }
                        }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (future) Icon(painterResource(R.drawable.game_ic_lock), null, Modifier.size((18 * scale).dp), tint = Color(0xFF6C7074))
                            Text("$id", color = if (active) Color.White else PlayInk, fontSize = ((if (future) 20 else 25) * scale).sp,
                                lineHeight = (27 * scale).sp, fontWeight = FontWeight.Black)
                        }
                    }
                    if (done != null) Box(Modifier.align(Alignment.Center).offset((23 * scale).dp, (21 * scale).dp)
                        .size((24 * scale).dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color(0xFF009A49))
                        .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.game_ic_check), null, Modifier.size((17 * scale).dp), tint = Color.White)
                    }
                    if(done!=null) Row(Modifier.align(Alignment.Center).offset(y=(41*scale).dp)
                        .height((18*scale).dp).clearAndSetSemantics {},horizontalArrangement=Arrangement.spacedBy((1*scale).dp)) {
                        repeat(3) {star ->
                            Image(painterResource(R.drawable.item_star),null,Modifier.size((17*scale).dp),
                                alpha=if(star<done.bestStars) 1f else .30f,
                                colorFilter=if(star<done.bestStars) null else ColorFilter.colorMatrix(ColorMatrix().apply {setToSaturation(0f)}))
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=(15*scale).dp,vertical=(15*scale).dp),
            horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            PlaySurface(Modifier.width((120*scale).dp).height((48*scale).dp),24.dp,enabled=first>1,
                description="Предыдущие уровни",onClick={first=(first-6).coerceAtLeast(1)}) {
                ReferenceText("Назад",Modifier.fillMaxSize(),21*scale,if(first>1) PlayInk else Color(0xFF8A938D),maxLines=1)
            }
            PlaySurface(Modifier.width((120*scale).dp).height((48*scale).dp),24.dp,enabled=first+6<=total,
                description="Следующие уровни",onClick={first=(first+6).coerceAtMost((total-5).coerceAtLeast(1))}) {
                ReferenceText("Дальше",Modifier.fillMaxSize(),21*scale,if(first+6<=total) PlayInk else Color(0xFF8A938D),maxLines=1)
            }
        }
    }
    replay?.let { id -> AlertDialog(onDismissRequest = { replay = null }, title = { Text("Повторить уровень $id?") },
        text = { Text("Повтор без наград. Подсказки бесплатны; можно улучшить звёзды.") },
        confirmButton = { TextButton(onClick = { replay = null; play(id, true) }) { Text("Играть") } },
        dismissButton = { TextButton(onClick = { replay = null }) { Text("Отмена") } }) }
    locked?.let { id -> AlertDialog(onDismissRequest = { locked = null }, title = { Text("Уровень $id закрыт") },
        text = { Text("Пройди уровень ${id - 1}, чтобы продолжить путь.") },
        confirmButton = { TextButton(onClick = { locked = null }) { Text("Понятно") } }) }
}
