package ru.plumsoftware.wordline.game

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ProgressionTest {
    @Test fun dateNeverMovesBackwards() {
        assertEquals(LocalDate.of(2026,11,20),effectiveDay("2026-11-20",LocalDate.of(2026,11,19)))
        assertEquals(LocalDate.of(2026,11,21),effectiveDay("2026-11-20",LocalDate.of(2026,11,21)))
    }
    @Test fun dailyScheduleIsStableIncludingBeforeEpoch() {
        assertEquals(364,dailyIndex(LocalDate.of(2025,12,31),365))
        assertEquals(0,dailyIndex(LocalDate.of(2026,1,1),365))
        assertEquals(0,dailyIndex(LocalDate.of(2027,1,1),365))
    }
    @Test fun completedCampaignChangesTaskWithoutDroppingReadyPrize() {
        val w=World(days=mapOf("2026-11-20" to DayStats(wins=3)))
        val ready=tasks(w,"2026-11-20",true)[1]
        assertEquals("levels",ready.id);assertEquals(3,ready.target);assertEquals(3,ready.progress)
        assertEquals(1,tasks(w,"2026-11-21",true)[1].target)
    }
    @Test fun achievementsUseDistinctLevelAndWordMetrics() {
        val list=achievements(World(totalWords=120,uniqueWords=setOf("КОТ"),noHintLevels=setOf(1,2)),10).associateBy {it.id}
        assertEquals(120,list.getValue("ACH04").progress)
        assertEquals(1,list.getValue("ACH06").progress)
        assertEquals(2,list.getValue("ACH07").progress)
        assertEquals(10,list.getValue("ACH01").progress)
    }
    @Test fun expandedAchievementsKeepExistingRewardsAndHaveRequirements() {
        val list=achievements(World(),0)
        assertEquals(30,list.size)
        assertEquals(list.size,list.map {it.id}.toSet().size)
        assertTrue(list.all {it.requirement.isNotBlank() && it.target>0 && it.reward>0})
        val legacy=list.filter {it.id in (1..12).map {id -> "ACH%02d".format(id)}}
        assertEquals(listOf(10,100,500,100,1000,100,10,50,10,7,20,5),legacy.map {it.target})
        assertEquals(listOf(20L,50L,100L,30L,75L,40L,30L,40L,40L,50L,75L,100L),legacy.map {it.reward})
    }
    @Test fun newAchievementsUseSavedDistinctProgress() {
        val world=World(visits=setOf("2026-11-20","2026-11-22"),bestStreak=1,
            dailyCompleted=setOf("2026-11-20"),trailEvents=setOf("campaign:1","daily:2026-11-20"),
            collectibles=setOf("location:0","trail:10"))
        val list=achievements(world,1).associateBy {it.id}
        assertEquals(2,list.getValue("ACH30").progress)
        assertEquals(1,list.getValue("ACH24").progress)
        assertEquals(1,list.getValue("ACH26").progress)
        assertEquals(2,list.getValue("ACH28").progress)
        assertEquals(2,list.getValue("ACH29").progress)
        assertEquals(1,list.getValue("ACH13").progress)
    }
    @Test fun tutorialLetterDoesNotChargeOrReduceStars() {
        val level=Level(1,listOf('К','О','Т'),3,3,listOf(Slot("s1","КОТ",0,0,Direction.ACROSS),Slot("s2","ТОК",0,2,Direction.DOWN)),emptySet())
        val result=hint(level,Session("tutorial",1,mode=Mode.TUTORIAL),Hint.LETTER,"0:0",Economy(),emptyMap())!!
        assertEquals(0L,result.cost);assertEquals(3,result.session.stars);assertTrue(result.session.tutorialLetterUsed)
        assertEquals(50L,hint(level,result.session,Hint.LETTER,"0:1",Economy(),emptyMap())!!.cost)
    }
}
