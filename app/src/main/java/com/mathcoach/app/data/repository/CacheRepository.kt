package com.mathcoach.app.data.repository

import com.mathcoach.app.data.local.CacheDao
import com.mathcoach.app.data.local.CacheEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheRepository @Inject constructor(
    private val dao: CacheDao
) {
    companion object {
        const val MAX_ENTRIES = 100
    }

    suspend fun get(hash: String): String? = dao.get(hash)?.analysisJson

    suspend fun put(hash: String, analysisJson: String) {
        dao.put(CacheEntity(hash, analysisJson, System.currentTimeMillis()))
        val count = dao.count()
        if (count > MAX_ENTRIES) {
            dao.evictOldest(count - MAX_ENTRIES)
        }
    }

    suspend fun clear() = dao.clear()
}
