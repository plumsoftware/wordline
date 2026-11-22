package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksScreen(state: AppState, vm: GameViewModel, calendar: ()->Unit, trail: ()->Unit,
                gift: ()->Unit, streak: ()->Unit, resume: (String)->Unit,
                back: ()->Unit = {}, settings: ()->Unit = {}) {
    val snapshot=requireNotNull(state.snapshot)
    val world=snapshot.world
    val items=tasks(world,world.effectiveDate,snapshot.completions.size==state.campaign?.levels?.size)
    ReferenceStage(highContrast=state.settings.highContrast,background=hubArtwork(state)) {
        ReferenceBack(frame(12,14,42,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(163,14,92,37),scale)
        ReferenceBack(frame(272,14,42,38),settings,R.drawable.settings_hq,"Настройки")
        Icon(painterResource(R.drawable.home_mountain),null,frame(129,73,71,31),tint=Color.White)
        ReferenceText("Маленькие\nпобеды",frame(40,109,248,69),34*scale,Color.White)
        VictoryLeaves(frame(61,145,37,34),true)
        VictoryLeaves(frame(243,145,37,34),false)
        items.forEachIndexed { index,task ->
            val claimed="task:${world.effectiveDate}:${task.id}" in world.claims
            val ready=task.progress>=task.target
            PlaySurface(frame(9,203+index*80,309,74),20.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal=(13*scale).dp),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy((10*scale).dp)) {
                    Image(painterResource(when(task.id) {"words"->R.drawable.item_magnifier;"levels"->R.drawable.item_badge_mountain;else->R.drawable.item_star}),
                        null,Modifier.size((43*scale).dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy((8*scale).dp)) {
                        val label=when(task.id) {"words"->"Найди 10 слов";"levels"->if(task.target==1) "Ежедневный кроссворд" else "Пройди 3 уровня";else->"Найди\nбонусное слово"}
                        Text(label,color=PlayInk,fontSize=(17*scale).sp,lineHeight=(18*scale).sp,fontWeight=FontWeight.Black)
                        if(!ready) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            ReferenceProgress(task.progress.toFloat()/task.target,Modifier.weight(1f).height((14*scale).dp))
                            Text("${task.progress.coerceAtMost(task.target)}/${task.target}",color=PlayInk,fontSize=(14*scale).sp,fontWeight=FontWeight.Bold)
                        }
                    }
                    if(ready) {
                        if(claimed) ReferenceCheck(Modifier.size((30*scale).dp).semantics {contentDescription="Награда получена"})
                        else VictoryButton("Забрать\n+${task.reward}",false,Modifier.width((96*scale).dp).height((48*scale).dp),scale,
                            !state.busy,{vm.claimTask(task.id)},textSize=16.5f)
                    } else Row(verticalAlignment=Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.item_coin),null,Modifier.size((27*scale).dp))
                        Text("+${task.reward}",color=PlayInk,fontSize=(19*scale).sp,fontWeight=FontWeight.Black)
                    }
                }
            }
        }
        PlaySurface(frame(19,459,289,66),22.dp,onClick=calendar,description="Ежедневный кроссворд") {
            Row(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(15.dp)) {
                Icon(painterResource(R.drawable.game_ic_calendar),null,Modifier.size((37*scale).dp),tint=Color(0xFF6520AF))
                ReferenceText("Ежедневный\nкроссворд",Modifier.weight(1f),19*scale,align=TextAlign.Left)
                Icon(painterResource(R.drawable.game_ic_chevron_right),null,tint=PlayInk)
            }
        }
        // These routes remain reachable after removing the reference's bottom navigation.
        Row(frame(12,551,303,48),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("Подарок" to gift,"Серия" to streak,"Тропа" to trail).forEach { (label,action) ->
                PlaySurface(Modifier.weight(1f).fillMaxHeight(),18.dp,onClick=action) {
                    ReferenceText(label,Modifier.padding(5.dp),15*scale)
                }
            }
        }
        snapshot.sessions.values.firstOrNull {it.mode==Mode.DAILY && !it.completed && !it.replay}?.let { s ->
            Box(frame(30,606,267,21).clickable {resume(s.id)}) { ReferenceText("Продолжить кроссворд",Modifier.fillMaxSize(),14*scale,Color.White) }
        }
    }
}

