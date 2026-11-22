package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.files.SaveCandidate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun currentProgressLevel(state: AppState): Int {
    val snapshot=state.snapshot ?: return 1
    val active=snapshot.profile?.campaignSessionId?.let(snapshot.sessions::get)
    return active?.takeUnless {it.completed}?.levelId
        ?: state.campaign?.levels?.firstOrNull {it.id !in snapshot.completions}?.id
        ?: state.campaign?.levels?.lastOrNull()?.id ?: 1
}

private fun importedProgressLevel(candidate: SaveCandidate, maxLevel: Int): Int {
    val id=candidate.payload.getJSONObject("profile").optString("campaignSessionId").takeUnless {it.isEmpty() || it=="null"}
    val sessions=candidate.payload.getJSONArray("sessions")
    for(i in 0 until sessions.length()) {
        val entry=sessions.getJSONObject(i)
        if(entry.getString("id")==id && !entry.getJSONObject("state").optBoolean("completed")) return entry.getInt("levelId")
    }
    return (candidate.completed+1).coerceAtMost(maxLevel)
}

@Composable
internal fun ImportConfirmationPanel(state: AppState, candidate: SaveCandidate, confirm: ()->Unit, cancel: ()->Unit) {
    val before=currentProgressLevel(state)
    val imported=importedProgressLevel(candidate,state.campaign?.levels?.lastOrNull()?.id ?: 1000)
    val exported=runCatching {Instant.parse(candidate.exportedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}
        .getOrDefault(candidate.exportedAt)
    ReferenceModal(cancel,510f,bottom=false,widthFraction=294f/327f,verticalOffsetFraction=14f/686f) {
        PlaySurface(frame(274,10,39,39),20.dp,onClick=cancel,description="Отменить импорт") {
            Icon(painterResource(R.drawable.game_ic_close),null,Modifier.fillMaxSize().padding(9.dp),tint=PlayInk)
        }
        DocumentMark(frame(118,26,96,94),saved=false)
        ReferenceText("Загрузить\nпрогресс?",frame(48,123,231,69),33*scale)
        ReferenceText("Монеты: ${state.snapshot?.profile?.coins ?: 0} → ${candidate.coins} · Формат ${candidate.root.optInt("schemaVersion")}",
            frame(20,190,287,15),12*scale,Color(0xFF294A65),maxLines=1)
        PlaySurface(frame(19,210,289,38),22.dp,colors=listOf(Color(0xFFFFECC0),Color(0xFFFFECC0))) {
            ReferenceText("Сейчас: уровень $before",Modifier.fillMaxSize().padding(horizontal=10.dp),22*scale,maxLines=1)
        }
        PlaySurface(frame(19,256,289,38),22.dp,colors=listOf(Color(0xFFE7D0F8),Color(0xFFE7D0F8))) {
            ReferenceText("Из файла: уровень $imported",Modifier.fillMaxSize().padding(horizontal=9.dp),22*scale,maxLines=1)
        }
        ReferenceText("Текущий прогресс\nбудет заменён",frame(39,305,249,46),22*scale,Color(0xFF294A65),
            weight=androidx.compose.ui.text.font.FontWeight.Normal)
        ReferenceText("Экспорт: $exported",frame(23,351,281,15),12*scale,Color(0xFF294A65),maxLines=1)
        VictoryButton("Загрузить",false,frame(22,371,283,60),scale,!state.busy,confirm,textSize=32f)
        PlaySurface(frame(22,440,283,52),30.dp,enabled=!state.busy,onClick=cancel) {
            ReferenceText("Отмена",Modifier.fillMaxSize(),29*scale,maxLines=1)
        }
    }
}

@Composable
internal fun SaveCompleteScreen(imported: Boolean, highContrast: Boolean, done: ()->Unit) {
    BackHandler(onBack=done)
    ReferenceStage(686f,highContrast=highContrast,background=R.drawable.game_lake_day,backgroundTopFraction=.17f) {
        PlaySurface(frame(18,156,291,379),32.dp) {}
        AccentRays(frame(55,203,62,83),left=true)
        AccentRays(frame(211,203,62,83),left=false)
        DocumentMark(frame(111,187,108,113),saved=true)
        ReferenceText(if(imported) "Прогресс загружен" else "Файл сохранён",frame(34,311,259,40),33*scale,maxLines=1)
        ReferenceText(if(imported) "Данные восстановлены\nиз выбранного файла" else "Можно перенести\nна другое устройство",
            frame(47,366,233,51),22*scale,Color(0xFF3A5271),weight=androidx.compose.ui.text.font.FontWeight.Medium)
        VictoryButton("Готово",false,frame(39,443,248,62),scale,true,done,textSize=32f)
    }
}

@Composable
internal fun DocumentMark(modifier: Modifier, saved: Boolean) {
    Canvas(modifier) {
        scale(size.width/96f,size.height/112f,pivot=Offset.Zero) {
            val page=Path().apply {
                moveTo(12f,5f);lineTo(53f,5f);lineTo(75f,27f);lineTo(75f,91f)
                quadraticTo(75f,99f,67f,99f);lineTo(12f,99f);quadraticTo(5f,99f,5f,91f)
                lineTo(5f,13f);quadraticTo(5f,5f,12f,5f);close()
            }
            drawPath(page,Color(0x33232343),style=Stroke(7f))
            drawPath(page,Brush.linearGradient(listOf(Color.White,Color(0xFFF1E8FC),Color(0xFFD8C8ED)),Offset(5f,5f),Offset(75f,99f)))
            drawPath(page,Color(0xFF7940CD),style=Stroke(3f))
            drawPath(Path().apply {moveTo(53f,5f);lineTo(53f,22f);quadraticTo(53f,27f,58f,27f);lineTo(75f,27f);close()},Color(0xFFD4ACF6))
            drawPath(Path().apply {moveTo(53f,5f);lineTo(53f,22f);quadraticTo(53f,27f,58f,27f);lineTo(75f,27f)},Color(0xFF7940CD),style=Stroke(3f))
            listOf(39f to 56f,53f to 56f,67f to 42f).forEach { (y,right) ->
                drawLine(Color(0xFF7940CD),Offset(20f,y),Offset(right,y),4f,cap=StrokeCap.Round)
            }
            val c=Offset(71f,84f)
            drawCircle(Color(0x33232232),24f,c+Offset(0f,2f))
            drawCircle(Brush.linearGradient(if(saved) listOf(Color(0xFF3BE578),Color(0xFF008C40)) else listOf(Color(0xFFC26DFA),Color(0xFF8B2CE0)),c-Offset(20f,20f),c+Offset(20f,20f)),23f,c)
            drawCircle(Color.White,23f,c,style=Stroke(3f))
            val mark=Path().apply {
                if(saved) {moveTo(60f,84f);lineTo(68f,92f);lineTo(83f,76f)}
                else {moveTo(71f,73f);lineTo(71f,94f);moveTo(62f,85f);lineTo(71f,94f);lineTo(80f,85f)}
            }
            drawPath(mark,Color.White,style=Stroke(4.5f,cap=StrokeCap.Round,join=StrokeJoin.Round))
        }
    }
}

@Composable
internal fun AccentRays(modifier: Modifier, left: Boolean) {
    Canvas(modifier) {
        val sx=size.width/62f;val sy=size.height/83f
        val rays=if(left) listOf(24f to 8f to (39f to 21f),8f to 41f to (31f to 41f),24f to 74f to (39f to 61f))
            else listOf(24f to 8f to (9f to 21f),40f to 41f to (17f to 41f),24f to 74f to (9f to 61f))
        rays.forEach { (from,to) ->
            drawLine(Color(0xFFFFBE0A),Offset(from.first*sx,from.second*sy),Offset(to.first*sx,to.second*sy),6f*sx,cap=StrokeCap.Round)
        }
    }
}
