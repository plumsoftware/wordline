package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Resolve an unlocked theme without changing the selected postcard background. */
internal fun selectedSceneArtwork(state: AppState, defaultArtwork: Int): Int {
    val wins=state.snapshot?.completions?.size ?: 0
    return when {
        state.settings.theme=="evening" && wins>=50 -> R.drawable.game_lake_evening
        state.settings.theme=="forest" && wins>=100 -> R.drawable.journey_bamboo
        state.settings.theme=="trail" && (state.snapshot?.world?.trailEvents?.size ?: 0)>=30 -> R.drawable.game_autumn_valley
        else -> defaultArtwork
    }
}

@Composable
private fun CardCoordinates(referenceHeight: Float, modifier: Modifier, content: @Composable ReferenceLayout.()->Unit) {
    BoxWithConstraints(modifier) {
        ReferenceLayout(maxWidth,maxHeight,referenceHeight,309f).content()
    }
}

@Composable
fun AppearanceScreen(state: AppState, vm: GameViewModel, back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    val count=snapshot.completions.size
    var more by remember {mutableStateOf(false)}
    val hub=LocalHubNavigation.current
    val notices=LocalNotices.current
    LaunchedEffect(more) {if(more) hub.seen(notices.appearance)}
    var locked by remember {mutableStateOf<String?>(null)}
    val background=state.settings.background.coerceIn(0,(count/100).coerceAtMost(9))
    ReferenceStage(highContrast=state.settings.highContrast,background=hubArtwork(state)) {
        ReferenceBack(frame(12,14,41,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(210,15,98,37),scale)
        GeneratedPageTitle("Оформление",pageTitleFrame())
        ThemeTile("Светлые горы",R.drawable.journey_altai,true,state.settings.theme=="day" && !state.settings.highContrast,
            null,frame(15,152,148,172),scale,!state.busy) {vm.appearance(background,"day")}
        ThemeTile("Вечерние горы",R.drawable.postcard_lake_evening,count>=50,state.settings.theme=="evening" && !state.settings.highContrast,
            "Уровень 50",frame(170,152,148,172),scale,!state.busy) {
            if(count>=50) vm.appearance(background,"evening") else locked="Пройди уровень 50, чтобы открыть вечернее оформление"
        }
        ThemeTile("Лесной край",R.drawable.journey_bamboo,count>=100,state.settings.theme=="forest" && !state.settings.highContrast,
            "Уровень 100",frame(15,336,207,167),scale,!state.busy) {
            if(count>=100) vm.appearance(background,"forest") else locked="Пройди уровень 100, чтобы открыть лесное оформление"
        }
        PlaySurface(frame(41,515,250,31),22.dp) {
            ReferenceText("Открываются за прохождение",Modifier.fillMaxSize().padding(horizontal=8.dp),17*scale,
                weight=androidx.compose.ui.text.font.FontWeight.Normal,maxLines=1)
        }
        PlaySurface(frame(79,568,169,37),23.dp,onClick={more=true},description="Другие темы и фоны") {
            ReferenceText("Другие темы и фоны",Modifier.fillMaxSize().padding(horizontal=8.dp),17*scale,maxLines=1)
            NewContentDot(notices.appearance.any {it.startsWith(notices.prefix+"background:")},Modifier.align(Alignment.TopEnd).offset(3.dp,(-3).dp))
        }
    }
    if(more) ReferenceModal({more=false},526f) {
        Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            ReferenceText("Темы и фоны",Modifier.fillMaxWidth(),28*scale)
            listOf("contrast" to "Высокий контраст","trail" to "Осенняя тропа").forEach { (id,title) ->
                val ready=id=="contrast" || snapshot.world.trailEvents.size>=30
                PlaySurface(Modifier.fillMaxWidth().heightIn(min=64.dp),18.dp,enabled=ready && !state.busy,
                    onClick={vm.appearance(background,id);more=false}) {
                    ReferenceText(if(ready) title else "$title · 30 этапов тропы",Modifier.padding(12.dp),20*scale)
                }
            }
            backgroundResources.forEachIndexed { index,artwork ->
                val ready=count>=index*100
                PlaySurface(Modifier.fillMaxWidth().heightIn(min=88.dp),20.dp,enabled=ready && !state.busy,
                    onClick={vm.appearance(index,"day");more=false}) {
                    Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Image(painterResource(artwork),null,Modifier.size(74.dp).clip(RoundedCornerShape(13.dp)),contentScale=ContentScale.Crop)
                        ReferenceText(collectionNames[index]+if(ready) "" else "\nУровень ${index*100}",Modifier.weight(1f),19*scale,align=TextAlign.Left)
                    }
                }
            }
            ReferenceAction("Готово",Modifier.fillMaxWidth().heightIn(min=50.dp),scale*.8f,onClick={more=false})
        }
    }
    locked?.let { message -> ReferenceModal({locked=null},223f,bottom=false,widthFraction=.92f) {
        ReferenceText("Оформление закрыто",frame(20,20,287,40),26*scale)
        ReferenceText(message,frame(26,75,275,65),20*scale)
        ReferenceAction("Понятно",frame(44,161,239,44),scale*.8f,onClick={locked=null})
    } }
}

