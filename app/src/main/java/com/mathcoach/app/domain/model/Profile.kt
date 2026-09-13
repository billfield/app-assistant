package com.mathcoach.app.domain.model

/**
 * 单个知识点的掌握度统计。与 Python data/models.py 中的 KnowledgeMastery 严格等价。
 */
data class KnowledgeMastery(
    val knowledgePoint: String,
    val totalAttempts: Int = 0,
    val correctCount: Int = 0,
    val partialCount: Int = 0,
    val wrongCount: Int = 0,
    val noAnswerCount: Int = 0,
    val lastPracticed: Long? = null,
    val streakCorrect: Int = 0,
    val streakWrong: Int = 0
) {
    val accuracyRate: Float
        get() {
            val graded = totalAttempts - noAnswerCount
            if (graded == 0) return 0f
            return (correctCount + 0.5f * partialCount) / graded
        }

    val masteryScore: Float
        get() {
            val confidence = (totalAttempts / 10f).coerceAtMost(1f)
            return 0.6f * accuracyRate + 0.4f * confidence
        }

    val isWeak: Boolean
        get() = totalAttempts > 0 && masteryScore < 0.5f

    val needsPractice: Boolean
        get() = masteryScore < 0.7f || streakWrong >= 2
}

data class LearningProfile(
    val knowledgeMastery: Map<String, KnowledgeMastery> = emptyMap(),
    val totalProblems: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun weakPoints(topN: Int? = null): List<KnowledgeMastery> {
        val weak = knowledgeMastery.values.filter { it.isWeak }.sortedBy { it.masteryScore }
        return topN?.let { weak.take(it) } ?: weak
    }

    fun strongPoints(): List<KnowledgeMastery> =
        knowledgeMastery.values
            .filter { it.totalAttempts > 0 && !it.isWeak }
            .sortedByDescending { it.masteryScore }
}

data class PracticeRecommendation(
    val knowledgePoint: String,
    val priority: Int,
    val reason: String,
    val difficulty: Difficulty,
    val count: Int
)
