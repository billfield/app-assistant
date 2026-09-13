package com.mathcoach.app.core.llm

import android.util.Base64
import com.mathcoach.app.data.local.SettingsDataStore
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class SseEvent {
    data class Delta(val text: String) : SseEvent()
    object Done : SseEvent()
    data class Error(val cause: Throwable) : SseEvent()
}

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String,
    val content: Any  // String or List<Map<String, Any>>
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 1.0,
    val stream: Boolean = true,
    @Json(name = "max_tokens") val maxTokens: Int = 8192
)

@JsonClass(generateAdapter = true)
data class ChatChunk(
    val choices: List<Choice>? = null
) {
    @JsonClass(generateAdapter = true)
    data class Choice(
        val delta: Delta? = null,
        @Json(name = "finish_reason") val finishReason: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class Delta(
        val content: String? = null
    )
}

@Singleton
class LlmClient @Inject constructor(
    private val okHttp: OkHttpClient,
    private val moshi: Moshi,
    private val settings: SettingsDataStore
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val chatChunkAdapter = moshi.adapter(ChatChunk::class.java)
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any>>(mapType)

    /**
     * 通用流式 chat completion 调用。
     *
     * @param messages 完整的 messages 列表（已包含 system + user）
     * @param modelOverride 显式指定模型名；为 null 用 settings.model
     */
    fun streamChat(
        messages: List<ChatMessage>,
        modelOverride: String? = null
    ): Flow<SseEvent> = callbackFlow {
        val cfg = settings.current()
        val model = modelOverride ?: cfg.model
        val apiKey = settings.getApiKey(cfg.provider)
        if (apiKey.isBlank()) {
            trySend(SseEvent.Error(IOException("API Key 未配置，请到设置页填写")))
            close()
            return@callbackFlow
        }
        if (cfg.baseUrl.isBlank()) {
            trySend(SseEvent.Error(IOException("Base URL 未配置")))
            close()
            return@callbackFlow
        }

        val requestBody = ChatRequest(model = model, messages = messages)
        val bodyJson = moshi.adapter(ChatRequest::class.java).toJson(requestBody)
        val request = Request.Builder()
            .url(cfg.baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        val call = okHttp.newCall(request)

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                trySend(SseEvent.Error(IOException("HTTP ${response.code}: $errBody")))
                close()
                return@callbackFlow
            }
            val source: BufferedSource = response.body?.source()
                ?: run {
                    trySend(SseEvent.Error(IOException("Empty response body")))
                    close()
                    return@callbackFlow
                }

            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isEmpty()) continue
                if (!line.startsWith("data:")) continue
                val data = line.substringAfter("data:").trim()
                if (data == "[DONE]") {
                    trySend(SseEvent.Done)
                    break
                }
                try {
                    val chunk = chatChunkAdapter.fromJson(data)
                    val content = chunk?.choices?.firstOrNull()?.delta?.content
                    if (!content.isNullOrEmpty()) {
                        trySend(SseEvent.Delta(content))
                    }
                } catch (_: Exception) {
                    // 非标准 chunk，忽略
                }
            }
            trySend(SseEvent.Done)
            close()
        } catch (e: Exception) {
            trySend(SseEvent.Error(e))
            close(e)
        }

        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    // ============ 业务方法 ============

    fun analyze(imageBytes: ByteArray, userText: String): Flow<SseEvent> {
        val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val content = listOf(
            mapOf("type" to "text", "text" to (userText.ifBlank { "请分析这道数学题。" })),
            mapOf(
                "type" to "image_url",
                "image_url" to mapOf("url" to "data:image/jpeg;base64,$b64")
            )
        )
        return streamChat(
            listOf(
                ChatMessage(role = "system", content = Prompts.ANALYSIS_PROMPT.format("", "")),
                ChatMessage(role = "user", content = content)
            )
        )
    }

    fun extract(imageBytes: ByteArray): Flow<SseEvent> {
        val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val content = listOf(
            mapOf("type" to "text", "text" to "请识别图片中的数学题目内容"),
            mapOf(
                "type" to "image_url",
                "image_url" to mapOf("url" to "data:image/jpeg;base64,$b64")
            )
        )
        return callbackFlow {
            val cfg = settings.current()
            val visionModel = cfg.visionModel.ifBlank { cfg.model }
            val messages = listOf(
                ChatMessage(role = "system", content = Prompts.VISION_EXTRACTION_PROMPT),
                ChatMessage(role = "user", content = content)
            )
            streamChat(messages, modelOverride = visionModel).collect { trySend(it) }
            close()
        }.flowOn(Dispatchers.IO)
    }

    fun solve(questionText: String, studentAnswer: String, userText: String): Flow<SseEvent> {
        return callbackFlow {
            val cfg = settings.current()
            val reasoningModel = cfg.reasoningModel.ifBlank { cfg.model }
            val prompt = Prompts.ANALYSIS_PROMPT.format(questionText, studentAnswer)
            val fullPrompt = "【用户要求】：$userText\n\n$prompt"
            val messages = listOf(
                ChatMessage(role = "system", content = Prompts.REASONING_SYSTEM),
                ChatMessage(role = "user", content = fullPrompt)
            )
            streamChat(messages, modelOverride = reasoningModel).collect { trySend(it) }
            close()
        }.flowOn(Dispatchers.IO)
    }

    fun generateSimilar(
        difficulty: String,
        questionType: String,
        knowledgePoints: List<String>,
        count: Int
    ): Flow<SseEvent> {
        val prompt = Prompts.SIMILAR_PROMPT_TEMPLATE.format(
            count,
            difficulty,
            questionType,
            knowledgePoints.joinToString("、")
        )
        return streamChat(
            listOf(
                ChatMessage(role = "system", content = "你是数学命题专家。"),
                ChatMessage(role = "user", content = prompt)
            )
        )
    }

    fun generateByTopic(userRequest: String, count: Int): Flow<SseEvent> {
        val prompt = Prompts.GENERATE_BY_TOPIC_PROMPT.format(count, userRequest)
        return streamChat(
            listOf(
                ChatMessage(role = "system", content = "你是数学命题专家。"),
                ChatMessage(role = "user", content = prompt)
            )
        )
    }

    fun generateTargeted(
        profileSummary: String,
        weakSpotsDetail: String,
        recommendations: String,
        count: Int
    ): Flow<SseEvent> {
        val prompt = Prompts.TARGETED_PRACTICE_PROMPT.format(
            profileSummary, weakSpotsDetail, recommendations, count
        )
        return streamChat(
            listOf(
                ChatMessage(role = "system", content = "你是一位资深的数学教育分析师和命题专家。"),
                ChatMessage(role = "user", content = prompt)
            )
        )
    }

    suspend fun collectToString(flow: Flow<SseEvent>): String {
        val sb = StringBuilder()
        var error: Throwable? = null
        flow.collect { ev ->
            when (ev) {
                is SseEvent.Delta -> sb.append(ev.text)
                is SseEvent.Error -> error = ev.cause
                SseEvent.Done -> Unit
            }
        }
        error?.let { throw it }
        return sb.toString()
    }
}
