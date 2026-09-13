package com.mathcoach.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntity::class, CacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun cacheDao(): CacheDao

    companion object {
        const val NAME = "mathcoach.db"
    }
}
