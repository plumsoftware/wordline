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
import ru.plumsoftware.wordline.game.Hint
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

data class AdsState(val phase: String = "idle",val message: String = "",val reward: Long = 0,val log: List<String> = emptyList(),
                    val balanceBefore: Long? = null,val balanceAfter: Long? = null,val loadingId: Long = 0,val hint: Hint? = null)
private data class RewardOffer(val repo: GameRepository,val placement: String,val resultId: String?,val hint: Hint?,val target: String?)
/** Application context for loaders; only a weak Activity reference is retained. SDK calls stay on Main. */
class AdsController(private val context: Context) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val _state=MutableStateFlow(AdsState());val state=_state.asStateFlow()
    private var activity=WeakReference<Activity>(null)
    private var ownerGeneration=0L
    private var request: Job?=null
    private var loadingSerial=0L
    private var loadingPresentation: CompletableDeferred<Unit>?=null
    private var cancelTransition: (()->Unit)?=null
    private var lastOffer: RewardOffer?=null
    fun retryRewarded() {val offer=lastOffer ?: return;clear();rewarded(offer.repo,offer.placement,offer.resultId,offer.hint,offer.target)}
    private var initialized=false
    private var allowed=false
    private var profileGeneration: String?=null
    fun profileChanged(generation: String) {
        if(profileGeneration!=generation) {profileGeneration=generation;ownerGeneration++;cancelLoading();pendingReward=null;interstitial?.setAdEventListener(null);interstitial=null;rewardedAd?.setAdEventListener(null);rewardedAd=null;update("idle")}
    }
    private val rewardedLoader by lazy {RewardedAdLoader(context)}
    private val interstitialLoader by lazy {InterstitialAdLoader(context)}
    private var interstitial: InterstitialAd?=null
    private var rewardedAd: RewardedAd?=null
    private var pendingReward: Pair<GameRepository,AdTicket>?=null
    private fun rewardedId(placement: String)=if(BuildConfig.DEBUG) "demo-rewarded-yandex" else if(placement=="win_extra") BuildConfig.LEVEL_COMPLETE_REWARDED_AD_UNIT_ID else BuildConfig.REWARDED_AD_UNIT_ID
    private val interstitialId get()=if(BuildConfig.DEBUG) "demo-interstitial-yandex" else BuildConfig.INTERSTITIAL_AD_UNIT_ID
    fun bind(owner: Activity) {activity=WeakReference(owner);ownerGeneration++}
    fun unbind(owner: Activity) {if(activity.get()===owner) {activity.clear();ownerGeneration++;cancelLoading()}}
    fun configure(enabled: Boolean) {
        allowed=enabled && BuildConfig.ADS_ENABLED
        if(!allowed) cancelLoading()
    }
    private fun update(phase: String,message: String="",reward: Long=0,balanceBefore: Long?=null,balanceAfter: Long?=null,hint: Hint?=null) {
        if(phase=="loading") {loadingSerial++;loadingPresentation=CompletableDeferred()}
        else loadingPresentation=null
        val events=if(BuildConfig.DEBUG) (_state.value.log+"$phase: $message").takeLast(20) else emptyList()
        _state.value=AdsState(phase,message,reward,events,balanceBefore,balanceAfter,loadingSerial,hint)
    }
    private fun trace(message: String) {
        if(BuildConfig.DEBUG) {
            android.util.Log.d("WordlineAds",message)
            _state.value=_state.value.copy(log=(_state.value.log+message).takeLast(20))
        }
    }
    fun loadingPresented(id: Long) {
        if(_state.value.phase=="loading" && _state.value.loadingId==id) loadingPresentation?.complete(Unit)
    }
    private suspend fun awaitLoadingDialog() {
        val presentation=loadingPresentation ?: throw CancellationException("Загрузка отменена")
        presentation.await()
        // Keep even a cached ad from replacing the first rendered dialog frame immediately.
        delay(350)
        currentCoroutineContext().ensureActive()
    }
    private fun resumed(): Activity? = activity.get()?.takeIf {a ->!a.isFinishing && !a.isDestroyed && (a as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED)==true}
    private suspend fun initialize() {
        if(initialized) return
        YandexAds.setUserConsent(false)
        YandexAds.setLocationTracking(false)
        YandexAds.setAgeRestricted(BuildConfig.AUDIENCE=="children" || (BuildConfig.DEBUG && BuildConfig.AUDIENCE.isBlank()))
        suspendCancellableCoroutine<Unit> {continuation ->YandexAds.initialize(context) {initialized=true;if(continuation.isActive) continuation.resume(Unit)}}
    }
    fun clear() {if(_state.value.phase !in setOf("loading","showing","committing","storageError")) update("idle")}
    fun cancelLoading() {
        if(_state.value.phase=="loading") {
            val continuation=cancelTransition
            request?.cancel();update("idle")
            continuation?.invoke()
        }
    }
    fun rewarded(repo: GameRepository,placement: String,resultId: String?=null,hint: Hint?=null,target: String?=null) {
        if(request?.isActive==true || _state.value.phase in setOf("loading","showing","committing","storageError")) return
        lastOffer=RewardOffer(repo,placement,resultId,hint,target)
        if(!allowed) {update("unavailable",if(BuildConfig.ADS_ENABLED) "Разреши загрузку рекламы в настройках. Основная игра доступна без неё." else "Видео недоступно в этой версии.");return}
        val generation=ownerGeneration
        request=scope.launch {
            var ticket: AdTicket?=null
            try {
                val reserved=repo.reserveAd(placement,resultId,hint,target)
                ticket=reserved
                update("loading","Готовим видео",reserved.reward,hint=reserved.hintKind)
                val ad=withTimeout(30_000) {
                    awaitLoadingDialog()
                    val unitId=rewardedId(placement)
                    require(unitId.isNotBlank()) {"Не настроен рекламный блок для этого магазина"}
                    initialize()
                    trace("Запрос rewarded: $placement")
                    val loaded=suspendCancellableCoroutine<RewardedAd> {continuation ->
                        rewardedLoader.loadAd(AdRequest.Builder(unitId).build(),object : RewardedAdLoadListener {
                            override fun onAdLoaded(ad: RewardedAd) {if(continuation.isActive) {trace("Rewarded загружена");continuation.resume(ad)} else ad.setAdEventListener(null)}
                            override fun onAdFailedToLoad(error: AdRequestError) {if(continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("Видео: ${error.code} · ${error.description}")))}
                        })
                        continuation.invokeOnCancellation {scope.launch {rewardedLoader.cancelLoading()}}
                    }
                    loaded
                }
                val owner=resumed()
                if(owner==null || generation!=ownerGeneration || !allowed) {ad.setAdEventListener(null);repo.finishAd(reserved,"cancelled");update("idle");return@launch}
                var received=false
                rewardedAd=ad
                ad.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() {if(profileGeneration!=reserved.generation) return;update("showing",reward=reserved.reward,hint=reserved.hintKind);scope.launch {runCatching {repo.adShown(reserved)}}}
                    override fun onRewarded(reward: Reward) {
                        if(profileGeneration!=reserved.generation || received) return
                        received=true;pendingReward=repo to reserved
                        commitReward()
                    }
                    override fun onAdDismissed() {
                        if(profileGeneration!=reserved.generation) {ad.setAdEventListener(null);return}
                        scope.launch {
                            delay(500)
                            if(!received) {runCatching {repo.finishAd(reserved,"dismissed")};update("incomplete","Бонус пока не подтверждён")}
                            // Retain the listener briefly for an already scheduled late reward; the ticket remains durable.
                            delay(30_000);ad.setAdEventListener(null);if(rewardedAd===ad) rewardedAd=null
                        }
                    }
                    override fun onAdFailedToShow(error: AdError) {if(profileGeneration!=reserved.generation) {ad.setAdEventListener(null);return};ad.setAdEventListener(null);if(rewardedAd===ad) rewardedAd=null;scope.launch {runCatching {repo.finishAd(reserved,"failed")};update("unavailable","Видео не открылось: ${error.description}")}}
                    override fun onAdClicked() = Unit
                    override fun onAdImpression(data: ImpressionData?) = Unit
                })
                update("showing",reward=reserved.reward,hint=reserved.hintKind)
                ad.show(owner)
            } catch(e: CancellationException) {
                withContext(NonCancellable) {ticket?.let {runCatching {repo.finishAd(it,"cancelled")}}}
                if(e is TimeoutCancellationException) update("unavailable","Подготовка видео заняла слишком много времени")
                throw e
            } catch(e: Exception) {ticket?.let {runCatching {repo.finishAd(it,"failed")}};update("unavailable",e.localizedMessage ?: "Видео недоступно")}
        }
    }
    fun commitReward() {
        if(_state.value.phase=="committing") return
        val (repo,ticket)=pendingReward ?: return
        update("committing","Сохраняем награду",ticket.reward,hint=ticket.hintKind)
        scope.launch {
            try {
                repo.confirmAd(ticket)
                val entry=repo.committedAdReward(ticket)
                val hintText=if(ticket.hintKind!=null) repo.committedHintText(ticket) else null
                pendingReward=null
                if(profileGeneration!=ticket.generation) return@launch
                if(entry==null) update("idle")
                else if(ticket.hintKind!=null) update("hintReward",hintText ?: "Подсказка открыта",hint=ticket.hintKind)
                else update("reward","Бонус получен",entry.amount,entry.balanceAfter-entry.amount,entry.balanceAfter)
            }
            catch(e: Exception) {if(e.message=="Профиль изменился") {pendingReward=null;update("idle");return@launch};update("storageError","Просмотр подтверждён. Не удалось записать награду: повтори сохранение, смотреть ещё раз не нужно.",ticket.reward,hint=ticket.hintKind)}
        }
    }
    fun transition(repo: GameRepository,levelId: Int?,onContinue: () -> Unit) {
        if(_state.value.phase!="idle" || request?.isActive==true) return
        request=scope.launch {
            if(levelId==null || !allowed || resumed()==null) {onContinue();return@launch}
            val eligible=try {repo.interstitialAllowed(levelId)}
                catch(e: CancellationException) {throw e}
                catch(e: Exception) {trace("Не удалось проверить допуск рекламы: ${e.localizedMessage}");false}
            if(!eligible) {onContinue();return@launch}
            val generation=ownerGeneration;val profile=profileGeneration
            var loaded: InterstitialAd?=null
            var shownJob: Job?=null
            var continued=false;val impressionId=java.util.UUID.randomUUID().toString()
            fun finish() {if(!continued) {
                continued=true;cancelTransition=null;loaded?.setAdEventListener(null)
                if(interstitial===loaded) interstitial=null
                scope.launch {
                    shownJob?.join()
                    if(profileGeneration==profile) {update("idle");if(generation==ownerGeneration) onContinue()}
                }
            }}
            cancelTransition=::finish
            update("loading","Загрузка рекламы")
            try {
                val ad=withTimeout(30_000) {
                    awaitLoadingDialog()
                    require(interstitialId.isNotBlank()) {"Не настроен межстраничный рекламный блок"}
                    initialize()
                    trace("Запрос interstitial после уровня $levelId")
                    suspendCancellableCoroutine<InterstitialAd> {continuation ->
                        interstitialLoader.loadAd(AdRequest.Builder(interstitialId).build(),object: InterstitialAdLoadListener {
                            override fun onAdLoaded(ad: InterstitialAd) {if(continuation.isActive) {trace("Interstitial загружена");continuation.resume(ad)} else ad.setAdEventListener(null)}
                            override fun onAdFailedToLoad(error: AdRequestError) {
                                if(continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("Межстраничная: ${error.code} · ${error.description}")))
                            }
                        })
                        continuation.invokeOnCancellation {scope.launch {interstitialLoader.cancelLoading()}}
                    }
                }
                loaded=ad;interstitial=ad
                val currentOwner=resumed()
                if(!allowed || currentOwner==null || generation!=ownerGeneration || profileGeneration!=profile) {finish();return@launch}
                ad.setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        if(profileGeneration!=profile) return
                        update("showing")
                        shownJob=scope.launch {runCatching {repo.interstitialShown(impressionId,levelId)}}
                    }
                    override fun onAdDismissed()=finish()
                    override fun onAdFailedToShow(error: AdError)=finish()
                    override fun onAdClicked()=Unit
                    override fun onAdImpression(data: ImpressionData?)=Unit
                })
                update("showing")
                ad.show(currentOwner)
            } catch(e: CancellationException) {finish();throw e}
            catch(e: Exception) {
                trace(e.localizedMessage ?: "Ошибка загрузки interstitial")
                if(BuildConfig.DEBUG) android.util.Log.w("WordlineAds","Не удалось загрузить межстраничную рекламу",e)
                finish()
            }
        }
    }
}
