package ru.plumsoftware.wordline

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.plumsoftware.wordline.ui.game.WordLineApp

class MainActivity : ComponentActivity() {
    private var openTasks by androidx.compose.runtime.mutableStateOf(false)
    private fun readNotificationIntent(intent: android.content.Intent?) {
        openTasks=intent?.getBooleanExtra(ru.plumsoftware.wordline.notifications.GameNotifications.OPEN_TASKS,false)==true ||
            intent?.getStringExtra("destination")=="tasks"
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readNotificationIntent(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readNotificationIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        (application as WordLineApplication).ads.bind(this)
        enableEdgeToEdge(
            statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced=false
            window.isNavigationBarContrastEnforced=false
        }
        hideSystemBars()
        setContent { WordLineApp(viewModel(),openTasks) {
            openTasks=false
            intent.removeExtra(ru.plumsoftware.wordline.notifications.GameNotifications.OPEN_TASKS)
            intent.removeExtra("destination")
        } }
    }
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window,window.decorView).apply {
            systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) hideSystemBars()
    }
    override fun onDestroy() {
        (application as WordLineApplication).ads.unbind(this)
        super.onDestroy()
    }
}
