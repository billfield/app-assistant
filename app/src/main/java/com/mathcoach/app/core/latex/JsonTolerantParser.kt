package com.mathcoach.app.core.latex

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * 容错 JSON 解析器，对应 Python latex_utils.parse_json_from_response。
 *
 * 处理 LLM 返回中常见问题：
 * - 前后夹杂说明文字
 * - markdown ```json 包裹
 * - 数组末尾多余逗号
 * - 数组/对象被截断
 * - 文本中混有多个独立 JSON 对象
 */
object JsonTolerantParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowStructuredMapKeys = true
    }

    fun parse(text: String): JsonElement? {
        var cleaned = text.trim()
        if (cleaned.isEmpty()) return null

        // 剥 markdown 代码块
        if (cleaned.startsWith("```")) {
            val lines = cleaned.lines().toMutableList()
            if (lines.isNotEmpty() && lines[0].startsWith("```")) {
                lines.removeAt(0)
            }
            if (lines.isNotEmpty() && lines.last().trim() == "```") {
                lines.removeAt(lines.size - 1)
            }
            cleaned = lines.joinToString("\n").trim()
        }

        // 直接解析
        tryParse(cleaned)?.let { return it }

        // 提取 [...]
        val firstBracket = cleaned.indexOf('[')
        val lastBracket = cleaned.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket > firstBracket) {
            val arrText = cleaned.substring(firstBracket, lastBracket + 1)
            tryParse(arrText)?.let { return it }

            // 修尾逗号
            val fixedTrailing = arrText.replace(Regex(""",\s*]"""), "]")
            tryParse(fixedTrailing)?.let { return it }

            // 截断数组：逐步回退到上一个 }
            for (i in arrText.length - 1 downTo 1) {
                if (arrText[i] == '}') {
                    val truncated = arrText.substring(0, i + 1) + "]"
                    tryParse(truncated)?.let { return it }
                }
            }
        }

        // 提取 {...}
        val firstBrace = cleaned.indexOf('{')
        val lastBrace = cleaned.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            tryParse(cleaned.substring(firstBrace, lastBrace + 1))?.let { return it }
        }

        // 提取所有独立 JSON 对象
        val objects = mutableListOf<JsonElement>()
        var depth = 0
        var start = -1
        var inString = false
        var escape = false
        cleaned.forEachIndexed { i, ch ->
            when {
                escape -> escape = false
                ch == '\\' && inString -> escape = true
                ch == '"' -> inString = !inString
                inString -> Unit
                ch == '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                ch == '}' -> {
                    depth--
                    if (depth == 0 && start != -1) {
                        tryParse(cleaned.substring(start, i + 1))?.let { objects.add(it) }
                        start = -1
                    }
                }
            }
        }
        return when {
            objects.isEmpty() -> null
            objects.size == 1 -> objects.first()
            else -> kotlinx.serialization.json.JsonArray(objects)
        }
    }

    private fun tryParse(text: String): JsonElement? = try {
        json.parseToJsonElement(text)
    } catch (_: Exception) {
        null
    }

    fun parseAsList(text: String): List<JsonElement> {
        val el = parse(text) ?: return emptyList()
        return try {
            el.jsonArray.toList()
        } catch (_: Exception) {
            listOf(el)
        }
    }

    fun parseAsObject(text: String): JsonElement? {
        val el = parse(text) ?: return null
        return try {
            el.jsonObject
            el
        } catch (_: Exception) {
            null
        }
    }
}
