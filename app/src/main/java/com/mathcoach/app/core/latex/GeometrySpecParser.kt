package com.mathcoach.app.core.latex

import com.mathcoach.app.domain.model.GeometryPrimitive
import com.mathcoach.app.domain.model.GeometrySpec
import com.mathcoach.app.domain.model.GeometryView
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 把 LLM 返回的 GeometrySpec JSON 解析成领域模型。
 * 解析失败一律返回 null，让上层降级为只显示文本。
 */
object GeometrySpecParser {

    fun parse(el: JsonElement?): GeometrySpec? {
        if (el == null) return null
        return try {
            val obj = el.jsonObject
            val points = parsePoints(obj["points"])
            val primitives = parsePrimitives(obj["primitives"])
            val view = parseView(obj["view"])
            if (points.isEmpty() && primitives.isEmpty()) return null
            GeometrySpec(points = points, primitives = primitives, view = view)
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePoints(el: JsonElement?): Map<String, Pair<Float, Float>> {
        if (el !is JsonObject) return emptyMap()
        val result = mutableMapOf<String, Pair<Float, Float>>()
        for ((key, value) in el) {
            val arr = value as? JsonArray ?: continue
            if (arr.size < 2) continue
            val x = arr[0].asFloatOrNull() ?: continue
            val y = arr[1].asFloatOrNull() ?: continue
            result[key] = x to y
        }
        return result
    }

    private fun parsePrimitives(el: JsonElement?): List<GeometryPrimitive> {
        if (el !is JsonArray) return emptyList()
        val result = mutableListOf<GeometryPrimitive>()
        for (item in el) {
            val obj = item as? JsonObject ?: continue
            val type = (obj["type"] as? JsonPrimitive)?.content ?: continue
            try {
                when (type) {
                    "polygon" -> {
                        val verts = (obj["vertices"] as? JsonArray)
                            ?.mapNotNull { (it as? JsonPrimitive)?.content }
                            ?: continue
                        result.add(GeometryPrimitive.Polygon(verts))
                    }
                    "segment" -> {
                        val p1 = (obj["p1"] as? JsonPrimitive)?.content ?: continue
                        val p2 = (obj["p2"] as? JsonPrimitive)?.content ?: continue
                        val style = (obj["style"] as? JsonPrimitive)?.content
                        result.add(GeometryPrimitive.Segment(p1, p2, style))
                    }
                    "circle" -> {
                        val center = (obj["center"] as? JsonPrimitive)?.content ?: continue
                        val radius = obj["radius"].asFloatOrNull()
                        val through = (obj["throughPoint"] as? JsonPrimitive)?.content
                        result.add(GeometryPrimitive.Circle(center, radius, through))
                    }
                    "angle" -> {
                        val vertex = (obj["vertex"] as? JsonPrimitive)?.content ?: continue
                        val from = (obj["from"] as? JsonPrimitive)?.content ?: continue
                        val to = (obj["to"] as? JsonPrimitive)?.content ?: continue
                        val label = (obj["label"] as? JsonPrimitive)?.content
                        result.add(GeometryPrimitive.Angle(vertex, from, to, label))
                    }
                    "label" -> {
                        val at = (obj["at"] as? JsonPrimitive)?.content ?: continue
                        val text = (obj["text"] as? JsonPrimitive)?.content ?: continue
                        val offsetArr = obj["offset"] as? JsonArray
                        val offset = if (offsetArr != null && offsetArr.size >= 2) {
                            val dx = offsetArr[0].asFloatOrNull() ?: 0f
                            val dy = offsetArr[1].asFloatOrNull() ?: 0.15f
                            dx to dy
                        } else 0f to 0.15f
                        result.add(GeometryPrimitive.Label(at, text, offset))
                    }
                    "point" -> {
                        val name = (obj["name"] as? JsonPrimitive)?.content ?: continue
                        result.add(GeometryPrimitive.Point(name))
                    }
                    "function" -> {
                        val expr = (obj["expr"] as? JsonPrimitive)?.content ?: continue
                        val range = obj["xRange"] as? JsonArray
                        val (xMin, xMax) = if (range != null && range.size >= 2) {
                            (range[0].asFloatOrNull() ?: -5f) to (range[1].asFloatOrNull() ?: 5f)
                        } else -5f to 5f
                        result.add(GeometryPrimitive.Function(expr, xMin, xMax))
                    }
                }
            } catch (_: Exception) {
                // 单条 primitive 解析失败跳过
            }
        }
        return result
    }

    private fun parseView(el: JsonElement?): GeometryView {
        if (el !is JsonObject) return GeometryView()
        val equal = (el["equal"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
        val showAxes = (el["showAxes"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
        return GeometryView(equal = equal, showAxes = showAxes)
    }

    private fun JsonElement?.asFloatOrNull(): Float? =
        (this as? JsonPrimitive)?.content?.toFloatOrNull()
}
