package ru.plumsoftware.wordline.game

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.plumsoftware.wordline.data.*

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    @Test fun initializationVictoryAndReplayCannotDuplicateMoney() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val campaign=loadCampaign(context)
            val repo=GameRepository(db,campaign)
            repo.initialize();repo.initialize()
            assertEquals(200L,db.gameDao().profile()!!.coins)
            val id=repo.start()
            campaign.levels.first().slots.forEach {repo.submitWord(id,it.word,"solve-${it.id}")}
            val after=db.gameDao().profile()!!.coins
            assertEquals(220L,after)
            campaign.levels.first().slots.forEach {repo.submitWord(id,it.word,"solve-${it.id}")}
            assertEquals(after,db.gameDao().profile()!!.coins)
            val replay=repo.start(1,true)
            campaign.levels.first().slots.forEach {repo.purchaseHint(replay,Hint.WORD,it.id,"hint-${it.id}")}
            assertEquals(after,db.gameDao().profile()!!.coins)
            assertEquals(3,repo.snapshot.first().completions.getValue(1).bestStars)
        } finally {db.close()}
    }
    @Test fun hintReceiptIsIdempotentAndInsufficientBalanceDoesNotReveal() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val campaign=loadCampaign(context)
            val repo=GameRepository(db,campaign)
            repo.initialize()
            val id=repo.start()
            repo.purchaseHint(id,Hint.LETTER,"0:0","unique-hint")
            repo.purchaseHint(id,Hint.LETTER,"0:1","unique-hint")
            assertEquals(150L,db.gameDao().profile()!!.coins)
            assertEquals(setOf("0:0"),db.gameDao().session(id)!!.toDomain().revealed)
            db.gameDao().save(db.gameDao().profile()!!.copy(coins=0))
            val before=db.gameDao().session(id)
            repo.purchaseHint(id,Hint.LETTER,"0:1","no-money")
            assertEquals(before,db.gameDao().session(id))
            assertNull(db.gameDao().operation("no-money"))
        } finally {db.close()}
    }
    @Test fun snapshotRestoresAcceptedWordsAndOrderAcrossRepositoryRecreation() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,GameDatabase::class.java).build()
        try {
            val campaign=loadCampaign(context)
            val first=GameRepository(db,campaign)
            first.initialize()
            val id=first.start()
            first.submitWord(id,campaign.levels.first().slots.first().word,"accepted")
            first.shuffle(id)
            val stored=db.gameDao().session(id)!!.toDomain()
            val second=GameRepository(db,campaign)
            second.initialize()
            assertEquals(id,second.start())
            assertEquals(stored,second.snapshot.first().sessions[id])
        } finally {db.close()}
    }
}
