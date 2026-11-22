package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*

@Composable
fun WelcomeScreen(busy: Boolean, onStart: ()->Unit, onSkip: ()->Unit, onPrivacy: ()->Unit = {}) {
    ReferenceStage(background=R.drawable.welcome_lake_reference) {
        JourneyWordmark(frame(27,77,273,139))
        WelcomeSlogan(frame(54,240,219,93))
        Box(frame(53,368,222,53),contentAlignment=Alignment.Center) {
            Row(Modifier.fillMaxWidth().height((53*scale).dp),horizontalArrangement=Arrangement.spacedBy((4*scale).dp)) {
                "ЛИСТ".forEach { letter -> PlaySurface(Modifier.weight(1f).fillMaxHeight(),(13*scale).dp) {
                    WelcomeLetter(letter,Modifier.fillMaxSize())
                } }
            }
        }
        Box(frame(47,450,233,63),contentAlignment=Alignment.Center) {
            ReferenceAction("Начать",Modifier.fillMaxWidth().height((63*scale).dp),scale,enabled=!busy,onClick=onStart)
        }
        Box(frame(80,530,168,45).semantics {
            customActions=listOf(CustomAccessibilityAction("Конфиденциальность") {onPrivacy();true})
        },contentAlignment=Alignment.Center) {
            ReferenceAction("Пропустить",Modifier.fillMaxWidth().height((45*scale).dp),scale,enabled=!busy,cream=true,onClick=onSkip)
        }

    }
}

@Composable
internal fun TutorialScene(state: AppState, level: Level, session: Session, enabled: Boolean, inputEpoch: Int,
                           back: ()->Unit, submit: (String)->Unit, shuffle: ()->Unit, dragging: (Boolean)->Unit, understood: ()->Unit) {
    val word=level.slots.first().word
    val guide=remember(level) {
        val available=level.letters.indices.toMutableList()
        word.mapNotNull { letter -> available.firstOrNull {level.letters[it]==letter}?.also {available.remove(it)} }
    }
    ReferenceStage(highContrast=state.settings.highContrast) {
        ReferenceBack(frame(13,14,42,37),back)
        PlaySurface(frame(104,16,114,33),22.dp) {ReferenceText("Уровень ${session.levelId}",Modifier.fillMaxSize(),16*scale)}
        ReferenceBalance(state.snapshot?.profile?.coins ?: 0,frame(235,16,81,33),scale*.88f)
        ReferenceBoard(level,session,"",state.settings.largeLetters,frame(50,103,227,104))
        PlaySurface(frame(23,229,281,79),32.dp) {
            Column(Modifier.fillMaxSize().padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                ReferenceText("Соедини буквы",size=28*scale)
                ReferenceText("Проведи пальцем: ${word.toList().joinToString(" → ")}",size=17*scale)
            }
        }
        LetterWheel(level,session,state.settings.tapInput,state.settings.largeLetters,enabled,inputEpoch,submit,shuffle,dragging,
            frame(51,314,225,225),guide=guide)
        Image(painterResource(R.drawable.tutorial_hand),null,frame(176,501,66,55))
        ReferenceAction("Понятно",frame(55,557,217,53),scale*.8f,onClick=understood)
    }
}
