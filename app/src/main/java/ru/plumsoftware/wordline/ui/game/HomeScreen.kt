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
) {
    ImmersiveGameScreen()
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
        location = if (location == 0) "Горное озеро" else locationNames[location],
        background = backgroundResources[location],
        progress = if (finished) 1f else (completed % 100) / 100f,
        giftClaimed = claimedGift,
        busy = state.busy,
        play = if (finished && active == null) map else play,
        map = map,
        settings = settings,
        tasks = tasks,
        gift = gift,
        wallet = wallet,
    )
}

/** S02 coordinates use a 327 × 555 content area plus the separate 72-unit navigation. */
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
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = maxWidth.value / 327f
        val fontScale = LocalDensity.current.fontScale
        val sceneHeight = maxOf(maxHeight, (460f * scale * fontScale).dp)
        BoxWithConstraints(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).height(sceneHeight),
        ) {
            val sceneWidth = maxWidth
            val height = maxHeight
            fun frame(x: Float, y: Float, width: Float, h: Float) = Modifier
                .offset(x = sceneWidth * (x / 327f), y = height * (y / 555f))
                .size(sceneWidth * (width / 327f), height * (h / 555f))

            HomeBackdrop(background)

            HomeSurface(frame(161f, 15f, 100f, 39f), 21f * scale, "Баланс: $coins монет", wallet) {
                Row(Modifier.fillMaxSize().padding(horizontal = (5 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.item_coin), null, Modifier.size((32 * scale).dp))
                    Text(
                        NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(coins),
                        Modifier.weight(1f), color = HomeInk, fontSize = (21 * scale).sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1,
                    )
                }
            }
            Box(frame(270f, 10f, 49f, 49f).clickable(role = Role.Button, onClickLabel = "Настройки", onClick = settings), contentAlignment = Alignment.Center) {
                HomeSurface(Modifier.size((43 * scale).dp, (39 * scale).dp), 15f * scale) {
                    Icon(painterResource(R.drawable.game_ic_settings), "Настройки", Modifier.padding((8 * scale).dp).fillMaxSize(), tint = HomeInk)
                }
            }

            Icon(painterResource(R.drawable.home_mountain), null, frame(128f, 154f, 72f, 26f), tint = Color.White)
            Box(frame(10f, 178f, 307f, 44f), contentAlignment = Alignment.Center) {
                Text(
                    location, color = Color.White, fontSize = (33 * scale).sp, fontWeight = FontWeight.Black,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    style = TextStyle(shadow = Shadow(Color(0x80355768), Offset(0f, 3f), 5f)),
                    modifier = Modifier.semantics { heading() },
                )
            }
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
                frame(44f, 339f, 242f, 72f)
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
            HomeSurface(frame(22f, 435f, 134f, 92f), 24f * scale, "Задание дня", tasks, enabled = !busy) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(painterResource(R.drawable.home_tasks), null, Modifier.size((39 * scale).dp))
                    Text("Задание\nдня", color = HomeInk, fontSize = (19 * scale).sp, lineHeight = (18 * scale).sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
            HomeSurface(frame(171f, 435f, 135f, 92f), 24f * scale, if (giftClaimed) "Подарок получен" else "Подарок", gift, enabled = !busy) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(painterResource(R.drawable.item_gift), null, Modifier.size((53 * scale).dp))
                    Text(if (giftClaimed) "Получен" else "Подарок", color = HomeInk, fontSize = (20 * scale).sp,
                        lineHeight = (22 * scale).sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun HomeBackdrop(@DrawableRes resource: Int) {
    val image = ImageBitmap.imageResource(resource)
    Canvas(Modifier.fillMaxSize()) {
        // The lake artwork contains a tall sky. S02 uses a tighter view of the mountains and lake.
        val top = if (resource == R.drawable.game_lake_day) (image.height * .228f).roundToInt() else 0
        drawImage(
            image = image,
            srcOffset = IntOffset(0, top),
            srcSize = IntSize(image.width, image.height - top),
            dstSize = IntSize(size.width.roundToInt(), (size.height * 627f / 555f).roundToInt()),
        )
    }
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

@Composable
fun HomeNavigation(route: String, navigate: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 327f
        Row(
            Modifier.fillMaxWidth().height((72 * scale).dp)
                .clip(RoundedCornerShape(topStart = (27 * scale).dp, topEnd = (27 * scale).dp))
                .background(Brush.verticalGradient(HomePaper)).selectableGroup()
                .padding(horizontal = (15 * scale).dp, vertical = (8 * scale).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(
                Triple("home", "Путь", R.drawable.home_mountain),
                Triple("tasks", "Задания", R.drawable.game_ic_tasks),
                Triple("collection", "Коллекция", R.drawable.home_leaf),
            ).forEach { (target, label, icon) ->
                val selected = route == target
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape((18 * scale).dp))
                        .then(if (selected) Modifier.background(Color(0xFF9BE7B6)) else Modifier)
                        .selectable(selected = selected, role = Role.Tab, onClick = { navigate(target) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(painterResource(icon), null, Modifier.size((30 * scale).dp), tint = if (selected) Color(0xFF064E48) else Color(0xFF182631))
                    Text(label, color = HomeInk, fontSize = (14 * scale).sp, lineHeight = (17 * scale).sp,
                        fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
    }
}

@Preview(name = "S02 — 327 × 627", widthDp = 327, heightDp = 627)
@Composable
private fun HomePreview() {
    GameTheme(highContrast = false) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                HomeScene(240, 24, "Горное озеро", R.drawable.game_lake_day, .67f, false, false,
                    {}, {}, {}, {}, {}, {})
            }
            HomeNavigation("home") {}
        }
    }
}
