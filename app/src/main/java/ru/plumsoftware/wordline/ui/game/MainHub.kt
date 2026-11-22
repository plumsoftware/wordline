package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*

internal data class HubNavigation(val go: (String)->Unit={},val seen: (Set<String>)->Unit={})
internal val LocalHubNavigation=staticCompositionLocalOf {HubNavigation()}
internal val LocalNotices=staticCompositionLocalOf {HubNotices()}

@Composable
internal fun GeneratedPageTitle(title: String,modifier: Modifier) {
    val resource=when(title) {
        "Оформление"->R.drawable.title_appearance_hq
        "Достижения"->R.drawable.title_achievements_hq
        "Твой путь"->R.drawable.title_statistics_hq
        else->R.drawable.title_collection_hq
    }
    val bitmap=ImageBitmap.imageResource(resource)
    // Remove the generated transparent margins at draw time; keep each plaque at the same size.
    val bounds=when(resource) {
        R.drawable.title_appearance_hq->listOf(14,19,2159,640)
        R.drawable.title_achievements_hq->listOf(6,39,2167,637)
        R.drawable.title_statistics_hq->listOf(14,14,2159,660)
        else->listOf(17,64,2154,643)
    }
    val painter=remember(bitmap,resource) {BitmapPainter(bitmap,IntOffset(bounds[0],bounds[1]),IntSize(bounds[2]-bounds[0],bounds[3]-bounds[1]))}
    Image(painter,title,modifier,contentScale=ContentScale.FillBounds)
}
internal fun ReferenceLayout.pageTitleFrame()=Modifier
    .offset(x=width*(10f/327f),y=height*(59f/referenceHeight)-width*(3.6f/327f))
    .size(width*(307f/327f),width*(79.2f/327f))
internal data class HubNotices(val prefix: String="",val tasks: Set<String> = emptySet(),val gift: Set<String> = emptySet(),
    val achievements: Set<String> = emptySet(),val appearance: Set<String> = emptySet(),val collection: Set<String> = emptySet()) {
    fun collectible(id: String)=prefix+"collectible:$id" in collection
}
internal fun hubArtwork(state: AppState): Int {
    val wins=state.snapshot?.completions?.size ?: 0
    return selectedSceneArtwork(state,backgroundResources[state.settings.background.coerceIn(0,(wins/100).coerceAtMost(9))])
}

private fun notices(state: AppState): HubNotices {
    val snapshot=state.snapshot ?: return HubNotices()
    val world=snapshot.world;val wins=snapshot.completions.size;val prefix="${world.generation}|"
    fun unread(keys: Iterable<String>)=keys.map {prefix+it}.filterNot {it in state.settings.seenContent}.toSet()
    return HubNotices(prefix,
        unread(tasks(world,world.effectiveDate,wins==state.campaign?.levels?.size).filter {it.progress>=it.target && "task:${world.effectiveDate}:${it.id}" !in world.claims}.map {"task:${world.effectiveDate}:${it.id}"}),
        unread(if("gift:${world.effectiveDate}" !in world.claims) listOf("gift:${world.effectiveDate}") else emptyList()),
        unread(achievements(world,wins).filter {it.progress>=it.target && "achievement:${it.id}" !in world.claims}.map {"achievement:${it.id}"}),
        unread(buildList {if(wins>=50) add("theme:evening");if(wins>=100) add("theme:forest");if(world.trailEvents.size>=30) add("theme:trail");for(i in 1..(wins/100).coerceAtMost(9)) add("background:$i")}),
        unread(world.collectibles.map {"collectible:$it"}))
}

@Composable
internal fun NewContentDot(visible: Boolean,modifier: Modifier=Modifier) {
    if(!visible) return
    val pulse=celebrationPulse(true)
    Box(modifier.size(11.dp).graphicsLayer {scaleX=pulse.value;scaleY=pulse.value}
        .shadow(2.dp,CircleShape).background(Color(0xFFF34555),CircleShape).border(1.5.dp,Color(0xFFFFF8E7),CircleShape)
        .semantics {contentDescription="Есть новое"})
}

