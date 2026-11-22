package ru.plumsoftware.wordline.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.locationNames
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val HomeInk = Color(0xFF061638)
private val HomePaper = listOf(Color(0xFFFFFBE9), Color(0xFFFFEBC2))

@Composable
fun HomeScreen(
    state: AppState,
    play: () -> Unit,
    map: () -> Unit,
    settings: () -> Unit,
    tasks: () -> Unit,
    gift: () -> Unit,
    wallet: () -> Unit,
    collection: () -> Unit = {},
) {
    val snapshot = requireNotNull(state.snapshot)
    val campaign = requireNotNull(state.campaign)
    val completed = snapshot.completions.size
    val active = snapshot.profile?.campaignSessionId?.let(snapshot.sessions::get)
    val finished = completed == campaign.levels.size
    val location = (completed / 100).coerceAtMost(9)
    val level = active?.levelId ?: (completed + 1).coerceAtMost(campaign.levels.size)
    val claimedGift = "gift:${snapshot.world.effectiveDate}" in snapshot.world.claims
    HomeScene(
        coins = snapshot.profile?.coins ?: 0,
        level = level,
        location = locationNames[location],
        background = selectedSceneArtwork(state, backgroundResources[state.settings.background.coerceIn(0, (completed / 100).coerceAtMost(9))]),
        progress = if (finished) 1f else (completed % 100) / 100f,
        giftClaimed = claimedGift,
        busy = state.busy,
        play = if (finished && active == null) map else play,
        map = map,
        settings = settings,
        tasks = tasks,
        gift = gift,
        wallet = wallet,
        collection = collection,
    )
}

