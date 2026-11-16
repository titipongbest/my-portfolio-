package com.example.model

import com.example.data.LiveQuote
import com.example.data.MarketPriceService
import com.example.data.MarketSnapshot
import com.example.data.QuoteSource

data class AnalyzedHolding(
    val holding: Holding,
    val quote: LiveQuote?,
    val effectiveCurrentPriceOriginal: Double,
    val effectiveCurrentPriceThb: Double,
    val costBasisThb: Double,
    val currentValueThb: Double,
    val profitLossThb: Double,
    val profitLossPercent: Double,
    val allocationPercentOfTotal: Double,
    val dailyChangePercent: Double?,
    val priceSource: QuoteSource,
    val isEstimatedUsingCost: Boolean
)

data class CategorySlice(
    val category: AssetCategory,
    val totalValueThb: Double,
    val totalCostThb: Double,
    val profitLossThb: Double,
    val profitLossPercent: Double,
    val percentOfPortfolio: Double,
    val holdingCount: Int
)

data class SectorSlice(
    val sectorId: String,
    val labelTh: String,
    val shortLabelTh: String,
    val sector: StockSector,
    val totalValueThb: Double,
    val totalCostThb: Double,
    val profitLossThb: Double,
    val profitLossPercent: Double,
    val percentOfTotalPortfolio: Double,
    val percentOfStockSleeve: Double,
    val holdings: List<AnalyzedHolding>
)

data class PortfolioSummary(
    val totalValueThb: Double,
    val totalCostThb: Double,
    val totalProfitLossThb: Double,
    val totalProfitLossPercent: Double,
    val totalStockValueThb: Double,
    val stockPercentOfPortfolio: Double,
    val analyzedHoldings: List<AnalyzedHolding>,
    val categorySlices: List<CategorySlice>,
    val stockSectorSlices: List<SectorSlice>,
    val usdToThbRate: Double,
    val isLiveFxRate: Boolean,
    val lastUpdatedMillis: Long?
)

object PortfolioAnalyzer {

