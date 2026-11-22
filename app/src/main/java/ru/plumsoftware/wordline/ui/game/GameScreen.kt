package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.data.Setting
import ru.plumsoftware.wordline.game.*

@Composable
fun GameScreen(state: AppState, session: Session, onBack: () -> Unit, onMap: () -> Unit,
               onSubmit: (String) -> Unit, onShuffle: () -> Unit, onHint: (Hint,String) -> Unit,
               onActive: (Boolean)->Unit = {}, onVideo: (Hint,String)->Unit = {_,_->}, online: Boolean = true,
               changeSetting: (Setting,Boolean)->Unit = {_,_->}, adPhase: String = "idle", onClueRead: ()->Unit = {}) {
    val campaign=requireNotNull(state.campaign)
    val level=campaign.levelFor(session)
    var pause by rememberSaveable(session.id) { mutableStateOf(false) }
    var bonus by rememberSaveable(session.id) { mutableStateOf(false) }
    var hintKind by remember(session.id) { mutableStateOf<Hint?>(null) }
    // Keep this baseline above the idle-only panel: advertising temporarily removes that panel.
    val hintVideoReceived=rememberRewardReceived(state,"hint",session.id,hintKind)
    var inputEpoch by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var tutorialDismissed by rememberSaveable(session.id) { mutableStateOf(false) }
    val owner=LocalLifecycleOwner.current
    val focused=LocalWindowInfo.current.isWindowFocused
    val clueNotice=state.clueNotice?.takeIf {it.sessionId==session.id}
    DisposableEffect(owner,session.id) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_STOP) {inputEpoch++;dragging=false}
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(focused) {if(!focused) {inputEpoch++;dragging=false}}
    fun showPause() {inputEpoch++;pause=true}
    BackHandler {
        when {
            clueNotice!=null -> onClueRead()
            hintKind!=null -> hintKind=null
            bonus -> bonus=false
            else -> {inputEpoch++;pause=!pause}
        }
    }
    val enabled=adPhase=="idle" && !state.busy && state.rewardOpening==null && !pause && !bonus && hintKind==null && clueNotice==null && !session.completed && focused
    SideEffect {onActive(focused && state.rewardOpening==null && !pause && !bonus && hintKind==null && clueNotice==null && !session.completed)}
    LaunchedEffect(session.solved) {
        if(session.solved.isNotEmpty()) {
            if(session.mode==Mode.TUTORIAL && !tutorialDismissed && !state.settings.reduceMotion) {
                kotlinx.coroutines.delay((300L+(level.slots.maxOfOrNull {it.word.length} ?: 1)*70L).coerceAtMost(1600L))
            }
            tutorialDismissed=true
        }
    }
    if(session.mode==Mode.TUTORIAL && !tutorialDismissed) {
        TutorialScene(state,level,session,enabled,inputEpoch,::showPause,onSubmit,onShuffle,{dragging=it}) {tutorialDismissed=true;inputEpoch++}
    } else {
        PlayScene(state,level,session,enabled,inputEpoch,online,
            onBack=::showPause,onBonus={inputEpoch++;bonus=true},onSubmit=onSubmit,
            onShuffle=onShuffle,onDragging={dragging=it},onHint={inputEpoch++;hintKind=it},adVisible=adPhase!="idle")
    }
    if(adPhase=="idle") {
    if(pause) PausePanel(state,session.mode==Mode.DAILY,{pause=false},{pause=false;onMap()},changeSetting)
    if(bonus) BonusWordsPanel(state,level,session) {bonus=false}
    hintKind?.let { kind -> HintPanel(state,level,session,kind,
        dismiss={hintKind=null},confirm={target ->hintKind=null;onHint(kind,target)},
        video=if(session.replay || session.mode==Mode.TUTORIAL) null else {target ->onVideo(kind,target)},videoReceived=hintVideoReceived) }
    clueNotice?.let {ClueReadPanel(it.text,onClueRead)}
    }
}
