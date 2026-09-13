package com.mathcoach.app.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathcoach.app.domain.model.SimilarProblem
import com.mathcoach.app.domain.usecase.GenerateState
import com.mathcoach.app.domain.usecase.TargetedPracticeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PracticeUiState(
    val generating: Boolean = false,
    val partialText: String = "",
    val problems: List<SimilarProblem> = emptyList(),
    val error: String? = null,
    val targetCount: Int = 5
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val targetedPracticeUseCase: TargetedPracticeUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(PracticeUiState())
    val ui: StateFlow<PracticeUiState> = _ui.asStateFlow()

    fun setTargetCount(count: Int) {
        _ui.update { it.copy(targetCount = count) }
    }

    fun generate() {
        viewModelScope.launch {
            _ui.update { it.copy(generating = true, error = null, partialText = "", problems = emptyList()) }
            targetedPracticeUseCase(_ui.value.targetCount).collect { state ->
                when (state) {
                    is GenerateState.Streaming -> _ui.update { it.copy(partialText = state.partial) }
                    is GenerateState.Done -> _ui.update { it.copy(problems = state.problems, partialText = state.rawResponse) }
                    is GenerateState.Error -> _ui.update { it.copy(error = state.message) }
                    GenerateState.Idle -> Unit
                }
            }
            _ui.update { it.copy(generating = false) }
        }
    }

    fun clear() {
        _ui.update { it.copy(problems = emptyList(), partialText = "", error = null) }
    }
}
