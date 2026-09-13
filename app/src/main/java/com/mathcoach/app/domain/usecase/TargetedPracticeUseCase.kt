package com.mathcoach.app.domain.usecase

import com.mathcoach.app.core.latex.ProblemAnalysisParser
import com.mathcoach.app.core.llm.LlmClient
import com.mathcoach.app.core.llm.SseEvent
import com.mathcoach.app.data.repository.HistoryRepository
import com.mathcoach.app.domain.analyzer.KnowledgeAnalyzer
import com.mathcoach.app.domain.analyzer.WeakSpotAnalyzer
import com.mathcoach.app.domain.model.SimilarProblem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TargetedPracticeUseCase @Inject constructor(
    private val llm: LlmClient,
    private val historyRepo: HistoryRepository,
    private val knowledgeAnalyzer: KnowledgeAnalyzer,
    private val weakSpotAnalyzer: WeakSpotAnalyzer
) {
    operator fun invoke(count: Int = 5): Flow<GenerateState> = flow {
        // 1. 聚合 profile
        val history = historyRepo.observeAllAsc().first()
        if (history.isEmpty()) {
            emit(GenerateState.Error("还没有练习记录，先去拍几道题吧"))
            return@flow
        }
        val profile = knowledgeAnalyzer.analyze(history)
        val recs = weakSpotAnalyzer.generateRecommendations(profile, max = 5)
        if (recs.isEmpty()) {
            emit(GenerateState.Error("没有需要练习的知识点，全部已掌握"))
            return@flow
        }

        // 2. 构造 prompt
        val promptText = weakSpotAnalyzer.buildTargetedPrompt(recs, profile)
        // buildTargetedPrompt 已经包含画像 + 薄弱点 + 建议三段；
        // 这里拆分传给 LLM
        val profileSummary = "总练习数：${profile.totalProblems}，知识点数：${profile.knowledgeMastery.size}"
        val weakDetail = recs.joinToString("\n") { rec ->
            val km = profile.knowledgeMastery[rec.knowledgePoint]!!
            "- ${rec.knowledgePoint}：掌握度 ${(km.masteryScore * 100).toInt()}%，正确率 ${(km.accuracyRate * 100).toInt()}%，" +
                "总练习 ${km.totalAttempts} 次" + (if (km.streakWrong > 0) "，连续错误 ${km.streakWrong} 次" else "")
        }
        val recsText = recs.joinToString("\n") { rec ->
            "- ${rec.knowledgePoint}：优先级 ${rec.priority}/10，难度 ${rec.difficulty.display}，${rec.count} 道。${rec.reason}"
        }

        // 3. 流式调用
        val sb = StringBuilder()
        var error: Throwable? = null
        llm.generateTargeted(profileSummary, weakDetail, recsText, count).collect { ev ->
            when (ev) {
                is SseEvent.Delta -> {
                    sb.append(ev.text)
                    emit(GenerateState.Streaming(sb.toString()))
                }
                is SseEvent.Error -> error = ev.cause
                SseEvent.Done -> Unit
            }
        }

        error?.let {
            emit(GenerateState.Error(it.message ?: "未知错误"))
            return@flow
        }

        val raw = sb.toString()
        val problems = ProblemAnalysisParser.parseSimilarList(raw)
        if (problems.isEmpty()) {
            emit(GenerateState.Error("解析练习失败：\n\n原始响应：\n$raw"))
            return@flow
        }
        emit(GenerateState.Done(problems, raw))
    }
}
