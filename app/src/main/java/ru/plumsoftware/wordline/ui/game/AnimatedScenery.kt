package ru.plumsoftware.wordline.ui.game

import android.content.Context
import android.graphics.Movie
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.plumsoftware.wordline.R

private val scenicLoops=mapOf(
    R.drawable.map_bamboo to R.raw.scene_map_bamboo,
    R.drawable.map_sahara to R.raw.scene_map_sahara,
    R.drawable.map_moscow to R.raw.scene_map_moscow,
    R.drawable.map_aurora to R.raw.scene_map_aurora,
    R.drawable.map_mediterranean to R.raw.scene_map_mediterranean,
    R.drawable.map_canyon to R.raw.scene_map_canyon,
    R.drawable.map_tuscany to R.raw.scene_map_tuscany,
    R.drawable.map_tropics to R.raw.scene_map_tropics,
    R.drawable.map_alps to R.raw.scene_map_alps,
    R.drawable.journey_altai to R.raw.scene_journey_altai,
    R.drawable.journey_bamboo to R.raw.scene_journey_bamboo,
    R.drawable.journey_sahara to R.raw.scene_journey_sahara,
    R.drawable.journey_moscow to R.raw.scene_journey_moscow,
    R.drawable.journey_aurora to R.raw.scene_journey_aurora,
    R.drawable.journey_mediterranean to R.raw.scene_journey_mediterranean,
    R.drawable.journey_canyon to R.raw.scene_journey_canyon,
    R.drawable.journey_tuscany to R.raw.scene_journey_tuscany,
    R.drawable.journey_tropics to R.raw.scene_journey_tropics,
    R.drawable.journey_alps to R.raw.scene_journey_alps,
    R.drawable.game_lake_day to R.raw.scene_game_lake_day,
    R.drawable.game_forest_river to R.raw.scene_game_forest_river,
    R.drawable.game_lake_evening to R.raw.scene_game_lake_evening,
    R.drawable.game_autumn_valley to R.raw.scene_game_autumn_valley,
    R.drawable.game_night_mountains to R.raw.scene_game_night_mountains,
    R.drawable.game_sea_coast to R.raw.scene_game_sea_coast,
    R.drawable.game_canyon to R.raw.scene_game_canyon,
    R.drawable.game_flower_meadow to R.raw.scene_game_flower_meadow,
    R.drawable.game_lotus_lake to R.raw.scene_game_lotus_lake,
    R.drawable.game_snow_valley to R.raw.scene_game_snow_valley,
    R.drawable.play_lake_reference to R.raw.scene_play_lake_reference,
    R.drawable.map_lake_reference to R.raw.scene_map_lake_reference,
    R.drawable.welcome_lake_reference to R.raw.scene_welcome_lake_reference,
    R.drawable.launch_lake_reference to R.raw.scene_launch_lake_reference,
)

/** Full resolution still underlay retains fine detail through the GIF's palette. */
@Composable
internal fun AnimatedScenery(resource: Int,modifier: Modifier=Modifier,topFraction: Float=0f,
                             contentScale: ContentScale=ContentScale.FillBounds,animate: Boolean=true) {
    val motion=LocalGameMotion.current && animate
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    var foreground by remember(owner) {mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))}
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver {_,_->foreground=owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)}
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    val raw=if(motion) scenicLoops[resource] else null
    val movie by produceState<Movie?>(null,raw,context.resources) {
        value=raw?.let {withContext(Dispatchers.IO) {
            runCatching {context.resources.openRawResource(it).use(Movie::decodeStream)}.getOrNull()
        }}
    }
    Box(modifier) {
        if(topFraction>0f) {
            val bitmap=ImageBitmap.imageResource(resource)
            Canvas(Modifier.fillMaxSize()) {
                val top=(bitmap.height*topFraction.coerceIn(0f,.8f)).toInt()
                drawImage(bitmap,srcOffset=IntOffset(0,top),srcSize=IntSize(bitmap.width,bitmap.height-top),
                    dstSize=IntSize(size.width.toInt(),size.height.toInt()))
            }
        } else Image(painterResource(resource),null,Modifier.fillMaxSize(),contentScale=contentScale)
        if(movie!=null) AndroidView(factory={SceneryLoopView(it)},modifier=Modifier.fillMaxSize(),
            update={it.scene(movie,topFraction,contentScale==ContentScale.Crop,motion && foreground)},
            onReset=null,onRelease={it.release()})
    }
}

/** Movie decodes GIF on API 26 too, without a new image library or network dependency. */
@Suppress("DEPRECATION")
private class SceneryLoopView(context: Context): View(context) {
    private var movie: Movie?=null
    private var topFraction=0f
    private var crop=false
    private var running=false
    private var start=0L
    private var elapsed=0
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {alpha=185}
    init {setLayerType(LAYER_TYPE_SOFTWARE,null);importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;isClickable=false;isFocusable=false}
    fun scene(next: Movie?,top: Float,crop: Boolean,active: Boolean) {
        if(movie!==next) {movie=next;elapsed=0;start=SystemClock.uptimeMillis()}
        this.topFraction=top.coerceIn(0f,.8f);this.crop=crop
        if(active && !running) start=SystemClock.uptimeMillis()-elapsed
        running=active;invalidate()
    }
    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val movie=movie ?: return
        if(width==0 || height==0 || movie.width()==0 || movie.height()==0) return
        if(running) elapsed=((SystemClock.uptimeMillis()-start)%(movie.duration().takeIf {it>0} ?: 6000)).toInt()
        movie.setTime(elapsed)
        val top=movie.height()*topFraction
        val sourceHeight=movie.height()-top
        canvas.save()
        if(crop) {
            val scale=maxOf(width.toFloat()/movie.width(),height/sourceHeight)
            canvas.translate((width-movie.width()*scale)/2f,(height-sourceHeight*scale)/2f-top*scale)
            canvas.scale(scale,scale)
        } else {
            canvas.scale(width.toFloat()/movie.width(),height/sourceHeight)
            canvas.translate(0f,-top)
        }
        movie.draw(canvas,0f,0f,paint)
        canvas.restore()
        if(running && windowVisibility==VISIBLE) postInvalidateDelayed(100L)
    }
    fun release() {running=false;movie=null}
}
