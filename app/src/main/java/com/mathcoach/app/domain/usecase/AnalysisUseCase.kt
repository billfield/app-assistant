package com.mathcoach.app.domain.usecase

import com.mathcoach.app.core.latex.ProblemAnalysisParser
import com.mathcoach.app.core.llm.LlmClient
import com.mathcoach.app.core.llm.SseEvent
import com.mathcoach.app.core.util.ImageCodec
import com.mathcoach.app.data.local.SettingsDataStore
import com.mathcoach.app.data.repository.CacheRepository
import com.mathcoach.app.data.repository.HistoryRepository
import com.mathcoach.app.domain.model.HistoryEntry
import com.mathcoach.app.domain.model.ProblemAnalysis
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class AnalysisState {
    object Idle : AnalysisState()
    object CheckingCache : AnalysisState()
    data class Streaming(val partial: String) : AnalysisState()
    data class Parsed(
        val analyses: List<ProblemAnalysis>,
        val rawResponse: String,
        val fromCache: Boolean
    ) : AnalysisState()
    data class Saved(val entryIds: List<String>) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

class AnalysisUseCase @Inject constructor(
    private val llm: LlmClient,
    private val settings: SettingsDataStore,
    private val cacheRepo: CacheRepository,
    private val historyRepo: HistoryRepository,
    private val moshi: Moshi
) {
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any>>(mapType)

    operator fun invoke(imageBytes: ByteArray, userText: String, imagePath: String?): Flow<AnalysisState> = flow {
        emit(AnalysisState.CheckingCache)

        val cfg = settings.current()
        val imageHash = ImageCodec.md5(imageBytes)
        val cacheKey = ImageCodec.md5(imageBytes, userText.toByteArray())

        // 查缓存
        if (cfg.cacheEnabled) {
            cacheRepo.get(cacheKey)?.let { cachedJson ->
                val cached = ProblemAnalysisParser.parseList(cachedJson)
                if (cached.isNotEmpty()) {
                    emit(AnalysisState.Parsed(cached, cachedJson, fromCache = true))
                    return@flow
                }
            }
        }

        // 调 LLM
        val sb = StringBuilder()
        var error: Throwable? = null

        val flow = if (cfg.useDualModel) {
            // 双模型：先 vision OCR 提取题目，再用 reasoning 模型深度分析
            flow {
                // 第一步：vision OCR
                val extractSb = StringBuilder()
                llm.extract(imageBytes).collect { ev ->
                    when (ev) {
                        is SseEvent.Delta -> extractSb.append(ev.text)
                        is SseEvent.Error -> throw ev.cause
                        SseEvent.Done -> Unit
                    }
                }
                val extracted = com.mathcoach.app.core.latex.ProblemAnalysisParser.parseVisionExtraction(extractSb.toString())
                    ?: (extractSb.toString() to "无")

                // 第二步：reasoning 解析
                llm.solve(extracted.first, extracted.second, userText).collect { ev ->
                    emit(ev)
                }
            }
        } else {
            llm.analyze(imageBytes, userText)
        }

        flow.collect { ev ->
            when (ev) {
                is SseEvent.Delta -> {
                    sb.append(ev.text)
                    emit(AnalysisState.Streaming(sb.toString()))
                }
                is SseEvent.Error -> error = ev.cause
                SseEvent.Done -> Unit
            }
        }

        error?.let {
            emit(AnalysisState.Error(it.message ?: "未知错误"))
            return@flow
        }

        val rawResponse = sb.toString()
        val parsed = ProblemAnalysisParser.parseList(rawResponse)
        if (parsed.isEmpty()) {
            emit(AnalysisState.Error("解析失败：无法从模型响应中提取有效 JSON。\n\n原始响应：\n$rawResponse"))
            return@flow
        }

        emit(AnalysisState.Parsed(parsed, rawResponse, fromCache = false))

        // 写缓存
        if (cfg.cacheEnabled) {
            try { cacheRepo.put(cacheKey, rawResponse) } catch (_: Exception) {}
        }

        // 自动保存历史（每道题一条）
        val ids = mutableListOf<String>()
        for (analysis in parsed) {
            val entry = HistoryEntry(
                id = historyRepo.newId(),
                timestampMillis = System.currentTimeMillis(),
                imageHash = imageHash,
                imagePath = imagePath,
                analysis = analysis,
                similarProblemsGenerated = 0,
                tags = emptyList(),
                notes = ""
            )
            try {
                historyRepo.insert(entry)
                ids.add(entry.id)
            } catch (_: Exception) {}
        }
        if (ids.isNotEmpty()) {
            emit(AnalysisState.Saved(ids))
        }
    }

}