@Composable
fun CalendarScreen(state: AppState, back: ()->Unit, play: (String)->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    val world=snapshot.world
    val today=LocalDate.parse(world.effectiveDate)
    val currentMonth=YearMonth.from(today)
    var monthKey by rememberSaveable {mutableStateOf(currentMonth.toString())}
    var selectedKey by rememberSaveable {mutableStateOf(today.toString())}
    val month=YearMonth.parse(monthKey)
    val selected=LocalDate.parse(selectedKey)
    val offset=month.atDay(1).dayOfWeek.value-1
    val weeks=(offset+month.lengthOfMonth()+6)/7
    val extra=(weeks-5).coerceAtLeast(0)*40
    val completed=world.dailyCompleted.count {it.startsWith(monthKey)}
    val done=selectedKey in world.dailyCompleted
    val playable=YearMonth.from(selected)==currentMonth && selected<=today && !done
    fun showMonth(next: YearMonth) {
        monthKey=next.toString()
        selectedKey=if(next==currentMonth) today.toString() else next.atDay(1).toString()
    }
    ReferenceStage(627f+extra,highContrast=state.settings.highContrast) {
        ReferenceBack(frame(12,13,41,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(225,13,89,37),scale)
        ReferenceBack(frame(30,82,40,38),{showMonth(month.minusMonths(1))},R.drawable.game_ic_chevron_left,"Предыдущий месяц")
        PlaySurface(frame(84,82,159,38),25.dp) {
            val name=month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL",Locale.forLanguageTag("ru"))).replaceFirstChar {it.titlecase(Locale.forLanguageTag("ru"))}
            ReferenceText(name,Modifier.fillMaxSize(),27*scale)
        }
        PlaySurface(frame(258,82,40,38),20.dp,enabled=month<currentMonth,onClick={showMonth(month.plusMonths(1))},description="Следующий месяц") {
            Icon(painterResource(R.drawable.game_ic_chevron_right),null,tint=if(month<currentMonth) PlayInk else Color.Gray)
        }
        PlaySurface(frame(8,136,311,263+extra),24.dp) {
            Column(Modifier.fillMaxSize().padding(horizontal=(10*scale).dp,vertical=(10*scale).dp)) {
                Row(Modifier.fillMaxWidth().height((25*scale).dp)) {
                    listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach { ReferenceText(it,Modifier.weight(1f).fillMaxHeight(),15*scale) }
                }
                repeat(weeks) { week ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        repeat(7) { day ->
                            val number=week*7+day-offset+1
                            Box(Modifier.weight(1f).fillMaxHeight().padding(2.dp)) {
                                if(number in 1..month.lengthOfMonth()) {
                                    val date=month.atDay(number)
                                    val key=date.toString()
                                    val isDone=key in world.dailyCompleted
                                    val isSelected=key==selectedKey
                                    PlaySurface(Modifier.fillMaxSize(),8.dp,
                                        colors=if(isSelected) listOf(Color(0xFF4FEA6D),Color(0xFF009640)) else PlayPaper,
                                        enabled=date<=today,onClick={selectedKey=key},
                                        description="$number, ${if(isDone) "пройден" else if(date>today) "будущий день" else if(isSelected) "выбран" else "доступен"}") {
                                        ReferenceText(number.toString(),Modifier.fillMaxSize(),17*scale,
                                            if(isSelected) Color.White else if(date>today) Color(0xFF7A7C82) else PlayInk)
                                    }
                                    if(isDone) ReferenceCheck(Modifier.align(Alignment.BottomEnd).offset(2.dp,3.dp).size((18*scale).dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        PlaySurface(frame(10,407+extra,307,123),25.dp) {
            Column(Modifier.fillMaxSize().padding((12*scale).dp),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy((11*scale).dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.game_ic_calendar),null,Modifier.size((34*scale).dp),tint=Color(0xFF6520AF))
                    ReferenceText(if(month==currentMonth) "Задание дня" else "Архив календаря",size=23*scale)
                }
                ReferenceAction(if(done) "Пройдено" else if(month!=currentMonth) "Только просмотр" else "Играть",
                    Modifier.fillMaxWidth().weight(1f),scale,enabled=playable && !state.busy,onClick={play(selectedKey)})
            }
        }
        PlaySurface(frame(10,538+extra,307,67),22.dp) {
            Row(Modifier.fillMaxSize().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Icon(painterResource(R.drawable.game_ic_collection),null,Modifier.size((37*scale).dp),tint=Color(0xFF6520AF))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Коллекция",color=PlayInk,fontSize=(17*scale).sp,fontWeight=FontWeight.Black)
                        Text("$completed/20",color=PlayInk,fontSize=(17*scale).sp,fontWeight=FontWeight.Black)
                    }
                    ReferenceProgress(completed/20f,Modifier.fillMaxWidth().height((15*scale).dp))
                }
            }
        }
    }
}

@Composable
fun GiftScreen(state: AppState, vm: GameViewModel, back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    val world=snapshot.world
    val claimed="gift:${world.effectiveDate}" in world.claims
    val current=if(claimed) Math.floorMod(world.giftStep-1,state.economy.gifts.size) else world.giftStep
    val reward=state.economy.gifts[current]
    ReferenceStage(686f,highContrast=state.settings.highContrast) {
        ReferenceBack(frame(12,14,42,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(225,14,90,37),scale)
        ReferenceTitle("Подарок дня",frame(16,119,295,61),scale,34f)
        Row(frame(14,206,299,70),horizontalArrangement=Arrangement.spacedBy((3*scale).dp)) {
            state.economy.gifts.forEachIndexed { index,amount ->
                PlaySurface(Modifier.weight(1f).fillMaxHeight(),13.dp,
                    colors=if(index==current) listOf(Color(0xFF62EF7D),Color(0xFF008F45)) else PlayPaper,
                    description="День ${index+1}, $amount монет${if(index==current) ", сегодня" else ""}") {
                    Column(Modifier.fillMaxSize().padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween) {
                        Text("${index+1}",color=if(index==current) Color.White else PlayInk,fontSize=(21*scale).sp,fontWeight=FontWeight.Black)
                        Image(painterResource(R.drawable.item_coin),null,Modifier.size((28*scale).dp))
                    }
                }
            }
        }
        if(!claimed) ReferenceGlow(frame(8,273,311,239),animate=true)
        AnimatedRewardImage(painterResource(if(claimed) R.drawable.gift_opened_original else R.drawable.item_gift_hq),
            if(claimed) "Подарок открыт и получен" else "Открыть подарок дня",frame(72,314,184,158)
                .clickable(enabled=!claimed && !state.busy,onClick=vm::claimGift),animate=!claimed)
        RewardPill(reward,"",scale,frame(73,474,181,62))
        ReferenceAction(if(claimed) "Получено" else "Забрать",frame(36,555,257,65),scale,
            enabled=!claimed && !state.busy,onClick=vm::claimGift)
        ReferenceText("Следующий подарок завтра",frame(25,629,277,24),18*scale,Color.White)
    }
}
