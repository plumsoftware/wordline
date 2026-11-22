package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.plumsoftware.wordline.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

internal class ReferenceLayout(val width: Dp, val height: Dp, val referenceHeight: Float, val referenceWidth: Float = 327f) {
    val scale get() = width.value / referenceWidth
    fun frame(x: Number, y: Number, w: Number, h: Number) = Modifier
        .offset(width * (x.toFloat()/referenceWidth), height * (y.toFloat()/referenceHeight))
        .size(width * (w.toFloat()/referenceWidth), height * (h.toFloat()/referenceHeight))
}

@Composable
internal fun ReferenceStage(referenceHeight: Float = 627f, backdrop: Boolean = true, highContrast: Boolean = false,
                            background: Int = R.drawable.play_lake_reference, backgroundTopFraction: Float = 0f,
                            content: @Composable ReferenceLayout.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stageWidth=maxWidth
        val topClearance=if(backdrop) gameHeaderClearance() else 0.dp
        val availableHeight=(maxHeight-topClearance).coerceAtLeast(1.dp)
        val font = LocalDensity.current.fontScale
        val expanded = font > 1.15f || maxWidth > maxHeight
        val stageHeight = if (expanded) maxOf(availableHeight, (referenceHeight * maxWidth.value / 327f * maxOf(1f,font*font)).dp) else availableHeight
        if (backdrop) {
            if (highContrast) Box(Modifier.fillMaxSize().background(Cream))
            else AnimatedScenery(background,Modifier.fillMaxSize(),topFraction=backgroundTopFraction)
        }
        Box(Modifier.fillMaxSize().padding(top=topClearance).then(if (expanded) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
            Box(Modifier.width(stageWidth).height(stageHeight)) { ReferenceLayout(stageWidth,stageHeight,referenceHeight).content() }
        }
    }
}

@Composable
internal fun ReferenceText(text: String, modifier: Modifier = Modifier, size: Float = 20f, color: Color = PlayInk,
                           weight: FontWeight = FontWeight.Black, align: TextAlign = TextAlign.Center, maxLines: Int = Int.MAX_VALUE) {
    Box(modifier, contentAlignment = Alignment.Center) {
        BasicText(text,maxLines=maxLines,style=TextStyle(color=color,fontSize=size.sp,lineHeight=(size*1.12f).sp,fontWeight=weight,textAlign=align),
            autoSize=TextAutoSize.StepBased(minFontSize=minOf(8f,size).sp,maxFontSize=size.sp))
    }
}

@Composable
internal fun ReferenceBack(modifier: Modifier, back: () -> Unit, icon: Int = R.drawable.game_ic_back, label: String = "Назад") {
    PlaySurface(modifier,13.dp,onClick=back,description=label) {
        Icon(painterResource(icon),null,Modifier.fillMaxSize().padding(8.dp),tint=if(icon==R.drawable.settings_hq) Color.Unspecified else PlayInk)
    }
}

@Composable
internal fun ReferenceBalance(coins: Long, modifier: Modifier, scale: Float = 1f) {
    PlaySurface(modifier,24.dp,description="$coins монет") {
        Row(Modifier.padding(horizontal=(5*scale).dp),verticalAlignment=Alignment.CenterVertically) {
            Image(painterResource(R.drawable.item_coin),null,Modifier.size((28*scale).dp))
            BasicText(NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(coins),Modifier.weight(1f),
                maxLines=1,style=TextStyle(color=PlayInk,fontSize=(21*scale).sp,fontWeight=FontWeight.Black),
                autoSize=TextAutoSize.StepBased(minFontSize=minOf(8f,21*scale).sp,maxFontSize=(21*scale).sp))
        }
    }
}

@Composable
internal fun ReferenceTitle(text: String, modifier: Modifier, scale: Float, size: Float = 32f) {
    Box(modifier) {
        Box(Modifier.fillMaxSize().padding(horizontal=(18*scale).dp).shadow(3.dp,VictoryBannerShape)
            .clip(VictoryBannerShape).background(Brush.verticalGradient(PlayPaper))) {
            ReferenceText(text,Modifier.fillMaxSize().padding(horizontal=6.dp),size*scale,maxLines=1)
        }
        VictoryLeaves(Modifier.align(Alignment.CenterStart).size((43*scale).dp,(50*scale).dp),true)
        VictoryLeaves(Modifier.align(Alignment.CenterEnd).size((43*scale).dp,(50*scale).dp),false)
    }
}

@Composable
internal fun ReferenceAction(text: String, modifier: Modifier, scale: Float = 1f, enabled: Boolean = true,
                             video: Boolean = false, cream: Boolean = false, onClick: () -> Unit) {
    if (cream) PlaySurface(modifier,32.dp,enabled=enabled,onClick=onClick) {
        ReferenceText(text,Modifier.fillMaxSize().padding(horizontal=10.dp),21*scale)
    } else VictoryButton(text,video,modifier,scale,enabled,onClick)
}

@Composable
internal fun ReferenceProgress(progress: Float, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(Color(0xFFD9D0BE))
        .border(1.dp,Color(0xFFB8AD98),RoundedCornerShape(50))
        .semantics { progressBarRangeInfo=ProgressBarRangeInfo(progress.coerceIn(0f,1f),0f..1f) }) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f,1f)).clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFF70F777),Color(0xFF00AF46),Color(0xFF008D3C)))))
    }
}

@Composable
internal fun ReferenceCheck(modifier: Modifier) {
    Box(modifier.clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFF81E84B),Color(0xFF009729))))
        .border(1.5.dp,Color.White,CircleShape),contentAlignment=Alignment.Center) {
        Icon(painterResource(R.drawable.game_ic_check),null,Modifier.fillMaxSize().padding(3.dp),tint=Color.White)
    }
}