@Composable
private fun ThemeTile(title: String, picture: Int, unlocked: Boolean, selected: Boolean, condition: String?,
                       modifier: Modifier, scale: Float, enabled: Boolean, click: ()->Unit) {
    val shape=RoundedCornerShape((23*scale).dp)
    Box(modifier.shadow(if(selected) 10.dp else 3.dp,shape,ambientColor=if(selected) Color(0xFFFFFF35) else Color.Black,
        spotColor=if(selected) Color(0xFF52EF65) else Color.Black)) {
        PlaySurface(Modifier.fillMaxSize().border(if(selected) 3.dp else 1.dp,if(selected) Color(0xFF66E965) else Color(0xFFFFF9E5),shape),23.dp,
            enabled=enabled,onClick=click,description="$title. ${if(selected) "Выбрано" else if(unlocked) "Доступно" else condition.orEmpty()}") {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val imageHeight=maxHeight*.78f
                Box(Modifier.fillMaxWidth().height(imageHeight).padding((5*scale).dp).clip(RoundedCornerShape((15*scale).dp))) {
                    Image(painterResource(picture),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    if(!unlocked) {
                        Box(Modifier.align(Alignment.Center).offset(y=9.dp).size((40*scale).dp).clip(CircleShape).background(Color(0xDFFFFFF2)),contentAlignment=Alignment.Center) {
                            Icon(painterResource(R.drawable.game_ic_lock),null,Modifier.size((29*scale).dp),tint=PlayInk)
                        }
                        PlaySurface(Modifier.align(Alignment.BottomCenter).padding(bottom=5.dp).fillMaxWidth(.63f).height((30*scale).dp),20.dp) {
                            ReferenceText(condition.orEmpty(),Modifier.fillMaxSize(),17*scale,maxLines=1)
                        }
                    }
                }
                if(selected) ReferenceCheck(Modifier.align(Alignment.TopEnd).padding(top=(7*scale).dp,end=(6*scale).dp).size((33*scale).dp))
                ReferenceText(title,Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.21f).padding(horizontal=5.dp),17*scale,maxLines=1)
            }
        }
    }
}

