package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.Holding
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holdings ORDER BY createdAtMillis DESC, id DESC")
    fun observeAllHoldings(): Flow<List<Holding>>

    @Query("SELECT * FROM holdings ORDER BY createdAtMillis DESC, id DESC")
    suspend fun getAllHoldingsOnce(): List<Holding>

    @Query("SELECT * FROM holdings WHERE id = :id LIMIT 1")
    suspend fun getHoldingById(id: Long): Holding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: Holding): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(holdings: List<Holding>)

    @Update
    suspend fun updateHolding(holding: Holding)

    @Query("DELETE FROM holdings WHERE id = :id")
    suspend fun deleteHoldingById(id: Long)

    @Query("DELETE FROM holdings")
    suspend fun deleteAllHoldings()

    @Query("UPDATE holdings SET sector = :newSector, updatedAtMillis = :updatedAt WHERE sector = :oldSector")
    suspend fun reassignSector(oldSector: String, newSector: String, updatedAt: Long)

    @Transaction
    suspend fun replaceAllHoldings(holdings: List<Holding>) {
        deleteAllHoldings()
        insertHoldings(holdings)
    }
}
