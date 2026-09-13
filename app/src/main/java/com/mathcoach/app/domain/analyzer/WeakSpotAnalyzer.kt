package com.mathcoach.app.domain.analyzer

import com.mathcoach.app.domain.model.Difficulty
import com.mathcoach.app.domain.model.KnowledgeMastery
import com.mathcoach.app.domain.model.LearningProfile
import com.mathcoach.app.domain.model.PracticeRecommendation
import javax.inject.Inject
import kotlin.math.min

/**
 * 与 Python analytics/weak_spots.py 严格等价的薄弱点分析。
 */
class WeakSpotAnalyzer @Inject constructor() {

    companion object {
        const val MASTERY_THRESHOLD_WEAK = 0.5f
        const val MASTERY_THRESHOLD_NEED_PRACTICE = 0.7f
    }

    fun identifyWeakSpots(profile: LearningProfile): List<KnowledgeMastery> =
        profile.knowledgeMastery.values
            .filter { it.needsPractice }
            .sortedWith(
                compareBy(
                    { it.masteryScore },
                    { -it.streakWrong },
                    { it.totalAttempts }
                )
            )

    fun generateRecommendations(profile: LearningProfile, max: Int = 5): List<PracticeRecommendation> {
        val weak = identifyWeakSpots(profile)
        return weak.take(max).map { km ->
            when {
                km.masteryScore < MASTERY_THRESHOLD_WEAK -> PracticeRecommendation(
                    knowledgePoint = km.knowledgePoint,
                    priority = min(10, ((1 - km.masteryScore) * 10 + km.streakWrong * 2).toInt()),
                    reason = "掌握度 ${"%.0f".format(km.masteryScore * 100)}%，亟需加强",
                    difficulty = if (km.totalAttempts > 3) Difficulty.FOUR else Difficulty.THREE,
                    count = 3
                )
                km.masteryScore < MASTERY_THRESHOLD_NEED_PRACTICE -> PracticeRecommendation(
                    knowledgePoint = km.knowledgePoint,
                    priority = min(7, ((MASTERY_THRESHOLD_NEED_PRACTICE - km.masteryScore) * 10).toInt()),
                    reason = "掌握度 ${"%.0f".format(km.masteryScore * 100)}%，需巩固",
                    difficulty = Difficulty.THREE,
                    count = 2
                )
                else -> PracticeRecommendation(
                    knowledgePoint = km.knowledgePoint,
                    priority = 3,
                    reason = "已掌握，温故知新",
                    difficulty = Difficulty.TWO,
                    count = 1
                )
            }
        }
    }

    fun buildTargetedPrompt(recs: List<PracticeRecommendation>, profile: LearningProfile): String {
        val sb = StringBuilder()
        sb.append("## 学习画像\n")
        sb.append("总练习数：${profile.totalProblems}\n")
        sb.append("知识点数量：${profile.knowledgeMastery.size}\n\n")

        sb.append("## 薄弱知识点详情\n")
        for (rec in recs) {
            val km = profile.knowledgeMastery[rec.knowledgePoint] ?: continue
            sb.append("- ${rec.knowledgePoint}：掌握度 ${"%.0f".format(km.masteryScore * 100)}%")
            sb.append("，正确率 ${"%.0f".format(km.accuracyRate * 100)}%")
            sb.append("，总练习 ${km.totalAttempts} 次")
            if (km.streakWrong > 0) sb.append("，连续错误 ${km.streakWrong} 次")
            sb.append("\n")
        }
        sb.append("\n## 练习建议\n")
        for (rec in recs) {
            sb.append("- ${rec.knowledgePoint}：优先级 ${rec.priority}/10，")
            sb.append("难度 ${rec.difficulty.display}，出题 ${rec.count} 道。${rec.reason}\n")
        }
        return sb.toString()
    }
}
