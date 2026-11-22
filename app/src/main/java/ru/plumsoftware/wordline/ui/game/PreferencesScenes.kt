package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.data.Setting
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun ReferenceLayout.PreferencesHeader(title: String, icon: Int, back: ()->Unit,
                                               iconAction: (()->Unit)? = null, actionLabel: String = title) {
    ReferenceBack(frame(15,14,42,38),back)
    PlaySurface(frame(132,63,65,66),40.dp,onClick=iconAction,description=if(iconAction!=null) actionLabel else null) {}
    ReferenceTitle(title,frame(34,103,259,59),scale,32f)
    Icon(painterResource(icon),null,frame(144,73,41,41),tint=if(icon==R.drawable.settings_hq) Color.Unspecified else PlayInk)
}

@Composable
internal fun SettingsItem(label: String, icon: Int, modifier: Modifier, scale: Float,
                         enabled: Boolean = true, value: Boolean? = null, click: ()->Unit = {}, change: (Boolean)->Unit = {}) {
    Row(modifier.then(if(value==null) Modifier.clickable(enabled=enabled,role=Role.Button,onClick=click) else Modifier),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy((17*scale).dp)) {
        Icon(painterResource(icon),null,Modifier.size((32*scale).dp),tint=PlayInk)
        ReferenceText(label,Modifier.weight(1f),21*scale,align=TextAlign.Left,maxLines=label.count {it=='\n'}+1)
        if(value!=null) ReferenceToggle(value,enabled,Modifier.size((61*scale).dp,(48*scale).dp)
            .semantics {contentDescription=label},change)
        else Icon(painterResource(R.drawable.game_ic_chevron_right),null,Modifier.size((21*scale).dp),tint=PlayInk)
    }
}

@Composable
fun SettingsScreen(state: AppState, back: ()->Unit, change: (Setting,Boolean)->Unit, help: ()->Unit,
                   save: ()->Unit = {}, about: ()->Unit = {}, accessibility: ()->Unit = {}) {
    var videoSettings by remember {mutableStateOf(false)}
    ReferenceStage(highContrast=state.settings.highContrast,background=R.drawable.game_lake_day,backgroundTopFraction=.17f) {
        PreferencesHeader("Настройки",R.drawable.settings_hq,back,{videoSettings=true},"Настройки видеорекламы")
        PlaySurface(frame(15,179,300,385),31.dp) {}
        SettingsItem("Музыка",R.drawable.game_ic_music,frame(43,191,250,51),scale,!state.busy,state.settings.music,
            change={change(Setting.MUSIC,it)})
        SettingsItem("Звуки",R.drawable.game_ic_sound_on,frame(43,247,250,51),scale,!state.busy,state.settings.effects,
            change={change(Setting.EFFECTS,it)})
        SettingsItem("Вибрация",R.drawable.game_ic_vibration,frame(43,303,250,51),scale,!state.busy,state.settings.vibration,
            change={change(Setting.VIBRATION,it)})
        listOf(243,298,360,412,459,507).forEach { y ->
            Box(frame(37,y,256,1).background(Color(0xFFD4C8B0)))
        }
        SettingsItem("Удобство",R.drawable.game_ic_accessibility,frame(43,364,249,46),scale,click=accessibility)
        SettingsItem("Сохранение",R.drawable.game_ic_save,frame(43,413,249,44),scale,click=save)
        SettingsItem("Как играть",R.drawable.game_ic_help,frame(43,461,249,44),scale,click=help)
        SettingsItem("Об игре",R.drawable.game_ic_info,frame(43,509,249,44),scale,click=about)
    }
    if(videoSettings) ReferenceModal({videoSettings=false},322f,bottom=false,widthFraction=.92f) {
        ReferenceText("Видео с наградой",frame(20,20,287,42),27*scale)
        SettingsItem("Загрузка видео",R.drawable.game_ic_play,frame(29,72,269,53),scale,!state.busy,state.settings.adsAllowed,
            change={change(Setting.ADS_ALLOWED,it)})
        ReferenceText("Рекламная сеть обрабатывает сетевые и технические данные при загрузке объявлений. Это необязательно для игры.",
            frame(25,134,277,103),18*scale)
        ReferenceAction("Готово",frame(42,254,243,48),scale*.8f,onClick={videoSettings=false})
    }
}

