package ru.plumsoftware.wordline.game

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.plumsoftware.wordline.data.*
import ru.plumsoftware.wordline.files.SaveRepository
import java.time.LocalDate

private class MutableClock(var date: LocalDate=LocalDate.of(2026,11,20)): GameClock {
    override fun today()=date
    override fun millis()=date.toEpochDay()*86_400_000
}
@RunWith(AndroidJUnit4::class)
class ProgressPersistenceTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun giftIsOnePerEffectiveDateAndClockRollbackDoesNotRepeatIt()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val clock=MutableClock();val repo=GameRepository(db,loadCampaign(context),clock=clock);repo.initialize()
            repo.claimGift();repo.claimGift();assertEquals(220L,db.gameDao().profile()!!.coins)
            clock.date=clock.date.minusDays(1);repo.foreground();repo.claimGift()
            assertEquals(220L,db.gameDao().profile()!!.coins)
            assertTrue(repo.world().clockNotice)
            clock.date=LocalDate.of(2026,11,21);repo.claimGift()
            assertEquals(245L,db.gameDao().profile()!!.coins);assertEquals(2,repo.world().giftStep)
        } finally {db.close()}
    }
    @Test fun rolloverAutoClaimsOnlyReadyUnclaimedTasks()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val clock=MutableClock();val repo=GameRepository(db,loadCampaign(context),clock=clock);repo.initialize()
            repo.exclusive {val w=repo.world();repo.save(w.copy(days=mapOf(w.effectiveDate to DayStats(words=10,wins=3,bonus=1))))}
            repo.claimTask("words");repo.claimTask("words");assertEquals(215L,db.gameDao().profile()!!.coins)
            clock.date=clock.date.plusDays(1);repo.foreground();repo.foreground()
            assertEquals(245L,db.gameDao().profile()!!.coins)
            assertEquals(2,repo.world().dailyReport.size)
        } finally {db.close()}
    }
    @Test fun dailyCompletionUsesDateAndCannotPayOnReplay()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val campaign=loadCampaign(context);val repo=GameRepository(db,campaign,clock=MutableClock());repo.initialize()
            val id=repo.startDaily("2026-11-20");val s=db.gameDao().session(id)!!.toDomain()
            campaign.levelFor(s).slots.forEach {repo.submitWord(id,it.word,"daily-${it.id}")}
            assertEquals(230L,db.gameDao().profile()!!.coins);assertTrue(db.gameDao().completions().isEmpty())
            val repeat=repo.startDaily("2026-11-20")
            campaign.levelFor(s).slots.forEach {repo.purchaseHint(repeat,Hint.WORD,it.id,"repeat-${it.id}")}
            assertEquals(230L,db.gameDao().profile()!!.coins);assertEquals(1,repo.world().trailEvents.size)
        } finally {db.close()}
    }
    @Test fun lateRewardAfterDismissCommitsOnce()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val repo=GameRepository(db,loadCampaign(context),clock=MutableClock());repo.initialize()
            val ticket=repo.reserveAd("wallet_coins",null)
            repo.adShown(ticket);repo.adShown(ticket);repo.finishAd(ticket,"dismissed")
            repo.confirmAd(ticket);repo.confirmAd(ticket)
            assertEquals(230L,db.gameDao().profile()!!.coins);assertEquals(1,repo.world().adShowings.size)
            assertEquals("committed",repo.world().adTickets.getValue(ticket.id).status)
        } finally {db.close()}
    }
    @Test fun saveRoundtripPreviewDoesNotMutateProfile()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val repo=GameRepository(db,loadCampaign(context),clock=MutableClock());repo.initialize();repo.claimGift()
            val saves=SaveRepository(context,repo)
            val before=db.gameDao().profile();val candidate=saves.validate(saves.exportBytes(GameSettings()))
            assertEquals(220L,candidate.coins);assertEquals(before,db.gameDao().profile())
        } finally {db.close()}
    }
    @Test fun versionOneDatabaseMigratesWithoutResettingWallet()=runBlocking {
        val name="migration-${java.util.UUID.randomUUID()}.db";val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file,null).use {old ->
            old.execSQL("CREATE TABLE profile (id INTEGER NOT NULL PRIMARY KEY, coins INTEGER NOT NULL, campaignSessionId TEXT)")
            old.execSQL("CREATE TABLE sessions (id TEXT NOT NULL PRIMARY KEY, levelId INTEGER NOT NULL, replay INTEGER NOT NULL, json TEXT NOT NULL)")
            old.execSQL("CREATE TABLE completions (levelId INTEGER NOT NULL PRIMARY KEY, bestStars INTEGER NOT NULL, completedAt INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE ledger (operationId TEXT NOT NULL PRIMARY KEY, reason TEXT NOT NULL, sourceId TEXT NOT NULL, amount INTEGER NOT NULL, balanceAfter INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            old.execSQL("INSERT INTO profile VALUES (1,123,NULL)")
            old.execSQL("INSERT INTO ledger VALUES ('profile:init','initial','profile',123,123,0)")
            old.version=1
        }
        val db=Room.databaseBuilder(context,GameDatabase::class.java,name).addMigrations(GameDatabase.MIGRATION_1_2).build()
        try {
            val repo=GameRepository(db,loadCampaign(context),clock=MutableClock());repo.initialize()
            assertEquals(123L,db.gameDao().profile()!!.coins)
            assertNotNull(db.gameDao().world());assertEquals(1,db.gameDao().ledger().size)
        } finally {db.close();context.deleteDatabase(name)}
    }
}
