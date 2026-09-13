package com.mathcoach.app.domain.analyzer

import com.mathcoach.app.domain.model.AnswerStatus
import com.mathcoach.app.domain.model.HistoryEntry
import com.mathcoach.app.domain.model.KnowledgeMastery
import com.mathcoach.app.domain.model.LearningProfile
import javax.inject.Inject

/**
 * 与 Python analytics/knowledge.py 严格等价的掌握度聚合。
 */
class KnowledgeAnalyzer @Inject constructor() {

    fun analyze(history: List<HistoryEntry>): LearningProfile {
        // 按时间正序遍历
        val sorted = history.sortedBy { it.timestampMillis }
        val map = mutableMapOf<String, KnowledgeMastery>()

        for (entry in sorted) {
            for (kp in entry.analysis.knowledgePoints) {
                val cur = map.getOrPut(kp) { KnowledgeMastery(knowledgePoint = kp) }
                val newCorrect = cur.correctCount + if (entry.analysis.answerStatus == AnswerStatus.CORRECT) 1 else 0
                val newPartial = cur.partialCount + if (entry.analysis.answerStatus == AnswerStatus.PARTIAL) 1 else 0
                val newWrong = cur.wrongCount + if (entry.analysis.answerStatus == AnswerStatus.WRONG) 1 else 0
                val newNoAnswer = cur.noAnswerCount + if (entry.analysis.answerStatus == AnswerStatus.NO_ANSWER) 1 else 0
                val (newStreakC, newStreakW) = when (entry.analysis.answerStatus) {
                    AnswerStatus.CORRECT -> (cur.streakCorrect + 1) to 0
                    AnswerStatus.WRONG -> 0 to (cur.streakWrong + 1)
                    else -> cur.streakCorrect to cur.streakWrong
                }
                map[kp] = cur.copy(
                    totalAttempts = cur.totalAttempts + 1,
                    correctCount = newCorrect,
                    partialCount = newPartial,
                    wrongCount = newWrong,
                    noAnswerCount = newNoAnswer,
                    lastPracticed = entry.timestampMillis,
                    streakCorrect = newStreakC,
                    streakWrong = newStreakW
                )
            }
        }

        return LearningProfile(
            knowledgeMastery = map,
            totalProblems = sorted.size,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * 滑动窗口正确率：(correct + 0.5*partial) / window_size。
     * 对应 Python KnowledgeAnalyzer.get_mastery_trend。
     */
    fun masteryTrend(history: List<HistoryEntry>, kp: String, window: Int = 5): List<Float> {
        val filtered = history
            .filter { it.analysis.knowledgePoints.contains(kp) }
            .sortedBy { it.timestampMillis }
        if (filtered.isEmpty()) return emptyList()

        val result = mutableListOf<Float>()
        for (i in filtered.indices) {
            val from = maxOf(0, i - window + 1)
            val slice = filtered.subList(from, i + 1)
            val correct = slice.count { it.analysis.answerStatus == AnswerStatus.CORRECT }
            val partial = slice.count { it.analysis.answerStatus == AnswerStatus.PARTIAL }
            val graded = slice.count { it.analysis.answerStatus != AnswerStatus.NO_ANSWER }
            if (graded == 0) {
                result.add(0f)
            } else {
                result.add((correct + 0.5f * partial) / graded)
            }
        }
        return result
    }
}
