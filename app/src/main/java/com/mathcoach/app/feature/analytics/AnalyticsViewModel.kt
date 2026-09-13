package com.mathcoach.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathcoach.app.data.repository.HistoryRepository
import com.mathcoach.app.domain.analyzer.KnowledgeAnalyzer
import com.mathcoach.app.domain.analyzer.WeakSpotAnalyzer
import com.mathcoach.app.domain.model.KnowledgeMastery
import com.mathcoach.app.domain.model.LearningProfile
import com.mathcoach.app.domain.model.PracticeRecommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
    private val knowledgeAnalyzer: KnowledgeAnalyzer,
    private val weakSpotAnalyzer: WeakSpotAnalyzer
) : ViewModel() {

    val profile: StateFlow<LearningProfile> = historyRepo.observeAllAsc()
        .map { knowledgeAnalyzer.analyze(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LearningProfile())

    val weakSpots: StateFlow<List<KnowledgeMastery>> = historyRepo.observeAllAsc()
        .map { weakSpotAnalyzer.identifyWeakSpots(knowledgeAnalyzer.analyze(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendations: StateFlow<List<PracticeRecommendation>> = historyRepo.observeAllAsc()
        .map {
            val p = knowledgeAnalyzer.analyze(it)
            weakSpotAnalyzer.generateRecommendations(p, max = 5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun trendFor(kp: String, history: List<com.mathcoach.app.domain.model.HistoryEntry>): List<Float> =
        knowledgeAnalyzer.masteryTrend(history, kp)
}
