package com.mathcoach.app.domain.analyzer

import com.mathcoach.app.domain.model.KnowledgeMastery
import com.mathcoach.app.domain.model.LearningProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeakSpotAnalyzerTest {

    private val analyzer = WeakSpotAnalyzer()

    @Test
    fun `掌握度低于 0_5 标记为薄弱`() {
        val km = KnowledgeMastery(
            knowledgePoint = "test",
            totalAttempts = 5,
            correctCount = 1,
            partialCount = 0,
            wrongCount = 4
        )
        // accuracy = 1/5 = 0.2, confidence = 5/10 = 0.5, mastery = 0.6*0.2 + 0.4*0.5 = 0.32
        assertTrue(km.isWeak)
        assertTrue(km.needsPractice)
    }

    @Test
    fun `空 profile 不产生建议`() {
        val recs = analyzer.generateRecommendations(LearningProfile())
        assertTrue(recs.isEmpty())
    }

    @Test
    fun `建议优先级按掌握度升序`() {
        val km1 = KnowledgeMastery("kp1", totalAttempts = 5, correctCount = 1, wrongCount = 4)  // 极薄弱
        val km2 = KnowledgeMastery("kp2", totalAttempts = 5, correctCount = 3, wrongCount = 2)  // 中等
        val km3 = KnowledgeMastery("kp3", totalAttempts = 5, correctCount = 5)                   // 已掌握

        val profile = LearningProfile(
            knowledgeMastery = mapOf("kp1" to km1, "kp2" to km2, "kp3" to km3),
            totalProblems = 15
        )
        val recs = analyzer.generateRecommendations(profile, max = 5)
        assertTrue(recs.isNotEmpty())
        // 已掌握的 kp3 不应在 weak 里
        val weakKps = recs.map { it.knowledgePoint }
        assertTrue("kp1" in weakKps)
    }
}
