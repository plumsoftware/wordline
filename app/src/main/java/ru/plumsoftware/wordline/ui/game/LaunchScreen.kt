package ru.plumsoftware.wordline.ui.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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

/** Remove generation fringes with the exact twelve-capsule UI silhouette. */
private val LaunchSpinnerSilhouette=object: Shape {
    override fun createOutline(size: Size,layoutDirection: LayoutDirection,density: Density): Outline {
        val s=size.minDimension;val cx=size.width/2f;val cy=size.height/2f
        val path=android.graphics.Path()
        repeat(12) {index ->
            val capsule=android.graphics.Path().apply {
                addRoundRect(cx-s*.049f,cy-s*.42f,cx+s*.049f,cy-s*.21f,
                    s*.049f,s*.049f,android.graphics.Path.Direction.CW)
            }
            capsule.transform(android.graphics.Matrix().apply {setRotate(index*30f,cx,cy)})
            path.addPath(capsule)
        }
        return Outline.Generic(path.asComposePath())
    }
}

/** S01 scenery, shared brand and a separately animated transparent loading sprite. */
@Composable
fun LaunchScreen(error: String?, onRetry: () -> Unit, reduceMotion: Boolean = false) {
    val rotation = if(reduceMotion || error!=null) 0f else {
        val transition=rememberInfiniteTransition(label="launch loading")
        val angle by transition.animateFloat(0f,360f,
            infiniteRepeatable(tween(1100,easing=LinearEasing),RepeatMode.Restart),label="loading rotation")
        angle
    }
    ReferenceStage(background=R.drawable.launch_lake_reference) {
        JourneyWordmark(frame(13,182,301,153))
        Box(frame(139,470,50,50),contentAlignment=androidx.compose.ui.Alignment.Center) {
            Image(painterResource(R.drawable.launch_loading_spinner),null,
                Modifier.size((50*scale).dp).graphicsLayer {rotationZ=rotation}.clip(LaunchSpinnerSilhouette).semantics {
                    contentDescription="Загрузка…"
                    liveRegion=LiveRegionMode.Polite
                    progressBarRangeInfo=ProgressBarRangeInfo.Indeterminate
                },contentScale=ContentScale.Fit)
        }
        ReferenceText("Загрузка...",frame(106,523,116,25).clearAndSetSemantics {},18*scale,Color.White)
    }
    if(error!=null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Не удалось загрузить игру") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = onRetry) { Text("Попробовать снова") } },
        )
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
