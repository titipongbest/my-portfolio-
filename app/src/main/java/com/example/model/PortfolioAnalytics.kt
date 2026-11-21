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
                quote != null &&
                    quote.currentPriceThb > 0.0 &&
                    quote.source != QuoteSource.UNAVAILABLE &&
                    quote.source != QuoteSource.MANUAL -> {
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
                    ?: if (sectorKey.startsWith("CUSTOM_")) StockSector.OTHER.labelTh else sectorKey
                val shortLabel = managed?.label
                    ?: StockSector.entries.find { it.name == sectorKey && it != StockSector.NONE }?.shortLabelTh
                    ?: if (sectorKey.startsWith("CUSTOM_")) StockSector.OTHER.shortLabelTh else sectorKey

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

    /**
     * Calculates exact rebalance amounts for each [AssetCategory].
     *
     * For fresh-cash (buy-only) rebalancing without selling existing holdings:
     * Adding money grows total portfolio value from V to V_target, so each targeted category k
     * with target weight p_k must satisfy (A_k + x_k) / V_target = p_k with x_k >= 0.
     * The minimum target total portfolio value is:
     *   V_target = max(V, max_{p_k > 0}(A_k / p_k), untargetedValue / (1 - sum(p_k)) [if sum(p_k) < 1])
     * and each category's required fresh-cash addition is:
     *   x_k = max(0, V_target * p_k - A_k).
     *
     * Example: Portfolio V = 100, Thai Stock = 100 (target 92%), Gold = 0 (target 8%):
     *   V_target = 100 / 0.92 = 108.6957
     *   Gold top-up = 108.6957 * 0.08 = 8.6957 -> 8.6957 / 108.6957 = 8.00% exactly.
     */
    fun calculatePlanRebalance(
        summary: PortfolioSummary,
        targetPercents: Map<AssetCategory, Double>
    ): PlanRebalanceSummary {
        val currentTotal = summary.totalValueThb.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val cleanTargets = AssetCategory.entries.associateWith { cat ->
            targetPercents[cat]?.takeIf { it.isFinite() && it > 0.0 }?.coerceIn(0.0, 100.0) ?: 0.0
        }
        val totalTargetPct = cleanTargets.values.sum()
        val actualByCat = summary.categorySlices.associateBy { it.category }

        if (currentTotal <= 0.0 || totalTargetPct <= 0.0 || totalTargetPct > 100.05) {
            return PlanRebalanceSummary(
                currentTotalThb = currentTotal,
                targetTotalThb = currentTotal,
                totalFreshCashNeededThb = 0.0,
                totalTargetPercent = totalTargetPct,
                isValidForCalculation = false,
                hasZeroTargetHoldings = false,
                byCategory = AssetCategory.entries.associateWith { cat ->
                    val actualVal = actualByCat[cat]?.totalValueThb ?: 0.0
                    val actualPct = actualByCat[cat]?.percentOfPortfolio ?: 0.0
                    val targetPct = cleanTargets[cat] ?: 0.0
                    CategoryRebalancePlan(
                        category = cat,
                        actualValueThb = actualVal,
                        actualPercent = actualPct,
                        targetPercent = targetPct,
                        diffPercent = targetPct - actualPct,
                        freshCashAddThb = 0.0,
                        swapDeltaThb = 0.0,
                        requiresSellForZeroTarget = false
                    )
                }
            )
        }

        val totalWeight = (totalTargetPct / 100.0).coerceAtMost(1.0)
        var targetTotal = currentTotal
        var untargetedValue = 0.0

        for (cat in AssetCategory.entries) {
            val actualVal = actualByCat[cat]?.totalValueThb ?: 0.0
            val weight = (cleanTargets[cat] ?: 0.0) / 100.0
            if (weight > 0.0) {
                targetTotal = maxOf(targetTotal, actualVal / weight)
            } else {
                untargetedValue += actualVal
            }
        }

        val remainingWeight = (1.0 - totalWeight).coerceAtLeast(0.0)
        val hasZeroTargetHoldings = untargetedValue > 0.01 && remainingWeight <= 1e-4
        if (untargetedValue > 0.01 && remainingWeight > 1e-4) {
            targetTotal = maxOf(targetTotal, untargetedValue / remainingWeight)
        }

        val byCategory = AssetCategory.entries.associateWith { cat ->
            val actualVal = actualByCat[cat]?.totalValueThb ?: 0.0
            val actualPct = actualByCat[cat]?.percentOfPortfolio ?: 0.0
            val targetPct = cleanTargets[cat] ?: 0.0
            val weight = targetPct / 100.0
            val swapDelta = currentTotal * weight - actualVal
            val requiresSellZero = targetPct <= 0.0 && actualVal > 0.01 && remainingWeight <= 1e-4
            val freshCashAdd = if (weight > 0.0) {
                (targetTotal * weight - actualVal).coerceAtLeast(0.0)
            } else {
                0.0
            }
            CategoryRebalancePlan(
                category = cat,
                actualValueThb = actualVal,
                actualPercent = actualPct,
                targetPercent = targetPct,
                diffPercent = targetPct - actualPct,
                freshCashAddThb = freshCashAdd,
                swapDeltaThb = swapDelta,
                requiresSellForZeroTarget = requiresSellZero
            )
        }

        val totalFreshCash = (targetTotal - currentTotal).coerceAtLeast(0.0)
        return PlanRebalanceSummary(
            currentTotalThb = currentTotal,
            targetTotalThb = targetTotal,
            totalFreshCashNeededThb = totalFreshCash,
            totalTargetPercent = totalTargetPct,
            isValidForCalculation = true,
            hasZeroTargetHoldings = hasZeroTargetHoldings,
            byCategory = byCategory
        )
    }

    private data class Quad(
        val orig: Double,
        val thb: Double,
        val source: QuoteSource,
        val isFallback: Boolean
    )
}

data class CategoryRebalancePlan(
    val category: AssetCategory,
    val actualValueThb: Double,
    val actualPercent: Double,
    val targetPercent: Double,
    val diffPercent: Double,
    val freshCashAddThb: Double,
    val swapDeltaThb: Double,
    val requiresSellForZeroTarget: Boolean
)

data class PlanRebalanceSummary(
    val currentTotalThb: Double,
    val targetTotalThb: Double,
    val totalFreshCashNeededThb: Double,
    val totalTargetPercent: Double,
    val isValidForCalculation: Boolean,
    val hasZeroTargetHoldings: Boolean,
    val byCategory: Map<AssetCategory, CategoryRebalancePlan>
)
