package ru.plumsoftware.wordline.ui.game

import android.content.Context
import android.graphics.Movie
import android.os.SystemClock
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.plumsoftware.wordline.R

@Composable
internal fun RewardOpeningPanel(receipt: RewardOpening,dismiss: ()->Unit) {
    key(receipt.id) {
        val motion=LocalGameMotion.current
        val context=LocalContext.current
        val raw=if(receipt.gift) R.raw.gift_opening else R.raw.chest_opening
        val movie by produceState<Movie?>(null,raw) {
            value=withContext(Dispatchers.IO) {runCatching {context.resources.openRawResource(raw).use(Movie::decodeStream)}.getOrNull()}
        }
        var started by remember {mutableStateOf(false)}
        var ready by remember {mutableStateOf(!motion)}
        LaunchedEffect(movie,motion) {
            if(!motion || movie==null) {
                // A decode failure falls back to the existing final artwork; it never loses a paid reward.
                if(!motion) ready=true else {delay(3000);ready=true}
            } else {started=true;delay(movie!!.duration().coerceAtLeast(1800).toLong());ready=true}
        }
        val counter=remember {Animatable(if(motion) 0f else receipt.amount.toFloat())}
        LaunchedEffect(ready) {if(ready) counter.animateTo(receipt.amount.toFloat(),tween(if(motion) 650 else 0))}
        ReferenceModal({if(ready) dismiss()},445f,bottom=false,widthFraction=.94f,leaves=true,animateAppearance=true) {
            ReferenceText(if(ready) "Награда получена!" else if(receipt.gift) "Открываем подарок…" else "Открываем сундук…",
                frame(20,25,287,43),27*scale)
            ReferenceGlow(frame(24,61,279,265),animate=!ready)
            Box(frame(49,71,229,239)) {
                if(movie!=null && motion && !ready && started) {
                    AndroidView(factory={OpeningMovieView(it)},modifier=Modifier.fillMaxSize(),
                        update={it.show(movie!!)},onReset=null,onRelease={it.release()})
                } else Image(painterResource(if(receipt.gift) {
                    if(ready) R.drawable.gift_opened_original else R.drawable.gift_closed_original
                } else if(ready) R.drawable.chest_opened_original else R.drawable.chest_closed_original),null,Modifier.fillMaxSize())
            }
            CelebrationConfetti(Modifier.fillMaxSize(),seed=receipt.id.hashCode(),active=ready)
            AnimatedVisibility(ready,frame(65,300,197,57),enter=fadeIn()+scaleIn(initialScale=.7f)) {
                RewardPill(counter.value.toLong().coerceIn(0,receipt.amount),"",scale,Modifier.fillMaxSize())
            }
            if(ready) ReferenceAction("Отлично!",frame(34,373,259,55),scale,onClick=dismiss)
            else ReferenceText("Сейчас откроется",frame(34,373,259,55),18*scale)
        }
    }
}

/** A real one-shot transparent GIF. Hold its final frame instead of looping or reopening. */
@Suppress("DEPRECATION")
private class OpeningMovieView(context: Context): View(context) {
    private var movie: Movie?=null
    private var start=0L
    init {setLayerType(LAYER_TYPE_SOFTWARE,null);importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
    fun show(next: Movie) {if(movie!==next) {movie=next;start=SystemClock.uptimeMillis()};invalidate()}
    override fun onDraw(canvas: android.graphics.Canvas) {
        val scene=movie ?: return
        val duration=scene.duration().coerceAtLeast(1)
        val elapsed=(SystemClock.uptimeMillis()-start).coerceAtMost((duration-1).toLong()).toInt()
        scene.setTime(elapsed)
        val scale=minOf(width.toFloat()/scene.width(),height.toFloat()/scene.height())
        canvas.save();canvas.translate((width-scene.width()*scale)/2,(height-scene.height()*scale)/2)
        canvas.scale(scale,scale);scene.draw(canvas,0f,0f);canvas.restore()
        if(elapsed<duration-1 && windowVisibility==VISIBLE) postInvalidateDelayed(30)
    }
    fun release() {movie=null}
}
