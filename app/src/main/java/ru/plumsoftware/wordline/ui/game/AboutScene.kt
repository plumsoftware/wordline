package ru.plumsoftware.wordline.ui.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.BuildConfig
import ru.plumsoftware.wordline.R

@Composable
fun AboutScreen(back: ()->Unit,article: (String)->Unit,help: ()->Unit,debug: ()->Unit={},highContrast: Boolean=false) {
    val context=LocalContext.current
    val clipboard=LocalClipboardManager.current
    var support by remember {mutableStateOf(false)}
    ReferenceStage(686f,highContrast=highContrast,background=R.drawable.game_lake_day,backgroundTopFraction=.17f) {
        ReferenceBack(frame(15,14,42,38),back)
        Icon(painterResource(R.drawable.home_mountain),null,frame(102,49,126,64),tint=Color.White)
        PlaySurface(frame(55,135,218,47).rotate(2f),15.dp,colors=listOf(Color(0xFF076E5D),Color(0xFF00493D))) {
            ReferenceText("Путешествие",Modifier.fillMaxSize(),25*scale,Color.White)
        }
        VictoryLeaves(frame(27,137,43,43),true)
        VictoryLeaves(frame(263,137,43,43),false)
        "СЛОВА".forEachIndexed {index,letter ->
            PlaySurface(frame(45+index*46,86+kotlin.math.abs(2-index)*3,48,55).rotate((index-2)*4f),12.dp) {
                ReferenceText(letter.toString(),Modifier.fillMaxSize(),35*scale)
            }
        }
        PlaySurface(frame(13,186,305,376),30.dp) {}
        ReferenceText(stringResource(R.string.app_name),frame(28,203,271,30),23*scale,maxLines=1)
        Box(frame(71,235,185,25).then(if(BuildConfig.DEBUG) Modifier.clickable(onClick=debug) else Modifier)) {
            ReferenceText("Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",Modifier.fillMaxSize(),18*scale,Color(0xFF3A5271),FontWeight.Medium,maxLines=1)
        }
        listOf(271,327,380,432,496).forEach {y -> Box(frame(37,y,262,1).background(Color(0xFFD1C3A8))) }
        SettingsItem("Как играть",R.drawable.game_ic_help,frame(36,278,264,46),scale,click=help)
        SettingsItem("Поддержка",R.drawable.about_ic_support,frame(36,332,264,46),scale,click={
            if(BuildConfig.SUPPORT_EMAIL.isBlank()) support=true
            else try {context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:"+BuildConfig.SUPPORT_EMAIL)))}
            catch(_: android.content.ActivityNotFoundException) {support=true}
        })
        SettingsItem("Конфиденциальность",R.drawable.game_ic_shield,frame(36,384,264,46),scale,click={article("privacy")})
        SettingsItem("Лицензии",R.drawable.game_ic_license,frame(36,438,264,46),scale,click={article("licenses")})
        ReferenceText("Прогресс хранится на устройстве",frame(30,516,267,28),18*scale,Color(0xFF294A65),FontWeight.Medium,maxLines=1)
    }
    if(support) ReferenceModal({support=false},240f,bottom=false,widthFraction=.92f) {
        ReferenceText("Поддержка",frame(20,18,287,40),28*scale)
        ReferenceText(if(BuildConfig.SUPPORT_EMAIL.isBlank()) "Контакт поддержки пока не указан издателем." else BuildConfig.SUPPORT_EMAIL,frame(25,69,277,70),19*scale)
        ReferenceAction(if(BuildConfig.SUPPORT_EMAIL.isBlank()) "Понятно" else "Скопировать адрес",frame(25,164,277,52),scale,onClick={
            if(BuildConfig.SUPPORT_EMAIL.isNotBlank()) clipboard.setText(AnnotatedString(BuildConfig.SUPPORT_EMAIL))
            support=false
        })
    }
}