@Composable
fun AchievementsScreen(state: AppState, vm: GameViewModel, back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    val all=achievements(snapshot.world,snapshot.completions.size)
    val first=listOf("ACH04","ACH07","ACH12")
    val ordered=first.map { id ->all.first {it.id==id} }+all.filter {it.id !in first}
    ReferenceStage(686f,highContrast=state.settings.highContrast,background=hubArtwork(state)) {
        ReferenceBack(frame(12,14,42,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(220,15,93,37),scale)
        GeneratedPageTitle("Достижения",pageTitleFrame())
        val sy=height.value/referenceHeight
        Column(frame(9,145,309,438).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy((12*sy).dp)) {
            ordered.forEachIndexed { index,item ->
                val cardHeight=when(index) {0->162f;2->128f;else->124f}
                AchievementCard(item,"achievement:${item.id}" in snapshot.world.claims,state.busy,
                    Modifier.fillMaxWidth().height((cardHeight*sy).dp),cardHeight,index==0) {vm.claimAchievement(item.id)}
            }
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementSpec, claimed: Boolean, busy: Boolean, modifier: Modifier,
                              cardHeight: Float, tall: Boolean, claim: ()->Unit) {
    val ready=item.progress>=item.target
    val title=when(item.id) {"ACH04"->"Первые\n100 слов";"ACH07"->"Без\nподсказок";"ACH12"->"Исследователь";else->item.title}
    val artwork=when(item.id) {"ACH07"->R.drawable.item_medal;"ACH12"->R.drawable.item_compass;else->R.drawable.item_trophy}
    PlaySurface(modifier,28.dp) {
        CardCoordinates(cardHeight,Modifier.fillMaxSize()) {
            Image(painterResource(artwork),null,frame(17,if(tall) 13 else 10,106,if(tall) 111 else 102))
            VictoryLeaves(frame(10,if(tall) 64 else 58,38,43),true)
            VictoryLeaves(frame(88,if(tall) 64 else 58,38,43),false)
            ReferenceText(title,frame(130,15,168,49),if(item.id=="ACH12") 23*scale else 24*scale,align=TextAlign.Left,maxLines=2)
            if(tall) {
                ReferenceProgress(item.progress.toFloat()/item.target,frame(132,74,148,17))
                if(ready) ReferenceCheck(frame(265,67,29,29))
                if(ready && !claimed) AchievementClaim(item.reward,frame(101,105,199,44),scale,!busy,claim)
                else PlaySurface(frame(125,105,173,37),22.dp) {
                    ReferenceText(if(claimed) "Получено" else "${item.progress.coerceAtMost(item.target)}/${item.target}",Modifier.fillMaxSize(),20*scale)
                }
            } else if(ready && !claimed) AchievementClaim(item.reward,frame(126,72,172,41),scale*.87f,!busy,claim)
            else PlaySurface(frame(125,70,173,35),22.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    ReferenceProgress(item.progress.toFloat()/item.target,Modifier.weight(1f).height((20*scale).dp))
                    if(claimed) ReferenceCheck(Modifier.size((25*scale).dp))
                    else ReferenceText("${item.progress.coerceAtMost(item.target)}/${item.target}",size=21*scale,maxLines=1)
                }
            }
        }
    }
}

@Composable
private fun AchievementClaim(reward: Long, modifier: Modifier, scale: Float, enabled: Boolean, claim: ()->Unit) {
    val shape=RoundedCornerShape(26.dp)
    Box(modifier.shadow(3.dp,shape).clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xFF6DF082),Color(0xFF09AE4A),Color(0xFF00873D))))
        .border(2.dp,Color(0xFFFFFFED),shape).clickable(enabled=enabled,role=Role.Button,onClick=claim)
        .semantics(mergeDescendants=true) {contentDescription="Забрать $reward монет"}) {
        Row(Modifier.fillMaxSize().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            ReferenceText("Забрать +$reward",Modifier.weight(1f),23*scale,Color.White,maxLines=1)
            Image(painterResource(R.drawable.item_coin),null,Modifier.size((28*scale).dp))
        }
    }
}

