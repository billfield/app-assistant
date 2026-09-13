package com.mathcoach.app.core.latex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTolerantParserTest {

    @Test
    fun `直接解析合法 JSON 对象`() {
        val result = JsonTolerantParser.parse("""{"题目": "1+1=?", "难度": "1星"}""")
        assertNotNull(result)
    }

    @Test
    fun `剥 markdown json 代码块`() {
        val text = """
            ```json
            {"题目": "1+1=?"}
            ```
        """.trimIndent()
        val result = JsonTolerantParser.parse(text)
        assertNotNull(result)
    }

    @Test
    fun `剥普通 markdown 代码块`() {
        val text = """
            ```
            {"题目": "1+1=?"}
            ```
        """.trimIndent()
        assertNotNull(JsonTolerantParser.parse(text))
    }

    @Test
    fun `提取前后带说明文字的数组`() {
        val text = """好的，这是生成的题目：
[{"题号":1,"题目":"1+1=?"},{"题号":2,"题目":"2+2=?"}]
希望对你有帮助。"""
        val result = JsonTolerantParser.parseAsList(text)
        assertEquals(2, result.size)
    }

    @Test
    fun `修复末尾多余逗号`() {
        val text = """[{"题号":1,"题目":"1+1=?",}]"""
        val result = JsonTolerantParser.parseAsList(text)
        assertEquals(1, result.size)
    }

    @Test
    fun `截断的数组返回已完整的对象`() {
        val text = """[{"题号":1,"题目":"1+1=?"},{"题号":2,"题目":"2+"""
        val result = JsonTolerantParser.parseAsList(text)
        assertEquals(1, result.size)
    }

    @Test
    fun `空字符串返回 null`() {
        assertNull(JsonTolerantParser.parse(""))
        assertNull(JsonTolerantParser.parse("   "))
    }

    @Test
    fun `完全非 JSON 文本返回 null`() {
        assertNull(JsonTolerantParser.parse("这是一段普通中文，没有 JSON"))
    }
}
