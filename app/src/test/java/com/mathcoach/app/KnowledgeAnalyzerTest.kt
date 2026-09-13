package com.mathcoach.app.domain.analyzer

import com.mathcoach.app.domain.model.AnswerStatus
import com.mathcoach.app.domain.model.Difficulty
import com.mathcoach.app.domain.model.HistoryEntry
import com.mathcoach.app.domain.model.ProblemAnalysis
import com.mathcoach.app.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeAnalyzerTest {

    private fun entry(kp: String, status: AnswerStatus, ts: Long): HistoryEntry = HistoryEntry(
        id = "id$ts",
        timestampMillis = ts,
        analysis = ProblemAnalysis(
            title = "test",
            questionType = QuestionType.SOLUTION,
            difficulty = Difficulty.THREE,
            knowledgePoints = listOf(kp),
            answerStatus = status,
            correctnessJudgment = "",
            detailedAnalysis = "",
            solution = ""
        )
    )

    private val analyzer = KnowledgeAnalyzer()

    @Test
    fun `空历史返回空画像`() {
        val profile = analyzer.analyze(emptyList())
        assertEquals(0, profile.totalProblems)
        assertTrue(profile.knowledgeMastery.isEmpty())
    }

    @Test
    fun `单个知识点三次全对得分应接近1`() {
        val history = (1..10).map { entry("勾股定理", AnswerStatus.CORRECT, it.toLong()) }
        val profile = analyzer.analyze(history)
        val km = profile.knowledgeMastery["勾股定理"]!!
        assertEquals(10, km.totalAttempts)
        assertEquals(10, km.correctCount)
        assertEquals(1f, km.accuracyRate, 0.001f)
        // mastery = 0.6 * 1 + 0.4 * min(10/10, 1) = 1.0
        assertEquals(1f, km.masteryScore, 0.001f)
    }

    @Test
    fun `掌握度公式严格等价 Python`() {
        // 4 次练习：2 完全正确，1 部分正确，1 完全错误
        val history = listOf(
            entry("一次函数", AnswerStatus.CORRECT, 1),
            entry("一次函数", AnswerStatus.CORRECT, 2),
            entry("一次函数", AnswerStatus.PARTIAL, 3),
            entry("一次函数", AnswerStatus.WRONG, 4)
        )
        val profile = analyzer.analyze(history)
        val km = profile.knowledgeMastery["一次函数"]!!
        assertEquals(4, km.totalAttempts)
        // accuracy = (2 + 0.5*1) / 4 = 0.625
        assertEquals(0.625f, km.accuracyRate, 0.001f)
        // confidence = min(4/10, 1) = 0.4
        // mastery = 0.6*0.625 + 0.4*0.4 = 0.375 + 0.16 = 0.535
        assertEquals(0.535f, km.masteryScore, 0.001f)
        // 0.5 <= 0.535 < 0.7 → not weak but needs practice
        assertTrue(km.needsPractice)
    }

    @Test
    fun `连续错误计数正确`() {
        val history = listOf(
            entry("全等三角形", AnswerStatus.CORRECT, 1),
            entry("全等三角形", AnswerStatus.WRONG, 2),
            entry("全等三角形", AnswerStatus.WRONG, 3),
            entry("全等三角形", AnswerStatus.WRONG, 4)
        )
        val profile = analyzer.analyze(history)
        val km = profile.knowledgeMastery["全等三角形"]!!
        assertEquals(3, km.streakWrong)
        assertEquals(0, km.streakCorrect)
    }

    @Test
    fun `正确答案会重置错误连击`() {
        val history = listOf(
            entry("相似三角形", AnswerStatus.WRONG, 1),
            entry("相似三角形", AnswerStatus.WRONG, 2),
            entry("相似三角形", AnswerStatus.CORRECT, 3)
        )
        val profile = analyzer.analyze(history)
        val km = profile.knowledgeMastery["相似三角形"]!!
        assertEquals(0, km.streakWrong)
        assertEquals(1, km.streakCorrect)
    }

    @Test
    fun `无作答不计入正确率分母`() {
        val history = listOf(
            entry("圆", AnswerStatus.CORRECT, 1),
            entry("圆", AnswerStatus.NO_ANSWER, 2),
            entry("圆", AnswerStatus.WRONG, 3)
        )
        val profile = analyzer.analyze(history)
        val km = profile.knowledgeMastery["圆"]!!
        // graded = 3 - 1 = 2, accuracy = 1 / 2 = 0.5
        assertEquals(0.5f, km.accuracyRate, 0.001f)
    }

    @Test
    fun `mastery trend 滑动窗口正确`() {
        val history = (1..7).map {
            entry("因式分解", if (it <= 5) AnswerStatus.CORRECT else AnswerStatus.WRONG, it.toLong())
        }
        val trend = analyzer.masteryTrend(history, "因式分解", window = 5)
        assertEquals(7, trend.size)
        // 前 5 个都是 1.0
        for (i in 0..4) assertEquals(1f, trend[i], 0.001f)
        // 第 6 个：窗口 = 1..5 全对 + 6 错 = (4+0)/5 = 0.8
        assertEquals(0.8f, trend[5], 0.001f)
        // 第 7 个：窗口 = 3..7 = 3对+2错 = 3/5 = 0.6
        assertEquals(0.6f, trend[6], 0.001f)
    }
}