private val StatisticsBadgeShape=androidx.compose.foundation.shape.GenericShape { size,_ ->
    val w=size.width;val h=size.height
    moveTo(0f,.64f*h)
    cubicTo(0f,.31f*h,.16f*w,.14f*h,.35f*w,.06f*h)
    cubicTo(.46f*w,-.015f*h,.53f*w,-.015f*h,.65f*w,.06f*h)
    cubicTo(.84f*w,.14f*h,w,.31f*h,w,.64f*h)
    cubicTo(w,.94f*h,.89f*w,h,.75f*w,h)
    cubicTo(.57f*w,.97f*h,.40f*w,.97f*h,.24f*w,h)
    cubicTo(.10f*w,h,0f,.94f*h,0f,.64f*h);close()
}

@Composable
fun StatisticsScreen(state: AppState, back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    val world=snapshot.world
    val today=LocalDate.parse(world.effectiveDate)
    val dates=(6 downTo 0).map {today.minusDays(it.toLong())}
    val values=dates.map {world.days[it.toString()]?.words ?: 0}
    val max=values.maxOrNull()?.coerceAtLeast(1) ?: 1
    var details by remember {mutableStateOf(false)}
    ReferenceStage(686f,highContrast=state.settings.highContrast,background=hubArtwork(state)) {
        ReferenceBack(frame(12,14,42,38),back)
        ReferenceBalance(snapshot.profile?.coins ?: 0,frame(218,15,95,37),scale)
        GeneratedPageTitle("Твой путь",pageTitleFrame())
        Box(frame(18,139,289,171).shadow(4.dp,StatisticsBadgeShape).clip(StatisticsBadgeShape)
            .background(Brush.verticalGradient(PlayPaper)).clickable(role=Role.Button,onClick={details=true}))
        Icon(painterResource(R.drawable.home_mountain),null,frame(118,152,91,34),tint=Color(0xFF005665))
        ReferenceText(NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(world.totalWords),frame(91,189,145,69),68*scale,maxLines=1)
        ReferenceText("найдено слов",frame(83,259,161,29),22*scale,maxLines=1)
        Laurel(frame(31,190,56,105),false)
        Laurel(frame(240,190,56,105),true)
        val data=listOf(Triple(snapshot.completions.size,"уровня",R.drawable.pref_ic_layers),
            Triple(world.streak,"дней\nподряд",R.drawable.game_ic_calendar),Triple(world.bonusWords,"бонусных\nслов",R.drawable.pref_ic_sparkle))
        data.forEachIndexed { index,(value,label,icon) ->
            PlaySurface(frame(5+index*107,319,102,117),24.dp) {
                Column(Modifier.fillMaxSize().padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Icon(painterResource(icon),null,Modifier.size((30*scale).dp),tint=Color(0xFF086B71))
                    ReferenceText(value.toString(),Modifier.fillMaxWidth(),32*scale,maxLines=1)
                    ReferenceText(label,Modifier.fillMaxWidth(),16*scale)
                }
            }
        }
        PlaySurface(frame(6,450,314,178),29.dp) {}
        ReferenceText("Активность за неделю",frame(31,466,264,26),22*scale,maxLines=1)
        Row(frame(24,501,280,84),horizontalArrangement=Arrangement.spacedBy((11*scale).dp),verticalAlignment=Alignment.Bottom) {
            values.forEachIndexed { index,value ->
                val label="${dates[index]}: $value слов"
                Box(Modifier.weight(1f).fillMaxHeight().semantics {contentDescription=label}) {
                    if(value>0) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(value.toFloat()/max)
                        .clip(RoundedCornerShape(topStart=(8*scale).dp,topEnd=(8*scale).dp,bottomStart=3.dp,bottomEnd=3.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF67DC85),Color(0xFF0AB347)))))
                }
            }
        }
        Row(frame(24,591,280,23),horizontalArrangement=Arrangement.spacedBy((11*scale).dp)) {
            dates.forEach { date ->
                ReferenceText(date.format(DateTimeFormatter.ofPattern("EE",Locale.forLanguageTag("ru"))).replaceFirstChar {it.uppercase()},Modifier.weight(1f).fillMaxHeight(),16*scale)
            }
        }
        if(values.all {it==0}) ReferenceText("Пока нет активности",frame(38,525,251,30),17*scale)
    }
    if(details) ReferenceModal({details=false},376f,bottom=false,widthFraction=.94f) {
        ReferenceText("Статистика",frame(20,20,287,42),29*scale)
        ReferenceText("Найденные слова: ${world.totalWords}\nУникальные слова: ${world.uniqueWords.size}\nБонусные слова: ${world.bonusWords}\nБонусные сундуки: ${world.chests}\nПодсказки: ${world.hints}\nЛучшая серия: ${world.bestStreak} дней\n\nНаходки и уникальные слова — разные показатели.",
            frame(28,81,271,213),18*scale,align=TextAlign.Left)
        ReferenceAction("Понятно",frame(43,315,241,45),scale*.8f,onClick={details=false})
    }
}

