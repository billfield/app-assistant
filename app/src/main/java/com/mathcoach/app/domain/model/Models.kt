package com.mathcoach.app.domain.model

enum class QuestionType(val display: String) {
    CHOICE("选择"),
    FILL_BLANK("填空"),
    SOLUTION("解答"),
    PROOF("证明"),
    OTHER("其他");

    companion object {
        fun from(value: String?): QuestionType =
            entries.firstOrNull { it.display == value } ?: OTHER
    }
}

enum class Difficulty(val display: String) {
    ONE("1星"), TWO("2星"), THREE("3星"), FOUR("4星"), FIVE("5星");

    companion object {
        fun from(value: String?): Difficulty =
            entries.firstOrNull { it.display == value } ?: THREE
    }
}

enum class AnswerStatus(val display: String) {
    NO_ANSWER("无作答"),
    CORRECT("完全正确"),
    PARTIAL("部分正确"),
    WRONG("完全错误");

    companion object {
        fun from(value: String?): AnswerStatus =
            entries.firstOrNull { it.display == value } ?: NO_ANSWER
    }
}

data class ProblemAnalysis(
    val title: String = "",
    val questionType: QuestionType = QuestionType.OTHER,
    val difficulty: Difficulty = Difficulty.THREE,
    val knowledgePoints: List<String> = emptyList(),
    val answerStatus: AnswerStatus = AnswerStatus.NO_ANSWER,
    val correctnessJudgment: String = "",
    val detailedAnalysis: String = "",
    val solution: String = "",
    val rawResponse: String? = null,
    val geometry: GeometrySpec? = null
)

data class HistoryEntry(
    val id: String,
    val timestampMillis: Long,
    val imageHash: String? = null,
    val imagePath: String? = null,
    val analysis: ProblemAnalysis,
    val similarProblemsGenerated: Int = 0,
    val tags: List<String> = emptyList(),
    val notes: String = ""
)

data class SimilarProblem(
    val index: Int,
    val title: String,
    val solution: String,
    val knowledgePoints: String,
    val geometry: GeometrySpec? = null
)

data class GeometrySpec(
    val points: Map<String, Pair<Float, Float>> = emptyMap(),
    val primitives: List<GeometryPrimitive> = emptyList(),
    val view: GeometryView = GeometryView()
)

data class GeometryView(
    val equal: Boolean = true,
    val showAxes: Boolean = false,
    val range: ClosedFloatingPointRange<Float>? = null
)

sealed class GeometryPrimitive {
    data class Polygon(val vertices: List<String>, val fill: Boolean = false) : GeometryPrimitive()
    data class Segment(val p1: String, val p2: String, val style: String? = null) : GeometryPrimitive()
    data class Circle(val center: String, val radius: Float? = null, val throughPoint: String? = null) : GeometryPrimitive()
    data class Angle(val vertex: String, val from: String, val to: String, val label: String? = null) : GeometryPrimitive()
    data class Label(val at: String, val text: String, val offset: Pair<Float, Float> = 0f to 0.15f) : GeometryPrimitive()
    data class Point(val name: String) : GeometryPrimitive()
    data class Function(val expr: String, val xMin: Float, val xMax: Float) : GeometryPrimitive()
}
