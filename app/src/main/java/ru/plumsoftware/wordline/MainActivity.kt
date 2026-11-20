package ru.plumsoftware.wordline

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.plumsoftware.wordline.ui.game.WordLineApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        (application as WordLineApplication).ads.bind(this)
        enableEdgeToEdge()
        setContent { WordLineApp(viewModel()) }
    }
    override fun onDestroy() {
        (application as WordLineApplication).ads.unbind(this)
        super.onDestroy()
    }
}
