package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.data.Setting
import ru.plumsoftware.wordline.game.*

@Composable
internal fun PausePanel(state: AppState, daily: Boolean, dismiss: ()->Unit, map: ()->Unit, change: (Setting,Boolean)->Unit) {
    ReferenceModal(dismiss,348f,bottom=false,widthFraction=296f/327f) {
        ReferenceText("Пауза",frame(20,19,287,42),34*scale)
        ReferenceAction("Продолжить",frame(27,77,273,54),scale*.82f,onClick=dismiss)
        ReferenceAction(if(daily) "К календарю" else "На карту",frame(27,145,273,52),scale,cream=true,onClick=map)
        Box(frame(27,218,273,1).background(Color(0xFFDCD0B9)))
        listOf(Triple("Звук",R.drawable.game_ic_sound_on,Setting.EFFECTS),Triple("Вибрация",R.drawable.game_ic_vibration,Setting.VIBRATION)).forEachIndexed { index,(label,icon,setting) ->
            Row(frame(37,234+index*51,253,43),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(17.dp)) {
                Icon(painterResource(icon),null,Modifier.size((32*scale).dp),tint=PlayInk)
                Text(label,Modifier.weight(1f),color=PlayInk,fontSize=(20*scale).sp,fontWeight=FontWeight.Bold)
                ReferenceToggle(if(setting==Setting.EFFECTS) state.settings.effects else state.settings.vibration,!state.busy,
                    Modifier.size((59*scale).dp,(43*scale).dp).semantics {contentDescription=label}) {change(setting,it)}
            }
        }
    }
}

@Composable
internal fun ReferenceToggle(checked: Boolean, enabled: Boolean, modifier: Modifier, change: (Boolean)->Unit) {
    Box(modifier.toggleable(value=checked,enabled=enabled,role=Role.Switch,onValueChange=change),contentAlignment=Alignment.Center) {
        Box(Modifier.fillMaxWidth(.9f).height(30.dp).clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(if(checked) listOf(Color(0xFF28D65D),Color(0xFF00913B)) else listOf(Color(0xFFD4D4CE),Color(0xFFAAAFA8))))) {
            Box(Modifier.align(if(checked) Alignment.CenterEnd else Alignment.CenterStart).padding(horizontal=3.dp)
                .size(25.dp).shadow(2.dp,CircleShape).background(Brush.verticalGradient(listOf(Color.White,Cream)),CircleShape))
        }
    }
}

