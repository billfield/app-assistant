package com.mathcoach.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val imageHash: String?,
    val imagePath: String?,
    val title: String,
    val questionType: String,
    val difficulty: String,
    val knowledgePointsJson: String,
    val answerStatus: String,
    val correctnessJudgment: String,
    val detailedAnalysis: String,
    val solution: String,
    val geometryJson: String?,
    val similarGenerated: Int,
    val tagsJson: String,
    val notes: String
)

@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val hash: String,
    val analysisJson: String,
    val timestampMillis: Long
)