@Composable
internal fun ReferenceGlow(modifier: Modifier,animate: Boolean=false) {
    val motion=animate && LocalGameMotion.current
    val transition=androidx.compose.animation.core.rememberInfiniteTransition(label="reward light")
    val angle by transition.animateFloat(0f,360f,androidx.compose.animation.core.infiniteRepeatable(
        androidx.compose.animation.core.tween(24000,easing=androidx.compose.animation.core.LinearEasing)),label="light rotation")
    val pulse by transition.animateFloat(.92f,1.08f,androidx.compose.animation.core.infiniteRepeatable(
        androidx.compose.animation.core.tween(2200),androidx.compose.animation.core.RepeatMode.Reverse),label="light breathing")
    Canvas(modifier.graphicsLayer {if(motion) {rotationZ=angle;scaleX=pulse;scaleY=pulse;alpha=.88f+.12f*(pulse-.92f)/.16f}}) {
        val c=center
        val r=size.minDimension/2
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFD4),Color(0xB3FFE764),Color.Transparent),c,r),r,c)
        repeat(24) { i ->
            val angle=i*2*PI/24
            drawPath(Path().apply {
                moveTo(c.x,c.y)
                lineTo(c.x+(cos(angle-.045)*r).toFloat(),c.y+(sin(angle-.045)*r).toFloat())
                lineTo(c.x+(cos(angle+.045)*r).toFloat(),c.y+(sin(angle+.045)*r).toFloat());close()
            },Brush.radialGradient(listOf(Color(0xCCFFF6AD),Color.Transparent),c,r))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReferenceModal(onDismiss: () -> Unit, referenceHeight: Float, bottom: Boolean = true, widthFraction: Float = 1f,
                            screenReferenceHeight: Float = 686f, verticalOffsetFraction: Float = 0f,
                            panelShape: Shape? = null, leaves: Boolean = false, animateAppearance: Boolean = false,
                            content: @Composable ReferenceLayout.() -> Unit) {
    if(bottom) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val font=LocalDensity.current.fontScale
            val panelHeight=(maxHeight*(referenceHeight/screenReferenceHeight)*maxOf(1f,font)).coerceAtMost(maxHeight*.94f)
            val sheet=rememberModalBottomSheetState(skipPartiallyExpanded=true)
            ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheet,dragHandle=null,
                sheetMaxWidth=maxWidth*widthFraction,
                shape=panelShape ?: RoundedCornerShape(topStart=36.dp,topEnd=36.dp),
                containerColor=Cream,scrimColor=Color(0x99081D38),
                contentWindowInsets={WindowInsets(0,0,0,0)}) {
                val view=LocalView.current
                SideEffect {
                    (view.parent as? DialogWindowProvider)?.window?.let {window ->
                        window.setDimAmount(0f)
                        WindowCompat.getInsetsController(window,view).apply {
                            systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            hide(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(panelHeight).background(Brush.verticalGradient(PlayPaper))) {
                    ReferenceStage(referenceHeight,backdrop=false,content=content)
                    // A generous target above the contents; sheet gestures and nested scrolling are native.
                    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(30.dp),contentAlignment=Alignment.Center) {
                        ReferenceHandle(Modifier.size(39.dp,5.dp))
                    }
                }
            }
        }
        return
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val view=LocalView.current
        val motion=animateAppearance && LocalGameMotion.current
        val appearance=remember {Animatable(if(motion) 0f else 1f)}
        LaunchedEffect(motion) {
            if(motion) appearance.animateTo(1f,spring(dampingRatio=.76f,stiffness=240f))
            else appearance.snapTo(1f)
        }
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setDimAmount(0f)
                WindowCompat.getInsetsController(window,view).apply {
                    systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().graphicsLayer {alpha=appearance.value.coerceIn(0f,1f)}
                .background(Color(0x99081D38)).clickable(onClick=onDismiss))
            val font=LocalDensity.current.fontScale
            val panelHeight=(maxHeight*(referenceHeight/screenReferenceHeight)*maxOf(1f,font)).coerceAtMost(maxHeight*.94f)
            val shape=panelShape ?: RoundedCornerShape(topStart=36.dp,topEnd=36.dp,bottomStart=if(bottom) 0.dp else 36.dp,bottomEnd=if(bottom) 0.dp else 36.dp)
            val offset=if(bottom) 0.dp else maxHeight*verticalOffsetFraction
            val decorationScale=maxWidth.value*widthFraction/327f
            Box(Modifier.align(if(bottom) Alignment.BottomCenter else Alignment.Center).offset(y=offset).fillMaxWidth(widthFraction).height(panelHeight)
                .graphicsLayer {
                    val progress=appearance.value
                    alpha=progress.coerceIn(0f,1f);scaleX=.90f+.10f*progress;scaleY=.90f+.10f*progress
                    translationY=(1f-progress)*24.dp.toPx()
                }) {
                Box(Modifier.fillMaxSize().shadow(8.dp,shape).clip(shape).background(Brush.verticalGradient(PlayPaper))
                    .pointerInput(Unit) { detectTapGestures { } }) {
                    ReferenceStage(referenceHeight,backdrop=false,content=content)
                }
                if(leaves) {
                    VictoryLeaves(Modifier.align(Alignment.TopStart).offset(x=(-2*decorationScale).dp,y=(-7*decorationScale).dp)
                        .size((49*decorationScale).dp,(51*decorationScale).dp),true)
                    VictoryLeaves(Modifier.align(Alignment.TopEnd).offset(x=(2*decorationScale).dp,y=(-7*decorationScale).dp)
                        .size((49*decorationScale).dp,(51*decorationScale).dp),false)
                }
            }
        }
    }
}

@Composable
internal fun ReferenceHandle(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(Color(0xFFD1C6AF)))
}
