package com.mathcoach.app.feature.solve

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathcoach.app.core.katex.KatexText
import com.mathcoach.app.domain.model.ProblemAnalysis

@Composable
fun SolveScreen(
    viewModel: SolveViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    var showCamera by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(viewModel::onImagePicked) }

    if (showCamera) {
        CameraScreen(
            onCaptured = { bytes ->
                viewModel.onCameraCapture(bytes)
                showCamera = false
            },
            onCancel = { showCamera = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("拍照解题", style = MaterialTheme.typography.titleLarge)

        // 图片选择/预览
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (ui.imageBytes != null) {
                    val bitmap = remember(ui.imageBytes) {
                        BitmapFactory.decodeByteArray(ui.imageBytes, 0, ui.imageBytes!!.size)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "题目",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(it.width.toFloat() / it.height),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = viewModel::clearImage) { Text("清除图片") }
                } else {
                    Text("尚未选择图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Button(onClick = { pickImageLauncher.launch("image/*") }) {
                            Text("📷 从相册选择")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showCamera = true }) {
                            Text("📸 拍照")
                        }
                    }
                }
            }
        }

        // 用户输入
        OutlinedTextField(
            value = ui.userInput,
            onValueChange = viewModel::onUserInputChange,
            label = { Text("提问（可选）") },
            placeholder = { Text("例如：请帮我分析这道题") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        // 提交按钮
        Button(
            onClick = viewModel::analyze,
            enabled = !ui.analyzing && ui.imageBytes != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ui.analyzing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(20.dp)
                        .width(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (ui.analyzing) "正在分析..." else "开始分析")
        }

        if (ui.fromCache) {
            Text("✓ 命中缓存", color = MaterialTheme.colorScheme.primary)
        }
        if (ui.savedEntryIds.isNotEmpty()) {
            Text("✓ 已保存 ${ui.savedEntryIds.size} 条到历史", color = MaterialTheme.colorScheme.primary)
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

        // 解析结果
        if (ui.parsedAnalyses.isNotEmpty()) {
            ui.parsedAnalyses.forEach { analysis ->
                AnalysisCard(
                    analysis = analysis,
                    onGenerateSimilar = { viewModel.generateSimilar(analysis, count = 3) },
                    generatingSimilar = ui.generatingSimilar
                )
            }

            // 同类题结果
            if (ui.similarProblems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("📚 同类练习题", style = MaterialTheme.typography.titleMedium)
                ui.similarProblems.forEach { problem ->
                    SimilarProblemCard(problem)
                }
            }
            ui.similarError?.let {
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
        } else if (ui.partialText.isNotBlank()) {
            // 流式中间态：先按文本展示
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "老师正在作答...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(ui.partialText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AnalysisCard(
    analysis: ProblemAnalysis,
    onGenerateSimilar: () -> Unit,
    generatingSimilar: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row {
                Text(
                    analysis.questionType.display,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    analysis.difficulty.display,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    analysis.answerStatus.display,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (analysis.answerStatus) {
                        com.mathcoach.app.domain.model.AnswerStatus.CORRECT -> MaterialTheme.colorScheme.tertiary
                        com.mathcoach.app.domain.model.AnswerStatus.WRONG -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (analysis.knowledgePoints.isNotEmpty()) {
                Text(
                    "知识点：" + analysis.knowledgePoints.joinToString("、"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 几何图形（如果有）
            analysis.geometry?.let { spec ->
                com.mathcoach.app.core.geometry.GeometryView(
                    spec = spec,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("题目", style = MaterialTheme.typography.titleSmall)
            KatexText(
                text = analysis.title,
                modifier = Modifier.fillMaxWidth()
            )

            if (analysis.correctnessJudgment.isNotBlank()) {
                Text("正误判断", style = MaterialTheme.typography.titleSmall)
                Text(analysis.correctnessJudgment, style = MaterialTheme.typography.bodyMedium)
            }

            if (analysis.detailedAnalysis.isNotBlank()) {
                Text("详细解析", style = MaterialTheme.typography.titleSmall)
                KatexText(
                    text = analysis.detailedAnalysis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (analysis.solution.isNotBlank()) {
                Text("解答过程", style = MaterialTheme.typography.titleSmall)
                KatexText(
                    text = analysis.solution,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedButton(
                onClick = onGenerateSimilar,
                enabled = !generatingSimilar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (generatingSimilar) "生成中..." else "📝 生成 3 道同类题")
            }
        }
    }
}

@Composable
private fun SimilarProblemCard(problem: com.mathcoach.app.domain.model.SimilarProblem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row {
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
