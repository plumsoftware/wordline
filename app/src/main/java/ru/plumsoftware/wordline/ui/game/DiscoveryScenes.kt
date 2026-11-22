package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import java.time.LocalDate

private fun dayWord(number: Int): String = when {
    number%100 in 11..14 -> "дней"
    number%10==1 -> "день"
    number%10 in 2..4 -> "дня"
    else -> "дней"
}

@Composable
fun StreakScreen(state: AppState, vm: GameViewModel, back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    val world=snapshot.world
    val milestone=world.streak/7*7
    val claimed=milestone>0 && "streak:${world.streakRun}:$milestone" in world.claims
    val ready=milestone>0 && !claimed
    val today=LocalDate.parse(world.effectiveDate)
    val monday=today.minusDays((today.dayOfWeek.value-1).toLong())
    ReferenceStage(686f,highContrast=state.settings.highContrast) {
        ReferenceBack(frame(12,14,42,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(225,14,89,37),scale)
        ReferenceGlow(frame(27,25,273,239))
        VictoryLeaves(frame(46,149,69,75),true)
        VictoryLeaves(frame(213,149,69,75),false)
        StreakFlame(frame(89,55,151,167))
        ReferenceText(world.streak.toString(),frame(117,130,92,80),75*scale,Color(0xFF592400))
        Box(frame(27,219,273,64).shadow(4.dp,VictoryBannerShape).clip(VictoryBannerShape)
            .background(Brush.verticalGradient(PlayPaper))) {
            ReferenceText("${world.streak} ${dayWord(world.streak)} подряд",Modifier.fillMaxSize().padding(horizontal=14.dp),32*scale)
        }
        PlaySurface(frame(8,315,311,76),23.dp) {
            Row(Modifier.fillMaxSize().padding(horizontal=(10*scale).dp,vertical=(10*scale).dp)) {
                listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEachIndexed { index,name ->
                    val date=monday.plusDays(index.toLong())
                    val visited=date.toString() in world.visits
                    Column(Modifier.weight(1f).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.SpaceBetween) {
                        ReferenceText(name,size=15*scale)
                        if(visited) ReferenceCheck(Modifier.size((28*scale).dp).semantics {contentDescription="$name, посещение засчитано"})
                        else Box(Modifier.size((28*scale).dp).clip(CircleShape).background(Color(0xFFE4DCC9))
                            .border(1.dp,Color(0xFFC7BDA6),CircleShape).semantics {
                                contentDescription="$name, ${if(date>today) "будущий день" else "посещения не было"}"
                            })
                    }
                }
            }
        }
        if(!claimed) ReferenceGlow(frame(13,399,196,162),animate=true)
        PlaySurface(frame(136,451,157,57),30.dp) {
            Row(Modifier.fillMaxSize().padding(start=(35*scale).dp,end=(14*scale).dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                Image(painterResource(R.drawable.item_coin_stack),null,Modifier.size((38*scale).dp))
                ReferenceText("+50",Modifier.weight(1f),33*scale)
            }
        }
        AnimatedRewardImage(painterResource(if(claimed) R.drawable.item_chest_open_hq else R.drawable.item_chest_closed_hq),null,
            frame(36,428,143,112).clickable(enabled=ready && !state.busy,onClick=vm::claimStreak),animate=!claimed)
        ReferenceAction(if(ready) "Забрать" else if(claimed) "Получено" else "Продолжить",frame(34,556,263,65),scale,
            enabled=!state.busy && !claimed,onClick={if(ready) vm.claimStreak() else back()})
        val remaining=7-world.streak%7
        ReferenceText(if(!ready && !claimed) "До награды $remaining ${dayWord(remaining)}" else "Продолжай в своём ритме",
            frame(33,628,261,28),18*scale,Color.White)
    }
}

@Composable
fun TrailScreen(state: AppState, vm: GameViewModel, back: ()->Unit, play: ()->Unit = back) {
    val snapshot=requireNotNull(state.snapshot)
    val world=snapshot.world
    val progress=world.trailEvents.size.coerceAtMost(30)
    var inspect by remember {mutableStateOf<Int?>(null)}
    ReferenceStage(686f,highContrast=state.settings.highContrast,background=R.drawable.game_autumn_valley,
        backgroundTopFraction=.15f) {
        ReferenceBack(frame(12,14,42,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(225,14,89,37),scale)
        PlaySurface(frame(51,204,225,43),25.dp) {
            ReferenceText("Личное приключение",Modifier.fillMaxSize().padding(top=(7*scale).dp),18*scale)
        }
        Box(frame(29,158,271,62).shadow(4.dp,VictoryBannerShape).clip(VictoryBannerShape)
            .background(Brush.verticalGradient(PlayPaper))) {
            ReferenceText("Осенняя тропа",Modifier.fillMaxSize().padding(horizontal=8.dp),32*scale)
        }
        AutumnLeaf(frame(17,191,47,47).rotate(-61f))
        AutumnLeaf(frame(264,191,47,47).rotate(31f))
        PlaySurface(frame(56,300,216,58),31.dp) {
            Box(Modifier.fillMaxSize()) {
                AutumnLeaf(Modifier.align(Alignment.CenterStart).padding(start=15.dp).size((48*scale).dp))
                Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.71f).padding(end=(22*scale).dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    ReferenceText("$progress/30",size=22*scale)
                    ReferenceProgress(progress/30f,Modifier.fillMaxWidth().height((13*scale).dp))
                }
            }
        }
        ReferenceProgress(progress/30f,frame(15,482,279,22).border(3.dp,Color(0xFFFFFBE6),RoundedCornerShape(50)))
        listOf(10,20,30).forEachIndexed { index,target ->
            val x=31+index*100
            val claimed="trail:$target" in world.claims
            val ready=progress>=target && !claimed
            PlaySurface(frame(x,401,67,67),40.dp,
                enabled=!state.busy,description="$target этапов. +${trailRewards.getValue(target)} монет и открытка. ${if(claimed) "Получено" else if(ready) "Забрать" else "Закрыто"}",
                onClick={if(ready) vm.claimTrail(target) else inspect=target}) {
                AnimatedRewardImage(painterResource(when(target) {10->R.drawable.item_coin_pile;20->if(claimed) R.drawable.gift_opened_original else R.drawable.item_gift_hq;else->if(claimed) R.drawable.item_chest_open_hq else R.drawable.item_chest_closed_hq}),
                    null,Modifier.fillMaxSize().padding(7.dp),animate=!claimed)
            }
            if(claimed) ReferenceCheck(frame(x+42,395,29,29))
            Box(frame(x+30,474,6,21).clip(RoundedCornerShape(50)).background(if(progress>=target) Green else Color(0xFFB8B6A9)))
            Box(frame(x+25,477,16,16).clip(CircleShape).background(if(progress>=target) Green else Color(0xFFDCDACD))
                .border(3.dp,Color(0xFFFFFFED),CircleShape))
            PlaySurface(frame(x+12,490,43,42),24.dp,enabled=!state.busy,onClick={if(ready) vm.claimTrail(target) else inspect=target}) {
                ReferenceText(target.toString(),Modifier.fillMaxSize(),22*scale)
            }
        }
        ReferenceAction(if(progress==30) "Тропа пройдена" else "Продолжить",frame(37,555,254,63),
            if(progress==30) scale*.8f else scale,enabled=!state.busy,onClick=if(progress==30) back else play)
    }
    inspect?.let { target ->
        ReferenceModal({inspect=null},276f,bottom=false,widthFraction=.9f) {
            ReferenceText("Этап $target",frame(20,22,287,39),29*scale)
            AnimatedRewardImage(painterResource(if(target==10) R.drawable.item_coin_pile else if(target==20) {
                if("trail:$target" in world.claims) R.drawable.gift_opened_original else R.drawable.item_gift_hq
            } else if("trail:$target" in world.claims) R.drawable.item_chest_open_hq else R.drawable.item_chest_closed_hq),null,frame(121,70,85,77),animate="trail:$target" !in world.claims)
            ReferenceText(if("trail:$target" in world.claims) "Награда получена: +${trailRewards.getValue(target)} монет и открытка"
                else "Осталось ${target-progress} этапов. Награда: +${trailRewards.getValue(target)} монет и открытка",frame(24,153,279,55),18*scale)
            ReferenceAction("Понятно",frame(43,217,241,46),scale*.8f,onClick={inspect=null})
        }
    }
}

internal val collectionNames=locationNames

@Composable
fun CollectionScreen(state: AppState, achievements: ()->Unit, statistics: ()->Unit, appearance: ()->Unit,
                     open: (Int)->Unit, openExtra: (String)->Unit = {}, settings: ()->Unit = {}, back: ()->Unit = {}) {
    val snapshot=requireNotNull(state.snapshot)
    val world=snapshot.world
    val order=listOf(0,1,2,5,3,4,6,7,8,9)
    val extras=world.collectibles.filter {!it.startsWith("location:")}.sorted()
    val pageCount=(order.size+3)/4+if(extras.isNotEmpty()) 1 else 0
    val pager=rememberPagerState(pageCount={pageCount})
    val scope=rememberCoroutineScope()
    var menu by remember {mutableStateOf(false)}
    var locked by remember {mutableStateOf<Int?>(null)}
    fun showPage(page: Int) {
        scope.launch {
            if(state.settings.reduceMotion) pager.scrollToPage(page) else pager.animateScrollToPage(page)
        }
    }
    ReferenceStage(highContrast=state.settings.highContrast,background=hubArtwork(state)) {
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(170,14,93,38),scale)
        ReferenceBack(frame(12,14,42,38),back)
        GeneratedPageTitle("Твои открытия",pageTitleFrame())
        HubFeatureShortcuts(frame(14,145,299,51),scale)
        HorizontalPager(pager,frame(14,208,299,325),pageSpacing=16.dp) { page ->
            if(page*4<order.size) Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy((11*scale).dp)) {
                repeat(2) { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy((9*scale).dp)) {
                        repeat(2) { col ->
                            val position=page*4+row*2+col
                            if(position<order.size) {
                                val index=order[position]
                                CollectionTile(index,"location:$index" in world.collectibles,Modifier.weight(1f).fillMaxHeight(),scale) {
                                    if("location:$index" in world.collectibles) open(index) else locked=index
                                }
                            } else Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                extras.forEach { id ->
                    PlaySurface(Modifier.fillMaxWidth().heightIn(min=84.dp),22.dp,onClick={openExtra(id)}) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(48.dp)) {
                                AnimatedRewardImage(painterResource(R.drawable.gift_opened_original),null,Modifier.fillMaxSize(),animate=false)
                                NewContentDot(LocalNotices.current.collectible(id),Modifier.align(Alignment.TopEnd))
                            }
                            ReferenceText(if(id.startsWith("trail:")) "Тропа: ${id.substringAfter(':')} этапов" else "Календарь: ${id.substringAfter(':')}",
                                Modifier.weight(1f),20*scale)
                        }
                    }
                }
            }
        }
        PlaySurface(frame(54,558,42,40),18.dp,enabled=pager.currentPage>0,
            onClick={showPage((pager.currentPage-1).coerceAtLeast(0))},description="Предыдущие открытки") {
            Icon(painterResource(R.drawable.game_ic_chevron_left),null,tint=PlayInk)
        }
        PlaySurface(frame(124,562,79,32),18.dp) {ReferenceText("${pager.currentPage+1} / $pageCount",Modifier.fillMaxSize(),17*scale)}
        PlaySurface(frame(232,558,42,40),18.dp,enabled=pager.currentPage<pageCount-1,
            onClick={showPage((pager.currentPage+1).coerceAtMost(pageCount-1))},description="Следующие открытки") {
            Icon(painterResource(R.drawable.game_ic_chevron_right),null,tint=PlayInk)
        }
    }
    locked?.let { index ->
        ReferenceModal({locked=null},232f,bottom=false,widthFraction=.9f) {
            ReferenceText(collectionNames[index],frame(20,22,287,42),26*scale)
            Icon(painterResource(R.drawable.game_ic_lock),null,frame(142,76,43,43),tint=PlayInk)
            ReferenceText("Пройди уровень ${20+index*100}, чтобы открыть эту открытку",frame(22,128,283,48),19*scale)
            ReferenceAction("Понятно",frame(44,183,239,40),scale*.74f,onClick={locked=null})
        }
    }
}