    fun analyze(
        holdings: List<Holding>,
        snapshot: MarketSnapshot,
        managedSectors: List<ManagedSector> = StockSector.defaultManagedSectors()
    ): PortfolioSummary {
        val fx = snapshot.usdToThbRate
        if (holdings.isEmpty()) {
            return PortfolioSummary(
                totalValueThb = 0.0,
                totalCostThb = 0.0,
                totalProfitLossThb = 0.0,
                totalProfitLossPercent = 0.0,
                totalStockValueThb = 0.0,
                stockPercentOfPortfolio = 0.0,
                analyzedHoldings = emptyList(),
                categorySlices = emptyList(),
                stockSectorSlices = emptyList(),
                usdToThbRate = fx,
                isLiveFxRate = snapshot.isLiveFxRate,
                lastUpdatedMillis = snapshot.lastUpdatedMillis
            )
        }

        val preliminary = holdings.map { holding ->
            val key = MarketPriceService.quoteKey(holding)
            val quote = snapshot.quotes[key]
            val costUnitThb = if (holding.currency == "USD") holding.buyPricePerUnit * fx else holding.buyPricePerUnit
            val costBasisThb = holding.quantity * costUnitThb

            val manualPrice = holding.manualCurrentPrice?.takeIf { it > 0.0 }
            val manualPriceThb = manualPrice?.let {
                if (holding.currency == "USD") it * fx else it
            }

            val (priceOrig, priceThb, source, isFallbackCost) = when {
                quote != null && quote.currentPriceThb > 0.0 && quote.source != QuoteSource.UNAVAILABLE -> {
                    Quad(quote.currentPriceOriginal, quote.currentPriceThb, quote.source, false)
                }
                manualPrice != null && manualPriceThb != null -> {
                    Quad(manualPrice, manualPriceThb, QuoteSource.MANUAL, false)
                }
                else -> {
                    Quad(holding.buyPricePerUnit, costUnitThb, QuoteSource.UNAVAILABLE, true)
                }
            }

            val currentValueThb = holding.quantity * priceThb
            val pnlThb = currentValueThb - costBasisThb
            val pnlPct = if (costBasisThb > 0.0) (pnlThb / costBasisThb) * 100.0 else 0.0

            AnalyzedHolding(
                holding = holding,
                quote = quote,
                effectiveCurrentPriceOriginal = priceOrig,
                effectiveCurrentPriceThb = priceThb,
                costBasisThb = costBasisThb,
                currentValueThb = currentValueThb,
                profitLossThb = pnlThb,
                profitLossPercent = pnlPct,
                allocationPercentOfTotal = 0.0,
                dailyChangePercent = quote?.changePercent24h,
                priceSource = source,
                isEstimatedUsingCost = isFallbackCost
            )
        }

        val totalValue = preliminary.sumOf { it.currentValueThb }
        val totalCost = preliminary.sumOf { it.costBasisThb }
        val totalPnl = totalValue - totalCost
        val totalPnlPct = if (totalCost > 0.0) (totalPnl / totalCost) * 100.0 else 0.0

        val withAllocation = preliminary.map { item ->
            val pct = if (totalValue > 0.0) (item.currentValueThb / totalValue) * 100.0 else 0.0
            item.copy(allocationPercentOfTotal = pct)
        }.sortedByDescending { it.currentValueThb }

        val categorySlices = withAllocation
            .groupBy { it.holding.assetCategory }
            .map { (cat, items) ->
                val catVal = items.sumOf { it.currentValueThb }
                val catCost = items.sumOf { it.costBasisThb }
                val catPnl = catVal - catCost
                val catPnlPct = if (catCost > 0.0) (catPnl / catCost) * 100.0 else 0.0
                val catWeight = if (totalValue > 0.0) (catVal / totalValue) * 100.0 else 0.0
                CategorySlice(
                    category = cat,
                    totalValueThb = catVal,
                    totalCostThb = catCost,
                    profitLossThb = catPnl,
                    profitLossPercent = catPnlPct,
                    percentOfPortfolio = catWeight,
                    holdingCount = items.size
                )
            }
            .sortedByDescending { it.totalValueThb }

        val stockItems = withAllocation.filter { it.holding.assetCategory.isStock }
        val totalStockValue = stockItems.sumOf { it.currentValueThb }
        val stockPctOfPortfolio = if (totalValue > 0.0) (totalStockValue / totalValue) * 100.0 else 0.0

        val sectorMap = managedSectors.associateBy { it.id }

        val sectorSlices = stockItems
            .groupBy { it.holding.sector.ifBlank { StockSector.OTHER.name } }
            .map { (sectorKey, items) ->
                val secVal = items.sumOf { it.currentValueThb }
                val secCost = items.sumOf { it.costBasisThb }
                val secPnl = secVal - secCost
                val secPnlPct = if (secCost > 0.0) (secPnl / secCost) * 100.0 else 0.0
                val pctOfTotal = if (totalValue > 0.0) (secVal / totalValue) * 100.0 else 0.0
                val pctOfStocks = if (totalStockValue > 0.0) (secVal / totalStockValue) * 100.0 else 0.0

                val enumSector = StockSector.entries.find { it.name == sectorKey } ?: StockSector.OTHER
                val managed = sectorMap[sectorKey]
                val fullLabel = managed?.fullLabel
                    ?: StockSector.entries.find { it.name == sectorKey && it != StockSector.NONE }?.labelTh
                    ?: sectorKey
                val shortLabel = managed?.label
                    ?: StockSector.entries.find { it.name == sectorKey && it != StockSector.NONE }?.shortLabelTh
                    ?: sectorKey

                SectorSlice(
                    sectorId = sectorKey,
                    labelTh = fullLabel,
                    shortLabelTh = shortLabel,
                    sector = enumSector,
                    totalValueThb = secVal,
                    totalCostThb = secCost,
                    profitLossThb = secPnl,
                    profitLossPercent = secPnlPct,
                    percentOfTotalPortfolio = pctOfTotal,
                    percentOfStockSleeve = pctOfStocks,
                    holdings = items.sortedByDescending { it.currentValueThb }
                )
            }
            .sortedByDescending { it.totalValueThb }

        return PortfolioSummary(
            totalValueThb = totalValue,
            totalCostThb = totalCost,
            totalProfitLossThb = totalPnl,
            totalProfitLossPercent = totalPnlPct,
            totalStockValueThb = totalStockValue,
            stockPercentOfPortfolio = stockPctOfPortfolio,
            analyzedHoldings = withAllocation,
            categorySlices = categorySlices,
            stockSectorSlices = sectorSlices,
            usdToThbRate = fx,
            isLiveFxRate = snapshot.isLiveFxRate,
            lastUpdatedMillis = snapshot.lastUpdatedMillis
        )
    }

    private data class Quad(
        val orig: Double,
        val thb: Double,
        val source: QuoteSource,
        val isFallback: Boolean
    )
}
