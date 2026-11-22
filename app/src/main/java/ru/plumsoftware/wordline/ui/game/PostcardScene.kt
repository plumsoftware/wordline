package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.R

/** S20: the illustration fills the screen, the live controls occupy its lower panel. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostcardScreen(state: AppState, index: Int, vm: GameViewModel, back: ()->Unit) {
    val title=collectionNames[index]
    val picture=postcardResources[index]
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        vm.exportPostcard(uri,picture,title)
    }
    val save: ()->Unit = {if(!state.busy) export.launch("connected-words-postcard-${index+1}.png")}
    val background=backgroundResources[index]
    ReferenceStage(627f,background=background,
        backgroundTopFraction=if(index==0) .17f else .10f) {
        ReferenceBack(frame(12,14,41,38),back)
        ReferenceBalance(state.snapshot?.profile?.coins ?: 0,frame(215,15,96,37),scale)
        Box(frame(0,58,327,315).semantics {contentDescription="Открытка: $title"})

        val panelShape=RoundedCornerShape(topStart=(44*scale).dp,topEnd=(44*scale).dp,
            bottomStart=(25*scale).dp,bottomEnd=(25*scale).dp)
        Box(frame(8,374,311,249).shadow(5.dp,panelShape).clip(panelShape)
            .background(Brush.verticalGradient(PlayPaper)).border(1.dp,Color(0xFFFFFCE9),panelShape))
        ReferenceText(title,frame(49,391,229,42),32*scale)
        VictoryLeaves(frame(7,394,44,50),true)
        VictoryLeaves(frame(277,394,44,50),false)
        PlaySurface(frame(54,436,218,36),23.dp) {
            ReferenceText("Открыто на уровне ${20+index*100}",Modifier.fillMaxSize().padding(horizontal=9.dp),18*scale)
        }
        VictoryButton("Сделать фоном",false,frame(26,493,275,56).semantics {
            stateDescription=if(state.settings.background==index) "Этот фон выбран" else "Применить фон открытки"
        },scale,enabled=!state.busy,action={vm.appearance(index,state.settings.theme)},textSize=28f)

        PlaySurface(frame(30,560,267,48).combinedClickable(
            enabled=!state.busy,role=Role.Button,onClickLabel="Поделиться открыткой",
            onClick={vm.share(picture,title)},onLongClickLabel="Сохранить открытку в файл",onLongClick=save
        ).semantics {
            customActions=listOf(CustomAccessibilityAction("Сохранить открытку в файл") {save();true})
        },30.dp) {
            Row(Modifier.fillMaxSize().padding(horizontal=(24*scale).dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy((13*scale).dp,Alignment.CenterHorizontally)) {
                Icon(painterResource(R.drawable.game_ic_share),null,Modifier.size((29*scale).dp),tint=PlayInk)
                ReferenceText("Поделиться",Modifier.weight(1f,fill=false),23*scale)
            }
        }
    }
}
