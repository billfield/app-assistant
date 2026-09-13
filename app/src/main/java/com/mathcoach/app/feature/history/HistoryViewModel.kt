package com.mathcoach.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathcoach.app.data.repository.HistoryRepository
import com.mathcoach.app.domain.model.AnswerStatus
import com.mathcoach.app.domain.model.HistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryFilter(
    val knowledgePoint: String? = null,
    val answerStatus: AnswerStatus? = null
)

data class HistoryStats(
    val total: Int = 0,
    val correct: Int = 0,
    val partial: Int = 0,
    val wrong: Int = 0,
    val noAnswer: Int = 0
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val allEntries: StateFlow<List<HistoryEntry>> = repo.observeAllDesc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filtered: StateFlow<List<HistoryEntry>> = combine(allEntries, _filter) { list, f ->
        list.filter { entry ->
            (f.knowledgePoint == null || entry.analysis.knowledgePoints.contains(f.knowledgePoint)) &&
                (f.answerStatus == null || entry.analysis.answerStatus == f.answerStatus)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<HistoryStats> = combine(allEntries, _filter) { list, _ ->
        HistoryStats(
            total = list.size,
            correct = list.count { it.analysis.answerStatus == AnswerStatus.CORRECT },
            partial = list.count { it.analysis.answerStatus == AnswerStatus.PARTIAL },
            wrong = list.count { it.analysis.answerStatus == AnswerStatus.WRONG },
            noAnswer = list.count { it.analysis.answerStatus == AnswerStatus.NO_ANSWER }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryStats())

    val allKnowledgePoints: StateFlow<List<String>> = combine(allEntries, _filter) { list, _ ->
        list.flatMap { it.analysis.knowledgePoints }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setKnowledgePoint(kp: String?) {
        _filter.value = _filter.value.copy(knowledgePoint = kp)
    }

    fun setAnswerStatus(status: AnswerStatus?) {
        _filter.value = _filter.value.copy(answerStatus = status)
    }

    fun clearFilter() {
        _filter.value = HistoryFilter()
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.deleteById(id) }
    }

    fun exportJson(entries: List<HistoryEntry>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        entries.forEachIndexed { idx, e ->
            sb.append("  {\n")
            sb.append("""    "id": "${e.id}",""").append("\n")
            sb.append("""    "timestamp": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date(e.timestampMillis))}",""").append("\n")
            sb.append("""    "image_hash": ${e.imageHash?.let { "\"$it\"" } ?: "null"},""").append("\n")
            sb.append("""    "analysis": {""").append("\n")
            sb.append("""      "题目": ${quote(e.analysis.title)},""").append("\n")
            sb.append("""      "题型": "${e.analysis.questionType.display}",""").append("\n")
            sb.append("""      "难度": "${e.analysis.difficulty.display}",""").append("\n")
            sb.append("""      "知识点": [${e.analysis.knowledgePoints.joinToString(",") { "\"$it\"" }}],""").append("\n")
            sb.append("""      "学生作答情况": "${e.analysis.answerStatus.display}",""").append("\n")
            sb.append("""      "正误判断": ${quote(e.analysis.correctnessJudgment)},""").append("\n")
            sb.append("""      "详细解析": ${quote(e.analysis.detailedAnalysis)},""").append("\n")
            sb.append("""      "解答过程": ${quote(e.analysis.solution)}""").append("\n")
            sb.append("    },\n")
            sb.append("""    "similar_problems_generated": ${e.similarProblemsGenerated},""").append("\n")
            sb.append("""    "tags": [${e.tags.joinToString(",") { "\"$it\"" }}],""").append("\n")
            sb.append("""    "notes": ${quote(e.notes)}""").append("\n")
            sb.append("  }")
            if (idx < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}