@Composable
internal fun HubFeatureShortcuts(modifier: Modifier,scale: Float=1f) {
    val hub=LocalHubNavigation.current;val notices=LocalNotices.current
    Row(modifier,horizontalArrangement=Arrangement.spacedBy((6*scale).dp)) {
        listOf(Triple("Достижения",R.drawable.hub_achievements_soft,"achievements"),Triple("Статистика",R.drawable.hub_statistics_soft,"statistics"),
            Triple("Оформление",R.drawable.hub_appearance_soft,"appearance")).forEach { (title,icon,route) ->
            PlaySurface(Modifier.weight(1f).fillMaxHeight(),(16*scale).dp,onClick={hub.go(route)}) {
                Column(Modifier.fillMaxSize().padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Box(Modifier.size((29*scale).dp)) {
                        Image(painterResource(icon),null,Modifier.fillMaxSize())
                        NewContentDot(when(route) {"achievements"->notices.achievements.isNotEmpty();"appearance"->notices.appearance.isNotEmpty();else->false},Modifier.align(Alignment.TopEnd).offset(4.dp,(-3).dp))
                    }
                    ReferenceText(title,Modifier.fillMaxWidth(),12*scale,maxLines=1)
                }
            }
        }
    }
}

@Composable
internal fun HubFrame(state: AppState,route: String,navigate: (String)->Unit,markSeen: (Set<String>)->Unit,content: @Composable (Modifier)->Unit) {
    val notices=notices(state)
    val marks=when {
        route=="tasks"->notices.tasks
        route=="gift"->notices.gift
        route=="achievements"->notices.achievements
        route=="appearance"->notices.appearance.filterNot {it.startsWith(notices.prefix+"background:")}.toSet()
        route=="postcard/{index}" || route=="extraPostcard/{id}"->emptySet() // An individual card acknowledges itself.
        else->emptySet()
    }
    LaunchedEffect(route,marks) {if(marks.isNotEmpty()) markSeen(marks)}
    val tab=when(route) {"home"->0;"tasks"->1;"collection","achievements","statistics","appearance"->2;else->-1}
    CompositionLocalProvider(LocalHubNavigation provides HubNavigation(navigate,markSeen),LocalNotices provides notices) {
        BoxWithConstraints(Modifier.widthIn(max=1000.dp).fillMaxSize()) {
            val scale=(maxWidth.value/327f).coerceAtMost(1.6f)
            Column(Modifier.fillMaxSize()) {
                content(Modifier.weight(1f).fillMaxWidth())
                if(tab>=0) MainTabs(tab,notices,Modifier.fillMaxWidth().height((72*scale).dp),scale,navigate)
            }
        }
    }
}

@Composable
private fun MainTabs(selected: Int,notices: HubNotices,modifier: Modifier,scale: Float,navigate: (String)->Unit) {
    val shape=RoundedCornerShape(topStart=(26*scale).dp,topEnd=(26*scale).dp)
    Row(modifier.shadow(4.dp,shape).clip(shape).background(Brush.verticalGradient(PlayPaper)).selectableGroup()
        .padding(horizontal=(5*scale).dp,vertical=(7*scale).dp),verticalAlignment=Alignment.CenterVertically) {
        listOf(Triple("Путь",R.drawable.home_nav_path,"home"),Triple("Задания",R.drawable.game_ic_tasks,"tasks"),Triple("Коллекция",R.drawable.home_leaf,"collection")).forEachIndexed {index,(title,icon,route) ->
            Box(Modifier.weight(1f).fillMaxHeight(),contentAlignment=Alignment.Center) {
                Column(Modifier.width((84*scale).dp).fillMaxHeight().clip(RoundedCornerShape((17*scale).dp))
                    .then(if(index==selected) Modifier.background(Brush.verticalGradient(PlayMint)) else Modifier)
                    .selectable(index==selected,role=Role.Tab,onClick={navigate(route)}),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Box(Modifier.size((31*scale).dp)) {
                        Icon(painterResource(icon),null,Modifier.fillMaxSize(),tint=PlayInk)
                        val dot=when(index) {0->notices.gift.isNotEmpty();1->notices.tasks.isNotEmpty();else->notices.collection.isNotEmpty() || notices.achievements.isNotEmpty() || notices.appearance.isNotEmpty()}
                        NewContentDot(dot,Modifier.align(Alignment.TopEnd).offset(5.dp,(-1).dp))
                    }
                    ReferenceText(title,size=15*scale,maxLines=1)
                }
            }
        }
    }
}
