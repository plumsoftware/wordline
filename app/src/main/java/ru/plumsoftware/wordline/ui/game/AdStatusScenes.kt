package ru.plumsoftware.wordline.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.ads.AdsState
import ru.plumsoftware.wordline.game.Hint
import kotlin.math.*

private val RewardPanelShape=object: Shape {
    override fun createOutline(size: Size,layoutDirection: LayoutDirection,density: Density): Outline {
        val w=size.width;val h=size.height
        return Outline.Generic(Path().apply {
            moveTo(0f,h*.17f);cubicTo(0f,0f,w*.28f,0f,w*.5f,0f)
            cubicTo(w*.72f,0f,w,0f,w,h*.17f);lineTo(w,h*.90f)
            quadraticTo(w,h,w*.86f,h);lineTo(w*.14f,h);quadraticTo(0f,h,0f,h*.90f);close()
        })
    }
}

@Composable
internal fun AdStatusPanel(ad: AdsState,state: AppState,clear: ()->Unit,retry: ()->Unit,commit: ()->Unit,
                           loadingPresented: (Long)->Unit = {}) {
    if(ad.phase in setOf("idle","showing")) return
    if(ad.phase=="hintReward") {ClueReadPanel(ad.message,clear,oneTime=ad.hint==Hint.CLUE);return}
    val pending=ad.phase in setOf("loading","committing","storageError")
    val dismiss: ()->Unit=when(ad.phase) {"loading","committing","storageError"->({});else->clear}
    val height=when(ad.phase) {"loading","committing"->323f;"storageError"->323f;"unavailable"->310f;"incomplete"->389f;else->390f}
    ReferenceModal(dismiss,height,bottom=false,widthFraction=297f/327f,screenReferenceHeight=650f,
        verticalOffsetFraction=if(ad.phase=="unavailable") -20f/650f else if(pending) -10f/650f else 6f/650f,
        panelShape=if(pending) null else RewardPanelShape,leaves=true) {
        LaunchedEffect(ad.phase,ad.loadingId) {
            if(ad.phase=="loading") {
                // Acknowledge frames from the dialog composition, rather than its underlying screen.
                withFrameNanos { }
                withFrameNanos { }
                loadingPresented(ad.loadingId)
            }
        }
        when(ad.phase) {
            "loading","committing" -> {
                ReferenceText(if(ad.phase!="loading") "Сохраняем награду" else if(ad.reward>0 || ad.hint!=null) "Готовим видео" else "Загрузка рекламы",frame(22,22,283,38),30*scale,maxLines=1)
                RewardSpinner(frame(126,65,75,75),state.settings.reduceMotion)
                if(ad.hint!=null) {
                    Image(painterResource(R.drawable.item_lightbulb),null,frame(40,149,76,74))
                    ReferenceText(when(ad.hint) {Hint.LETTER->"Награда:\nоткрыть букву";Hint.WORD->"Награда:\nоткрыть слово";else->"Награда:\nтекст подсказки"},frame(121,154,185,66),22*scale)
                } else if(ad.reward>0) {
                    PlaySurface(frame(22,148,283,74),22.dp,colors=listOf(Color(0xFFF9E8C6),Color(0xFFF2DDB3))) {}
                    Image(painterResource(R.drawable.item_coin_stack),null,frame(49,156,83,58))
                    ReferenceText("Награда:\n+${ad.reward} монет",frame(149,157,155,54),23*scale,align=androidx.compose.ui.text.style.TextAlign.Left)
                } else ReferenceText("Игра продолжится\nпосле рекламы",frame(32,154,263,63),21*scale,weight=FontWeight.Medium)
                ReferenceText("Подождите…",frame(43,245,241,52),23*scale)
            }
            "storageError" -> {
                DocumentMark(frame(119,23,96,95),true)
                ReferenceText("Не удалось сохранить\nнаграду",frame(20,122,287,62),27*scale)
                ReferenceText("Просмотр учтён. Повторите сохранение.",frame(25,188,277,47),18*scale)
                VictoryButton("Повторить сохранение",false,frame(20,247,287,57),scale,true,commit,textSize=23f)
            }
            "unavailable" -> {
                Image(painterResource(R.drawable.item_cloud),null,frame(88,25,150,90))
                AccentRays(frame(207,13,45,57),false)
                ReferenceText("Сейчас нет видео",frame(20,128,287,38),30*scale,maxLines=1)
                val detail=if(ad.message.contains("лимит",true) || ad.message.contains("разреш",true) || ad.message.contains("сегодня",true) || ad.message.contains("версии",true) || ad.message.contains("уже",true)) ad.message else "Попробуй позже"
                ReferenceText(detail,frame(28,174,271,40),21*scale,weight=FontWeight.Medium)
                VictoryButton("Продолжить игру",false,frame(21,221,285,60),scale,true,clear,textSize=27f)
            }
            "incomplete" -> {
                Image(painterResource(R.drawable.item_video_clapper),null,frame(113,10,108,91))
                Box(frame(189,56,44,44).background(Brush.verticalGradient(listOf(Color(0xFFFF8390),Color(0xFFF52C4E))),CircleShape)
                    .border(1.dp,Color.White,CircleShape),contentAlignment=Alignment.Center) {
                    ReferenceText("×",Modifier.fillMaxSize(),39*scale,Color.White)
                }
                ReferenceText("Просмотр\nне завершён",frame(32,109,263,64),30*scale)
                ReferenceText("Бонус не получен",frame(30,182,267,29),20*scale,weight=FontWeight.Medium)
                VictoryButton("Продолжить игру",false,frame(21,220,285,60),scale,true,clear,textSize=27f)
                VictoryButton("Смотреть снова ·",true,frame(24,300,279,62),scale,true,retry,textSize=23f)
            }
            "reward" -> {
                ReferenceGlow(frame(34,0,260,170))
                RewardSparkles(frame(28,18,271,122))
                Image(painterResource(R.drawable.item_coin_stack),null,frame(85,31,151,109))
                ReferenceText("+${ad.reward} монет",frame(22,145,283,48),40*scale,maxLines=1)
                PlaySurface(frame(25,202,277,52),29.dp) {
                    Row(Modifier.fillMaxSize().padding(horizontal=(18*scale).dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy((7*scale).dp)) {
                        Image(painterResource(R.drawable.item_coin),null,Modifier.size((34*scale).dp))
                        ReferenceText("${ad.balanceBefore ?: state.snapshot?.profile?.coins ?: 0}",Modifier.weight(1f),27*scale,maxLines=1)
                        ReferenceText("➜",Modifier.width((39*scale).dp),34*scale,Color(0xFF0DB84D))
                        Image(painterResource(R.drawable.item_coin),null,Modifier.size((34*scale).dp))
                        ReferenceText("${ad.balanceAfter ?: state.snapshot?.profile?.coins ?: 0}",Modifier.weight(1f),27*scale,maxLines=1)
                    }
                }
                ReferenceText("Спасибо за просмотр",frame(28,267,271,27),20*scale,weight=FontWeight.Medium)
                VictoryButton("Продолжить",false,frame(21,307,285,61),scale,true,clear,textSize=29f)
            }
        }
    }
}

@Composable
private fun RewardSparkles(modifier: Modifier) {
    Canvas(modifier) {
        listOf(Triple(.15f,.33f,.085f),Triple(.81f,.34f,.082f),Triple(.09f,.62f,.04f),Triple(.84f,.72f,.052f)).forEach { (x,y,r) ->
            val cx=x*size.width;val cy=y*size.height;val radius=r*size.height
            drawPath(Path().apply {
                moveTo(cx,cy-radius);quadraticTo(cx+radius*.18f,cy-radius*.18f,cx+radius,cy)
                quadraticTo(cx+radius*.18f,cy+radius*.18f,cx,cy+radius)
                quadraticTo(cx-radius*.18f,cy+radius*.18f,cx-radius,cy)
                quadraticTo(cx-radius*.18f,cy-radius*.18f,cx,cy-radius);close()
            },Color(0xFFFFBC0A))
        }
    }
}

@Composable
private fun RewardSpinner(modifier: Modifier,reduceMotion: Boolean) {
    val transition=rememberInfiniteTransition(label="video preparation")
    val rotation by transition.animateFloat(0f,360f,infiniteRepeatable(tween(1100,easing=LinearEasing)),label="spinner")
    Canvas(modifier) {
        val radius=size.minDimension*.36f
        repeat(12) {index ->
            val angle=((index*30f+(if(reduceMotion) 0f else rotation))*PI/180).toFloat()
            val direction=Offset(cos(angle),sin(angle))
            drawLine(Color(0xFF009C65).copy(alpha=.15f+index/12f*.85f),center+direction*radius*.68f,
                center+direction*radius,size.minDimension*.10f,StrokeCap.Round)
        }
    }
}
