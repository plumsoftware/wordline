package ru.plumsoftware.wordline.ui.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.plumsoftware.wordline.R

/** The supplied S01 artwork is rendered directly, including its lettering and loading mark. */
@Composable
fun LaunchScreen(error: String?, onRetry: () -> Unit) {
    ImmersiveGameScreen()

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.launch_s01),
            contentDescription = null,
            // Keep the complete composition visible on every display, without cropping the logo.
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize().semantics {
                contentDescription = "Слова. Путешествие. Загрузка…"
                liveRegion = LiveRegionMode.Polite
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
        )
        if (error != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Не удалось загрузить игру") },
                text = { Text(error) },
                confirmButton = { TextButton(onClick = onRetry) { Text("Попробовать снова") } },
            )
        }
    }
}

@Composable
fun ImmersiveGameScreen() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.activity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousBehavior = controller?.systemBarsBehavior
        val insets = if (view.isAttachedToWindow) ViewInsets(view) else null
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            if (previousBehavior != null) controller?.systemBarsBehavior = previousBehavior
            if (insets?.statusVisible != false) controller?.show(WindowInsetsCompat.Type.statusBars())
            if (insets?.navigationVisible != false) controller?.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

}

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

private class ViewInsets(view: android.view.View) {
    private val insets = androidx.core.view.ViewCompat.getRootWindowInsets(view)
    val statusVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars())
    val navigationVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars())
}

@Preview(name = "S01 — original 327 × 627", widthDp = 327, heightDp = 627)
@Composable
private fun LaunchScreenPreview() {
    GameTheme(highContrast = false) { LaunchScreen(error = null, onRetry = {}) }
}
