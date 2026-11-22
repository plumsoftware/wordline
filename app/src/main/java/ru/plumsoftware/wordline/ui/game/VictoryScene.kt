package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.Mode
import ru.plumsoftware.wordline.game.Session
import ru.plumsoftware.wordline.game.levelFor
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun ResultScreen(state: AppState, session: Session, onNext: () -> Unit, onHome: () -> Unit,
                 reward: () -> Unit = {}, daily: () -> Unit = {}) {
    val campaign = requireNotNull(state.campaign)
    val lastLevel = session.mode == Mode.CAMPAIGN && session.levelId == campaign.levels.size
    val claimed = state.snapshot?.world?.adTickets?.values?.any { it.resultId == session.id && it.placement=="win_extra" && it.status == "committed" } == true
    val videoReason = adUnavailable(state, "win_extra", session.id)
    var wordsOpen by rememberSaveable(session.id) { mutableStateOf(false) }
    var proceeding by rememberSaveable(session.id) { mutableStateOf(false) }
    LaunchedEffect(proceeding) {
        if (proceeding) { delay(750); proceeding = false }
    }
    BackHandler(onBack = onHome)
    VictoryScene(
        caption = when (session.mode) {
            Mode.DAILY -> "Кроссворд за ${session.dateKey} пройден"
            Mode.TUTORIAL -> "Обучение завершено"
            else -> "Уровень ${session.levelId} пройден"
        },
        stars = session.stars,
        rewardAmount = if (session.replay || session.mode == Mode.TUTORIAL) null else session.reward,
        rewardLabel = if (session.replay) "Повтор без наград" else "Готово!",
        nextLabel = when {
            session.mode == Mode.DAILY -> "К календарю"
            session.replay || lastLevel -> "На главную"
            else -> "Дальше"
        },
        busy = state.busy || proceeding,
        showVideo = !session.replay && session.mode != Mode.TUTORIAL,
        extraReward = state.economy.winExtra,
        videoClaimed = claimed,
        videoReason = videoReason,
        highContrast = state.settings.highContrast,
        onNext = { if (!proceeding) { proceeding = true; onNext() } },
        onVideo = reward,
        onWords = { wordsOpen = true },
        onDaily = if (lastLevel) daily else null,
        bonusComplete = campaign.levelFor(session).bonusPool.isNotEmpty() && session.bonus.size==campaign.levelFor(session).bonusPool.size && !session.replay && session.mode!=Mode.TUTORIAL,
    )
    // Definitions remain available without inserting an extra card into the S06 composition.
    if (wordsOpen) AlertDialog(onDismissRequest = { wordsOpen = false }, title = { Text("Найденные слова") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                campaign.levelFor(session).slots.forEach { slot ->
                    campaign.dictionary[slot.word]?.let { entry -> Text("${entry.display} — ${entry.definition}") }
                }
                Text("Баланс: ${state.snapshot?.profile?.coins ?: 0} монет")
            }
        }, confirmButton = { TextButton(onClick = { wordsOpen = false }) { Text("К результату") } })
}

internal val VictoryBannerShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(.16f * w, .025f * h)
    cubicTo(.35f * w, -.015f * h, .66f * w, -.015f * h, .84f * w, .025f * h)
    cubicTo(.96f * w, .05f * h, w, .18f * h, w, .43f * h)
    cubicTo(w, .72f * h, .97f * w, .96f * h, .85f * w, .985f * h)
    cubicTo(.65f * w, .95f * h, .35f * w, .95f * h, .15f * w, .985f * h)
    cubicTo(.035f * w, .96f * h, 0f, .77f * h, 0f, .46f * h)
    cubicTo(0f, .20f * h, .045f * w, .065f * h, .16f * w, .025f * h)
    close()
}