@Composable
private fun CollectionTile(index: Int, owned: Boolean, modifier: Modifier, scale: Float, click: ()->Unit) {
    PlaySurface(modifier,22.dp,onClick=click,
        description="${collectionNames[index]}. ${if(owned) "Открыто" else "Откроется после уровня ${20+index*100}"}") {
        BoxWithConstraints(Modifier.fillMaxSize().border((2.5f*scale).dp,Color(0xFFFFFBE8),RoundedCornerShape(22.dp))) {
            val imageHeight=maxHeight*.78f
            Box(Modifier.fillMaxWidth().height(imageHeight).padding(start=(6*scale).dp,end=(6*scale).dp,top=(6*scale).dp)
                .clip(RoundedCornerShape((16*scale).dp))) {
                AnimatedScenery(postcardResources[index],Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                if(!owned) {
                    Box(Modifier.fillMaxSize().background(Color(0x17203548)))
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom=(33*scale).dp).size((39*scale).dp)
                        .clip(CircleShape).background(Color(0xDFFFFFF2)),contentAlignment=Alignment.Center) {
                        Icon(painterResource(R.drawable.game_ic_lock),null,Modifier.size((28*scale).dp),tint=PlayInk)
                    }
                    PlaySurface(Modifier.align(Alignment.BottomCenter).padding(bottom=(6*scale).dp).fillMaxWidth(.89f).height((30*scale).dp),18.dp) {
                        ReferenceText("Уровень ${20+index*100}",Modifier.fillMaxSize(),17*scale)
                    }
                }
            }
            NewContentDot(owned && LocalNotices.current.collectible("location:$index"),Modifier.align(Alignment.TopStart).padding((10*scale).dp))
            if(owned) ReferenceCheck(Modifier.align(Alignment.TopEnd).padding(top=(7*scale).dp,end=(6*scale).dp).size((33*scale).dp))
            ReferenceText(collectionNames[index],Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.21f)
                .padding(horizontal=(5*scale).dp),17*scale)
        }
    }
}

