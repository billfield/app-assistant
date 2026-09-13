package com.mathcoach.app.core.latex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexUtilsTest {

    @Test
    fun `unescape 处理字面量换行`() {
        val input = """第一行\n第二行"""
        assertEquals("第一行\n第二行", LatexUtils.unescape(input))
    }

    @Test
    fun `unescape 处理 triangle 中的 tab`() {
        // \t 会被 JSON 解析为 tab，需要还原为 \triangle
        val input = "在\triangle ABC 中"
        val result = LatexUtils.unescape(input)
        assertTrue(result.contains("\\triangle"))
    }

    @Test
    fun `wrapLatex 已整体包裹的字符串不变`() {
        val input = """$$\frac{1}{2}$$"""
        val result = LatexUtils.wrapLatex(input)
        assertTrue(result.startsWith("$$"))
        assertTrue(result.endsWith("$$"))
    }

    @Test
    fun `wrapLatex 给未包裹的 frac 加 $`() {
        val input = """所以 \frac{1}{2} 是答案"""
        val result = LatexUtils.wrapLatex(input)
        assertTrue(result.contains("$\\frac{1}{2}$"))
    }

    @Test
    fun `wrapLatex 处理 begin-end 块`() {
        val input = """解：\begin{aligned} x &= 1 \\ y &= 2 \end{aligned} 完毕"""
        val result = LatexUtils.wrapLatex(input)
        assertTrue(result.contains("$$"))
        assertTrue(result.contains("\\begin{aligned}"))
    }
}
