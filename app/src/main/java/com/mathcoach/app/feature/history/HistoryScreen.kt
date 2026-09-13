package com.mathcoach.app.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathcoach.app.core.katex.KatexText
import com.mathcoach.app.domain.model.AnswerStatus
import com.mathcoach.app.domain.model.HistoryEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val filtered by viewModel.filtered.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val allKp by viewModel.allKnowledgePoints.collectAsState()
    var detailEntry by remember { mutableStateOf<HistoryEntry?>(null) }
    var showExport by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📚 历史记录", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { showExport = true }) {
                Text("导出")
            }
        }

        // 统计卡
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("总数", stats.total.toString(), Modifier.weight(1f))
            StatCard("完全正确", stats.correct.toString(), Modifier.weight(1f))
            StatCard("部分正确", stats.partial.toString(), Modifier.weight(1f))
            StatCard("完全错误", stats.wrong.toString(), Modifier.weight(1f))
        }

        // 筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.knowledgePoint == null && filter.answerStatus == null,
                onClick = { viewModel.clearFilter() },
                label = { Text("全部") }
            )
            AnswerStatus.entries.forEach { status ->
                FilterChip(
                    selected = filter.answerStatus == status,
                    onClick = {
                        viewModel.setAnswerStatus(if (filter.answerStatus == status) null else status)
                    },
                    label = { Text(status.display) }
                )
            }
        }

        if (allKp.isNotEmpty()) {
            var kpExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = kpExpanded,
                onExpandedChange = { kpExpanded = !kpExpanded }
            ) {
                OutlinedTextField(
                    value = filter.knowledgePoint ?: "全部知识点",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("按知识点筛选") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(kpExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = kpExpanded,
                    onDismissRequest = { kpExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("全部知识点") },
                        onClick = {
                            viewModel.setKnowledgePoint(null)
                            kpExpanded = false
                        }
                    )
                    allKp.forEach { kp ->
                        DropdownMenuItem(
                            text = { Text(kp) },
                            onClick = {
                                viewModel.setKnowledgePoint(kp)
                                kpExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 列表
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无历史记录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { entry ->
                    HistoryListItem(
                        entry = entry,
                        onClick = { detailEntry = entry },
                        onDelete = { viewModel.delete(entry.id) }
                    )
                }
            }
        }
    }

    // 详情对话框
    detailEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { detailEntry = null },
            confirmButton = {
                TextButton(onClick = { detailEntry = null }) { Text("关闭") }
            },
            title = { Text("题目详情") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("题型：${entry.analysis.questionType.display}")
                    Text("难度：${entry.analysis.difficulty.display}")
                    Text("作答：${entry.analysis.answerStatus.display}")
                    if (entry.analysis.knowledgePoints.isNotEmpty()) {
                        Text("知识点：${entry.analysis.knowledgePoints.joinToString("、")}")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("题目", style = MaterialTheme.typography.titleSmall)
                    KatexText(text = entry.analysis.title, modifier = Modifier.fillMaxWidth())
                    if (entry.analysis.detailedAnalysis.isNotBlank()) {
                        Text("详细解析", style = MaterialTheme.typography.titleSmall)
                        KatexText(text = entry.analysis.detailedAnalysis, modifier = Modifier.fillMaxWidth())
                    }
                    if (entry.analysis.solution.isNotBlank()) {
                        Text("解答过程", style = MaterialTheme.typography.titleSmall)
                        KatexText(text = entry.analysis.solution, modifier = Modifier.fillMaxWidth())
                    }
                    if (entry.notes.isNotBlank()) {
                        Text("备注", style = MaterialTheme.typography.titleSmall)
                        Text(entry.notes)
                    }
                }
            }
        )
    }

    // 导出对话框
    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            confirmButton = {
                TextButton(onClick = {
                    val json = viewModel.exportJson(filtered)
                    try {
                        val dir = context.getExternalFilesDir(null)
                        val file = File(dir, "mathcoach_history_${System.currentTimeMillis()}.json")
                        file.writeText(json)
                    } catch (_: Exception) {}
                    showExport = false
                }) { Text("保存到文件") }
            },
            dismissButton = {
                TextButton(onClick = { showExport = false }) { Text("取消") }
            },
            title = { Text("导出历史") },
            text = { Text("将 ${filtered.size} 条记录导出为 JSON 文件，保存到 App 的外部存储目录。") }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryListItem(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.analysis.title.take(50) + if (entry.analysis.title.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Text(
                    sdf.format(Date(entry.timestampMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    entry.analysis.questionType.display,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    entry.analysis.difficulty.display,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    entry.analysis.answerStatus.display,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (entry.analysis.answerStatus) {
                        AnswerStatus.CORRECT -> MaterialTheme.colorScheme.tertiary
                        AnswerStatus.WRONG -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复。") }
        )
    }
}