@Composable
private fun StreakFlame(modifier: Modifier) {
    Canvas(modifier) {
        scale(size.width/151f,size.height/167f,pivot=Offset.Zero) {
            val outer=Path().apply {
                moveTo(75f,163f)
                cubicTo(34f,168f,4f,148f,5f,112f)
                cubicTo(3f,88f,13f,69f,27f,56f)
                cubicTo(23f,80f,37f,86f,40f,59f)
                cubicTo(43f,37f,74f,24f,87f,4f)
                cubicTo(111f,17f,122f,44f,114f,65f)
                cubicTo(105f,85f,123f,91f,126f,60f)
                cubicTo(142f,77f,148f,101f,146f,122f)
                cubicTo(143f,150f,118f,166f,75f,163f);close()
            }
            drawPath(outer,Color(0xFFFFF8C8),style=Stroke(9f))
            drawPath(outer,Brush.verticalGradient(listOf(Color(0xFFFFC51B),Color(0xFFFF9600),Color(0xFFFFB800)),startY=0f,endY=165f))
            drawPath(outer,Color(0xFFF38600),style=Stroke(4f))
            val inner=Path().apply {
                moveTo(77f,153f)
                cubicTo(48f,156f,24f,140f,26f,116f)
                cubicTo(27f,103f,36f,89f,44f,83f)
                cubicTo(42f,105f,57f,109f,60f,88f)
                cubicTo(64f,63f,83f,46f,91f,32f)
                cubicTo(100f,55f,96f,78f,86f,96f)
                cubicTo(77f,115f,98f,122f,108f,101f)
                cubicTo(127f,131f,110f,154f,77f,153f);close()
            }
            drawPath(inner,Brush.verticalGradient(listOf(Color(0xFFFFFF8A),Color(0xFFFFFFA6),Color(0xFFFFD03B)),startY=38f,endY=160f))
            drawPath(Path().apply {moveTo(35f,145f);cubicTo(50f,157f,99f,161f,119f,143f)},Color(0x99FFF7A2),style=Stroke(3f))
        }
    }
}

