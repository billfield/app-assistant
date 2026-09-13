package com.mathcoach.app.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.mathcoach.app.domain.model.KnowledgeMastery
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 雷达图：展示 N 个知识点的 mastery_score。
 * 选掌握度 top/2 最高 + top/2 最低，对齐 Python create_mastery_radar。
 */
@Composable
fun MasteryRadarChart(
    items: List<KnowledgeMastery>,
    modifier: Modifier = Modifier,
    topN: Int = 8
) {
    if (items.isEmpty()) return

    // 选最高 + 最低各一半
    val sorted = items.sortedByDescending { it.masteryScore }
    val half = topN / 2
    val selected = (sorted.take(half) + sorted.takeLast(half)).distinctBy { it.knowledgePoint }
    if (selected.size < 3) return

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = min(size.width, size.height) / 2 * 0.7f
        val n = selected.size

        // 外圈
        for (ring in 1..5) {
            val rr = r * ring / 5
            val path = Path()
            for (i in 0..n) {
                val angle = (Math.PI * 2 * i / n - Math.PI / 2).toFloat()
                val x = cx + rr * cos(angle)
                val y = cy + rr * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFFE5E7EB), style = Stroke(width = 1f))
        }

        // 轴线
        for (i in 0 until n) {
            val angle = (Math.PI * 2 * i / n - Math.PI / 2).toFloat()
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            drawLine(Color(0xFFD1D5DB), Offset(cx, cy), Offset(x, y), strokeWidth = 1f)
        }

        // 数据多边形
        val dataPath = Path()
        for (i in 0..n) {
            val idx = i % n
            val angle = (Math.PI * 2 * idx / n - Math.PI / 2).toFloat()
            val rr = r * selected[idx].masteryScore.coerceIn(0f, 1f)
            val x = cx + rr * cos(angle)
            val y = cy + rr * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, Color(0x334CAF50))
        drawPath(dataPath, Color(0xFF4CAF50), style = Stroke(width = 3f))

        // 数据点
        for (i in 0 until n) {
            val angle = (Math.PI * 2 * i / n - Math.PI / 2).toFloat()
            val rr = r * selected[i].masteryScore.coerceIn(0f, 1f)
            val x = cx + rr * cos(angle)
            val y = cy + rr * sin(angle)
            drawCircle(Color(0xFF4CAF50), radius = 5f, center = Offset(x, y))
        }

        // 标签
        for (i in 0 until n) {
            val angle = (Math.PI * 2 * i / n - Math.PI / 2).toFloat()
            val lr = r * 1.18f
            val x = cx + lr * cos(angle)
            val y = cy + lr * sin(angle)
            drawContext.canvas.nativeCanvas.drawText(
                selected[i].knowledgePoint.take(8),
                x, y,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}