@Composable
fun AccessibilityScreen(state: AppState, vm: GameViewModel, back: ()->Unit) {
    var tapSettings by remember {mutableStateOf(false)}
    ReferenceStage(highContrast=state.settings.highContrast,background=R.drawable.game_lake_day,backgroundTopFraction=.17f) {
        PreferencesHeader("Удобство",R.drawable.game_ic_accessibility,back,{tapSettings=true},"Ввод букв касаниями")
        PlaySurface(frame(13,180,301,377),31.dp) {}
        ReferenceText("Крупные буквы",frame(32,208,193,33),23*scale,align=TextAlign.Left)
        ReferenceToggle(state.settings.largeLetters,!state.busy,frame(234,200,62,48).semantics {contentDescription="Крупные буквы"}) {
            vm.setting(Setting.LARGE_LETTERS,it)
        }
        "ЛИС".forEachIndexed { index,letter ->
            PlaySurface(frame(49+index*77,259,74,79),18.dp) {
                ReferenceText(letter.toString(),Modifier.fillMaxSize(),(if(state.settings.largeLetters) 58 else 46)*scale)
            }
        }
        Box(frame(32,363,263,1).background(Color(0xFFC7B99E)))
        SettingsItem("Уменьшить\nанимации",R.drawable.pref_ic_motion,frame(35,377,261,66),scale,!state.busy,state.settings.reduceMotion,
            change={vm.setting(Setting.REDUCE_MOTION,it)})
        Box(frame(32,453,263,1).background(Color(0xFFC7B99E)))
        SettingsItem("Высокий\nконтраст",R.drawable.game_ic_contrast,frame(35,466,261,66),scale,!state.busy,state.settings.highContrast,
            change={vm.setting(Setting.CONTRAST,it)})
    }
    if(tapSettings) ReferenceModal({tapSettings=false},335f,bottom=false,widthFraction=.92f) {
        ReferenceText("Ввод касаниями",frame(20,20,287,42),27*scale)
        SettingsItem("Нажимать буквы",R.drawable.game_ic_touch,frame(29,74,269,53),scale,!state.busy,state.settings.tapInput,
            change={vm.setting(Setting.TAP_INPUT,it)})
        ReferenceText("Нажимай на буквы, затем «Проверить». Стереть последнюю букву можно повторным нажатием. При TalkBack этот способ включается автоматически.",
            frame(26,137,275,112),18*scale)
        ReferenceAction("Готово",frame(42,267,243,48),scale*.8f,onClick={tapSettings=false})
    }
}

@Composable
internal fun ReferenceIconAction(label: String, icon: Int, modifier: Modifier, scale: Float, enabled: Boolean, click: ()->Unit) {
    val shape=RoundedCornerShape((32*scale).dp)
    Box(modifier.shadow(4.dp,shape).clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xFF6FF584),Color(0xFF0CB64E),Color(0xFF008C40))))
        .border((2.5f*scale).dp,Color(0xFFFFFFEB),shape)
        .clickable(enabled=enabled,role=Role.Button,onClick=click).semantics(mergeDescendants=true) {contentDescription=label}) {
        Row(Modifier.fillMaxSize().padding(horizontal=(25*scale).dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy((14*scale).dp)) {
            Icon(painterResource(icon),null,Modifier.size((32*scale).dp),tint=Color.White)
            ReferenceText(label,Modifier.weight(1f),24*scale,Color.White,align=TextAlign.Left,maxLines=1)
        }
    }
}