@Composable
private fun AutumnLeaf(modifier: Modifier) {
    Canvas(modifier) {
        scale(size.width/44f,size.height/44f,pivot=Offset.Zero) {
            val leaf=Path().apply {
                moveTo(8f,36f);lineTo(6f,28f);lineTo(1f,22f);lineTo(11f,21f);lineTo(7f,8f)
                lineTo(18f,14f);lineTo(23f,1f);lineTo(27f,13f);lineTo(40f,5f)
                lineTo(35f,21f);lineTo(43f,20f);lineTo(34f,30f);lineTo(23f,33f)
                lineTo(10f,37f);close()
            }
            drawPath(leaf,Brush.linearGradient(listOf(Color(0xFFFFDD31),Color(0xFFFFAA04),Color(0xFFE86800)),Offset(30f,4f),Offset(7f,36f)))
            drawPath(leaf,Color(0xFFE98B00),style=Stroke(.6f))
            drawPath(Path().apply {
                moveTo(4f,42f);lineTo(30f,12f)
                moveTo(13f,31f);lineTo(10f,16f)
                moveTo(20f,24f);lineTo(22f,9f)
                moveTo(14f,30f);lineTo(31f,27f)
                moveTo(22f,21f);lineTo(35f,17f)
            },Color(0xFFB65500),style=Stroke(1f))
        }
    }
}
