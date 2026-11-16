package com.example.data

import com.example.data.local.HoldingDao
import com.example.model.Holding
import kotlinx.coroutines.flow.Flow

enum class ImportMode {
    MERGE,
    REPLACE
}

class PortfolioRepository(
    private val holdingDao: HoldingDao
) {
    fun observeHoldings(): Flow<List<Holding>> = holdingDao.observeAllHoldings()

    suspend fun getHoldingsOnce(): List<Holding> = holdingDao.getAllHoldingsOnce()

    suspend fun getHolding(id: Long): Holding? = holdingDao.getHoldingById(id)

    suspend fun addHolding(holding: Holding): Result<Long> {
        return try {
            val now = System.currentTimeMillis()
            val sanitized = holding.copy(
                id = 0L,
                symbol = holding.symbol.trim().uppercase().take(32),
                displayName = holding.displayName.trim().ifBlank { holding.symbol.trim().uppercase() }.take(80),
                createdAtMillis = now,
                updatedAtMillis = now
            )
            val newId = holdingDao.insertHolding(sanitized)
            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHolding(holding: Holding): Result<Unit> {
        if (holding.id <= 0L) {
            return Result.failure(IllegalArgumentException("ไม่พบรหัสรายการสำหรับแก้ไข"))
        }
        return try {
            val sanitized = holding.copy(
                symbol = holding.symbol.trim().uppercase().take(32),
                displayName = holding.displayName.trim().ifBlank { holding.symbol.trim().uppercase() }.take(80),
                updatedAtMillis = System.currentTimeMillis()
            )
            holdingDao.updateHolding(sanitized)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHolding(id: Long): Result<Unit> {
        if (id <= 0L) {
            return Result.failure(IllegalArgumentException("ไม่พบรหัสรายการสำหรับลบ"))
        }
        return try {
            holdingDao.deleteHoldingById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reassignSector(oldSectorId: String, newSectorId: String): Result<Unit> {
        return try {
            holdingDao.reassignSector(oldSectorId, newSectorId, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToJson(managedSectors: List<com.example.model.ManagedSector> = emptyList()): Result<String> {
        return try {
            val items = holdingDao.getAllHoldingsOnce()
            Result.success(PortfolioJsonCodec.encode(items, managedSectors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importHoldings(holdings: List<Holding>, mode: ImportMode): Result<Int> {
        return try {
            when (mode) {
                ImportMode.MERGE -> holdingDao.insertHoldings(holdings.map { it.copy(id = 0L) })
                ImportMode.REPLACE -> holdingDao.replaceAllHoldings(holdings.map { it.copy(id = 0L) })
            }
            Result.success(holdings.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
