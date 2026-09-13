package com.mathcoach.app.core.latex

import com.mathcoach.app.domain.model.AnswerStatus
import com.mathcoach.app.domain.model.Difficulty
import com.mathcoach.app.domain.model.ProblemAnalysis
import com.mathcoach.app.domain.model.QuestionType
import com.mathcoach.app.domain.model.SimilarProblem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * 把 LLM 返回的 ProblemAnalysis JSON 解析成领域模型。
 */
object ProblemAnalysisParser {

    fun parseList(text: String): List<ProblemAnalysis> {
        val elements = JsonTolerantParser.parseAsList(text)
        return elements.mapNotNull { parse(it as? JsonObject) }
    }

    fun parse(obj: JsonObject?): ProblemAnalysis? {
        obj ?: return null
        return try {
            val title = obj.stringOrNull("题目").orEmpty()
            val questionType = QuestionType.from(obj.stringOrNull("题型"))
            val difficulty = Difficulty.from(obj.stringOrNull("难度"))
            val kp = (obj["知识点"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.content }
                ?: obj.stringOrNull("知识点")?.split("、", ",", "，")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: emptyList()
            val answerStatus = AnswerStatus.from(obj.stringOrNull("学生作答情况"))
            val correctnessJudgment = obj.stringOrNull("正误判断").orEmpty()
            val detailedAnalysis = obj.stringOrNull("详细解析").orEmpty()
            val solution = obj.stringOrNull("解答过程").orEmpty()
            val geometry = GeometrySpecParser.parse(obj["几何图形"])
            ProblemAnalysis(
                title = LatexUtils.unescape(title),
                questionType = questionType,
                difficulty = difficulty,
                knowledgePoints = kp,
                answerStatus = answerStatus,
                correctnessJudgment = LatexUtils.unescape(correctnessJudgment),
                detailedAnalysis = LatexUtils.unescape(detailedAnalysis),
                solution = LatexUtils.unescape(solution),
                geometry = geometry
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseSimilarList(text: String): List<SimilarProblem> {
        val elements = JsonTolerantParser.parseAsList(text)
        return elements.mapIndexedNotNull { idx, el ->
            val obj = el as? JsonObject ?: return@mapIndexedNotNull null
            try {
                val title = obj.stringOrNull("题目").orEmpty()
                val solution = obj.stringOrNull("解答").orEmpty()
                val kp = obj.stringOrNull("知识点").orEmpty()
                val geometry = GeometrySpecParser.parse(obj["几何图形"])
                SimilarProblem(
                    index = obj.intOrNull("题号") ?: (idx + 1),
                    title = LatexUtils.unescape(title),
                    solution = LatexUtils.unescape(solution),
                    knowledgePoints = kp,
                    geometry = geometry
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun parseVisionExtraction(text: String): Pair<String, String>? {
        val obj = JsonTolerantParser.parseAsObject(text) as? JsonObject ?: return null
        val title = obj.stringOrNull("题目") ?: return null
        val answer = obj.stringOrNull("学生作答") ?: "无"
        return title to answer
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.let {
            if (it.isString) it.content else it.content
        }

    private fun JsonObject.intOrNull(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()
}
