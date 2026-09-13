package com.mathcoach.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestampMillis DESC")
    fun observeAllDesc(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestampMillis ASC")
    fun observeAllAsc(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestampMillis ASC")
    suspend fun listAllAsc(): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun byId(id: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Update
    suspend fun update(entity: HistoryEntity)

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache WHERE hash = :hash")
    suspend fun get(hash: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CacheEntity)

    @Query("SELECT COUNT(*) FROM cache")
    suspend fun count(): Int

    @Query("DELETE FROM cache WHERE hash IN (SELECT hash FROM cache ORDER BY timestampMillis ASC LIMIT :evictCount)")
    suspend fun evictOldest(evictCount: Int)

    @Query("DELETE FROM cache")
    suspend fun clear()
}
