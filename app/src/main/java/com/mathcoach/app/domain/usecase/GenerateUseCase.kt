package com.mathcoach.app.domain.usecase

import com.mathcoach.app.core.latex.ProblemAnalysisParser
import com.mathcoach.app.core.llm.LlmClient
import com.mathcoach.app.core.llm.SseEvent
import com.mathcoach.app.domain.model.ProblemAnalysis
import com.mathcoach.app.domain.model.SimilarProblem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class GenerateState {
    object Idle : GenerateState()
    data class Streaming(val partial: String) : GenerateState()
    data class Done(val problems: List<SimilarProblem>, val rawResponse: String) : GenerateState()
    data class Error(val message: String) : GenerateState()
}

class GenerateSimilarUseCase @Inject constructor(
    private val llm: LlmClient
) {
    operator fun invoke(source: ProblemAnalysis, count: Int = 3): Flow<GenerateState> = flow {
        val sb = StringBuilder()
        var error: Throwable? = null

        llm.generateSimilar(
            difficulty = source.difficulty.display,
            questionType = source.questionType.display,
            knowledgePoints = source.knowledgePoints,
            count = count
        ).collect { ev ->
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
            emit(GenerateState.Error("解析同类题失败：模型响应不是有效 JSON。\n\n原始响应：\n$raw"))
            return@flow
        }
        emit(GenerateState.Done(problems, raw))
    }
}

class GenerateByTopicUseCase @Inject constructor(
    private val llm: LlmClient
) {
    operator fun invoke(userRequest: String, count: Int = 3): Flow<GenerateState> = flow {
        val sb = StringBuilder()
        var error: Throwable? = null

        llm.generateByTopic(userRequest, count).collect { ev ->
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
            emit(GenerateState.Error("解析生成题目失败：模型响应不是有效 JSON。\n\n原始响应：\n$raw"))
            return@flow
        }
        emit(GenerateState.Done(problems, raw))
    }
}