@Composable
fun SaveScreen(state: AppState, vm: GameViewModel, back: ()->Unit) {
    val snapshot=requireNotNull(state.snapshot)
    val phase=state.filePhase
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"),vm::exportTo)
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(),vm::readSave)
    var details by remember {mutableStateOf(false)}
    var menu by remember {mutableStateOf(false)}
    val active=snapshot.profile?.campaignSessionId?.let(snapshot.sessions::get)
    val level=active?.takeUnless {it.completed}?.levelId
        ?: state.campaign?.levels?.firstOrNull {it.id !in snapshot.completions}?.id ?: state.campaign?.levels?.lastOrNull()?.id ?: 1
    val blocked=state.busy || phase in setOf("reading","applying","preparing")
    val safeBack: ()->Unit = {
        if(phase in setOf("reading","confirm")) vm.cancelFile() else if(!blocked) back()
    }
    BackHandler(enabled=phase in setOf("reading","confirm","applying","preparing")) {
        if(phase in setOf("reading","confirm")) vm.cancelFile()
    }
    if(phase in setOf("exported","imported")) {
        SaveCompleteScreen(phase=="imported",state.settings.highContrast,vm::cancelFile)
        return
    }
    if(phase=="confirm" && state.candidate!=null) {
        ReferenceStage(686f,highContrast=state.settings.highContrast,background=R.drawable.game_lake_day,backgroundTopFraction=.17f) {}
        ImportConfirmationPanel(state,state.candidate,vm::importSave,vm::cancelFile)
        return
    }
    ReferenceStage(highContrast=state.settings.highContrast,background=R.drawable.game_lake_day,backgroundTopFraction=.17f) {
        PreferencesHeader("Сохранение",R.drawable.game_ic_save,safeBack,{menu=true},"Дополнительные действия сохранения")
        Box(frame(131,62,66,64)) {
            DropdownMenu(expanded=menu,onDismissRequest={menu=false},containerColor=Cream) {
                DropdownMenuItem(text={Text("Предыдущая копия")},enabled=!blocked,onClick={menu=false;vm.previousSave()})
                DropdownMenuItem(text={Text("О сохранении и переносе")},onClick={menu=false;details=true})
            }
        }
        PlaySurface(frame(13,179,302,385),31.dp) {}
        ReferenceText("Твой прогресс",frame(43,201,242,36),30*scale)
        PlaySurface(frame(29,247,272,39),23.dp,colors=listOf(Color(0xFFCBF5DB),Color(0xFFCBF5DB))) {
            Row(Modifier.fillMaxSize().padding(horizontal=(30*scale).dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy((12*scale).dp)) {
                Icon(painterResource(R.drawable.pref_ic_phone),null,Modifier.size((26*scale).dp),tint=PlayInk)
                ReferenceText("На этом устройстве",Modifier.weight(1f),20*scale,align=TextAlign.Left,maxLines=1)
            }
        }
        Row(frame(35,302,267,34),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy((10*scale).dp)) {
            Image(painterResource(R.drawable.item_coin),null,Modifier.size((32*scale).dp))
            ReferenceText("Уровень $level · ${snapshot.profile?.coins ?: 0} монет",Modifier.weight(1f),20*scale,align=TextAlign.Left,maxLines=1)
        }
        ReferenceIconAction("Экспорт в файл",R.drawable.game_ic_export,frame(29,363,273,58),scale,!blocked) {
            vm.prepareExport {export.launch("connected-words-save-${snapshot.world.effectiveDate}.wordsave")}
        }
        ReferenceIconAction("Импорт из файла",R.drawable.game_ic_import,frame(29,435,273,58),scale,!blocked) {
            picker.launch(arrayOf("application/json","application/octet-stream","*/*"))
        }
        Box(frame(41,511,247,1).background(Color(0xFFBEB299)))
        Box(frame(43,522,241,28).clickable(role=Role.Button,onClick={details=true})) {
            ReferenceText("Сохрани файл для переноса",Modifier.fillMaxSize(),18*scale,Color(0xFF294A65),weight=androidx.compose.ui.text.font.FontWeight.Normal,maxLines=1)
        }
    }
    if(details) ReferenceModal({details=false},376f,bottom=false,widthFraction=.94f) {
        ReferenceText("Сохранение прогресса",frame(20,18,287,43),27*scale)
        val last=snapshot.world.lastExportAt
        val time=if(last==0L) "Экспортов пока не было" else "Последний экспорт: ${Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}"
        ReferenceText("$time\n\nОблачной синхронизации нет. При удалении игры данные могут исчезнуть.\n\nПеред импортом создаётся страховочная копия. Хранятся три последние внутренние копии. Внешние файлы игра не удаляет. Настройки удобства устройства сохраняются.",
            frame(24,72,279,232),17*scale)
        ReferenceAction("Понятно",frame(42,318,243,43),scale*.8f,onClick={details=false})
    }
    if(phase in setOf("reading","applying","preparing")) AlertDialog(
        onDismissRequest={if(phase=="reading") vm.cancelFile()},
        title={Text(when(phase) {"reading"->"Проверяем файл";"applying"->"Загружаем прогресс";else->"Готовим сохранение"})},
        text={CircularProgressIndicator()},confirmButton={if(phase=="reading") TextButton(onClick=vm::cancelFile) {Text("Отмена")}})
}
