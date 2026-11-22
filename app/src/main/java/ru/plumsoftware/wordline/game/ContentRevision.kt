package ru.plumsoftware.wordline.game

/** Retired answers are kept here only to recognize historical saves, never as playable vocabulary. */
val retiredContentWords=setOf("РОМ","БАР","СЕМЯ","МОР")

/** v3 preserves every slot ID, coordinate and length, so paid cells and solved slots remain valid. */
fun Session.adaptToContent(campaign: Campaign): Session {
    if(campaign.version<3) return this
    val level=campaign.levelFor(this)
    val cleanBonus=bonus.intersect(level.bonusPool)
    val cleanOrder=if(order.sorted()==level.letters.indices.toList()) order else level.letters.indices.toList()
    if(cleanBonus==bonus && cleanOrder==order) return this
    return copy(bonus=cleanBonus,order=cleanOrder,revision=revision.coerceAtMost(Long.MAX_VALUE-1)+1)
}