/** S06: all positions are relative to its 327 × 686 reference, all text is live. */
@Composable
internal fun VictoryScene(
    caption: String, stars: Int, rewardAmount: Long?, rewardLabel: String = "",
    nextLabel: String = "Дальше", busy: Boolean = false,
    showVideo: Boolean = true, extraReward: Long = 20, videoClaimed: Boolean = false,
    videoReason: String? = null, highContrast: Boolean = false,
    onNext: () -> Unit, onVideo: () -> Unit, onWords: () -> Unit, onDaily: (() -> Unit)? = null,
    bonusComplete: Boolean = false,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = (maxWidth.value / 327f).coerceAtMost(1.8f)
        val fontScale = LocalDensity.current.fontScale
        val adaptive = fontScale > 1.15f || maxWidth > maxHeight || maxWidth >= 650.dp
        if (!highContrast) {
            AnimatedScenery(R.drawable.play_lake_reference,Modifier.fillMaxSize().blur(3.dp))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                Color(0x88344F61), Color(0x22416C6C), Color.Transparent))))
        } else Box(Modifier.fillMaxSize().background(Cream))

        VictoryCelebration(Modifier.fillMaxSize(), drawConfetti = !highContrast)
        val motion=LocalGameMotion.current
        val counter=remember(rewardAmount) {Animatable(if(motion) 0f else rewardAmount?.toFloat() ?: 0f)}
        LaunchedEffect(rewardAmount,motion) {
            if(rewardAmount!=null) {
                if(!motion) counter.snapTo(rewardAmount.toFloat())
                else {delay(600);counter.animateTo(rewardAmount.toFloat(),tween(1000,easing=FastOutSlowInEasing))}
            }
        }
        val displayedReward=rewardAmount?.let {counter.value.toLong().coerceIn(0,it)}
        val videoLabel = if (videoClaimed) "Награда получена" else "Ещё +$extraReward за видео"
        val videoEnabled = !busy && !videoClaimed && videoReason == null
        if (adaptive) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                VictoryStars(stars, Modifier.widthIn(max = 330.dp).fillMaxWidth().height(170.dp))
                Box(Modifier.widthIn(max = 520.dp).fillMaxWidth()
                    .shadow(4.dp, VictoryBannerShape).clip(VictoryBannerShape)
                    .background(Brush.verticalGradient(PlayPaper)).clickable(role = Role.Button, onClick = onWords)
                    .semantics { contentDescription = "Отлично! $caption. Посмотреть найденные слова" }) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Отлично!", color = PlayInk, fontSize = 36.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text(caption, color = PlayInk, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                PopIn(Modifier.widthIn(max = 360.dp).fillMaxWidth().height((68 * fontScale).dp),550) {
                    RewardPill(displayedReward,rewardLabel,1f,Modifier.fillMaxSize())
                }
                if(bonusComplete) ReferenceText("Включая бонус за дополнительные слова",size=18f)
                VictoryButton(nextLabel, false, Modifier.widthIn(max = 520.dp).fillMaxWidth(), 1f, !busy, onNext, flexible = true)
                if (showVideo) VictoryButton(videoLabel, !videoClaimed, Modifier.widthIn(max = 520.dp).fillMaxWidth(), 1f,
                    videoEnabled, onVideo, videoReason, flexible = true)
                onDaily?.let { action -> TextButton(onClick = action) { Text("Ежедневный кроссворд") } }
            }
        } else {
            fun frame(x: Float, y: Float, w: Float, h: Float) = Modifier
                .offset(maxWidth * (x / 327f), maxHeight * (y / 686f))
                .size(maxWidth * (w / 327f), maxHeight * (h / 686f))
            VictoryStars(stars, frame(35f, 73f, 255f, 116f))
            Box(frame(33f, 193f, 261f, 103f).shadow((4 * scale).dp, VictoryBannerShape)
                .clip(VictoryBannerShape).background(Brush.verticalGradient(PlayPaper))
                .border(1.dp, Color(0xFFFFFEED), VictoryBannerShape)
                .clickable(role = Role.Button, onClickLabel = "Найденные слова", onClick = onWords)) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(.59f), contentAlignment = Alignment.BottomCenter) {
                    Text("Отлично!", color = PlayInk, fontSize = (42 * scale).sp, fontWeight = FontWeight.Black,
                        style = victoryTextStyle, lineHeight = (46 * scale).sp, modifier = Modifier.semantics { heading() })
                }
                Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = (17 * scale).dp), contentAlignment = Alignment.Center) {
                    Text(caption, color = PlayInk, fontSize = ((if (caption.length > 24) 14 else if (caption.length > 21) 17 else 20) * scale).sp,
                        fontWeight = FontWeight.Bold, style = victoryTextStyle, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
            VictoryLeaves(frame(15f, 224f, 43f, 50f), left = true)
            VictoryLeaves(frame(268f, 224f, 43f, 50f), left = false)
            PopIn(frame(79f,322f,167f,63f),550) {RewardPill(displayedReward,rewardLabel,scale,Modifier.fillMaxSize())}
            if(bonusComplete) ReferenceText("Бонус за все слова включён",frame(43f,386f,241f,24f),15*scale,Color.White)
            VictoryButton(nextLabel, false, frame(37f, 416f, 254f, 66f), scale, !busy, onNext)
            if (showVideo) VictoryButton(videoLabel, !videoClaimed, frame(37f, 498f, 254f, 65f), scale,
                videoEnabled, onVideo, videoReason)
            onDaily?.let { action ->
                TextButton(onClick = action, modifier = frame(50f, 580f, 227f, 48f)) { Text("Ежедневный кроссворд", color = Color.White) }
            }
        }
    }
}

