package com.mathcoach.app.feature.solve

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathcoach.app.core.util.ImageCodec
import com.mathcoach.app.domain.model.ProblemAnalysis
import com.mathcoach.app.domain.usecase.AnalysisState
import com.mathcoach.app.domain.usecase.AnalysisUseCase
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class SolveUiState(
    val imageUri: Uri? = null,
    val imageBytes: ByteArray? = null,
    val imagePath: String? = null,
    val userInput: String = "",
    val analyzing: Boolean = false,
    val partialText: String = "",
    val parsedAnalyses: List<ProblemAnalysis> = emptyList(),
    val fromCache: Boolean = false,
    val savedEntryIds: List<String> = emptyList(),
    val error: String? = null,
    // 同类题生成
    val generatingSimilar: Boolean = false,
    val similarProblems: List<com.mathcoach.app.domain.model.SimilarProblem> = emptyList(),
    val similarError: String? = null
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

@HiltViewModel
class SolveViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analysisUseCase: AnalysisUseCase,
    private val generateSimilarUseCase: com.mathcoach.app.domain.usecase.GenerateSimilarUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(SolveUiState())
    val ui: StateFlow<SolveUiState> = _ui.asStateFlow()

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val bytes = ImageCodec.uriToJpegBytes(context, uri) ?: run {
                _ui.update { it.copy(error = "无法读取图片") }
                return@launch
            }
            val path = saveImageToDisk(bytes)
            _ui.update {
                it.copy(
                    imageUri = uri,
                    imageBytes = bytes,
                    imagePath = path,
                    error = null,
                    parsedAnalyses = emptyList(),
                    partialText = ""
                )
            }
        }
    }

    fun onCameraCapture(bytes: ByteArray) {
        viewModelScope.launch {
            val path = saveImageToDisk(bytes)
            _ui.update {
                it.copy(
                    imageUri = null,
                    imageBytes = bytes,
                    imagePath = path,
                    error = null,
                    parsedAnalyses = emptyList(),
                    partialText = ""
                )
            }
        }
    }

    fun onUserInputChange(text: String) {
        _ui.update { it.copy(userInput = text) }
    }

    fun clearImage() {
        _ui.update {
            it.copy(
                imageUri = null,
                imageBytes = null,
                imagePath = null,
                parsedAnalyses = emptyList(),
                partialText = ""
            )
        }
    }

    fun analyze() {
        val bytes = _ui.value.imageBytes ?: run {
            _ui.update { it.copy(error = "请先选择或拍摄图片") }
            return
        }
        val userText = _ui.value.userInput.ifBlank { "请分析这道数学题。" }
        val path = _ui.value.imagePath

        viewModelScope.launch {
            _ui.update { it.copy(analyzing = true, error = null, partialText = "", parsedAnalyses = emptyList()) }
            analysisUseCase(bytes, userText, path).collect { state ->
                when (state) {
                    is AnalysisState.CheckingCache -> Unit
                    is AnalysisState.Streaming -> _ui.update { it.copy(partialText = state.partial) }
                    is AnalysisState.Parsed -> _ui.update {
                        it.copy(
                            parsedAnalyses = state.analyses,
                            partialText = state.rawResponse,
                            fromCache = state.fromCache
                        )
                    }
                    is AnalysisState.Saved -> _ui.update { it.copy(savedEntryIds = state.entryIds) }
                    is AnalysisState.Error -> _ui.update { it.copy(error = state.message) }
                    AnalysisState.Idle -> Unit
                }
            }
            _ui.update { it.copy(analyzing = false) }
        }
    }

    private fun saveImageToDisk(bytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "images")
            dir.mkdirs()
            val file = File(dir, "${ImageCodec.md5(bytes)}.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun generateSimilar(source: ProblemAnalysis, count: Int = 3) {
        viewModelScope.launch {
            _ui.update { it.copy(generatingSimilar = true, similarError = null, similarProblems = emptyList()) }
            generateSimilarUseCase(source, count).collect { state ->
                when (state) {
                    is com.mathcoach.app.domain.usecase.GenerateState.Streaming -> Unit
                    is com.mathcoach.app.domain.usecase.GenerateState.Done ->
                        _ui.update { it.copy(similarProblems = state.problems) }
                    is com.mathcoach.app.domain.usecase.GenerateState.Error ->
                        _ui.update { it.copy(similarError = state.message) }
                    com.mathcoach.app.domain.usecase.GenerateState.Idle -> Unit
                }
            }
            _ui.update { it.copy(generatingSimilar = false) }
        }
    }
}