@Composable
internal fun BonusWordsPanel(state: AppState, level: Level, session: Session, dismiss: ()->Unit) {
    var definition by remember {mutableStateOf<String?>(null)}
    val pool=level.bonusPool.size
    val complete=pool>0 && session.bonus.size>=pool
    val goal=if(complete) pool else if(pool>=5) 5 else pool
    val count=if(complete) pool else if(pool>=5) session.bonus.size%5 else session.bonus.size
    ReferenceModal(dismiss,448f) {
        if(complete) CelebrationConfetti(Modifier.fillMaxSize(),seed=session.id.hashCode())
        ReferenceText(if(definition==null) "Дополнительные\nслова" else definition.orEmpty(),frame(20,29,287,59),29*scale)
        if(definition==null) {
            Column(frame(43,106,241,112).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(session.bonus.isEmpty()) ReferenceText("Здесь появятся дополнительные слова",Modifier.fillMaxWidth().padding(10.dp),18*scale)
                session.bonus.sorted().forEach { word ->
                    PlaySurface(Modifier.fillMaxWidth().heightIn(min=(47*scale).dp),26.dp,onClick={definition=word}) {
                        Box(Modifier.fillMaxSize().padding(horizontal=17.dp)) {
                            ReferenceCheck(Modifier.align(Alignment.CenterStart).size((30*scale).dp))
                            ReferenceText(word,Modifier.align(Alignment.Center).padding(vertical=6.dp),29*scale)
                        }
                    }
                }
            }
            PlaySurface(frame(19,250,276,46),26.dp) {
                Row(Modifier.fillMaxSize().padding(start=20.dp,end=45.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("$count/$goal",color=Green,fontSize=(22*scale).sp,fontWeight=FontWeight.Black)
                    ReferenceProgress(if(goal>0) count.toFloat()/goal else 0f,Modifier.weight(1f).height((22*scale).dp))
                }
            }
            if(!complete) ReferenceGlow(frame(222,223,98,94),animate=true)
            AnimatedRewardImage(painterResource(if(complete) R.drawable.gift_opened_original else R.drawable.item_gift_hq),null,frame(239,236,65,65),animate=!complete)
            ReferenceText(if(goal==0) "В этом уровне нет бонусных слов" else if(complete) "Все слова найдены" else "Найди ещё ${minOf(goal-count,pool-session.bonus.size)} слова",
                frame(35,312,257,31),20*scale)
            ReferenceAction("Продолжить",frame(31,365,265,55),scale*.83f,onClick=dismiss)
        } else {
            ReferenceText(state.campaign?.dictionary?.get(definition)?.definition ?: "В словаре игры пока нет определения",
                frame(27,113,273,195),20*scale)
            ReferenceAction("К списку слов",frame(31,365,265,55),scale*.8f,onClick={definition=null})
        }
    }
}

@Composable
internal fun ClueReadPanel(definition: String,dismiss: ()->Unit,oneTime: Boolean=true) {
    ReferenceModal(dismiss,400f,bottom=false,widthFraction=.92f,leaves=true,animateAppearance=true) {
        Column(Modifier.fillMaxSize().padding((22*scale).dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.item_open_book),null,Modifier.size((58*scale).dp))
            Spacer(Modifier.height((8*scale).dp))
            ReferenceText("Подсказка",Modifier.fillMaxWidth(),29*scale)
            Spacer(Modifier.height((12*scale).dp))
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),contentAlignment=Alignment.Center) {
                Text(definition,color=PlayInk,fontSize=(23*scale).sp,lineHeight=(29*scale).sp,
                    fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                    modifier=Modifier.fillMaxWidth().semantics {liveRegion=LiveRegionMode.Polite})
            }
            Spacer(Modifier.height((12*scale).dp))
            if(oneTime) Text("После закрытия окна подсказка исчезнет.\nПрочитай её внимательно.",
                color=PlayInk.copy(alpha=.65f),fontSize=(14*scale).sp,lineHeight=(18*scale).sp,
                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height((16*scale).dp))
            ReferenceAction("Понятно",Modifier.fillMaxWidth().height((55*scale).dp),scale,onClick=dismiss)
        }
    }
}

