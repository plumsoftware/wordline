package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.R

@Composable
internal fun OfflineNotice(modifier: Modifier,scale: Float) {
    Column(modifier.semantics {liveRegion=LiveRegionMode.Polite},horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy((5*scale).dp)) {
        PlaySurface(Modifier.width((184*scale).dp).height((33*scale).dp),22.dp) {
            Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                Icon(painterResource(R.drawable.game_ic_wifi_off),null,Modifier.size((29*scale).dp),tint=Color(0xFFCD243F))
                Spacer(Modifier.width((9*scale).dp))
                ReferenceText("Без интернета",size=16*scale,maxLines=1)
            }
        }
        ReferenceText("Можно продолжать играть",Modifier.fillMaxWidth().height((19*scale).dp),14*scale,Color.White,FontWeight.Bold,maxLines=1)
    }
}

@Composable
internal fun OfflineVideoFooter(modifier: Modifier,scale: Float) {
    val shape=RoundedCornerShape(50)
    Row(modifier.shadow(3.dp,shape).clip(shape).background(Brush.verticalGradient(listOf(Color(0xFFC3B1DA),Color(0xFF977CBA))))
        .border((2*scale).dp,Color(0xFFE9DFEF),shape).semantics(mergeDescendants=true) {disabled()},
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
        PlaySurface(Modifier.size((30*scale).dp,(24*scale).dp),6.dp) {
            Icon(painterResource(R.drawable.game_ic_play),null,Modifier.size((20*scale).dp),tint=Color(0xFF9875BA))
        }
        Spacer(Modifier.width((10*scale).dp))
        ReferenceText("Видео недоступно",size=17*scale,color=Color(0xFFF7F2FB),maxLines=1)
    }
}
