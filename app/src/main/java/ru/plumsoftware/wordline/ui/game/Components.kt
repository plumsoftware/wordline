package ru.plumsoftware.wordline.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import java.text.NumberFormat
import java.util.Locale

val Ink=Color(0xFF10223F)
val Cream=Color(0xFFFFF8E8)
val Green=Color(0xFF087B3E)
val PaleGreen=Color(0xFFC7F3D6)
val Gold=Color(0xFFF4B43B)
val Mist=Color(0xFFE7F0E9)

@Composable
fun GameTheme(highContrast: Boolean, theme: String = "day", reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGameMotion provides !reduceMotion) {
    MaterialTheme(colorScheme=lightColorScheme(primary=if(theme=="evening") Color(0xFF3B508C) else Green,onPrimary=Color.White,
        primaryContainer=PaleGreen,onPrimaryContainer=Ink,background=Cream,
        surface=if(highContrast) Color.White else Cream,onSurface=Ink,onBackground=Ink,
        secondary=Color(0xFF6520AF),onSecondary=Color.White,outline=Color(0xFF586B72)), content=content)
    }
}
@Composable
fun Landscape(highContrast: Boolean, background: Int = 0, theme: String = "day", animate: Boolean=true, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Cream)) {
        if(!highContrast) {
            AnimatedScenery(if(theme=="evening") R.drawable.game_lake_evening else backgroundResources[background.coerceIn(0,9)],Modifier.fillMaxSize(),contentScale=ContentScale.Crop,animate=animate)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.18f),Color.Transparent,Cream.copy(alpha=.93f)))))
        }
        content()
    }
}
@Composable
fun GameIcon(@DrawableRes icon: Int, description: String? = null, modifier: Modifier = Modifier, tint: Color = Ink) {
    Icon(painterResource(icon),description,modifier.size(24.dp),tint=tint)
}
@Composable
fun ToolButton(@DrawableRes icon: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,shadowElevation=2.dp) {
        IconButton(onClick=onClick,enabled=enabled,modifier=Modifier.size(48.dp)) { GameIcon(icon,description) }
    }
}
@Composable
fun Balance(coins: Long) {
    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,modifier=Modifier.clearAndSetSemantics {
        contentDescription="${NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(coins)} монет"
    }) {
        Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            GameIcon(R.drawable.game_ic_coin,tint=Color(0xFF95600A))
            Text(NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru")).format(coins),fontWeight=FontWeight.Bold)
        }
    }
}
@Composable
fun MainButton(text: String, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick=onClick,enabled=enabled,modifier=modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(20.dp),contentPadding=PaddingValues(16.dp)) {
        Text(text,fontSize=18.sp,fontWeight=FontWeight.Bold)
    }
}
@Composable
fun Paper(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier=modifier,shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,shadowElevation=2.dp) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable
fun PageHeader(title: String, coins: Long? = null, back: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top=gameHeaderClearance()).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        ToolButton(R.drawable.game_ic_back,"Назад",onClick=back)
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        if(coins!=null) Balance(coins)
    }
}
@Composable
fun StarRating(stars: Int, modifier: Modifier = Modifier) {
    Row(modifier.clearAndSetSemantics { contentDescription="Звёзды: $stars из 3" },horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        repeat(3) { index -> GameIcon(R.drawable.game_ic_star,modifier=Modifier.size(32.dp),tint=if(index<stars) Color(0xFF95600A) else Color(0xFF78858C)) }
    }
}
@Composable
fun CrosswordBoard(level: Level, session: Session, largeLetters: Boolean, modifier: Modifier = Modifier,
                   onCell: ((Cell)->Unit)? = null, selectedCell: String? = null) {
    val cells=remember(level) { level.cells }
    val visible=remember(level,session.solved,session.revealed) { visibleCells(level,session) }
    val description=level.slots.joinToString(". ") { slot ->
        val mask=slot.cells.mapIndexed { i,c -> if(c.key in visible) slot.word[i].toString() else "пропуск" }.joinToString(" ")
        "Слово ${slot.id.drop(1)}, ${if(slot.direction==Direction.ACROSS) "по горизонтали" else "по вертикали"}: $mask"
    }
    BoxWithConstraints(modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
        val density=LocalDensity.current
        val largeScale=density.fontScale>1.5f
        val tile=if(largeScale) with(density) { (if(largeLetters) 30.sp else 26.sp).toDp() }+16.dp else if(onCell!=null) 48.dp else (maxWidth/level.columns).coerceAtMost(48.dp)
        Box(Modifier.horizontalScroll(rememberScrollState())) {
        Column(if(onCell==null) Modifier.clearAndSetSemantics { contentDescription=description } else Modifier,verticalArrangement=Arrangement.spacedBy(0.dp)) {
            repeat(level.rows) { row ->
                Row {
                    repeat(level.columns) { col ->
                        val cell=Cell(row,col);val ch=cells[cell];val open=cell.key in visible
                        Box(Modifier.size(tile).then(if(onCell!=null && ch!=null) Modifier.clickable {onCell(cell)}.semantics {contentDescription="Строка ${row+1}, столбец ${col+1}, ${if(open) "открыта" else "пустая"}";selected=selectedCell==cell.key} else Modifier).padding(2.dp).then(if(ch!=null) Modifier
                            .clip(RoundedCornerShape(9.dp)).background(if(open) PaleGreen else MaterialTheme.colorScheme.surface)
                            .border(if(open || selectedCell==cell.key) 2.dp else 1.dp,if(selectedCell==cell.key) Ink else if(open) Green else Color(0xFF7E9091),RoundedCornerShape(9.dp)) else Modifier),contentAlignment=Alignment.Center) {
                            if(ch!=null && open) Text(ch.toString(),color=Ink,fontSize=if(largeLetters) 26.sp else 22.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }
    }
}
