package com.mathcoach.app.feature.analytics

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathcoach.app.domain.model.KnowledgeMastery

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val weakSpots by viewModel.weakSpots.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "📊 知识点掌握度",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("总览") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("薄弱项") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("建议") })
        }

        Box(modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewTab(profile.knowledgeMastery.values.toList())
                1 -> WeakSpotsTab(weakSpots)
                2 -> RecommendationsTab(recommendations)
            }
        }
    }
}

@Composable
private fun OverviewTab(items: List<KnowledgeMastery>) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("暂无数据，去拍几道题吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("掌握度雷达图", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                MasteryRadarChart(items = items)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("知识点列表（按掌握度排序）", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                items.sortedByDescending { it.masteryScore }.forEach { km ->
                    MasteryRow(km)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun MasteryRow(km: KnowledgeMastery) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(km.knowledgePoint, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${(km.masteryScore * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (km.masteryScore >= 0.7f) MaterialTheme.colorScheme.tertiary
                        else if (km.masteryScore >= 0.5f) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.error
            )
        }
        LinearProgressIndicator(
            progress = { km.masteryScore.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "练习 ${km.totalAttempts} 次 / 正确 ${km.correctCount} / 部分 ${km.partialCount} / 错误 ${km.wrongCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeakSpotsTab(weakSpots: List<KnowledgeMastery>) {
    if (weakSpots.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("✨ 没有明显的薄弱项", color = MaterialTheme.colorScheme.tertiary)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        weakSpots.forEach { km ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(km.knowledgePoint, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "掌握度 ${(km.masteryScore * 100).toInt()}%，正确率 ${(km.accuracyRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (km.streakWrong > 0) {
                        Text(
                            "⚠️ 连续错误 ${km.streakWrong} 次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsTab(recommendations: List<com.mathcoach.app.domain.model.PracticeRecommendation>) {
    if (recommendations.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("暂无练习建议", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recommendations.forEach { rec ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rec.knowledgePoint, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            "优先级 ${rec.priority}/10",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(rec.reason, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "建议：${rec.difficulty.display} 难度 × ${rec.count} 道",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
