package ru.plumsoftware.wordline.game

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import ru.plumsoftware.wordline.data.parseCampaign
import java.io.File

class GameRulesTest {
    private val level=Level(1,listOf('К','О','Т'),3,3,listOf(
        Slot("a","КОТ",0,0,Direction.ACROSS),Slot("d","ТОК",0,2,Direction.DOWN)),emptySet())
    private val session=Session("test",1,order=listOf(0,1,2))
    private val dictionary=mapOf("КОТ" to Entry("КОТ","КОТ","Домашнее животное"),"ТОК" to Entry("ТОК","ТОК","Движение зарядов"))

    @Test fun normalizationKeepsRussianAlphabet() {
        assertEquals("ЕЛКА",wordKey("ёлка"))
        assertFalse(canSpell("КОТТ",level.letters))
        assertFalse(canSpell("КOТ",level.letters)) // Latin O is not Cyrillic О.
        assertTrue(canSpell("АННА",listOf('А','Н','Н','А')))
        assertFalse(canSpell("АННА",listOf('А','Н')))
    }
    @Test fun targetRevealsSharedCellAndDoesNotRepeat() {
        val first=submit(level,session,"кот",dictionary.keys)
        assertEquals(Answer.TARGET,first.answer)
        assertTrue("0:2" in visibleCells(level,first.session))
        val second=submit(level,first.session,"КОТ",dictionary.keys)
        assertEquals(Answer.ALREADY_FOUND,second.answer)
        assertEquals(first.session,second.session)
    }
    @Test fun intersectingCellsCanFinishAnotherSlot() {
        val s=session.copy(solved=setOf("a"),revealed=setOf("1:2","2:2"))
        assertTrue(settle(level,s).completed)
        assertEquals(setOf("a","d"),settle(level,s).solved)
    }
    @Test fun hintsOnlyAffectChosenHiddenCell() {
        val result=hint(level,session,Hint.LETTER,"0:0",Economy(),dictionary)!!
        assertEquals(50L,result.cost)
        assertEquals(setOf("0:0"),result.session.revealed)
        assertEquals(2,result.session.stars)
        assertNull(hint(level,result.session,Hint.LETTER,"0:0",Economy(),dictionary))
        assertTrue(session.revealed.isEmpty())
    }
    @Test fun clueDoesNotRevealAndRereadingIsFree() {
        val first=hint(level,session,Hint.CLUE,"a",Economy(),dictionary)!!
        val second=hint(level,first.session,Hint.CLUE,"a",Economy(),dictionary)!!
        assertEquals(150L,first.cost)
        assertEquals(0L,second.cost)
        assertEquals(1,second.session.hintCount)
        assertTrue(visibleCells(level,second.session).isEmpty())
    }
    @Test fun replayHintsAreFreeButReduceStars() {
        val result=hint(level,session.copy(replay=true),Hint.WORD,"a",Economy(),dictionary)!!
        assertEquals(0L,result.cost)
        assertEquals(2,result.session.stars)
    }
    @Test fun unknownAndRepeatedBonusHaveNoEffect() {
        val l=level.copy(bonusPool=setOf("ТОР"),letters=listOf('К','О','Т','Р'))
        val first=submit(l,session,"ТОР",setOf("ТОР"))
        assertEquals(Answer.BONUS,first.answer)
        assertEquals(Answer.ALREADY_FOUND,submit(l,first.session,"ТОР",setOf("ТОР")).answer)
        assertEquals(session,submit(l,session,"КОР",dictionary.keys).session)
    }
    @Test fun fastMovementFindsIntermediateLettersInOrder() {
        val centers=listOf(Point(0f,0f),Point(50f,0f),Point(100f,0f))
        assertEquals(listOf(0,1,2),crossedTokens(Point(-20f,0f),Point(120f,0f),centers,10f))
        assertEquals(listOf(0),appendToken(listOf(0,1),0))
        assertEquals(listOf(0,1,2),appendToken(listOf(0,1,2),0))
    }
    @Test fun packagedCampaignIsValidAndSolvable() {
        val dir=File(requireNotNull(System.getProperty("gameContentDir")))
        val manifest=JSONObject(File(dir,"manifest.json").readText())
        val campaign=parseCampaign(File(dir,"campaign-v1.json").readBytes(),manifest.getString("sha256"))
        assertEquals(1000,campaign.levels.size)
        assertEquals(365,campaign.dailyLevels.size)
        (campaign.levels+campaign.dailyLevels).forEach { l ->
            var s=Session("level-${l.id}",l.id,order=l.letters.indices.toList())
            l.slots.forEach { s=submit(l,s,it.word,campaign.dictionary.keys).session }
            assertTrue("Level ${l.id}",s.completed)
            assertEquals(3,s.stars)
        }
    }
    @Test(expected=IllegalArgumentException::class) fun invalidChecksumRejectsContent() {
        parseCampaign("{}".toByteArray(),"invalid")
    }
    @Test(expected=IllegalArgumentException::class) fun crossingWithDifferentLettersIsRejected() {
        validateCampaign(Campaign(1,listOf(level.copy(slots=level.slots+Slot("bad","КОТ",0,2,Direction.DOWN))),dictionary))
    }
}
