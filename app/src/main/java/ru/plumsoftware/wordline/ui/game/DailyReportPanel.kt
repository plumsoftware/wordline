package ru.plumsoftware.wordline.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.plumsoftware.wordline.R

private data class DailyReportReward(val title: String,val amount: Long?)
private val reportAmount=Regex("^(.*): \\+(\\d+) монет$")

/** A receipt for awards already committed by the day rollover; closing never pays again. */
@Composable
internal fun DailyReportPanel(report: List<String>,busy: Boolean,coinCap: Long,dismiss: ()->Unit) {
    val rewards=remember(report,coinCap) {
        report.map {line ->
            val match=reportAmount.matchEntire(line)
            val amount=match?.groupValues?.get(2)?.toLongOrNull()?.takeIf {it in 0..coinCap}
            DailyReportReward(if(amount!=null) match!!.groupValues[1] else line,amount)
        }
    }
    val total=remember(rewards) {
        if(rewards.all {it.amount!=null}) runCatching {rewards.fold(0L) {sum,reward ->Math.addExact(sum,reward.amount!!)}}.getOrNull()
        else null
    }
    val motion=LocalGameMotion.current
    val counter=remember(total) {Animatable(if(motion) 0f else total?.toFloat() ?: 0f)}
    LaunchedEffect(total,motion) {
        if(total!=null) {
            if(!motion) counter.snapTo(total.toFloat())
            else {delay(260);counter.animateTo(total.toFloat(),tween(850,easing=FastOutSlowInEasing))}
        }
    }
    val height=464f+(rewards.size-1).coerceIn(0,3)*52f
    val close: ()->Unit={if(!busy) dismiss()}
    ReferenceModal(close,height,bottom=false,widthFraction=.91f,leaves=true,animateAppearance=true) {
        ReferenceGlow(frame(38,58,251,170))
        CelebrationConfetti(Modifier.fillMaxSize(),seed=report.hashCode(),origin=Offset(.5f,.25f))
        ReferenceText("Награды\nпрошлого дня",frame(25,19,277,66),29*scale,maxLines=2)
        PopIn(frame(84,86,159,119),80) {
            AnimatedRewardImage(painterResource(R.drawable.item_chest_open_hq),null,Modifier.fillMaxSize(),animate=false)
        }
        PopIn(frame(48,124,25,25),220) {
            Image(painterResource(R.drawable.item_star),null,Modifier.fillMaxSize())
        }
        PopIn(frame(258,102,22,22),360) {
            Image(painterResource(R.drawable.item_star),null,Modifier.fillMaxSize())
        }
        PopIn(frame(69,207,189,52).clearAndSetSemantics {
            contentDescription=if(total==null) "Награды уже зачислены" else "Уже зачислено $total монет"
        },170) {
            RewardPill(total?.let {counter.value.toLong().coerceIn(0,it)},"Награды начислены",scale,Modifier.fillMaxSize())
        }
        ReferenceText("Уже зачислено на баланс",frame(34,265,259,25),16*scale,
            Color(0xFF406B54),FontWeight.Bold,maxLines=1)
        Column(frame(20,301,287,height-387).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy((7*scale).dp)) {
            rewards.forEachIndexed {index,reward ->
                PopIn(Modifier.fillMaxWidth(),220L+(index*65L).coerceAtMost(450L)) {
                    PlaySurface(Modifier.fillMaxWidth().heightIn(min=(54*scale).dp),(20*scale).dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=(12*scale).dp,vertical=(10*scale).dp),
                            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy((9*scale).dp)) {
                            ReferenceCheck(Modifier.size((25*scale).dp))
                            ReferenceText(reward.title,Modifier.weight(1f),18*scale,align=TextAlign.Left,maxLines=3)
                            reward.amount?.let {amount ->
                                ReferenceText("+$amount",Modifier.widthIn(max=(91*scale).dp),23*scale,Color(0xFF008B42),maxLines=1)
                            }
                        }
                    }
                }
            }
        }
        PopIn(frame(26,height-76,275,54),380) {
            VictoryButton("Отлично!",false,Modifier.fillMaxSize(),scale,!busy,close,textSize=28f)
        }
    }
}
