package ru.plumsoftware.wordline.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

internal val LocalGameMotion=staticCompositionLocalOf {true}

/** Shared by every gift/chest, including small cards, dialogs and milestone rewards. */
@Composable
internal fun AnimatedRewardImage(painter: Painter,contentDescription: String?,modifier: Modifier,animate: Boolean=true) {
    val pulse=if(animate && LocalGameMotion.current) {
        val transition=rememberInfiniteTransition(label="reward breathing")
        transition.animateFloat(1f,1.055f,infiniteRepeatable(tween(1700,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="reward scale")
    } else rememberUpdatedState(1f)
    Box(modifier) {
        if(animate) ReferenceGlow(Modifier.matchParentSize().graphicsLayer {scaleX=1.4f;scaleY=1.4f},animate=true)
        Image(painter,contentDescription,Modifier.matchParentSize().graphicsLayer {scaleX=pulse.value;scaleY=pulse.value})
    }
}

@Composable
internal fun CurrentLevelGlow(modifier: Modifier) {
    val motion=LocalGameMotion.current
    val transition=rememberInfiniteTransition(label="current level aura")
    val angle by transition.animateFloat(0f,360f,infiniteRepeatable(tween(18000,easing=LinearEasing)),label="aura rotation")
    val pulse by transition.animateFloat(.92f,1.08f,infiniteRepeatable(tween(2300,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="aura breathing")
    Canvas(modifier) {
        val radius=size.minDimension*.48f*(if(motion) pulse else 1f)
        drawCircle(Brush.radialGradient(listOf(Color(0xDFFFFA9B),Color(0x88FFE653),Color.Transparent),center,radius),radius)
        rotate(if(motion) angle else 0f,center) {
            repeat(16) {ray ->
                val a=ray*2*PI/16
                drawPath(Path().apply {
                    moveTo(center.x,center.y)
                    lineTo(center.x+(cos(a-.035)*radius).toFloat(),center.y+(sin(a-.035)*radius).toFloat())
                    lineTo(center.x+(cos(a+.035)*radius).toFloat(),center.y+(sin(a+.035)*radius).toFloat());close()
                },Brush.radialGradient(listOf(Color(0x00FFF4AF),Color(0x99FFE776),Color.Transparent),center,radius))
            }
        }
    }
}

@Composable
internal fun celebrationPulse(active: Boolean): State<Float> {
    if(!active || !LocalGameMotion.current) return rememberUpdatedState(1f)
    val transition=rememberInfiniteTransition(label="bonus pulse")
    return transition.animateFloat(1f,1.12f,
        infiniteRepeatable(tween(650,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="bonus scale")
}

@Composable
internal fun PopIn(modifier: Modifier,delayMillis: Long=0,content: @Composable BoxScope.()->Unit) {
    val motion=LocalGameMotion.current
    val scale=remember {Animatable(if(motion) 0f else 1f)}
    LaunchedEffect(motion) {
        if(!motion) scale.snapTo(1f)
        else {delay(delayMillis);scale.animateTo(1f,spring(dampingRatio=.58f,stiffness=230f))}
    }
    Box(modifier.graphicsLayer {scaleX=scale.value;scaleY=scale.value;alpha=scale.value.coerceIn(0f,1f)},content=content)
}

@Composable
internal fun AnimatedCrosswordLetter(letter: Char,rank: Int?,modifier: Modifier,size: Float) {
    val motion=LocalGameMotion.current
    val scale=remember {Animatable(if(rank!=null && motion) .08f else 1f)}
    LaunchedEffect(rank,motion) {
        if(rank==null || !motion) scale.snapTo(1f)
        else {
            scale.snapTo(.08f);delay(rank*70L)
            scale.animateTo(1f,spring(dampingRatio=.48f,stiffness=420f))
        }
    }
    ReferenceText(letter.toString(),modifier.graphicsLayer {
        scaleX=scale.value;scaleY=scale.value;alpha=scale.value.coerceIn(0f,1f)
    },size,maxLines=1)
}

private data class ConfettiPiece(val x: Float,val y: Float,val speed: Float,val size: Float,val angle: Float,val color: Int)

/** Small native particles without pointer handlers. */
@Composable
internal fun CelebrationConfetti(modifier: Modifier,seed: Int=0,loop: Boolean=false,active: Boolean=true,
                                 origin: Offset=Offset(.15f,.48f)) {
    if(!active || !LocalGameMotion.current) return
    val progress=remember(seed,loop) {Animatable(0f)}
    val pieces=remember(seed) {
        val random=Random(seed)
        List(if(loop) 64 else 42) {ConfettiPiece(random.nextFloat(),random.nextFloat(),1f+random.nextInt(2),
            3f+random.nextFloat()*4f,random.nextFloat()*360f,random.nextInt(6))}
    }
    val colors=remember {listOf(Color(0xFFFFD638),Color(0xFFD650FA),Color(0xFF77E788),Color(0xFFFF89C8),Color(0xFFFFCA87),Color(0xFFB8DAFF))}
    LaunchedEffect(seed,loop) {
        if(loop) progress.animateTo(1f,infiniteRepeatable(tween(4800,easing=LinearEasing)))
        else progress.animateTo(1f,tween(1900,easing=LinearEasing))
    }
    Canvas(modifier) {
        val t=progress.value
        if(!loop && t>=1f) return@Canvas
        pieces.forEach {piece ->
            val phase=if(loop) (t*piece.speed+piece.y)%1f else t
            val x=if(loop) piece.x*size.width+sin(phase*PI*4+piece.angle).toFloat()*size.width*.04f
                else origin.x*size.width+(piece.x-.5f)*size.width*.9f*phase
            val y=if(loop) -20f+phase*(size.height+40f)
                else origin.y*size.height-(.18f+piece.y*.28f)*size.height*phase+size.height*.56f*phase*phase
            val alpha=if(loop) minOf(1f,phase*12f,(1f-phase)*12f) else (1f-phase).coerceIn(0f,1f)
            val w=piece.size.dp.toPx()
            val h=w*(.4f+abs(cos(phase*PI*5)).toFloat()*.5f)
            rotate(piece.angle+phase*480f,Offset(x,y)) {
                drawRect(colors[piece.color].copy(alpha=alpha),Offset(x-w/2,y-h/2),Size(w,h))
            }
        }
    }
}
