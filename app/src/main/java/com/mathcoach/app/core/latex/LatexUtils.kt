package com.mathcoach.app.core.latex

/**
 * LaTeX 处理工具，对应 Python latex_utils.py。
 */
object LatexUtils {

    private val LATEX_CMD_RE = Regex(
        """\\(?:dfrac|tfrac|frac|sqrt|triangle|sum|prod|int|lim|""" +
            """alpha|beta|gamma|delta|epsilon|theta|lambda|mu|pi|sigma|""" +
            """phi|omega|times|cdot|pm|leq|geq|neq|approx|sim|vec|hat|""" +
            """bar|tilde|mathbf|mathrm|mathcal|mathbb|boxed|overline|""" +
            """underline|left|right|infty|ldots|cdots|displaystyle|text|""" +
            """because|therefore|Rightarrow|Leftrightarrow|to|sin|cos|""" +
            """tan|cot|sec|csc|log|ln|exp|max|min|gcd)"""
    )

    /**
     * 处理模型返回 JSON 字符串里残留的字面量转义序列。
     * 对应 Python unescape_text。
     */
    fun unescape(text: String): String {
        var t = text
        t = t.replace("\\n", "\n")
        t = t.replace("\\\"", "\"")
        // 特殊处理 \t 在 \triangle 中
        t = Regex("""\t(riangle)""").replace(t, "\\\\triangle")
        if (t.contains("\t")) t = t.replace("\t", "\\t")
        if (t.contains("\b")) t = t.replace("\b", "\\b")
        if (t.contains("\u000C")) t = t.replace("\u000C", "\\f")
        if (t.contains("\r")) t = t.replace("\r", "\\r")
        // \n 命令（neq, not, nu, nabla, nearrow, narrow）
        t = Regex("""(?<![\n\r])\n(earrow|warrow|abla|eq|ot|u)(?=\S)""")
            .replace(t) { "\\" + it.groupValues[1] }
        return t
    }

