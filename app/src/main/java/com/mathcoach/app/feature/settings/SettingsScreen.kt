package com.mathcoach.app.feature.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mathcoach.app.core.llm.Provider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val provider = Provider.fromId(ui.settings.provider)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("服务商与模型", style = MaterialTheme.typography.titleLarge)

        // 服务商选择
        var providerExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = !providerExpanded }
        ) {
            OutlinedTextField(
                value = provider.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("服务商") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = providerExpanded,
                onDismissRequest = { providerExpanded = false }
            ) {
                viewModel.providers().forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.label) },
                        onClick = {
                            viewModel.onProviderChange(p.id)
                            providerExpanded = false
                        }
                    )
                }
            }
        }

        // Base URL 显示
        if (provider == Provider.CUSTOM) {
            OutlinedTextField(
                value = ui.settings.customBaseUrl,
                onValueChange = viewModel::onCustomBaseUrlChange,
                label = { Text("Base URL") },
                placeholder = { Text("https://api.example.com/v1") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Base URL", style = MaterialTheme.typography.labelSmall)
                    Text(provider.defaultBaseUrl, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // API Key
        OutlinedTextField(
            value = ui.apiKey,
            onValueChange = viewModel::onApiKeyChange,
            label = { Text("${provider.label} API Key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = {
                Text("⚠️ 密钥明文保存在本地，仅在手机丢失或被 Root 后有泄露风险")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = viewModel::saveApiKey) { Text("保存 Key") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !ui.testing
            ) {
                Text(if (ui.testing) "测试中..." else "测试连接")
            }
        }
        if (ui.saved) {
            Text("✓ 已保存", color = MaterialTheme.colorScheme.primary)
        }
        ui.testResult?.let { Text(it) }

        Spacer(Modifier.height(8.dp))

        // 主模型
        OutlinedTextField(
            value = ui.settings.model,
            onValueChange = viewModel::onModelChange,
            label = { Text("主模型名称") },
            placeholder = { Text(provider.defaultReasoningModel) },
            supportingText = { Text(provider.modelHint) },
            modifier = Modifier.fillMaxWidth()
        )

        // 双模型开关
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("启用双模型模式", modifier = Modifier.weight(1f))
                    Switch(
                        checked = ui.settings.useDualModel,
                        onCheckedChange = viewModel::onUseDualModelChange
                    )
                }
                if (ui.settings.useDualModel) {
                    OutlinedTextField(
                        value = ui.settings.visionModel,
                        onValueChange = viewModel::onVisionModelChange,
                        label = { Text("👁️ 视觉识别模型") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ui.settings.reasoningModel,
                        onValueChange = viewModel::onReasoningModelChange,
                        label = { Text("🧠 主力推理模型") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 缓存
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用缓存")
                    Text(
                        "相同题目直接返回上次结果",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = ui.settings.cacheEnabled,
                    onCheckedChange = viewModel::onCacheEnabledChange
                )
            }
        }
    }
}
