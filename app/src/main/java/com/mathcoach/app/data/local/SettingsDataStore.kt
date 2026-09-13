package com.mathcoach.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mathcoach.app.core.llm.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val provider: String = Provider.QWEN.id,
    val baseUrl: String = Provider.QWEN.defaultBaseUrl,
    val model: String = "",
    val visionModel: String = "qwen-vl-max",
    val reasoningModel: String = "qwen-vl-max",
    val useDualModel: Boolean = false,
    val cacheEnabled: Boolean = true,
    val customBaseUrl: String = ""
)

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val PROVIDER = stringPreferencesKey("provider")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val VISION_MODEL = stringPreferencesKey("vision_model")
        val REASONING_MODEL = stringPreferencesKey("reasoning_model")
        val USE_DUAL_MODEL = booleanPreferencesKey("use_dual_model")
        val CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
        val CUSTOM_BASE_URL = stringPreferencesKey("custom_base_url")
        fun apiKey(providerId: String) = stringPreferencesKey("api_key_$providerId")
        fun providerModel(providerId: String) = stringPreferencesKey("model_$providerId")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val providerId = prefs[Keys.PROVIDER] ?: Provider.QWEN.id
        val provider = Provider.fromId(providerId)
        UserSettings(
            provider = providerId,
            baseUrl = if (provider == Provider.CUSTOM)
                prefs[Keys.CUSTOM_BASE_URL].orEmpty()
            else
                provider.defaultBaseUrl,
            model = prefs[Keys.MODEL] ?: "",
            visionModel = prefs[Keys.VISION_MODEL] ?: provider.defaultVisionModel,
            reasoningModel = prefs[Keys.REASONING_MODEL] ?: provider.defaultReasoningModel,
            useDualModel = prefs[Keys.USE_DUAL_MODEL] ?: false,
            cacheEnabled = prefs[Keys.CACHE_ENABLED] ?: true,
            customBaseUrl = prefs[Keys.CUSTOM_BASE_URL] ?: ""
        )
    }

    suspend fun current(): UserSettings = settingsFlow.first()

    suspend fun setProvider(providerId: String) {
        context.dataStore.edit { it[Keys.PROVIDER] = providerId }
    }

    suspend fun setModel(model: String) {
        context.dataStore.edit { it[Keys.MODEL] = model }
    }

    suspend fun setVisionModel(model: String) {
        context.dataStore.edit { it[Keys.VISION_MODEL] = model }
    }

    suspend fun setReasoningModel(model: String) {
        context.dataStore.edit { it[Keys.REASONING_MODEL] = model }
    }

    suspend fun setUseDualModel(use: Boolean) {
        context.dataStore.edit { it[Keys.USE_DUAL_MODEL] = use }
    }

    suspend fun setCacheEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CACHE_ENABLED] = enabled }
    }

    suspend fun setCustomBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.CUSTOM_BASE_URL] = url }
    }

    suspend fun getApiKey(providerId: String): String =
        context.dataStore.data.first()[Keys.apiKey(providerId)] ?: ""

    suspend fun setApiKey(providerId: String, key: String) {
        context.dataStore.edit { it[Keys.apiKey(providerId)] = key }
    }

    val apiKeyFlow: Flow<Pair<String, String>> = context.dataStore.data.map { prefs ->
        val providerId = prefs[Keys.PROVIDER] ?: Provider.QWEN.id
        providerId to (prefs[Keys.apiKey(providerId)] ?: "")
    }
}
