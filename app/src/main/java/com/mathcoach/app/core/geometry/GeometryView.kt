package com.mathcoach.app.core.geometry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.mathcoach.app.domain.model.GeometryPrimitive
import com.mathcoach.app.domain.model.GeometrySpec
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Compose Canvas 渲染 GeometrySpec。
 *
 * - 自动 fit bounds（含 padding）
 * - Y 轴翻转（数学坐标系 → 屏幕坐标系）
 * - 等比例缩放（view.equal=true 时）
 * - 标签用 Android native Canvas 绘制
 */
@Composable
fun GeometryView(
    spec: GeometrySpec,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        if (spec.points.isEmpty()) return@Canvas

        // 计算 bounds
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for ((_, p) in spec.points) {
            minX = min(minX, p.first)
            maxX = max(maxX, p.first)
            minY = min(minY, p.second)
            maxY = max(maxY, p.second)
        }
        // 圆/函数可能扩大 bounds（按半径扩）
        for (prim in spec.primitives) {
            when (prim) {
                is GeometryPrimitive.Circle -> {
                    val c = spec.points[prim.center] ?: continue
                    val r = prim.radius ?: 1f
                    minX = min(minX, c.first - r)
                    maxX = max(maxX, c.first + r)
                    minY = min(minY, c.second - r)
                    maxY = max(maxY, c.second + r)
                }
                is GeometryPrimitive.Function -> {
                    minX = min(minX, prim.xMin)
                    maxX = max(maxX, prim.xMax)
                }
                else -> Unit
            }
        }
        if (!minX.isFinite() || !maxX.isFinite() || maxX - minX < 1e-6f) return@Canvas

        val pad = 0.1f * max(maxX - minX, maxY - minY)
        minX -= pad; maxX += pad; minY -= pad; maxY += pad

        val w = size.width
        val h = size.height
        val dataW = maxX - minX
        val dataH = maxY - minY

        val scale = if (spec.view.equal) min(w / dataW, h / dataH) else min(w / dataW, h / dataH)
        val offsetX = (w - dataW * scale) / 2
        val offsetY = (h - dataH * scale) / 2

        fun toScreen(p: Pair<Float, Float>): Offset {
            val x = (p.first - minX) * scale + offsetX
            val y = h - ((p.second - minY) * scale + offsetY)  // Y 翻转
            return Offset(x, y)
        }

        // 坐标轴
        if (spec.view.showAxes) {
            val axisColor = Color(0xFF9CA3AF)
            // X 轴
            if (minY < 0 && maxY > 0) {
                val y0 = toScreen(minX to 0f).y
                drawLine(axisColor, Offset(0f, y0), Offset(w, y0), strokeWidth = 1f)
            }
            // Y 轴
            if (minX < 0 && maxX > 0) {
                val x0 = toScreen(0f to minY).x
                drawLine(axisColor, Offset(x0, 0f), Offset(x0, h), strokeWidth = 1f)
            }
        }

        // 绘制 primitives
        val strokeColor = Color(0xFF1F2937)
        val fillColor = Color(0x1A4F46E5)
        val angleColor = Color(0xFF0891B2)

        for (prim in spec.primitives) {
            when (prim) {
                is GeometryPrimitive.Polygon -> {
                    if (prim.vertices.size < 2) continue
                    val path = Path()
                    var first = true
                    for (vName in prim.vertices) {
                        val pt = spec.points[vName] ?: continue
                        val sp = toScreen(pt)
                        if (first) { path.moveTo(sp.x, sp.y); first = false }
                        else path.lineTo(sp.x, sp.y)
                    }
                    path.close()
                    if (prim.fill) drawPath(path, fillColor)
                    drawPath(path, strokeColor, style = Stroke(width = 2f))
                }

                is GeometryPrimitive.Segment -> {
                    val p1 = spec.points[prim.p1] ?: continue
                    val p2 = spec.points[prim.p2] ?: continue
                    val sp1 = toScreen(p1)
                    val sp2 = toScreen(p2)
                    drawLine(
                        color = strokeColor,
                        start = sp1,
                        end = sp2,
                        strokeWidth = if (prim.style == "dashed") 2f else 2f
                    )
                }

                is GeometryPrimitive.Circle -> {
                    val center = spec.points[prim.center] ?: continue
                    val radius = prim.radius ?: run {
                        val through = prim.throughPoint?.let { spec.points[it] } ?: return@run null
                        val dx = through.first - center.first
                        val dy = through.second - center.second
                        kotlin.math.sqrt(dx * dx + dy * dy)
                    } ?: continue
                    val sc = toScreen(center)
                    val r = radius * scale
                    drawCircle(strokeColor, radius = r, center = sc, style = Stroke(width = 2f))
                }

                is GeometryPrimitive.Angle -> {
                    val v = spec.points[prim.vertex] ?: continue
                    val a = spec.points[prim.from] ?: continue
                    val b = spec.points[prim.to] ?: continue
                    val angA = atan2(a.second - v.second, a.first - v.first)
                    val angB = atan2(b.second - v.second, b.first - v.first)
                    var diff = angB - angA
                    while (diff > Math.PI.toFloat() * 2) diff -= (Math.PI * 2).toFloat()
                    while (diff < 0) diff += (Math.PI * 2).toFloat()
                    if (diff > Math.PI) diff = (Math.PI * 2).toFloat() - diff

                    val r = 0.15f * max(dataW, dataH)
                    val sv = toScreen(v)
                    val steps = 16
                    var prevX = sv.x + r * scale * cos(angA)
                    var prevY = sv.y - r * scale * sin(angA)
                    for (i in 1..steps) {
                        val t = angA + diff * i / steps
                        val x = sv.x + r * scale * cos(t)
                        val y = sv.y - r * scale * sin(t)
                        drawLine(angleColor, Offset(prevX, prevY), Offset(x, y), strokeWidth = 2f)
                        prevX = x; prevY = y
                    }
                }

                is GeometryPrimitive.Label -> {
                    val at = spec.points[prim.at] ?: continue
                    val sp = toScreen(at)
                    val labelOffset = Offset(
                        prim.offset.first * scale,
                        -prim.offset.second * scale  // Y 翻转
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        prim.text,
                        sp.x + labelOffset.x,
                        sp.y + labelOffset.y,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 32f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                    )
                }

                is GeometryPrimitive.Point -> {
                    val p = spec.points[prim.name] ?: continue
                    val sp = toScreen(p)
                    drawCircle(strokeColor, radius = 4f, center = sp)
                }

                is GeometryPrimitive.Function -> {
                    // 用多边形近似 y = f(x)。支持几种简单形式：kx+b、ax^2+bx+c
                    val pts = sampleFunction(prim.expr, prim.xMin, prim.xMax, 50) ?: continue
                    var prev: Offset? = null
                    for (pt in pts) {
                        val sp = toScreen(pt)
                        prev?.let {
                            drawLine(strokeColor, it, sp, strokeWidth = 2f)
                        }
                        prev = sp
                    }
                }
            }
        }
    }
}