@Composable
internal fun HintPanel(state: AppState, level: Level, session: Session, kind: Hint, dismiss: ()->Unit,
                       confirm: (String)->Unit, video: ((String)->Unit)?,videoReceived: Boolean=false) {
    val coins=state.snapshot?.profile?.coins ?: 0
    val free=session.replay || session.mode==Mode.TUTORIAL && !session.tutorialLetterUsed && kind==Hint.LETTER
    val basePrice=if(free) 0L else when(kind) {Hint.LETTER->state.economy.letter;Hint.WORD->state.economy.word;Hint.CLUE->state.economy.clue}
    var stage by remember(kind,session.id) {mutableStateOf("offer")}
    var target by remember(kind,session.id) {mutableStateOf<String?>(null)}
    var cell by remember(kind,session.id) {mutableStateOf<Cell?>(null)}
    val visible=visibleCells(level,session)
    val price=if(kind==Hint.CLUE && target in session.clues) 0 else basePrice
    val videoReason=adUnavailable(state,"hint",session.id)
    val forVideo=stage=="videoSelect"
    LaunchedEffect(coins) {if(stage=="insufficient" && coins>=price) stage=if(target==null) "offer" else "select"}
    if(stage=="insufficient") {
        InsufficientPanel((price-coins).coerceAtLeast(0),price,dismiss,
            video=if(video==null) null else ({stage="videoSelect"}),videoEnabled=videoReason==null && !state.busy && !videoReceived,videoReceived=videoReceived)
    } else if(stage=="offer") {
        ReferenceModal(dismiss,383f,screenReferenceHeight=627f) {
            ReferenceText(when(kind) {Hint.LETTER->"Открыть букву?";Hint.WORD->"Открыть слово?";Hint.CLUE->"Нужен намёк?"},frame(20,28,287,36),29*scale)
            Image(painterResource(if(kind==Hint.CLUE) R.drawable.item_open_book else R.drawable.item_lightbulb),null,frame(89,69,149,99))
            CoinAction(basePrice,frame(22,176,283,59),scale,!state.busy) {
                stage=if(coins<basePrice && !(kind==Hint.CLUE && session.clues.isNotEmpty())) "insufficient" else "select"
            }
            if(video!=null) ReferenceAction(if(videoReceived) "Подсказка получена" else "Подсказка за видео",frame(22,248,283,58),scale,video=!videoReceived,
                enabled=videoReason==null && !state.busy && !videoReceived,onClick={stage="videoSelect"})
            ReferenceAction("Отмена",frame(35,318,257,45),scale,cream=true,onClick=dismiss)
        }
    } else ReferenceModal({stage="offer"},520f) {
        Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            ReferenceText(if(kind==Hint.LETTER) "Выбери пустую клетку" else "Выбери слово",Modifier.fillMaxWidth(),26*scale)
            ReferenceText(if(forVideo) "Выбранная подсказка откроется после просмотра видео" else "${if(price==0L) "Бесплатно" else "$price монет"} · списание после подтверждения",Modifier.fillMaxWidth(),15*scale)
            CrosswordBoard(level,session,false,Modifier.fillMaxWidth(),onCell={ selected ->
                cell=selected
                if(kind==Hint.LETTER) target=selected.key.takeIf {it !in visible}
                else target=level.slots.filter {it.id !in session.solved && selected in it.cells}.singleOrNull()?.id
            },selectedCell=cell?.key)
            if(kind!=Hint.LETTER) {
                level.slots.filter {it.id !in session.solved && (cell==null || cell in it.cells)}.forEach { slot ->
                    val mask=slot.cells.mapIndexed { i,c ->if(c.key in visible) slot.word[i] else '□'}.joinToString(" ")
                    PlaySurface(Modifier.fillMaxWidth().heightIn(min=48.dp),14.dp,colors=if(target==slot.id) PlayMint else PlayPaper,onClick={target=slot.id}) {
                        ReferenceText("$mask · ${if(slot.direction==Direction.ACROSS) "горизонтально" else "вертикально"}",Modifier.padding(10.dp),17*scale)
                    }
                }
            }
            ReferenceAction(if(forVideo) "Открыть за видео" else if(price==0L) "Открыть" else "Открыть за $price",Modifier.fillMaxWidth().heightIn(min=56.dp),scale*.8f,video=forVideo,
                enabled=target!=null && !state.busy && (!forVideo || videoReason==null && !videoReceived),onClick={
                    if(forVideo) target?.let {video?.invoke(it)}
                    else if(coins<price) stage="insufficient" else target?.let(confirm)
                })
            ReferenceAction("Отмена",Modifier.fillMaxWidth().heightIn(min=48.dp),scale,cream=true,onClick=dismiss)
        }
    }
}

@Composable
private fun CoinAction(price: Long, modifier: Modifier, scale: Float, enabled: Boolean, click: ()->Unit) {
    Box(modifier) {
        ReferenceAction("",Modifier.matchParentSize(),scale,enabled=enabled,onClick=click)
        Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            if(price>0) {Image(painterResource(R.drawable.item_coin),null,Modifier.size((38*scale).dp));Spacer(Modifier.width((12*scale).dp))}
            ReferenceText(if(price==0L) "Бесплатно" else "$price монет",size=28*scale,color=Color.White)
        }
    }
}

@Composable
private fun InsufficientPanel(missing: Long, price: Long, dismiss: ()->Unit, video: (()->Unit)?, videoEnabled: Boolean,videoReceived: Boolean=false) {
    ReferenceModal(dismiss,465f) {
        ReferenceBack(frame(272,10,39,39),dismiss,R.drawable.game_ic_close,"Закрыть")
        ReferenceGlow(frame(26,23,275,173))
        Image(painterResource(R.drawable.item_coin_pile),null,frame(70,69,185,112))
        VictoryLeaves(frame(42,114,52,58),true)
        VictoryLeaves(frame(229,114,52,58),false)
        ReferenceText("Нужно ещё $missing",frame(28,182,271,44),32*scale)
        ReferenceText("Подсказка стоит $price монет",frame(23,226,281,33),20*scale)
        ReferenceAction("Продолжить\nбез подсказки",frame(21,271,285,73),scale*.92f,onClick=dismiss)
        if(video!=null) ReferenceAction(if(videoReceived) "Подсказка получена" else "Подсказка за видео",frame(21,359,285,70),scale,video=!videoReceived,enabled=videoEnabled,onClick=video)
    }
}
