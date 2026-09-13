package com.mathcoach.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathcoach.app.core.llm.LlmClient
import com.mathcoach.app.core.llm.Provider
import com.mathcoach.app.core.llm.SseEvent
import com.mathcoach.app.data.local.SettingsDataStore
import com.mathcoach.app.data.local.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val apiKey: String = "",
    val testing: Boolean = false,
    val testResult: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsDataStore,
    private val llm: LlmClient
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            store.settingsFlow.collect { s ->
                _ui.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            val initialProvider = store.settingsFlow.first().provider
            _ui.update { it.copy(apiKey = store.getApiKey(initialProvider)) }
        }
    }

    fun onProviderChange(providerId: String) {
        viewModelScope.launch {
            store.setProvider(providerId)
            val key = store.getApiKey(providerId)
            _ui.update { it.copy(apiKey = key, testResult = null) }
        }
    }

    fun onApiKeyChange(key: String) {
        _ui.update { it.copy(apiKey = key, saved = false) }
    }

    fun saveApiKey() {
        viewModelScope.launch {
            store.setApiKey(_ui.value.settings.provider, _ui.value.apiKey)
            _ui.update { it.copy(saved = true) }
        }
    }

    fun onModelChange(model: String) {
        viewModelScope.launch { store.setModel(model) }
    }

    fun onVisionModelChange(model: String) {
        viewModelScope.launch { store.setVisionModel(model) }
    }

    fun onReasoningModelChange(model: String) {
        viewModelScope.launch { store.setReasoningModel(model) }
    }

    fun onUseDualModelChange(use: Boolean) {
        viewModelScope.launch { store.setUseDualModel(use) }
    }

    fun onCacheEnabledChange(enabled: Boolean) {
        viewModelScope.launch { store.setCacheEnabled(enabled) }
    }

    fun onCustomBaseUrlChange(url: String) {
        viewModelScope.launch { store.setCustomBaseUrl(url) }
    }

    fun testConnection() {
        viewModelScope.launch {
            saveApiKey()
            _ui.update { it.copy(testing = true, testResult = null) }
            val messages = listOf(
                com.mathcoach.app.core.llm.ChatMessage("system", "You are a helpful assistant."),
                com.mathcoach.app.core.llm.ChatMessage("user", "用 1 句话回答：1+1=?")
            )
            val sb = StringBuilder()
            var error: Throwable? = null
            llm.streamChat(messages).collect { ev ->
                when (ev) {
                    is SseEvent.Delta -> sb.append(ev.text)
                    is SseEvent.Error -> error = ev.cause
                    SseEvent.Done -> Unit
                }
            }
            _ui.update {
                it.copy(
                    testing = false,
                    testResult = error?.let { e -> "❌ ${e.message}" }
                        ?: "✅ 连接成功，回复：${sb.toString().take(60)}"
                )
            }
        }
    }

    fun providers(): List<Provider> = Provider.entries
}
