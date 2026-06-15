package com.studyminder.data.model

data class RankInfo(
    val name: String,
    val requiredPoints: Int,
    val quote: String,
    val assetFileName: String,
    val tier: RankTier,
    val starCount: Int = 0,
    val glowColor: Long = 0xFF4CAF50,
    val glowColor2: Long = 0xFF4CAF50,
    val isOverflowBadge: Boolean = false
)

enum class RankTier { STAR, MOON, DIAMOND, CROWN, TRIDENT, INFINITY, COSMOS }

object RankSystem {
    val ranks = listOf(
        RankInfo("Acolyte", 5,
            "Takes the first step into the pursuit of knowledge.",
            "Acolyte.png", RankTier.STAR, starCount = 3,
            glowColor = 0xFF66BB6A, glowColor2 = 0xFF43A047),
        RankInfo("Runecaster", 10,
            "Learns to wield the fundamental runes of wisdom.",
            "Runecaster.png", RankTier.STAR, starCount = 4,
            glowColor = 0xFF00BFA5, glowColor2 = 0xFF00897B),
        RankInfo("Shadowsage", 15,
            "Discovers hidden truths beyond ordinary learning.",
            "Shadowsage.png", RankTier.STAR, starCount = 5,
            glowColor = 0xFF7C4DFF, glowColor2 = 0xFF6200EA),
        RankInfo("Voidwalker", 25,
            "Ventures into the unknown realms of knowledge.",
            "Voidwalker.png", RankTier.MOON,
            glowColor = 0xFF1565C0, glowColor2 = 0xFF0D47A1),
        RankInfo("Nightwarden", 40,
            "Guards discipline and consistency against distraction.",
            "Nightwarden.png", RankTier.MOON,
            glowColor = 0xFF4A148C, glowColor2 = 0xFF6A1B9A),
        RankInfo("Soulkeeper", 65,
            "Preserves wisdom through dedication and effort.",
            "Soulkeeper.png", RankTier.MOON,
            glowColor = 0xFF2E7D32, glowColor2 = 0xFF1B5E20),
        RankInfo("Riftbinder", 105,
            "Bridges fragments of knowledge into mastery.",
            "Riftbinder.png", RankTier.MOON,
            glowColor = 0xFF00695C, glowColor2 = 0xFF004D40),
        RankInfo("Dreadscribe", 170,
            "Records achievements within the forbidden archives.",
            "Dreadscribe.png", RankTier.MOON,
            glowColor = 0xFFB71C1C, glowColor2 = 0xFF880E4F),
        RankInfo("Abysslord", 275,
            "Commands profound understanding beyond most students.",
            "Abysslord.png", RankTier.DIAMOND,
            glowColor = 0xFF311B92, glowColor2 = 0xFF1A237E),
        RankInfo("Starcaller", 445,
            "Draws insight from distant horizons of learning.",
            "Starcaller.png", RankTier.DIAMOND,
            glowColor = 0xFF006064, glowColor2 = 0xFF00838F),
        RankInfo("Chronomancer", 720,
            "Masters time management and long-term discipline.",
            "Chronomancer.png", RankTier.DIAMOND,
            glowColor = 0xFF33691E, glowColor2 = 0xFF558B2F),
        RankInfo("Archon", 1165,
            "Leads through exceptional academic excellence.",
            "Archon.png", RankTier.DIAMOND,
            glowColor = 0xFFE65100, glowColor2 = 0xFFBF360C),
        RankInfo("Sovereign", 1885,
            "Reigns over vast domains of accumulated knowledge.",
            "Sovereign.png", RankTier.CROWN, isOverflowBadge = true,
            glowColor = 0xFFFFD700, glowColor2 = 0xFFFFA000),
        RankInfo("Eternal", 3050,
            "Achieves legendary status among all scholars.",
            "Eternal.png", RankTier.TRIDENT, isOverflowBadge = true,
            glowColor = 0xFFE040FB, glowColor2 = 0xFFAA00FF),
        RankInfo("Transcendent", 4935,
            "Surpasses conventional limits of learning.",
            "Transcendent.png", RankTier.INFINITY, isOverflowBadge = true,
            glowColor = 0xFF40C4FF, glowColor2 = 0xFF0091EA),
        RankInfo("The Unending", 7985,
            "Continues growing beyond what was thought possible.",
            "Unending.png", RankTier.INFINITY, isOverflowBadge = true,
            glowColor = 0xFF69F0AE, glowColor2 = 0xFF00E676),
        RankInfo("Finalform", 12920,
            "The ultimate embodiment of knowledge, discipline, and perseverance. Few will ever reach this rank.",
            "Finalform.png", RankTier.COSMOS, isOverflowBadge = true,
            glowColor = 0xFFFF6D00, glowColor2 = 0xFFFFD600)
    )

    fun getCurrentRank(totalPoints: Int): RankInfo {
        var current = ranks[0]
        for (rank in ranks) {
            if (totalPoints >= rank.requiredPoints) current = rank
            else break
        }
        return current
    }

    fun getNextRank(totalPoints: Int): RankInfo? {
        val currentIdx = ranks.indexOfFirst { it.name == getCurrentRank(totalPoints).name }
        return if (currentIdx >= 0 && currentIdx < ranks.size - 1) ranks[currentIdx + 1] else null
    }

    fun getPointsInCurrentRank(totalPoints: Int): Int {
        val current = getCurrentRank(totalPoints)
        val idx = ranks.indexOf(current)
        val prevRequired = if (idx <= 0) 0 else ranks[idx - 1].requiredPoints
        return totalPoints - prevRequired
    }

    fun getPointsRequiredForCurrentRank(totalPoints: Int): Int {
        val current = getCurrentRank(totalPoints)
        val idx = ranks.indexOf(current)
        val prevRequired = if (idx <= 0) 0 else ranks[idx - 1].requiredPoints
        return current.requiredPoints - prevRequired
    }
}
