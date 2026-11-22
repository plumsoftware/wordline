package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.Level
import ru.plumsoftware.wordline.game.Session
import kotlin.math.roundToInt

@Composable
internal fun ZoomableCrossword(level: Level,session: Session,selection: String,large: Boolean,modifier: Modifier,
                              minimumTile: Dp=0.dp) {
    val big=level.columns>=6 || level.rows>=6
    if(!big) {ReferenceBoard(level,session,selection,large,modifier,minimumTile);return}
    var zoom by rememberSaveable(session.id) {mutableFloatStateOf(if(level.columns>=7 || level.rows>=7) 1.25f else 1f)}
    BoxWithConstraints(modifier.clipToBounds()) {
        val fit=minOf((maxWidth-24.dp)/level.columns,(maxHeight-44.dp).coerceAtLeast(1.dp)/level.rows,maxWidth*(44f/327f))
        val tile=maxOf(minimumTile,fit,28.dp)*zoom
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(44.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.End) {
                ReferenceText("${(zoom*100).roundToInt()}%",Modifier.width(52.dp),15f)
                PlaySurface(Modifier.size(40.dp),16.dp,enabled=zoom>1f,onClick={zoom=(zoom-.25f).coerceAtLeast(1f)},description="Уменьшить кроссворд") {
                    Icon(painterResource(R.drawable.game_ic_zoom_out),null,Modifier.size(25.dp),tint=PlayInk)
                }
                Spacer(Modifier.width(7.dp))
                PlaySurface(Modifier.size(40.dp),16.dp,enabled=zoom<2.5f,onClick={zoom=(zoom+.25f).coerceAtMost(2.5f)},description="Увеличить кроссворд") {
                    Icon(painterResource(R.drawable.game_ic_zoom_in),null,Modifier.size(25.dp),tint=PlayInk)
                }
                Spacer(Modifier.width(12.dp))
            }
            ReferenceBoard(level,session,selection,large,Modifier.weight(1f).fillMaxWidth(),minimumTile=tile)
        }
    }
}
