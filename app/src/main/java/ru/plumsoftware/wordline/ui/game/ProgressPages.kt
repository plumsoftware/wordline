package ru.plumsoftware.wordline.ui.game

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.wordline.BuildConfig
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.game.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val backgroundResources=listOf(R.drawable.journey_altai,R.drawable.journey_bamboo,R.drawable.journey_sahara,R.drawable.journey_moscow,R.drawable.journey_aurora,R.drawable.journey_mediterranean,R.drawable.journey_canyon,R.drawable.journey_tuscany,R.drawable.journey_tropics,R.drawable.journey_alps)
val postcardResources=listOf(R.drawable.journey_altai,R.drawable.journey_bamboo,R.drawable.journey_sahara,R.drawable.journey_moscow,R.drawable.journey_aurora,R.drawable.journey_mediterranean,R.drawable.journey_canyon,R.drawable.journey_tuscany,R.drawable.journey_tropics,R.drawable.journey_alps)

@Composable
fun ProgressPage(title: String,back: ()->Unit,content: @Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeader(title,back=back)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable
fun RewardRow(title: String,progress: Int,target: Int,reward: Long,claimed: Boolean,busy: Boolean,claim: ()->Unit) {
    Paper(Modifier.fillMaxWidth()) {
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Text("${progress.coerceAtMost(target)} из $target · +$reward монет")
        LinearProgressIndicator(progress={(progress.toFloat()/target).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
        Button(onClick=claim,enabled=progress>=target && !claimed && !busy,modifier=Modifier.fillMaxWidth()) {Text(if(claimed) "Получено" else if(progress>=target) "Забрать +$reward" else "В процессе")}
    }
}
@Composable
fun ExtraPostcardScreen(state: AppState,id: String,vm: GameViewModel,back: ()->Unit) {
    val title=if(id.startsWith("trail:")) "Тропа: ${id.substringAfter(':')} листьев" else "Календарь: ${id.substringAfter(':')}"
    val drawable=postcardResources.first()
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {uri ->vm.exportPostcard(uri,drawable,title)}
    ProgressPage(title,back) {
        Paper {
            Image(painterResource(drawable),title,Modifier.fillMaxWidth())
            Text(title,fontWeight=FontWeight.Bold)
            Text("Открытка получена и сохранена в коллекции.")
            MainButton("Сделать фоном",!state.busy,{vm.appearance(0,state.settings.theme)})
            OutlinedButton(onClick={vm.share(drawable,title)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Поделиться")}
            OutlinedButton(onClick={export.launch("connected-words-postcard.png")},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text("Сохранить открытку в файл")}
            if(state.feedback.isNotBlank()) Text(state.feedback)
        }
    }
}