/**
 * 极简函数采样器：支持 kx+b、ax^2+bx+c 形式的 LaTeX/数学字符串。
 * 不支持的表达式返回 null（不绘制）。
 */
private fun sampleFunction(expr: String, xMin: Float, xMax: Float, steps: Int): List<Pair<Float, Float>>? {
    val clean = expr.replace(" ", "").replace("y=", "").replace("$", "")
    // 线性：kx+b（例如 "2x+1"、"-x+3"、"x"）
    val linear = Regex("""^([+-]?\d*\.?\d*)\*?x(?:([+-]\d+\.?\d*))?$""").matchEntire(clean)
    if (linear != null) {
        val k = linear.groupValues[1].let {
            when {
                it.isEmpty() || it == "+" -> 1f
                it == "-" -> -1f
                else -> it.toFloatOrNull() ?: return null
            }
        }
        val b = linear.groupValues.getOrNull(2)?.toFloatOrNull() ?: 0f
        return (0..steps).map { i ->
            val x = xMin + (xMax - xMin) * i / steps
            x to (k * x + b)
        }
    }
    // 二次：ax^2+bx+c
    val quad = Regex("""^([+-]?\d*\.?\d*)\*?x\^?2(?:([+-]\d+\.?\d*)\*?x)?(?:([+-]\d+\.?\d*))?$""").matchEntire(clean)
    if (quad != null) {
        val a = quad.groupValues[1].let {
            when {
                it.isEmpty() || it == "+" -> 1f
                it == "-" -> -1f
                else -> it.toFloatOrNull() ?: return null
            }
        }
        val b = quad.groupValues.getOrNull(2)?.toFloatOrNull() ?: 0f
        val c = quad.groupValues.getOrNull(3)?.toFloatOrNull() ?: 0f
        return (0..steps).map { i ->
            val x = xMin + (xMax - xMin) * i / steps
            x to (a * x * x + b * x + c)
        }
    }
    return null
}