@Composable
private fun Laurel(modifier: Modifier, mirrored: Boolean) {
    BoxWithConstraints(modifier.graphicsLayer {scaleX=if(mirrored) -1f else 1f}) {
        val w=maxWidth;val h=maxHeight
        repeat(5) { index ->
            Image(painterResource(R.drawable.item_leaf_token),null,
                Modifier.offset(w*(.04f+index*.08f),h*(index*.16f)).size(w*.63f,h*.3f).rotate(-35f+index*13))
        }
    }
}

@Composable
fun UnlockScreen(index: Int, done: ()->Unit) {
    val location=index.coerceIn(0,9)
    BackHandler(onBack=done)
    ReferenceStage(686f,background=backgroundResources[location],backgroundTopFraction=if(location==1) .07f else .12f) {
        ReferenceGlow(frame(13,0,301,237))
        PlaySurface(frame(69,66,189,80),40.dp) {}
        ReferenceText("Открыт\nновый мир",frame(66,67,195,72),31*scale)
        VictoryLeaves(frame(37,106,57,58),true)
        VictoryLeaves(frame(238,106,57,58),false)
        PlaySurface(frame(30,190,270,69),40.dp) {ReferenceText(collectionNames[location],Modifier.fillMaxSize().padding(horizontal=14.dp),34*scale,maxLines=1)}
        Box(frame(62,247,207,185).rotate(3f).shadow(4.dp,RoundedCornerShape(18.dp)).background(Cream,RoundedCornerShape(18.dp)))
        Box(frame(54,254,222,181).rotate(-5f).shadow(7.dp,RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Cream).padding((7*scale).dp)) {
            Image(painterResource(postcardResources[location]),collectionNames[location],Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp)),contentScale=ContentScale.Crop)
        }
        UnlockSparkles(frame(0,0,327,686))
        VictoryButton("Отправиться",false,frame(33,478,264,65),scale,true,done,textSize=32f)
        PlaySurface(frame(62,560,206,51),31.dp,onClick=done) {ReferenceText("Позже",Modifier.fillMaxSize(),25*scale)}
    }
}

@Composable
private fun UnlockSparkles(modifier: Modifier) {
    Canvas(modifier) {
        val points=listOf(19f to 108f,39f to 70f,26f to 183f,299f to 167f,310f to 83f,19f to 375f,298f to 408f,22f to 579f,304f to 560f,283f to 48f)
        points.forEachIndexed { i,(x,y) ->
            val p=Offset(size.width*x/327f,size.height*y/686f)
            val r=(if(i%3==0) 6f else 4f)*size.width/327f
            drawPath(Path().apply {
                moveTo(p.x,p.y-r);lineTo(p.x+r*.35f,p.y-r*.2f);lineTo(p.x+r,p.y)
                lineTo(p.x+r*.2f,p.y+r*.3f);lineTo(p.x,p.y+r);lineTo(p.x-r*.3f,p.y+r*.15f)
                lineTo(p.x-r,p.y);lineTo(p.x-r*.2f,p.y-r*.3f);close()
            },Color(0xFFFFE767))
        }
    }
}
