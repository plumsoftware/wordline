package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.audio.AudioEngine
import ru.plumsoftware.wordline.game.*

@Composable
fun WordLineApp(vm: GameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ad by vm.ads.state.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val context=LocalContext.current;val view=LocalView.current
    val audio=remember(context.applicationContext) {AudioEngine(context.applicationContext)}
    val latestSettings by rememberUpdatedState(state.settings)
    val owner=LocalLifecycleOwner.current
    var foreground by remember {mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver {_,event ->
            if(event==Lifecycle.Event.ON_RESUME) {foreground=true;vm.foreground()}
            if(event==Lifecycle.Event.ON_PAUSE) foreground=false
        }
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer);audio.release()}
    }
    LaunchedEffect(vm,audio) {vm.cues.collect {cue ->audio.play(cue);if(latestSettings.vibration) view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)}}
    LaunchedEffect(state.settings.adsAllowed) {vm.ads.configure(state.settings.adsAllowed);if(state.settings.adsAllowed) vm.ads.warmUp()}
    LaunchedEffect(foreground,state.loading) {if(foreground && !state.loading) while(true) {delay(60_000);vm.foreground()}}
    var showProgress by remember {mutableStateOf(false)}
    LaunchedEffect(state.busy) {showProgress=false;if(state.busy) {delay(150);showProgress=true}}
    val count=state.snapshot?.completions?.size ?: 0
    val background=state.settings.background.coerceIn(0,(count/100).coerceAtMost(9))
    val configuredTheme=state.settings.theme
    val theme=if(configuredTheme=="evening" && count<50 || configuredTheme=="forest" && count<100 || configuredTheme=="trail" && (state.snapshot?.world?.trailEvents?.size ?: 0)<30) "day" else configuredTheme
    var launchPresented by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(900)
        launchPresented = true
    }
    GameTheme(state.settings.highContrast,theme) {
        val initialSnapshot = state.snapshot
        if (!launchPresented || state.loading || state.campaign == null ||
            initialSnapshot?.profile == null || initialSnapshot.world.effectiveDate.isBlank()) {
            LaunchScreen(error = state.error, onRetry = vm::load)
            return@GameTheme
        }
        Landscape(state.settings.highContrast,background,theme) {
            val nav=rememberNavController()
            val backStack by nav.currentBackStackEntryAsState()
            val route=backStack?.destination?.route.orEmpty()
            var gameUnpaused by remember {mutableStateOf(true)}
            val adActive=ad.phase in setOf("loading","showing","committing","storageError")
            val audioActive=foreground && !adActive && state.filePhase !in setOf("reading","applying","preparing") && route!="save" && (route!="game/{id}" || gameUnpaused)
            LaunchedEffect(state.settings,audioActive) {audio.update(state.settings,audioActive)}
            val clockActive=route=="game/{id}" && gameUnpaused && foreground && !adActive && !state.busy
            LaunchedEffect(clockActive) {
                if(clockActive) {var pending=0L;try {while(true) {delay(1_000);pending+=1_000;if(pending>=5_000) {vm.activeTime(pending);pending=0}}} finally {if(pending>0) vm.activeTime(pending)}}
            }
            var generation by remember {mutableStateOf<String?>(null)}
            LaunchedEffect(state.snapshot?.world?.generation) {
                val current=state.snapshot?.world?.generation
                if(current!=null && current!="loading") {
                    if(generation!=null && current!=generation) nav.navigate("home") {popUpTo(nav.graph.id) {inclusive=true}}
                    vm.ads.profileChanged(current)
                    generation=current
                }
            }
            Box(Modifier.fillMaxSize().then(if(route=="home") Modifier else Modifier.safeDrawingPadding()),contentAlignment=Alignment.TopCenter) {
                val campaign=state.campaign;val snapshot=state.snapshot
                if(campaign!=null && snapshot?.profile!=null && snapshot.world.effectiveDate.isNotBlank()) {
                    val game:(String)->Unit={id ->nav.navigate("game/$id") {launchSingleTop=true}}
                    fun home() {nav.navigate("home") {popUpTo("home") {inclusive=true}}}
                    Column(Modifier.widthIn(max=1000.dp).fillMaxSize()) {
                        NavHost(navController=nav,startDestination="entry",modifier=Modifier.weight(1f).fillMaxWidth(),
                            enterTransition={androidx.compose.animation.EnterTransition.None},exitTransition={androidx.compose.animation.ExitTransition.None}) {
                            composable("entry") {LaunchedEffect(Unit) {nav.navigate(if(state.settings.onboardingCompleted) "home" else "welcome") {popUpTo("entry") {inclusive=true}}}}
                            composable("welcome") {WelcomeScreen(state.busy,onStart={vm.onboard(true) {id ->nav.navigate("home") {popUpTo("welcome") {inclusive=true}};if(id!=null) game(id)}},onSkip={vm.onboard(false) {nav.navigate("home") {popUpTo("welcome") {inclusive=true}}}},onPrivacy={nav.navigate("article/privacy")})}
                            composable("home") {HomeScreen(state,play={vm.start(navigate=game)},map={nav.navigate("map")},settings={nav.navigate("settings")},tasks={nav.navigate("tasks")},gift={nav.navigate("gift")},wallet={nav.navigate("wallet")})}
                            composable("tasks") {TasksScreen(state,vm,calendar={nav.navigate("calendar")},trail={nav.navigate("trail")},gift={nav.navigate("gift")},streak={nav.navigate("streak")},resume=game)}
                            composable("collection") {CollectionScreen(state,achievements={nav.navigate("achievements")},statistics={nav.navigate("statistics")},appearance={nav.navigate("appearance")},open={nav.navigate("postcard/$it")},openExtra={nav.navigate("extraPostcard/$it")})}
                            composable("map") {MapScreen(state,back={nav.popBackStack()},play={level,replay ->vm.start(level,replay,game)})}
                            composable("calendar") {CalendarScreen(state,back={nav.popBackStack()},play={vm.daily(it,game)})}
                            composable("gift") {GiftScreen(state,vm,back={nav.popBackStack()})}
                            composable("streak") {StreakScreen(state,vm,back={nav.popBackStack()})}
                            composable("trail") {TrailScreen(state,vm,back={nav.popBackStack()})}
                            composable("achievements") {AchievementsScreen(state,vm,back={nav.popBackStack()})}
                            composable("statistics") {StatisticsScreen(state,back={nav.popBackStack()})}
                            composable("appearance") {AppearanceScreen(state,vm,back={nav.popBackStack()})}
                            composable("postcard/{index}",arguments=listOf(navArgument("index") {type=NavType.IntType})) {entry ->
                                val index=requireNotNull(entry.arguments).getInt("index")
                                if(index in 0..9 && "location:$index" in snapshot.world.collectibles) PostcardScreen(state,index,vm,back={nav.popBackStack()})
                            }
                            composable("extraPostcard/{id}") {entry ->val id=entry.arguments?.getString("id").orEmpty();if(id in snapshot.world.collectibles) ExtraPostcardScreen(state,id,vm,back={nav.popBackStack()})}
                            composable("unlock/{index}",arguments=listOf(navArgument("index") {type=NavType.IntType})) {entry ->
                                val index=requireNotNull(entry.arguments).getInt("index")
                                UnlockScreen(index) {vm.locationSeen(index) {nav.navigate("map") {popUpTo("home")}}}
                            }
                            composable("settings") {SettingsScreen(state,back={nav.popBackStack()},change=vm::setting,help={nav.navigate("help")},save={nav.navigate("save")},about={nav.navigate("about")},accessibility={nav.navigate("accessibility")})}
                            composable("accessibility") {AccessibilityScreen(state,vm,back={nav.popBackStack()})}
                            composable("save") {SaveScreen(state,vm,back={nav.popBackStack()})}
                            composable("about") {AboutScreen(back={nav.popBackStack()},article={nav.navigate("article/$it")},help={nav.navigate("help")},debug={nav.navigate("debugAds")})}
                            if(ru.plumsoftware.wordline.BuildConfig.DEBUG) composable("debugAds") {DebugAdsScreen(state,ad,back={nav.popBackStack()})}
                            composable("article/{kind}") {entry ->ArticleScreen(entry.arguments?.getString("kind") ?: "privacy",back={nav.popBackStack()})}
                            composable("wallet") {WalletScreen(state,vm,back={nav.popBackStack()})}
                            composable("help") {HelpScreen(state.economy,back={nav.popBackStack()})}
                            composable("game/{id}",arguments=listOf(navArgument("id") {type=NavType.StringType})) {entry ->
                                val id=requireNotNull(entry.arguments?.getString("id"));val session=snapshot.sessions[id]
                                if(session!=null) {
                                    LaunchedEffect(session.completed) {if(session.completed) nav.navigate("result/$id") {popUpTo("game/$id") {inclusive=true};launchSingleTop=true}}
                                    GameScreen(state,session,onBack={nav.popBackStack()},onMap={if(session.mode==Mode.DAILY) nav.navigate("calendar") {popUpTo("home")} else nav.navigate("map") {popUpTo("home")}},
                                        onSubmit={vm.submit(id,it)},onShuffle={vm.shuffle(id)},onHint={kind,target ->vm.purchase(id,kind,target)},
                                        onActive={gameUnpaused=it},onVideo={vm.reward("hint_coins",id)},online=online)
                                } else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator()}
                            }
                            composable("result/{id}",arguments=listOf(navArgument("id") {type=NavType.StringType})) {entry ->
                                val id=requireNotNull(entry.arguments?.getString("id"));val session=snapshot.sessions[id]
                                if(session!=null) ResultScreen(state,session,onNext={
                                    val unlocked=(session.levelId/100).coerceAtMost(9)
                                    if(session.mode==Mode.CAMPAIGN && !session.replay && session.levelId%100==0 && unlocked !in snapshot.world.acknowledgedLocations) nav.navigate("unlock/$unlocked")
                                    else vm.next(id) {next ->if(next!=null) nav.navigate("game/$next") {popUpTo("result/$id") {inclusive=true}} else if(session.mode==Mode.DAILY) nav.navigate("calendar") {popUpTo("home")} else home()}
                                },onHome={home()},reward={vm.reward("win_extra",id)},daily={nav.navigate("calendar")})
                            }
                        }
                        if(route in setOf("home","tasks","collection")) HomeNavigation(route) {target ->
                            if(route!=target) nav.navigate(target) {popUpTo("home") {saveState=true};launchSingleTop=true;restoreState=true}
                        }
                    }
                }
                if(showProgress) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            if(state.error!=null && !state.loading && state.campaign!=null) AlertDialog(onDismissRequest=vm::clearError,title={Text("Действие не выполнено")},text={Text(state.error!!)},confirmButton={TextButton(onClick=vm::clearError) {Text("Понятно")}})
            if(state.snapshot?.world?.clockNotice==true && ad.phase=="idle" && state.error==null) AlertDialog(onDismissRequest={vm.clockNotice()},title={Text("Дата на устройстве изменилась")},text={Text("Календарь и дневные награды продолжатся с ${state.snapshot!!.world.effectiveDate}. Обычная игра доступна.")},confirmButton={TextButton(onClick={vm.clockNotice()}) {Text("Понятно")}})
            if(state.snapshot?.world?.dailyReport?.isNotEmpty()==true && state.snapshot?.world?.clockNotice!=true && route=="home" && ad.phase=="idle" && state.error==null) AlertDialog(onDismissRequest={vm.dismissReport()},title={Text("Награды прошлого дня")},text={Text(state.snapshot!!.world.dailyReport.joinToString("\n"))},confirmButton={TextButton(onClick={vm.dismissReport()}) {Text("Понятно")}})
            if(ad.phase !in setOf("idle","showing")) AlertDialog(onDismissRequest={if(ad.phase=="loading") vm.ads.cancelLoading() else vm.ads.clear()},title={Text(when(ad.phase) {"loading"->"Готовим видео";"reward"->"Бонус получен";"incomplete"->"Просмотр не завершён";"committing"->"Сохраняем награду";"storageError"->"Не удалось сохранить награду";else->"Видео недоступно"})},
                text={Column {Text(ad.message);if(ad.reward>0) Text("+${ad.reward} монет · Реклама");if(ad.phase in setOf("loading","committing")) CircularProgressIndicator()}},
                dismissButton={if(ad.phase=="incomplete") TextButton(onClick=vm.ads::retryRewarded) {Text("Смотреть снова · Реклама")}},
                confirmButton={if(ad.phase=="storageError") TextButton(onClick=vm.ads::commitReward) {Text("Повторить сохранение")} else if(ad.phase!="committing") TextButton(onClick={if(ad.phase=="loading") vm.ads.cancelLoading() else vm.ads.clear()}) {Text(if(ad.phase=="loading") "Отмена" else "Продолжить")}})
        }
    }
}