    /**
     * 自动为未包裹的 LaTeX 片段加 $...$ 或 $$...$$。
     * 对应 Python wrap_latex。
     */
    fun wrapLatex(text: String): String {
        if (text.isEmpty()) return text
        var t = text
        // \tag{...} 转纯文本
        t = Regex("""\\tag\{([^}]+)\}""").replace(t) { "(${it.groupValues[1]})" }
        t = t.trim()

        // 已被整体包裹的不处理
        if (t.startsWith("$$") && t.endsWith("$$")) {
            val inner = t.substring(2, t.length - 2).trim()
            return "$$\n$inner\n$$"
        }
        if (t.startsWith("$") && t.endsWith("$") && !t.startsWith("$$")) return t

        // 修复不匹配 $ 对
        t = Regex("""(?<![a-zA-Z0-9$])\$([^$\n]+?)\$\$(?![a-zA-Z0-9$])""")
            .replace(t) { "$" + it.groupValues[1] + "$" }
        t = Regex("""(?<![a-zA-Z0-9$])\$\$([^$\n]+?)\$(?!\$)""")
            .replace(t) { "$$" + it.groupValues[1] + "$$" }

        // 独立 \begin{}...\end{} 块
        val envPattern = Regex("""(?<!\$)(\\begin\{[a-zA-Z*]+\}.*?\\end\{[a-zA-Z*]+\})(?!\$)""", RegexOption.DOT_MATCHES_ALL)
        t = envPattern.replace(t) { "$$\n${it.groupValues[1].trim()}\n$$" }

        // 逐行处理
        val lines = t.split("\n")
        val result = mutableListOf<String>()
        var inDisplay = false

        for (line in lines) {
            val stripped = line.trimEnd()
            if (stripped.isEmpty()) {
                result.add(line)
                continue
            }
            if (stripped == "$$") {
                inDisplay = !inDisplay
                result.add(line)
                continue
            }
            if (inDisplay) {
                result.add(line)
                continue
            }
            if (!LATEX_CMD_RE.containsMatchIn(stripped)) {
                result.add(line)
                continue
            }

            var cur = stripped
            // 去掉行尾未匹配的单个 $
            if (cur.endsWith("$") && !cur.endsWith("$$") && cur.count { it == '$' } % 2 == 1) {
                cur = cur.dropLast(1)
            }

            // 已整体包裹的行跳过
            if ((cur.startsWith("$$") && cur.endsWith("$$")) ||
                (cur.startsWith("$") && cur.endsWith("$") && !cur.startsWith("$$"))
            ) {
                result.add(cur)
                continue
            }

            // 纯数学行（无中文）→ 用 $$ 包裹整行
            val hasChinese = cur.any { it in '\u4e00'..'\u9fff' }
            if (!cur.contains("$") && !hasChinese) {
                result.add("$$\n$cur\n$$")
                continue
            }

            // 行内混合：含 $ 或中文
            val protected = mutableListOf<String>()
            fun saveMath(m: MatchResult): String {
                protected.add(m.value)
                return "__MATH${protected.size - 1}__"
            }

            var temp = Regex("""\$\$.*?\$\$""", RegexOption.DOT_MATCHES_ALL).replace(cur, ::saveMath)
            temp = Regex("""\$.*?\$""").replace(temp, ::saveMath)

            // 收集需要包裹的区域
            val regions = mutableListOf<IntRange>()
            val breakChars = " \n\t，。；：！？、（）\"\"''【】《》$\u200B"
            for (m in LATEX_CMD_RE.findAll(temp)) {
                var left = m.range.first
                while (left > 0) {
                    val ch = temp[left - 1]
                    if (breakChars.contains(ch)) break
                    if (ch in '\u4e00'..'\u9fff') break
                    left--
                }
                var right = m.range.last + 1
                while (right < temp.length && !breakChars.contains(temp[right])) right++
                val expr = temp.substring(left, right)
                if (expr.isNotEmpty() && !expr.contains("$")) {
                    regions.add(left until right)
                }
            }

            if (regions.isNotEmpty()) {
                val sorted = regions.sortedBy { it.first }
                val merged = mutableListOf(sorted.first())
                for (r in sorted.drop(1)) {
                    val last = merged.last()
                    if (r.first <= last.last + 1) {
                        merged[merged.lastIndex] = last.first..maxOf(last.last, r.last)
                    } else {
                        merged.add(r)
                    }
                }
                val sb = StringBuilder()
                var pos = 0
                for (r in merged) {
                    sb.append(temp.substring(pos, r.first))
                    sb.append("$").append(temp.substring(r.first, r.last + 1)).append("$")
                    pos = r.last + 1
                }
                sb.append(temp.substring(pos))
                temp = sb.toString()
            }

            for ((i, ph) in protected.withIndex()) {
                temp = temp.replace("__MATH${i}__", ph)
            }
            result.add(temp)
        }

        t = result.joinToString("\n")

        // 最终补救：为常见未包裹的 LaTeX 命令补加 $ 包裹
        val protected2 = mutableListOf<String>()
        fun saveMath2(m: MatchResult): String {
            protected2.add(m.value)
            return "__MATH${protected2.size - 1}__"
        }
        var temp = Regex("""\$\$.*?\$\$""", RegexOption.DOT_MATCHES_ALL).replace(t, ::saveMath2)
        temp = Regex("""\$.*?\$""").replace(temp, ::saveMath2)

        temp = Regex("""\\text\{([^{}]*)\}""").replace(temp) { "$\\text{${it.groupValues[1]}}$" }
        temp = Regex("""\\frac\{([^{}]*)\}\{([^{}]*)\}""").replace(temp) {
            "$\\frac{${it.groupValues[1]}}{${it.groupValues[2]}}$"
        }
        temp = Regex(
            """\\(because|therefore|triangle|angle|perp|sim|boxed|""" +
                """Rightarrow|Leftrightarrow|cdot|pm|leq|geq|neq|approx|""" +
                """times|sqrt|vec|hat|bar|tilde|mathbf|mathrm|mathcal|mathbb|""" +
                """overline|underline|left|right|displaystyle|infty|ldots|cdots|""" +
                """alpha|beta|gamma|delta|epsilon|theta|lambda|mu|pi|sigma|phi|""" +
                """omega|sin|cos|tan|cot|sec|csc|log|ln|exp|max|min|gcd|circ|degree)""" +
                """(?![a-zA-Z])"""
        ).replace(temp) { "$\\${it.groupValues[1]}$" }

        for ((i, ph) in protected2.withIndex()) {
            temp = temp.replace("__MATH${i}__", ph)
        }
        return temp
    }
}
