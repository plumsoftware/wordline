package ru.plumsoftware.wordline.ui.game

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.plumsoftware.wordline.data.CompletionRow
import ru.plumsoftware.wordline.data.GameSnapshot
import ru.plumsoftware.wordline.data.ProfileRow
import ru.plumsoftware.wordline.game.*
import java.io.File

/** Exercise live controls and capture S03/S04/S05 for visual review. */
class ReferenceUiTest {
    @get:Rule val compose = createComposeRule()
    private val level = Level(24, "ЛИСТ".toList(), 3, 4, listOf(
        Slot("s1", "ЛИСТ", 0, 0, Direction.ACROSS),
        Slot("s2", "ЛИС", 0, 0, Direction.DOWN),
        Slot("s3", "ТИС", 0, 3, Direction.DOWN),
    ), emptySet())
    private val session = Session("visual-fixture", 24, solved = setOf("s2"), order = listOf(0, 1, 2, 3))
    private val state = AppState(campaign = Campaign(1, (1..30).map { level.copy(id = it) }, emptyMap()),
        snapshot = GameSnapshot(ProfileRow(coins = 240), (1..23).associateWith { CompletionRow(it, 3, 0) }, emptyMap()), loading = false)

    private fun scene(submit: (String) -> Unit = {}) {
        compose.setContent {
            ImmersiveGameScreen()
            GameTheme(false) { PlayScene(state, level, session, true, 0, true, {}, {}, submit, {}, {}, {}) }
        }
        compose.waitForIdle()
    }

    @Test fun tapInputRequiresExplicitConfirmation() {
        val submitted = mutableListOf<String>()
        scene { submitted += it }
        screenshot("S04-live")
        "ЛИСТ".forEachIndexed { index, letter ->
            compose.onNodeWithContentDescription("Буква $letter, позиция ${index + 1} из 4").performClick()
        }
        assertEquals(emptyList<String>(), submitted)
        compose.onNodeWithText("Проверить").performClick()
        assertEquals(listOf("ЛИСТ"), submitted)
        compose.onNodeWithText("Проверить").assertDoesNotExist()
    }

    @Test fun swipeSubmitsOneWordAndShowsSelection() {
        val submitted = mutableListOf<String>()
        scene { submitted += it }
        val root = compose.onRoot()
        val bounds = root.fetchSemanticsNode().boundsInRoot
        val w = bounds.width
        val h = bounds.height
        val wheel = minOf(w * .70f, h * .335f)
        val center = Offset(w / 2, h - w * (118f / 327f) - wheel / 2)
        val radius = wheel / 3
        root.performTouchInput {
            down(center + Offset(0f, -radius))
            moveTo(center + Offset(radius, 0f), 160)
            moveTo(center + Offset(0f, radius), 160)
            moveTo(center + Offset(-radius, 0f), 160)
        }
        screenshot("S05-live")
        root.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf("ЛИСТ"), submitted)
    }

    @Test fun mapLocksFutureLevelsAndConfirmsReplay() {
        val launches = mutableListOf<Pair<Int, Boolean>>()
        compose.setContent {
            ImmersiveGameScreen()
            GameTheme(false) { MapScreen(state, {}, { id, replay -> launches += id to replay }) }
        }
        screenshot("S03-live")
        compose.onNodeWithContentDescription("Уровень 25. Закрыт. Пройди уровень 24").performClick()
        compose.onNodeWithText("Пройди уровень 24, чтобы продолжить путь.").assertIsDisplayed()
        assertEquals(emptyList<Pair<Int, Boolean>>(), launches)
        compose.onNodeWithText("Понятно").performClick()
        compose.onNodeWithContentDescription("Уровень 23. Пройден, 3 звезды. Повтор без наград").performClick()
        compose.onNodeWithText("Играть").performClick()
        assertEquals(listOf(23 to true), launches)
        compose.onNodeWithContentDescription("Уровень 24. Продолжить").performClick()
        assertEquals(listOf(23 to true, 24 to false), launches)
    }

    @Test fun largeTextKeepsHintsAndTapInputReachable() {
        compose.setContent {
            ImmersiveGameScreen()
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                GameTheme(false) { PlayScene(state, level, session, true, 0, true, {}, {}, {}, {}, {}, {}) }
            }
        }
        compose.onNodeWithContentDescription("Буква, 50 монет").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Буква Т, позиция 4 из 4").performScrollTo().performClick()
        compose.onNodeWithText("Проверить").performScrollTo().assertIsDisplayed()
        screenshot("S04-large-text")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val output = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "visual-qa/$name.png")
        output.parentFile?.mkdirs()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
