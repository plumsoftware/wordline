package ru.plumsoftware.wordline.ui.game

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.plumsoftware.wordline.WordLineApplication
import ru.plumsoftware.wordline.audio.Cue
import ru.plumsoftware.wordline.data.*
import ru.plumsoftware.wordline.files.*
import ru.plumsoftware.wordline.game.*
import java.util.UUID

data class AppState(
    val campaign: Campaign?=null,val snapshot: GameSnapshot?=null,val settings: GameSettings=GameSettings(),
    val loading: Boolean=true,val busy: Boolean=false,val error: String?=null,val feedback: String="",
    val economy: Economy=Economy(),val filePhase: String="idle",val candidate: SaveCandidate?=null,
)
class GameViewModel(application: Application): AndroidViewModel(application) {
    private val app=application as WordLineApplication
    private val _state=MutableStateFlow(AppState());val state=_state.asStateFlow()
    private var repository: GameRepository?=null
    private var saves: SaveRepository?=null
    private var loadingJob: Job?=null
    private var fileJob: Job?=null
    private var exportBytes: ByteArray?=null
    private var lastShuffle=-300L
    val ads=app.ads
    val online=connectivity(app).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),true)
    private val _cues=MutableSharedFlow<Cue>(extraBufferCapacity=8);val cues=_cues.asSharedFlow()
    init {load()}
    fun load() {
        if(loadingJob?.isActive==true) return
        _state.update {it.copy(loading=true,error=null)}
        loadingJob=viewModelScope.launch {
            try {
                val repo=withContext(Dispatchers.IO) {GameRepository(app.database,loadCampaign(app),loadEconomy(app)).also {it.initialize()}}
                repository=repo;saves=SaveRepository(app,repo)
                combine(repo.snapshot,app.settings.settings) {snapshot,settings ->snapshot to settings}.collect { (snapshot,settings) ->
                    _state.update {it.copy(campaign=repo.campaign,snapshot=snapshot,settings=settings,economy=repo.economy,loading=false)}
                }
            } catch(e: CancellationException) {throw e}
            catch(e: Exception) {_state.update {it.copy(loading=false,error="Не удалось открыть данные. ${e.localizedMessage ?: "Попробуйте снова."}")}}
        }
    }
    private fun command(block: suspend (GameRepository) -> Unit) {
        val repo=repository ?: return
        if(_state.value.busy) return
        _state.update {it.copy(busy=true,feedback="")}
        viewModelScope.launch {
            try {block(repo)}
            catch(e: CancellationException) {throw e}
            catch(e: Exception) {_state.update {it.copy(error=e.localizedMessage ?: "Не удалось сохранить действие. Попробуйте снова.")}}
            finally {_state.update {it.copy(busy=false)}}
        }
    }
    fun clearError() {_state.update {it.copy(error=null)}}
    fun foreground() {if(repository!=null && !_state.value.busy) command {it.foreground()}}
    fun activeTime(millis: Long) {val repo=repository ?: return;viewModelScope.launch {try {repo.activeTime(millis)} catch(e: Exception) {_state.update {it.copy(error="Не удалось сохранить активность")}}}}
    fun start(levelId: Int?=null,replay: Boolean=false,navigate: (String)->Unit)=command {navigate(it.start(levelId,replay))}
    fun daily(date: String,navigate: (String)->Unit)=command {navigate(it.startDaily(date))}
    fun next(id: String,navigate: (String?)->Unit) {
        val repo=repository ?: return
        val s=_state.value.snapshot?.sessions?.get(id) ?: return
        ads.transition(repo,!s.replay && s.mode==Mode.CAMPAIGN) {command {navigate(it.next(id))}}
    }
    fun submit(id: String,word: String)=command {repo ->
        val message=repo.submitWord(id,word,UUID.randomUUID().toString(),_state.value.snapshot?.sessions?.get(id)?.revision)
        _state.update {it.copy(feedback=message)}
        _cues.tryEmit(when(message) {"Слово найдено!"->Cue.SUCCESS;"Дополнительное слово!"->Cue.BONUS;else->Cue.NEUTRAL})
    }
    fun purchase(id: String,hint: Hint,target: String)=command {repo ->
        val message=repo.purchaseHint(id,hint,target,UUID.randomUUID().toString(),_state.value.snapshot?.sessions?.get(id)?.revision)
        _state.update {it.copy(feedback=message)};_cues.tryEmit(Cue.SUCCESS)
    }
    fun shuffle(id: String) {
        val now=SystemClock.elapsedRealtime();if(now-lastShuffle<300 || _state.value.busy) return
        lastShuffle=now;command {it.shuffle(id)}
    }
    fun setting(setting: Setting,value: Boolean)=command {app.settings.set(setting,value)}
    fun onboard(start: Boolean,navigate: (String?)->Unit)=command {repo ->
        app.settings.set(Setting.TUTORIAL,start);app.settings.set(Setting.ONBOARDING,true)
        navigate(if(start) repo.tutorial() else null)
    }
    fun claimTask(id: String)=command {it.claimTask(id);_cues.tryEmit(Cue.REWARD)}
    fun claimGift()=command {it.claimGift();_cues.tryEmit(Cue.REWARD)}
    fun claimStreak()=command {it.claimStreak();_cues.tryEmit(Cue.REWARD)}
    fun claimTrail(milestone: Int)=command {it.claimTrail(milestone);_cues.tryEmit(Cue.REWARD)}
    fun claimAchievement(id: String)=command {it.claimAchievement(id);_cues.tryEmit(Cue.REWARD)}
    fun dismissReport()=command {it.dismissReport()}
    fun clockNotice()=command {it.dismissClockNotice()}
    fun locationSeen(index: Int,navigate: ()->Unit = {})=command {it.acknowledgeLocation(index);navigate()}
    fun appearance(background: Int,theme: String)=command {
        val count=_state.value.snapshot?.completions?.size ?: 0
        require(background in 0..(count/100).coerceAtMost(9)) {"Сначала открой эту локацию"}
        val trail=_state.value.snapshot?.world?.trailEvents?.size ?: 0
        require(theme=="day" || theme=="contrast" || theme=="evening" && count>=50 || theme=="forest" && count>=100 || theme=="trail" && trail>=30) {"Оформление пока закрыто"}
        app.settings.appearance(background,theme);app.settings.set(Setting.CONTRAST,theme=="contrast")
    }
    fun reward(placement: String,resultId: String?=null) {repository?.let {ads.rewarded(it,placement,resultId)}}
    fun prepareExport(onReady: ()->Unit)=command {
        _state.update {it.copy(filePhase="preparing")}
        exportBytes=withContext(Dispatchers.IO) {requireNotNull(saves).exportBytes(_state.value.settings)}
        _state.update {it.copy(filePhase="ready")};onReady()
    }
    fun exportTo(uri: Uri?) {
        if(uri==null) {exportBytes=null;_state.update {it.copy(filePhase="idle")};return}
        command {
            try {requireNotNull(saves).exportTo(uri,requireNotNull(exportBytes));exportBytes=null;_state.update {it.copy(filePhase="exported")}}
            catch(e: Exception) {_state.update {it.copy(filePhase="idle")};throw e}
        }
    }
    fun readSave(uri: Uri?) {
        if(uri==null) return
        if(fileJob?.isActive==true || _state.value.busy) return
        _state.update {it.copy(filePhase="reading")}
        fileJob=viewModelScope.launch {
            try {val candidate=requireNotNull(saves).read(uri);_state.update {it.copy(filePhase="confirm",candidate=candidate)}}
            catch(e: CancellationException) {_state.update {it.copy(filePhase="idle")};throw e}
            catch(e: Exception) {_state.update {it.copy(filePhase="idle",error="Файл не загружен: ${e.localizedMessage ?: "неверное сохранение"}")}}
        }
    }
    fun cancelFile() {if(_state.value.filePhase!="applying") {fileJob?.cancel();_state.update {it.copy(filePhase="idle",candidate=null)}}}
    fun importSave()=command {
        val candidate=requireNotNull(_state.value.candidate)
        _state.update {it.copy(filePhase="applying")}
        try {withContext(Dispatchers.IO) {requireNotNull(saves).apply(candidate,_state.value.settings)};_state.update {it.copy(filePhase="imported",candidate=null,feedback="Прогресс восстановлен")}}
        catch(e: Exception) {_state.update {it.copy(filePhase="confirm")};throw e}
    }
    fun previousSave()=command {
        val candidate=requireNotNull(saves).previous() ?: error("Предыдущей копии пока нет")
        _state.update {it.copy(filePhase="confirm",candidate=candidate)}
    }
    fun exportPostcard(uri: Uri?,drawable: Int,title: String) {
        if(uri==null) return
        command {
            withContext(Dispatchers.IO) {
                val bytes=postcardBytes(app,drawable,title)
                app.contentResolver.openOutputStream(uri,"wt").use {out ->requireNotNull(out);out.write(bytes);out.flush()}
            }
            _state.update {it.copy(feedback="Открытка сохранена")}
        }
    }
    fun share(drawable: Int,title: String)=command {withContext(Dispatchers.IO) {sharePostcard(app,drawable,title)}}
}
