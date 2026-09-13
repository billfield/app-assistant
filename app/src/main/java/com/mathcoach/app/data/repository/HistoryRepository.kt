package com.mathcoach.app.data.repository

import com.mathcoach.app.data.local.HistoryDao
import com.mathcoach.app.data.local.HistoryEntity
import com.mathcoach.app.domain.model.AnswerStatus
import com.mathcoach.app.domain.model.Difficulty
import com.mathcoach.app.domain.model.HistoryEntry
import com.mathcoach.app.domain.model.ProblemAnalysis
import com.mathcoach.app.domain.model.QuestionType
import com.mathcoach.app.domain.model.GeometrySpec
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    private val moshi: Moshi
) {
    private val listType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(listType)

    fun observeAllDesc(): Flow<List<HistoryEntry>> =
        dao.observeAllDesc().map { list -> list.mapNotNull { toDomain(it) } }

    fun observeAllAsc(): Flow<List<HistoryEntry>> =
        dao.observeAllAsc().map { list -> list.mapNotNull { toDomain(it) } }

    suspend fun byId(id: String): HistoryEntry? = dao.byId(id)?.let { toDomain(it) }

    suspend fun insert(entry: HistoryEntry) = dao.insert(toEntity(entry))

    suspend fun update(entry: HistoryEntry) = dao.update(toEntity(entry))

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun newId(): String = UUID.randomUUID().toString().take(8)

    fun toEntity(entry: HistoryEntry): HistoryEntity = HistoryEntity(
        id = entry.id,
        timestampMillis = entry.timestampMillis,
        imageHash = entry.imageHash,
        imagePath = entry.imagePath,
        title = entry.analysis.title,
        questionType = entry.analysis.questionType.display,
        difficulty = entry.analysis.difficulty.display,
        knowledgePointsJson = stringListAdapter.toJson(entry.analysis.knowledgePoints),
        answerStatus = entry.analysis.answerStatus.display,
        correctnessJudgment = entry.analysis.correctnessJudgment,
        detailedAnalysis = entry.analysis.detailedAnalysis,
        solution = entry.analysis.solution,
        geometryJson = entry.analysis.geometry?.let { geometryToJson(it) },
        similarGenerated = entry.similarProblemsGenerated,
        tagsJson = stringListAdapter.toJson(entry.tags),
        notes = entry.notes
    )

    fun toDomain(entity: HistoryEntity): HistoryEntry? = try {
        val kp: List<String> = stringListAdapter.fromJson(entity.knowledgePointsJson) ?: emptyList()
        val tags: List<String> = stringListAdapter.fromJson(entity.tagsJson) ?: emptyList()
        HistoryEntry(
            id = entity.id,
            timestampMillis = entity.timestampMillis,
            imageHash = entity.imageHash,
            imagePath = entity.imagePath,
            analysis = ProblemAnalysis(
                title = entity.title,
                questionType = QuestionType.from(entity.questionType),
                difficulty = Difficulty.from(entity.difficulty),
                knowledgePoints = kp,
                answerStatus = AnswerStatus.from(entity.answerStatus),
                correctnessJudgment = entity.correctnessJudgment,
                detailedAnalysis = entity.detailedAnalysis,
                solution = entity.solution,
                geometry = entity.geometryJson?.let { geometryFromJson(it) }
            ),
            similarProblemsGenerated = entity.similarGenerated,
            tags = tags,
            notes = entity.notes
        )
    } catch (_: Exception) {
        null
    }

    private fun geometryToJson(spec: GeometrySpec): String {
        // 简单序列化：用 Map 结构
        val points = spec.points.mapValues { listOf(it.value.first, it.value.second) }
        val primitives = spec.primitives.map { prim ->
            when (prim) {
                is com.mathcoach.app.domain.model.GeometryPrimitive.Polygon ->
                    mapOf("type" to "polygon", "vertices" to prim.vertices)
                is com.mathcoach.app.domain.model.GeometryPrimitive.Segment ->
                    mapOf("type" to "segment", "p1" to prim.p1, "p2" to prim.p2, "style" to (prim.style ?: ""))
                is com.mathcoach.app.domain.model.GeometryPrimitive.Circle ->
                    mapOf("type" to "circle", "center" to prim.center, "radius" to (prim.radius ?: 0f))
                is com.mathcoach.app.domain.model.GeometryPrimitive.Angle ->
                    mapOf("type" to "angle", "vertex" to prim.vertex, "from" to prim.from, "to" to prim.to, "label" to (prim.label ?: ""))
                is com.mathcoach.app.domain.model.GeometryPrimitive.Label ->
                    mapOf("type" to "label", "at" to prim.at, "text" to prim.text, "offset" to listOf(prim.offset.first, prim.offset.second))
                is com.mathcoach.app.domain.model.GeometryPrimitive.Point ->
                    mapOf("type" to "point", "name" to prim.name)
                is com.mathcoach.app.domain.model.GeometryPrimitive.Function ->
                    mapOf("type" to "function", "expr" to prim.expr, "xRange" to listOf(prim.xMin, prim.xMax))
            }
        }
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any>>(mapType)
        return adapter.toJson(mapOf(
            "points" to points,
            "primitives" to primitives,
            "view" to mapOf("equal" to spec.view.equal, "showAxes" to spec.view.showAxes)
        ))
    }

    private fun geometryFromJson(jsonStr: String): GeometrySpec? {
        return try {
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any>>(mapType)
            val map = adapter.fromJson(jsonStr) ?: return null
            @Suppress("UNCHECKED_CAST")
            val pointsMap = (map["points"] as? Map<String, List<Double>>)?.mapValues {
                val coords = it.value
                (coords.getOrNull(0)?.toFloat() ?: 0f) to (coords.getOrNull(1)?.toFloat() ?: 0f)
            } ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val primsRaw = (map["primitives"] as? List<Map<String, Any>>) ?: emptyList()
            val prims = primsRaw.mapNotNull { prim ->
                when (prim["type"] as? String) {
                    "polygon" -> {
                        @Suppress("UNCHECKED_CAST")
                        val verts = (prim["vertices"] as? List<String>) ?: return@mapNotNull null
                        com.mathcoach.app.domain.model.GeometryPrimitive.Polygon(verts)
                    }
                    "segment" -> com.mathcoach.app.domain.model.GeometryPrimitive.Segment(
                        prim["p1"] as? String ?: return@mapNotNull null,
                        prim["p2"] as? String ?: return@mapNotNull null,
                        prim["style"] as? String
                    )
                    "circle" -> com.mathcoach.app.domain.model.GeometryPrimitive.Circle(
                        prim["center"] as? String ?: return@mapNotNull null,
                        (prim["radius"] as? Double)?.toFloat(),
                        prim["throughPoint"] as? String
                    )
                    "angle" -> com.mathcoach.app.domain.model.GeometryPrimitive.Angle(
                        prim["vertex"] as? String ?: return@mapNotNull null,
                        prim["from"] as? String ?: return@mapNotNull null,
                        prim["to"] as? String ?: return@mapNotNull null,
                        prim["label"] as? String
                    )
                    "label" -> {
                        @Suppress("UNCHECKED_CAST")
                        val offsetList = (prim["offset"] as? List<Double>) ?: listOf(0.0, 0.15)
                        com.mathcoach.app.domain.model.GeometryPrimitive.Label(
                            prim["at"] as? String ?: return@mapNotNull null,
                            prim["text"] as? String ?: return@mapNotNull null,
                            (offsetList.getOrNull(0)?.toFloat() ?: 0f) to (offsetList.getOrNull(1)?.toFloat() ?: 0.15f)
                        )
                    }
                    "point" -> com.mathcoach.app.domain.model.GeometryPrimitive.Point(
                        prim["name"] as? String ?: return@mapNotNull null
                    )
                    "function" -> {
                        @Suppress("UNCHECKED_CAST")
                        val range = (prim["xRange"] as? List<Double>) ?: listOf(-5.0, 5.0)
                        com.mathcoach.app.domain.model.GeometryPrimitive.Function(
                            prim["expr"] as? String ?: return@mapNotNull null,
                            range.getOrNull(0)?.toFloat() ?: -5f,
                            range.getOrNull(1)?.toFloat() ?: 5f
                        )
                    }
                    else -> null
                }
            }
            GeometrySpec(points = pointsMap, primitives = prims)
        } catch (_: Exception) {
            null
        }
    }
}
