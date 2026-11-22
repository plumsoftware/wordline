package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.data.*
import ru.plumsoftware.wordline.game.Session
import ru.plumsoftware.wordline.game.Economy
import ru.plumsoftware.wordline.game.Mode
import ru.plumsoftware.wordline.game.levelFor
import ru.plumsoftware.wordline.game.locationNames

@Composable
fun HelpScreen(economy: Economy, back: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        PageHeader("Как играть",back=back)
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            listOf("1. Соединяй" to "Проведи по буквам, чтобы собрать слово. Вернись к предыдущей букве, чтобы исправить жест. Отпусти палец — слово проверится.",
                "2. Или нажимай" to "Нажимай на буквы и затем «Проверить». Одну позицию можно выбрать один раз. Последняя выбранная буква снимается повторным нажатием. Этот способ работает и с TalkBack.",
                "3. Открывай пересечения" to "Найденное слово открывает общие буквы. Полностью раскрытое пересечениями слово засчитывается автоматически. Ё и Е в игре считаются одной буквой.",
                "4. Помощь рядом" to "Перемешивание бесплатно. Буква стоит ${economy.letter} монет, слово — ${economy.word}, намёк — ${economy.clue}. Сначала выбери клетку или слово, затем подтверди покупку. Неверные ответы не штрафуются.",
                "5. Собирай звёзды" to "Без подсказок — три звезды, одна подсказка — две, две и больше — одна. Повтор уровня не выдаёт монеты, но позволяет улучшить звёзды. Подсказки на повторе бесплатны и всё равно учитываются в оценке.").forEach { (title,body) ->
                Paper {Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(body)}
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
