package com.mathcoach.app.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathcoach.app.core.katex.KatexText
import com.mathcoach.app.domain.model.SimilarProblem

@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🎯 针对性练习", style = MaterialTheme.typography.titleLarge)
        Text(
            "基于你的学习画像和薄弱知识点，AI 生成针对性练习题。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 题数选择
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("生成题数", modifier = Modifier.weight(1f))
                    Text("${ui.targetCount}", style = MaterialTheme.typography.titleMedium)
                }
                Slider(
                    value = ui.targetCount.toFloat(),
                    onValueChange = { viewModel.setTargetCount(it.toInt()) },
                    valueRange = 3f..10f,
                    steps = 6,
                    enabled = !ui.generating
                )
            }
        }

        Row {
            Button(
                onClick = viewModel::generate,
                enabled = !ui.generating,
                modifier = Modifier.weight(1f)
            ) {
                if (ui.generating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (ui.generating) "生成中..." else "生成 ${ui.targetCount} 道针对性练习")
            }
            if (ui.problems.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = viewModel::clear) {
                    Text("清空")
                }
            }
        }

        ui.error?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // 流式中间态
        if (ui.generating && ui.problems.isEmpty() && ui.partialText.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "AI 正在出题...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(ui.partialText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 题目卡片
        ui.problems.forEach { problem ->
            PracticeCard(problem)
        }
    }
}

@Composable
private fun PracticeCard(problem: SimilarProblem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "第 ${problem.index} 题",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (problem.knowledgePoints.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        problem.knowledgePoints,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            problem.geometry?.let { spec ->
                com.mathcoach.app.core.geometry.GeometryView(
                    spec = spec,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("题目", style = MaterialTheme.typography.titleSmall)
            KatexText(text = problem.title, modifier = Modifier.fillMaxWidth())

            if (problem.solution.isNotBlank()) {
                Text("解答", style = MaterialTheme.typography.titleSmall)
                KatexText(text = problem.solution, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