/** S02: full composition, including its three destinations. */
@Composable
private fun HomeScene(
    coins: Long,
    level: Int,
    location: String,
    @DrawableRes background: Int,
    progress: Float,
    giftClaimed: Boolean,
    busy: Boolean,
    play: () -> Unit,
    map: () -> Unit,
    settings: () -> Unit,
    tasks: () -> Unit,
    gift: () -> Unit,
    wallet: () -> Unit,
    collection: () -> Unit = {},
) {
    val topClearance=gameHeaderClearance()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = maxWidth.value / 327f
        val fontScale = LocalDensity.current.fontScale
        val sceneHeight = maxOf(maxHeight, (555f * scale * fontScale).dp)
        BoxWithConstraints(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).height(sceneHeight),
        ) {
            val sceneWidth = maxWidth
            val height = maxHeight
            fun frame(x: Float, y: Float, width: Float, h: Float) = Modifier
                .offset(x = sceneWidth * (x / 327f), y = height * (y / 555f))
                .size(sceneWidth * (width / 327f), height * (h / 555f))

            HomeBackdrop(background)

            HomeSurface(frame(161f, 15f, 100f, 39f).offset(y=topClearance), 21f * scale, "Баланс: $coins монет", wallet) {
                Row(Modifier.fillMaxSize().padding(horizontal = (5 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.item_coin), null, Modifier.size((32 * scale).dp))
                    Text(
                        NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(coins),
                        Modifier.weight(1f), color = HomeInk, fontSize = (21 * scale).sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1,
                    )
                }
            }
            Box(frame(270f, 10f, 49f, 49f).offset(y=topClearance).clickable(role = Role.Button, onClickLabel = "Настройки", onClick = settings), contentAlignment = Alignment.Center) {
                HomeSurface(Modifier.size((43 * scale).dp, (39 * scale).dp), 15f * scale) {
                    Icon(painterResource(R.drawable.settings_hq), "Настройки", Modifier.padding((6 * scale).dp).fillMaxSize(), tint = Color.Unspecified)
                }
            }

            HubFeatureShortcuts(frame(10f,72f,307f,57f).offset(y=topClearance/2),scale)
            LocationRibbon(location,frame(6f,151f,315f,82f))
            HomeSurface(frame(62f, 238f, 205f, 57f), 32f * scale, "Уровень $level. Карта пути", map) {
                Column(Modifier.fillMaxSize().padding(horizontal = (18 * scale).dp, vertical = (6 * scale).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Уровень $level", color = HomeInk, fontWeight = FontWeight.Black, fontSize = (18 * scale).sp, lineHeight = (21 * scale).sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.fillMaxWidth().height((15 * scale).dp)
                            .clip(RoundedCornerShape(50)).background(Color(0xFFE3DCCB))
                            .border((1.5f * scale).dp, Color(0xFFB6AF9F), RoundedCornerShape(50))
                            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) },
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFF5BEB62), Color(0xFF009A42)))))
                    }
                    Spacer(Modifier.height((3 * scale).dp))
                }
            }
            Box(
                frame(44f, 318f, 242f, 66f)
                    .shadow((5 * scale).dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Color(0xFF6AF17B), Color(0xFF05B947), Color(0xFF00863D))))
                    .border((3 * scale).dp, Color(0xFFFFFDEB), RoundedCornerShape(50))
                    .clickable(enabled = !busy, role = Role.Button, onClick = play),
                contentAlignment = Alignment.Center,
            ) {
                Text("Играть", color = Color.White, fontSize = (37 * scale).sp, fontWeight = FontWeight.Black,
                    style = TextStyle(shadow = Shadow(Color(0x6631673C), Offset(0f, 2f), 2f)))
            }
            HomeSurface(frame(91f,394f,145f,40f),23f*scale,"Карта",map,enabled=!busy) {
                ReferenceText("Карта",Modifier.fillMaxSize(),23*scale,maxLines=1)
            }
            HomeSurface(frame(22f,446f,134f,91f),24f*scale,"Задание дня",tasks,enabled=!busy) {
                Column(Modifier.fillMaxSize().padding(top=(3*scale).dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.height((46*scale).dp).fillMaxWidth(),contentAlignment=Alignment.Center) {
                        Image(painterResource(R.drawable.home_tasks),null,Modifier.size((39*scale).dp))
                    }
                    ReferenceText("Задание\nдня",Modifier.fillMaxWidth(),19*scale)
                }
            }
            HomeSurface(frame(171f,446f,135f,91f),24f*scale,if(giftClaimed) "Подарок получен" else "Подарок",gift,enabled=!busy) {
                Column(Modifier.fillMaxSize().padding(top=(3*scale).dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.height((46*scale).dp).fillMaxWidth(),contentAlignment=Alignment.Center) {
                        Box(Modifier.size((46*scale).dp)) {
                            AnimatedRewardImage(painterResource(if(giftClaimed) R.drawable.gift_opened_original else R.drawable.item_gift_hq),null,Modifier.fillMaxSize(),animate=!giftClaimed)
                            NewContentDot(LocalNotices.current.gift.isNotEmpty(),Modifier.align(Alignment.TopEnd))
                        }
                    }
                    ReferenceText(if(giftClaimed) "Получен" else "Подарок",Modifier.fillMaxWidth(),20*scale)
                }
            }

        }
    }
}

@Composable
private fun HomeBackdrop(@DrawableRes resource: Int) {
    AnimatedScenery(resource,Modifier.fillMaxSize(),topFraction=if(resource==R.drawable.game_lake_day) .228f else 0f)
}

@Composable
private fun HomeSurface(
    modifier: Modifier,
    corner: Float,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner.dp)
    Box(
        modifier.shadow(3.dp, shape).clip(shape).background(Brush.verticalGradient(HomePaper))
            .border(1.dp, Color(0xFFFFFCEB), shape)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .then(if (description != null) Modifier.semantics(mergeDescendants = true) { contentDescription = description } else Modifier),
        content = content,
    )
}

@Preview(name = "S02 — 327 × 627", widthDp = 327, heightDp = 627)
@Composable
private fun HomePreview() {
    GameTheme(highContrast = false) {
        HubFrame(AppState(),"home",{}, {}) {contentModifier ->
            Box(contentModifier) {
                HomeScene(240, 24, "Алтайское озеро", R.drawable.journey_altai, .67f, false, false,
                    {}, {}, {}, {}, {}, {})
            }

        }
    }
}
