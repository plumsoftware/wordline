package ru.plumsoftware.wordline.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.plumsoftware.wordline.game.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** One serialized transaction owns money, accepted events, claims and profile generation. */
class GameRepository(internal val db: GameDatabase, val campaign: Campaign,
                     val economy: Economy = Economy(), private val clock: GameClock = DeviceClock) {
    internal val dao=db.gameDao()
    private val mutex=Mutex()
    var processStartActive: Long = 0; private set
    val snapshot=combine(dao.observeProfile(),dao.observeCompletions(),dao.observeSessions(),dao.observeWorld()) { p,c,s,w ->
        GameSnapshot(p,c.associateBy {it.levelId},s.associate {it.id to it.toDomain()},w?.let {worldFromJson(org.json.JSONObject(it.json))} ?: World(generation="loading",streakRun="loading"))
    }
    internal fun resetActivityBaseline(value: Long) {processStartActive=value}
    internal suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { db.withTransaction {block()} }
    internal suspend fun world(): World = worldFromJson(org.json.JSONObject(requireNotNull(dao.world()).json))
    internal suspend fun save(world: World) {dao.save(WorldRow(json=world.toJson().toString()))}
    internal suspend fun record(id: String, reason: String, source: String, amount: Long): Long {
        if(dao.operation(id)!=null) return 0
        val profile=requireNotNull(dao.profile())
        require(amount in -economy.cap..economy.cap && profile.coins+amount>=0) {"Недостаточно монет"}
        val balance=(profile.coins+amount).coerceAtMost(economy.cap)
        dao.save(profile.copy(coins=balance))
        dao.record(LedgerRow(id,reason,source,balance-profile.coins,balance,clock.millis()))
        return balance-profile.coins
    }
    suspend fun initialize() = exclusive {
        if(dao.profile()==null) {
            dao.save(ProfileRow(coins=0))
            record("profile:init","initial","profile",economy.initial)
        }
        if(dao.world()==null) {
            // Explicit v1 -> v2 data migration, retaining every original table and receipt.
            val sessions=dao.sessions().map {it.toDomain()}.filter {!it.replay}
            val completions=dao.completions()
            val words=sessions.flatMap {s ->campaign.levelFor(s).slots.filter {it.id in s.solved}.map {it.word}+s.bonus}
            save(World(totalWords=words.size,uniqueWords=words.toSet(),bonusWords=sessions.sumOf {it.bonus.size},
                hints=sessions.sumOf {it.hintCount},noHintLevels=completions.filter {it.bestStars==3}.map {it.levelId}.toSet(),
                trailEvents=completions.take(30).map {"campaign:${it.levelId}"}.toSet(),
                collectibles=completions.filter {(it.levelId-20)%100==0}.map {"location:${(it.levelId-20)/100}"}.toSet()))
        }
        val current=day()
        processStartActive=current.activeMillis
        save(current.copy(adTickets=current.adTickets.mapValues {(_,a) ->if(a.status in setOf("pending","shown","dismissed")) a.copy(status="interrupted") else a}))
    }
    private suspend fun day(): World {
        var w=world()
        val candidate=clock.today();val effective=effectiveDay(w.effectiveDate,candidate);val date=effective.toString()
        val finished=dao.completions().size==campaign.levels.size
        if(w.effectiveDate.isNotBlank() && date!=w.effectiveDate) {
            tasks(w,w.effectiveDate,finished).filter {it.progress>=it.target}.forEach {t ->
                val id="task:${w.effectiveDate}:${t.id}"
                if(id !in w.claims) {val amount=record(id,"task-auto",w.effectiveDate,t.reward);w=w.copy(claims=w.claims+id,dailyReport=(w.dailyReport+"${t.title}: +$amount монет").takeLast(20))}
            }
            val milestone=w.streak/7*7
            val key="streak:${w.streakRun}:$milestone"
            if(milestone>0 && key !in w.claims) {val amount=record(key,"streak-auto",w.streakRun,50);w=w.copy(claims=w.claims+key,dailyReport=(w.dailyReport+"Серия посещений: +$amount монет").takeLast(20))}
        }
        if(date !in w.visits) {
            val consecutive=w.effectiveDate.isNotBlank() && LocalDate.parse(w.effectiveDate).plusDays(1)==effective
            val streak=if(consecutive) w.streak+1 else 1
            w=w.copy(streak=streak,bestStreak=maxOf(streak,w.bestStreak),streakRun=if(consecutive) w.streakRun else UUID.randomUUID().toString(),visits=w.visits+date)
        }
        val warning="clock:$date"
        if(candidate<effective && warning !in w.claims) w=w.copy(clockNotice=true,claims=w.claims+warning)
        w=w.copy(effectiveDate=date)
        save(w)
        return w
    }
    suspend fun foreground() = exclusive {day();Unit}
    suspend fun dismissReport() = exclusive {save(world().copy(dailyReport=emptyList()))}
    suspend fun dismissClockNotice() = exclusive {save(world().copy(clockNotice=false))}
    suspend fun acknowledgeLocation(index: Int) = exclusive {val w=world();save(w.copy(acknowledgedLocations=w.acknowledgedLocations+index))}
    suspend fun activeTime(millis: Long) = exclusive {
        require(millis in 0..10_000)
        val w=day();save(w.withDay(w.effectiveDate) {it.copy(activeMillis=it.activeMillis+millis)}.copy(activeMillis=w.activeMillis+millis))
    }
    suspend fun start(levelId: Int? = null, replay: Boolean = false): String = exclusive {
        day()
        val profile=requireNotNull(dao.profile())
        if(levelId==null && !replay) profile.campaignSessionId?.let {if(dao.session(it)!=null) return@exclusive it}
        val level=if(levelId==null) campaign.levels.firstOrNull {dao.completion(it.id)==null} else campaign.levels.firstOrNull {it.id==levelId}
        requireNotNull(level) {"Кампания пройдена"}
        require(level.id==1 || dao.completion(level.id-1)!=null) {"Сначала пройдите предыдущий уровень"}
        require((dao.completion(level.id)!=null)==replay) {"Пройденный уровень доступен в режиме повтора"}
        val id=if(replay) "replay-${UUID.randomUUID()}" else "campaign-${level.id}"
        if(dao.session(id)==null) dao.save(Session(id,level.id,replay,order=level.letters.indices.toList()).toRow())
        if(!replay) dao.save(requireNotNull(dao.profile()).copy(campaignSessionId=id))
        id
    }
    suspend fun tutorial(): String = exclusive {
        val id="tutorial";val level=campaign.levels.first()
        if(dao.session(id)==null) dao.save(Session(id,level.id,order=level.letters.indices.toList(),mode=Mode.TUTORIAL).toRow())
        id
    }
    suspend fun startDaily(date: String): String = exclusive {
        var w=day();val d=LocalDate.parse(date);val today=LocalDate.parse(w.effectiveDate)
        val oldActive=dao.sessions().map {it.toDomain()}.firstOrNull {it.mode==Mode.DAILY && !it.completed && !it.replay}
        if(oldActive?.dateKey==date) return@exclusive oldActive.id
        require(!d.isAfter(today) && YearMonth.from(d)==YearMonth.from(today)) {"Доступны прошедшие дни текущего месяца"}
        require(oldActive==null || oldActive.dateKey==date) {"Сначала закончи ежедневный кроссворд за ${oldActive?.dateKey}"}
        val index=w.dailyAssignments[date] ?: campaign.dailyLevels[dailyIndex(d,campaign.dailyLevels.size)].id
        val level=campaign.dailyLevels.first {it.id==index}
        val replay=date in w.dailyCompleted
        val id=if(replay) "daily-replay-${UUID.randomUUID()}" else "daily-$date"
        if(dao.session(id)==null) dao.save(Session(id,level.id,replay,order=level.letters.indices.toList(),mode=Mode.DAILY,dateKey=date).toRow())
        w=w.copy(dailyAssignments=w.dailyAssignments+(date to index));save(w)
        id
    }
    suspend fun next(id: String): String? = exclusive {
        val s=requireNotNull(dao.session(id)).toDomain();require(s.completed)
        if(s.mode==Mode.TUTORIAL) {
            val key="campaign-1";val l=campaign.levels.first()
            if(dao.session(key)==null) dao.save(Session(key,1,order=l.letters.indices.toList()).toRow())
            dao.save(requireNotNull(dao.profile()).copy(campaignSessionId=key));return@exclusive key
        }
        if(s.replay || s.mode==Mode.DAILY) return@exclusive null
        val level=campaign.levels.firstOrNull {it.id==s.levelId+1} ?: return@exclusive null
        val key="campaign-${level.id}"
        if(dao.session(key)==null) dao.save(Session(key,level.id,order=level.letters.indices.toList()).toRow())
        dao.save(requireNotNull(dao.profile()).copy(campaignSessionId=key));key
    }
    private suspend fun finish(s: Session, w: World): Pair<Session,World> {
        if(!s.completed) return s to w
        if(s.mode==Mode.TUTORIAL) return s.copy(reward=0) to w
        var world=w;var reward=0L
        if(s.mode==Mode.CAMPAIGN) {
            val old=dao.completion(s.levelId)
            dao.save(CompletionRow(s.levelId,maxOf(old?.bestStars ?: 0,s.stars),old?.completedAt ?: clock.millis()))
            if(s.stars==3) world=world.copy(noHintLevels=world.noHintLevels+s.levelId)
            if(!s.replay && old==null) {
                reward=record("campaign:first:${s.levelId}","victory",s.id,economy.victory)
                world=world.withDay(w.effectiveDate) {it.copy(wins=it.wins+1)}.copy(winsSinceInterstitial=world.winsSinceInterstitial+1)
                if((s.levelId-20)%100==0 && s.levelId>=20) world=world.copy(collectibles=world.collectibles+"location:${(s.levelId-20)/100}")
            }
        } else if(!s.replay && s.dateKey !in world.dailyCompleted) {
            reward=record("daily:${s.dateKey}","daily",s.id,economy.daily)
            world=world.withDay(w.effectiveDate) {it.copy(daily=it.daily+1)}.copy(dailyCompleted=world.dailyCompleted+requireNotNull(s.dateKey),winsSinceInterstitial=world.winsSinceInterstitial+1)
            val month=s.dateKey.take(7)
            if(world.dailyCompleted.count {it.startsWith(month)}>=20) world=world.copy(collectibles=world.collectibles+"month:$month")
        }
        if(!s.replay) {
            val event=if(s.mode==Mode.DAILY) "daily:${s.dateKey}" else "campaign:${s.levelId}"
            if(world.trailEvents.size<30) world=world.copy(trailEvents=world.trailEvents+event)
        }
        return s.copy(reward=reward) to world
    }
    suspend fun submitWord(id: String, word: String, operationId: String, expectedRevision: Long? = null): String = exclusive {
        var w=day()
        if(dao.operation(operationId)!=null) return@exclusive "Действие уже сохранено"
        val s=requireNotNull(dao.session(id)).toDomain()
        if(expectedRevision!=null && expectedRevision!=s.revision) return@exclusive "Поле обновилось. Попробуй ещё раз."
        val level=campaign.levelFor(s);val result=submit(level,s,word,campaign.dictionary.keys)
        if(result.session!=s) {
            record(operationId,"word",id,0)
            val changed=result.session.solved-s.solved
            val addedWords=level.slots.filter {it.id in changed}.map {it.word}+(result.session.bonus-s.bonus)
            val extra=result.session.bonus.size-s.bonus.size
            if(!s.replay && s.mode!=Mode.TUTORIAL) {
                w=w.withDay(w.effectiveDate) {it.copy(words=it.words+addedWords.size,bonus=it.bonus+extra)}
                    .copy(totalWords=w.totalWords+addedWords.size,uniqueWords=w.uniqueWords+addedWords,bonusWords=w.bonusWords+extra)
                if(result.session.bonus.size/5>s.bonus.size/5) {
                    record("chest:$id:${result.session.bonus.size/5}","bonus-chest",id,economy.chest)
                    w=w.copy(chests=w.chests+1)
                }
            }
            val (next,world)=finish(result.session.copy(revision=s.revision+1),w)
            dao.save(next.toRow());save(world)
        }
        when(result.answer) {
            Answer.TARGET -> "Слово найдено!";Answer.BONUS -> "Дополнительное слово!"
            Answer.ALREADY_FOUND -> "Уже найдено";Answer.OUTSIDE_LEVEL -> "Слово верное, но не входит в этот уровень"
            Answer.UNKNOWN -> "Попробуй другое слово";Answer.IMPOSSIBLE -> "Слово нельзя собрать из этих букв";Answer.FINISHED -> "Уровень уже завершён"
        }
    }
    suspend fun purchaseHint(id: String, kind: Hint, target: String, operationId: String, expectedRevision: Long? = null): String = exclusive {
        var w=day()
        if(dao.operation(operationId)!=null) return@exclusive "Подсказка уже сохранена"
        val s=requireNotNull(dao.session(id)).toDomain()
        if(expectedRevision!=null && expectedRevision!=s.revision) return@exclusive "Поле обновилось. Выбери подсказку заново."
        val result=hint(campaign.levelFor(s),s,kind,target,economy,campaign.dictionary) ?: return@exclusive "Выбранный элемент уже раскрыт"
        val balance=requireNotNull(dao.profile()).coins
        if(balance<result.cost) return@exclusive "Недостаточно монет. Нужно ещё ${result.cost-balance}. Можно продолжить без подсказки."
        record(operationId,"hint:${kind.name}",id,-result.cost)
        if(!s.replay && s.mode!=Mode.TUTORIAL) w=w.copy(hints=w.hints+(result.session.hintCount-s.hintCount))
        val (next,world)=finish(result.session.copy(revision=s.revision+1),w)
        dao.save(next.toRow());save(world)
        result.definition ?: "Подсказка открыта"
    }
    suspend fun shuffle(id: String) = exclusive {
        val s=requireNotNull(dao.session(id)).toDomain()
        if(!s.completed) {
            var order=s.order.shuffled();if(order==s.order) order=order.drop(1)+order.first()
            dao.save(s.copy(order=order,revision=s.revision+1).toRow())
        }
    }
    suspend fun claimTask(taskId: String) = exclusive {
        val w=day();val task=tasks(w,w.effectiveDate,dao.completions().size==campaign.levels.size).first {it.id==taskId}
        val key="task:${w.effectiveDate}:$taskId"
        require(task.progress>=task.target) {"Задание ещё не завершено"}
        if(key !in w.claims) {record(key,"task",w.effectiveDate,task.reward);save(w.copy(claims=w.claims+key))}
    }
    suspend fun claimGift() = exclusive {
        val w=day();val key="gift:${w.effectiveDate}"
        if(key !in w.claims) {record(key,"gift",w.effectiveDate,economy.gifts[w.giftStep]);save(w.copy(giftStep=(w.giftStep+1)%economy.gifts.size,claims=w.claims+key))}
    }
    suspend fun claimStreak() = exclusive {
        val w=day();val milestone=w.streak/7*7;require(milestone>0)
        val key="streak:${w.streakRun}:$milestone"
        if(key !in w.claims) {record(key,"streak",w.streakRun,50);save(w.copy(claims=w.claims+key))}
    }
    suspend fun claimTrail(milestone: Int) = exclusive {
        val w=day();require(milestone in trailRewards && w.trailEvents.size>=milestone)
        val key="trail:$milestone"
        if(key !in w.claims) {record(key,"trail",key,trailRewards.getValue(milestone));save(w.copy(claims=w.claims+key,collectibles=w.collectibles+"trail:$milestone"))}
    }
    suspend fun claimAchievement(id: String) = exclusive {
        val w=day();val a=achievements(w,dao.completions().size).first {it.id==id};require(a.progress>=a.target)
        val key="achievement:$id"
        if(key !in w.claims) {record(key,"achievement",id,a.reward);save(w.copy(claims=w.claims+key))}
    }
    suspend fun markExported() = exclusive {val w=world();save(w.copy(lastExportAt=clock.millis()))}
    suspend fun reserveAd(placement: String, resultId: String?): AdTicket = exclusive {
        val w=day()
        require(placement in setOf("hint_coins","wallet_coins","win_extra"))
        if(placement=="hint_coins") {
            val session=requireNotNull(resultId?.let {dao.session(it)}).toDomain()
            require(!session.completed && !session.replay && session.mode!=Mode.TUTORIAL) {"Видео недоступно в этой сессии"}
        }
        require(w.adTickets.values.none {it.status in setOf("pending","shown")}) {"Другое видео ещё открыто"}
        require(w.adTickets.values.count {it.date==w.effectiveDate && it.status in setOf("pending","shown","committed")}<economy.rewardedMax) {"Бонусные видео на сегодня закончились"}
        val last=w.adShowings.lastOrNull {it.rewarded}
        require(last==null || w.activeMillis-last.activeMillis>=60_000) {"Следующее видео будет доступно после минуты игры"}
        if(placement=="win_extra") {
            val s=requireNotNull(resultId?.let {dao.session(it)}).toDomain()
            require(s.completed && !s.replay && s.mode!=Mode.TUTORIAL)
            require(w.adTickets.values.none {it.resultId==resultId && it.placement==placement && it.status=="committed"}) {"Бонус уже получен"}
        }
        val a=AdTicket(UUID.randomUUID().toString(),w.generation,placement,w.effectiveDate,resultId,if(placement=="win_extra") economy.winExtra else economy.rewarded)
        save(w.copy(adTickets=w.adTickets+(a.id to a)));a
    }
    suspend fun adShown(ticket: AdTicket) = exclusive {
        val w=world();if(w.generation!=ticket.generation) return@exclusive
        val a=w.adTickets[ticket.id] ?: return@exclusive
        if(a.status in setOf("failed","cancelled","interrupted") || w.adShowings.any {it.ticketId==ticket.id}) return@exclusive
        save(w.copy(adTickets=w.adTickets+(a.id to if(a.status=="committed") a else a.copy(status="shown")),adShowings=(w.adShowings+AdShowing(ticket.date,maxOf(clock.millis(),w.adShowings.lastOrNull()?.wallMillis ?: 0),w.activeMillis,true,ticket.id)).takeLast(200)))
    }
    suspend fun finishAd(ticket: AdTicket, status: String) = exclusive {
        val w=world();if(w.generation!=ticket.generation) return@exclusive
        val a=w.adTickets[ticket.id] ?: return@exclusive
        if(a.status!="committed") save(w.copy(adTickets=w.adTickets+(a.id to a.copy(status=status))))
    }
    suspend fun confirmAd(ticket: AdTicket): Long = exclusive {
        val w=world();check(w.generation==ticket.generation) {"Профиль изменился"}
        val a=w.adTickets[ticket.id] ?: return@exclusive 0L
        if(a.status=="committed" || a.status in setOf("failed","interrupted","cancelled")) return@exclusive 0L
        val amount=record("ad:${ticket.id}","rewarded",ticket.placement,ticket.reward)
        save(w.copy(adTickets=w.adTickets+(a.id to a.copy(status="committed"))));amount
    }
    suspend fun interstitialAllowed(): Boolean = exclusive {
        val w=day();val rule=economy.ads;val count=dao.completions().size
        val last=w.adShowings.lastOrNull {!it.rewarded};val rewarded=w.adShowings.lastOrNull {it.rewarded}
        val wall=maxOf(clock.millis(),w.adShowings.lastOrNull()?.wallMillis ?: 0)
        count>=rule.firstLevels && w.activeMillis>=rule.firstSeconds*1000 && w.winsSinceInterstitial>=rule.minWins &&
            w.activeMillis-processStartActive>=rule.minSeconds*1000 && (last==null || w.activeMillis-last.activeMillis>=rule.minSeconds*1000) &&
            (rewarded==null || w.activeMillis-rewarded.activeMillis>=rule.rewardedCooldown*1000) &&
            w.adShowings.count {!it.rewarded && wall-it.wallMillis in 0..3_599_999}<rule.hourMax &&
            w.adShowings.count {!it.rewarded && it.date==w.effectiveDate}<rule.dayMax
    }
    suspend fun interstitialShown(impressionId: String) = exclusive {
        val w=day();if(w.adShowings.any {it.ticketId==impressionId}) return@exclusive;save(w.copy(winsSinceInterstitial=0,adShowings=(w.adShowings+AdShowing(w.effectiveDate,maxOf(clock.millis(),w.adShowings.lastOrNull()?.wallMillis ?: 0),w.activeMillis,false,impressionId)).takeLast(200)))
    }
}
data class GameSnapshot(val profile: ProfileRow?, val completions: Map<Int,CompletionRow>, val sessions: Map<String,Session>, val world: World = World())