private val victoryTextStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

@Composable
private fun VictoryStars(stars: Int, modifier: Modifier) {
    BoxWithConstraints(modifier.clearAndSetSemantics { contentDescription = "$stars звезды из 3" }) {
        val starData = listOf(Triple(0f, .26f, .35f), Triple(.29f, 0f, .43f), Triple(.64f, .26f, .35f))
        starData.forEachIndexed { index, (x, y, fraction) ->
            val filter = if (index < stars) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            PopIn(Modifier.offset(maxWidth*x,maxHeight*y).size(maxWidth*fraction),120L+index*170L) {
                Image(painterResource(R.drawable.item_star),null,Modifier.fillMaxSize(),
                    colorFilter=filter,alpha=if(index<stars) 1f else .5f)
            }
        }
    }
}

@Composable
internal fun VictoryLeaves(modifier: Modifier, left: Boolean) {
    Box(modifier) {
        Image(painterResource(R.drawable.item_leaf_token), null, Modifier.fillMaxSize(.8f).rotate(if (left) -62f else 32f))
        Image(painterResource(R.drawable.item_leaf_token), null,
            Modifier.align(Alignment.BottomCenter).fillMaxSize(.64f).rotate(if (left) -110f else 80f))
    }
}

@Composable
internal fun RewardPill(amount: Long?, label: String, scale: Float, modifier: Modifier) {
    Box(modifier.clearAndSetSemantics { contentDescription = if (amount == null) label else "+$amount монет уже начислено" }) {
        PlaySurface(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.83f).fillMaxHeight(.76f), (25 * scale).dp) {
            val textSize=(if(amount==null) 15 else 34)*scale
            BasicText(if(amount==null) label else "+$amount",Modifier.padding(start=(26*scale).dp,end=(12*scale).dp),
                maxLines=1,autoSize=TextAutoSize.StepBased(minFontSize=minOf(8f,textSize).sp,maxFontSize=textSize.sp),
                style=victoryTextStyle.copy(color=PlayInk,fontSize=textSize.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center))
        }
        if (amount != null) {
            Image(painterResource(R.drawable.item_coin_stack), null, Modifier.align(Alignment.TopStart).offset(x = (23 * scale).dp).fillMaxHeight().width((54 * scale).dp))
            Image(painterResource(R.drawable.item_coin_stack), null, Modifier.align(Alignment.BottomStart).fillMaxHeight(.73f).width((46 * scale).dp))
        }
    }
}

