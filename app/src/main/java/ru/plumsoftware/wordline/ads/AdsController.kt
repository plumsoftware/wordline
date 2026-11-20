package ru.plumsoftware.wordline.ads

import android.app.Activity
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.yandex.mobile.ads.common.*
import com.yandex.mobile.ads.rewarded.*
import com.yandex.mobile.ads.interstitial.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.plumsoftware.wordline.BuildConfig
import ru.plumsoftware.wordline.data.GameRepository
import ru.plumsoftware.wordline.game.AdTicket
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

data class AdsState(val phase: String = "idle",val message: String = "",val reward: Long = 0,val log: List<String> = emptyList())
/** Application context for loaders; only a weak Activity reference is retained. SDK calls stay on Main. */
class AdsController(private val context: Context) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val _state=MutableStateFlow(AdsState());val state=_state.asStateFlow()
    private var activity=WeakReference<Activity>(null)
    private var ownerGeneration=0L
    private var request: Job?=null
    private var lastOffer: Triple<GameRepository,String,String?>?=null
    fun retryRewarded() {val offer=lastOffer ?: return;clear();rewarded(offer.first,offer.second,offer.third)}
    private var initialized=false
    private var allowed=false
    private var profileGeneration: String?=null
    fun profileChanged(generation: String) {
        if(profileGeneration!=generation) {profileGeneration=generation;ownerGeneration++;cancelLoading();pendingReward=null;interstitial?.setAdEventListener(null);interstitial=null;update("idle");preload()}
    }
    private val rewardedLoader=RewardedAdLoader(context)
    private val interstitialLoader=InterstitialAdLoader(context)
    private var interstitial: InterstitialAd?=null
    private var pendingReward: Pair<GameRepository,AdTicket>?=null
    private val rewardedId get()=if(BuildConfig.DEBUG) "demo-rewarded-yandex" else BuildConfig.REWARDED_AD_UNIT_ID
    private val interstitialId get()=if(BuildConfig.DEBUG) "demo-interstitial-yandex" else BuildConfig.INTERSTITIAL_AD_UNIT_ID
    fun bind(owner: Activity) {activity=WeakReference(owner);ownerGeneration++}
    fun unbind(owner: Activity) {if(activity.get()===owner) {activity.clear();ownerGeneration++;cancelLoading()}}
    fun configure(enabled: Boolean) {
        allowed=enabled && BuildConfig.ADS_ENABLED
        if(!allowed) {cancelLoading();interstitial?.setAdEventListener(null);interstitial=null}
    }
    private fun update(phase: String,message: String="",reward: Long=0) {
        val events=if(BuildConfig.DEBUG) (_state.value.log+"$phase: $message").takeLast(20) else emptyList()
        _state.value=AdsState(phase,message,reward,events)
    }
    private fun resumed(): Activity? = activity.get()?.takeIf {a ->!a.isFinishing && !a.isDestroyed && (a as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED)==true}
    private suspend fun initialize() {
        if(initialized) return
        YandexAds.setUserConsent(false)
        YandexAds.setLocationTracking(false)
        YandexAds.setAgeRestricted(BuildConfig.AUDIENCE=="children" || (BuildConfig.DEBUG && BuildConfig.AUDIENCE.isBlank()))
        suspendCancellableCoroutine<Unit> {continuation ->YandexAds.initialize(context) {initialized=true;if(continuation.isActive) continuation.resume(Unit)}}
    }
    fun clear() {if(_state.value.phase !in setOf("loading","showing","committing")) update("idle")}
    fun cancelLoading() {
        if(_state.value.phase=="loading") {request?.cancel();rewardedLoader.cancelLoading();update("idle")}
    }
    fun rewarded(repo: GameRepository,placement: String,resultId: String?=null) {
        if(_state.value.phase in setOf("loading","showing","committing","storageError")) return
        lastOffer=Triple(repo,placement,resultId)
        if(!allowed) {update("unavailable",if(BuildConfig.ADS_ENABLED) "Разреши загрузку рекламы в настройках. Основная игра доступна без неё." else "Видео недоступно в этой версии.");return}
        val generation=ownerGeneration
        request=scope.launch {
            var ticket: AdTicket?=null
            try {
                val reserved=repo.reserveAd(placement,resultId)
                ticket=reserved
                update("loading","Готовим видео",reserved.reward)
                val ad=withTimeout(5_000) {
                    initialize()
                    suspendCancellableCoroutine<RewardedAd> {continuation ->
                        rewardedLoader.loadAd(AdRequest.Builder(rewardedId).build(),object : RewardedAdLoadListener {
                            override fun onAdLoaded(ad: RewardedAd) {if(continuation.isActive) continuation.resume(ad) else ad.setAdEventListener(null)}
                            override fun onAdFailedToLoad(error: AdRequestError) {if(continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("Сейчас нет видео. Попробуй позже.")))}
                        })
                        continuation.invokeOnCancellation {scope.launch {rewardedLoader.cancelLoading()}}
                    }
                }
                val owner=resumed()
                if(owner==null || generation!=ownerGeneration || !allowed) {ad.setAdEventListener(null);repo.finishAd(reserved,"cancelled");update("idle");return@launch}
                var received=false
                ad.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() {if(profileGeneration!=reserved.generation) return;update("showing",reward=reserved.reward);scope.launch {runCatching {repo.adShown(reserved)}}}
                    override fun onRewarded(reward: Reward) {
                        if(profileGeneration!=reserved.generation) return
                        received=true;pendingReward=repo to reserved
                        commitReward()
                    }
                    override fun onAdDismissed() {
                        if(profileGeneration!=reserved.generation) {ad.setAdEventListener(null);return}
                        scope.launch {
                            delay(500)
                            if(!received) {runCatching {repo.finishAd(reserved,"dismissed")};update("incomplete","Бонус пока не подтверждён")}
                            // Retain the listener briefly for an already scheduled late reward; the ticket remains durable.
                            delay(30_000);ad.setAdEventListener(null)
                        }
                    }
                    override fun onAdFailedToShow(error: AdError) {if(profileGeneration!=reserved.generation) {ad.setAdEventListener(null);return};ad.setAdEventListener(null);scope.launch {runCatching {repo.finishAd(reserved,"failed")};update("unavailable","Видео не открылось. Продолжай игру.")}}
                    override fun onAdClicked() = Unit
                    override fun onAdImpression(data: ImpressionData?) = Unit
                })
                update("showing",reward=reserved.reward)
                ad.show(owner)
            } catch(e: CancellationException) {
                withContext(NonCancellable) {ticket?.let {runCatching {repo.finishAd(it,"cancelled")}}}
                if(e is TimeoutCancellationException) update("unavailable","Подготовка видео заняла слишком много времени")
                throw e
            } catch(e: Exception) {ticket?.let {runCatching {repo.finishAd(it,"failed")}};update("unavailable",e.localizedMessage ?: "Видео недоступно")}
        }
    }
    fun commitReward() {
        val (repo,ticket)=pendingReward ?: return
        update("committing","Сохраняем награду",ticket.reward)
        scope.launch {
            try {repo.confirmAd(ticket);pendingReward=null;update("reward","Бонус получен",ticket.reward)}
            catch(e: Exception) {if(e.message=="Профиль изменился") {pendingReward=null;update("idle");return@launch};update("storageError","Просмотр подтверждён. Не удалось записать награду: повтори сохранение, смотреть ещё раз не нужно.",ticket.reward)}
        }
    }
    fun preload() {
        if(!allowed || interstitial!=null || !initialized) return
        val generation=ownerGeneration
        interstitialLoader.loadAd(AdRequest.Builder(interstitialId).build(),object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {if(allowed && generation==ownerGeneration) interstitial=ad else ad.setAdEventListener(null)}
            override fun onAdFailedToLoad(error: AdRequestError) = Unit
        })
    }
    fun transition(repo: GameRepository,eligible: Boolean,onContinue: () -> Unit) {
        scope.launch {
            if(_state.value.phase in setOf("loading","showing","committing","storageError")) return@launch
            val ad=interstitial;val owner=resumed()
            if(!eligible || !allowed || ad==null || owner==null || _state.value.phase!="idle" || !runCatching {repo.interstitialAllowed()}.getOrDefault(false)) {onContinue();return@launch}
            interstitial=null;var continued=false;val impressionId=java.util.UUID.randomUUID().toString()
            fun finish() {if(!continued) {continued=true;ad.setAdEventListener(null);update("idle");onContinue();preload()}}
            ad.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {update("showing");scope.launch {runCatching {repo.interstitialShown(impressionId)}}}
                override fun onAdDismissed()=finish()
                override fun onAdFailedToShow(error: AdError)=finish()
                override fun onAdClicked()=Unit
                override fun onAdImpression(data: ImpressionData?)=Unit
            })
            update("showing")
            try {ad.show(owner)} catch(e: Exception) {finish()}
        }
    }
    suspend fun warmUp() {if(allowed) {try {withTimeout(5_000) {initialize()};preload()} catch(e: CancellationException) {throw e} catch(e: Exception) {update("idle")} }}
}