@Composable
internal fun VictoryButton(label: String, video: Boolean, modifier: Modifier, scale: Float,
                          enabled: Boolean, action: () -> Unit, unavailable: String? = null, flexible: Boolean = false,
                          textSize: Float? = null) {
    val shape = RoundedCornerShape((36 * scale).dp)
    val palette = if (video) listOf(Color(0xFFD77DFF), Color(0xFF9630EB), Color(0xFF6421BA))
        else listOf(Color(0xFF73F68A), Color(0xFF08B74C), Color(0xFF00833F))
    Box(modifier.then(if (flexible) Modifier.heightIn(min = 66.dp) else Modifier)
        .shadow((5 * scale).dp, shape).clip(shape).background(Brush.verticalGradient(palette))
        .border((2.5f * scale).dp, Color(0xFFFFFFEA), shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = action)
        .semantics(mergeDescendants = true) {
            contentDescription = if (video) "$label. Реклама" else label
            if (unavailable != null) stateDescription = unavailable
        }, contentAlignment = Alignment.Center) {
        val labelSize = textSize ?: if (video) 19f else if (label.length > 10) 26f else 34f
        if (video) Row(Modifier.fillMaxWidth().padding(horizontal = (23 * scale).dp, vertical = (9 * scale).dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)) {
            Box(Modifier.size((32 * scale).dp, (28 * scale).dp).clip(RoundedCornerShape((8 * scale).dp)).background(Color.White), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size((15 * scale).dp)) {
                    drawPath(Path().apply { moveTo(size.width * .16f, 0f); lineTo(size.width, size.height / 2); lineTo(size.width * .16f, size.height); close() }, Color(0xFF7130BC))
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(label,style=victoryTextStyle.copy(color=Color.White,fontSize=(labelSize*scale).sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center,lineHeight=(23*scale).sp),
                    autoSize=TextAutoSize.StepBased(minFontSize=minOf(8f,labelSize*scale).sp,maxFontSize=(labelSize*scale).sp),
                    maxLines=if(flexible) Int.MAX_VALUE else 1)
                Text("Реклама", color = Color.White, fontSize = (14 * scale).sp, lineHeight = (17 * scale).sp, style = victoryTextStyle)
            }
        } else BasicText(label,Modifier.padding(horizontal=(16*scale).dp,vertical=if(flexible) 14.dp else 0.dp),
            maxLines=if(flexible) Int.MAX_VALUE else label.count {it=='\n'}+1,
            autoSize=TextAutoSize.StepBased(minFontSize=minOf(8f,labelSize*scale).sp,maxFontSize=(labelSize*scale).sp),
            style=victoryTextStyle.copy(color=Color.White,fontSize=(labelSize*scale).sp,fontWeight=FontWeight.Black,
                textAlign=TextAlign.Center,shadow=Shadow(Color(0x5530623D),Offset(0f,2f),2f)))
    }
}

@Composable
private fun VictoryCelebration(modifier: Modifier, drawConfetti: Boolean) {
    Canvas(modifier) {
        val sx = size.width / 327f
        val sy = size.height / 686f
        val center = Offset(163 * sx, 134 * sy)
        val glow = Brush.radialGradient(listOf(Color(0xDDFFF6A4), Color(0x77FFE69C), Color.Transparent), center, 113 * sx)
        drawCircle(glow, 113 * sx, center)
        repeat(26) { index ->
            val angle = Math.toRadians((index * 360.0 / 26) - 90)
            val spread = .034
            val radius = 150 * sx
            drawPath(Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + (cos(angle - spread) * radius).toFloat(), center.y + (sin(angle - spread) * radius).toFloat())
                lineTo(center.x + (cos(angle + spread) * radius).toFloat(), center.y + (sin(angle + spread) * radius).toFloat())
                close()
            }, Brush.radialGradient(listOf(Color(0x99FFF6C5), Color.Transparent), center, radius))
        }
    }
    if(drawConfetti) CelebrationConfetti(modifier,seed=67,loop=true)
}

@Preview(name = "S06 — 327 × 686", widthDp = 327, heightDp = 686)
@Composable
private fun VictoryPreview() {
    GameTheme(false) { VictoryScene("Уровень 24 пройден", 3, 20L, onNext = {}, onVideo = {}, onWords = {}) }
}

@Preview(name = "S06 — крупный текст", widthDp = 327, heightDp = 686, fontScale = 2f)
@Composable
private fun VictoryLargeTextPreview() {
    GameTheme(false) { VictoryScene("Уровень 24 пройден", 3, 20L, onNext = {}, onVideo = {}, onWords = {}) }
}
