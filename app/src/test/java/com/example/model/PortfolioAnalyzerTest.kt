package com.example.model

import com.example.data.DividendEvent
import com.example.data.LiveQuote
import com.example.data.MarketPriceService
import com.example.data.MarketSnapshot
import com.example.data.QuoteSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class PortfolioAnalyzerTest {

    @Test
    fun analyze_computesPnlAllocationAndStockSectorPercentages() {
        val ptt = Holding(
            id = 1L,
            symbol = "PTT",
            displayName = "ปตท.",
            category = AssetCategory.THAI_STOCK.name,
            sector = StockSector.ENERGY.name,
            quantity = 1000.0,
            buyPricePerUnit = 30.0,
            currency = "THB"
        )
        val lh = Holding(
            id = 2L,
            symbol = "LH",
            displayName = "แลนด์แอนด์เฮ้าส์",
            category = AssetCategory.THAI_STOCK.name,
            sector = StockSector.PROPERTY.name,
            quantity = 2000.0,
            buyPricePerUnit = 10.0,
            currency = "THB"
        )
        val aot = Holding(
            id = 3L,
            symbol = "AOT",
            displayName = "ท่าอากาศยานไทย",
            category = AssetCategory.THAI_STOCK.name,
            sector = StockSector.INFRASTRUCTURE.name,
            quantity = 500.0,
            buyPricePerUnit = 60.0,
            currency = "THB"
        )
        val btc = Holding(
            id = 4L,
            symbol = "BTC",
            displayName = "Bitcoin",
            category = AssetCategory.CRYPTO.name,
            sector = StockSector.NONE.name,
            quantity = 0.01,
            buyPricePerUnit = 50000.0, // USD
            currency = "USD"
        )

        val snapshot = MarketSnapshot(
            usdToThbRate = 35.0,
            isLiveFxRate = true,
            quotes = mapOf(
                "THAI_STOCK:PTT" to LiveQuote("THAI_STOCK:PTT", "PTT", 35.0, "THB", 35.0, 1.5, QuoteSource.LIVE_YAHOO),
                "THAI_STOCK:LH" to LiveQuote("THAI_STOCK:LH", "LH", 7.5, "THB", 7.5, -1.0, QuoteSource.LIVE_YAHOO),
                "THAI_STOCK:AOT" to LiveQuote("THAI_STOCK:AOT", "AOT", 60.0, "THB", 60.0, 0.0, QuoteSource.LIVE_YAHOO),
                "CRYPTO:BTC" to LiveQuote("CRYPTO:BTC", "BTC", 60000.0, "USD", 2100000.0, 2.5, QuoteSource.LIVE_COINGECKO)
            )
        )

        // Values in THB:
        // PTT: 1000 * 35 = 35,000 (cost 30,000, pnl +5,000)
        // LH: 2000 * 7.5 = 15,000 (cost 20,000, pnl -5,000)
        // AOT: 500 * 60 = 30,000 (cost 30,000, pnl 0)
        // BTC: 0.01 * 2,100,000 = 21,000 (cost 0.01 * 50,000 * 35 = 17,500, pnl +3,500)
        val summary = PortfolioAnalyzer.analyze(listOf(ptt, lh, aot, btc), snapshot)

        assertEquals(101_000.0, summary.totalValueThb, 0.01)
        assertEquals(97_500.0, summary.totalCostThb, 0.01)
        assertEquals(3_500.0, summary.totalProfitLossThb, 0.01)
        assertEquals(80_000.0, summary.totalStockValueThb, 0.01)

        val totalAllocationSum = summary.analyzedHoldings.sumOf { it.allocationPercentOfTotal }
        assertEquals(100.0, totalAllocationSum, 0.01)

        val energySector = summary.stockSectorSlices.first { it.sector == StockSector.ENERGY }
        assertEquals(35_000.0 / 101_000.0 * 100.0, energySector.percentOfTotalPortfolio, 0.01)
        assertEquals(35_000.0 / 80_000.0 * 100.0, energySector.percentOfStockSleeve, 0.01)

        val infraSector = summary.stockSectorSlices.first { it.sector == StockSector.INFRASTRUCTURE }
        assertEquals(30_000.0 / 101_000.0 * 100.0, infraSector.percentOfTotalPortfolio, 0.01)

        val propSector = summary.stockSectorSlices.first { it.sector == StockSector.PROPERTY }
        assertEquals(15_000.0 / 101_000.0 * 100.0, propSector.percentOfTotalPortfolio, 0.01)

        // Category-scoped analysis (strictly Thai stocks) sums sector percentages to 100% of that category
        val thaiStockOnlySummary = PortfolioAnalyzer.analyze(listOf(ptt, lh, aot), snapshot)
        assertEquals(80_000.0, thaiStockOnlySummary.totalValueThb, 0.01)
        assertEquals(
            100.0,
            thaiStockOnlySummary.stockSectorSlices.sumOf { it.percentOfTotalPortfolio },
            0.01
        )
    }

    @Test
    fun buildUpcomingDividendProjections_projectsSemiAnnualThaiAndQuarterlyUsDividends() {
        val nowCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.MAY, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // Semi-annual Thai stock payouts in the trailing 380 days (Sep 2025 & Mar 2026)
        val sep2025 = Calendar.getInstance(Locale.US).apply {
            set(2025, Calendar.SEPTEMBER, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val mar2026 = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.MARCH, 12, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val (thaiTrailing, thaiUpcoming) = MarketPriceService.buildUpcomingDividendProjections(
            historicalEvents = listOf(
                DividendEvent(exDateMillis = sep2025, amountPerShare = 0.80),
                DividendEvent(exDateMillis = mar2026, amountPerShare = 1.30)
            ),
            heldQuantity = 1000.0,
            currency = "THB",
            usdToThbRate = 35.0,
            nowMillis = nowMillis
        )

        assertEquals(2.10, thaiTrailing, 0.001)
        assertEquals(2, thaiUpcoming.size)
        assertTrue(thaiUpcoming.all { it.expectedExDateMillis > nowMillis })
        assertEquals(800.0, thaiUpcoming[0].estimatedPayoutThb, 0.01)
        assertEquals(1300.0, thaiUpcoming[1].estimatedPayoutThb, 0.01)

        // Quarterly US stock with a confirmed upcoming XD date close to the projected slot
        val q1 = Calendar.getInstance(Locale.US).apply {
            set(2025, Calendar.AUGUST, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val q2 = Calendar.getInstance(Locale.US).apply {
            set(2025, Calendar.NOVEMBER, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val q3 = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.FEBRUARY, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val q4 = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.MAY, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val confirmedAug = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.AUGUST, 12, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val (usTrailing, usUpcoming) = MarketPriceService.buildUpcomingDividendProjections(
            historicalEvents = listOf(
                DividendEvent(q1, 0.25),
                DividendEvent(q2, 0.25),
                DividendEvent(q3, 0.25),
                DividendEvent(q4, 0.26)
            ),
            heldQuantity = 10.0,
            currency = "USD",
            usdToThbRate = 35.0,
            confirmedExDateMillis = confirmedAug,
            confirmedDividendPerShare = 0.26,
            nowMillis = nowMillis
        )

        assertEquals(1.01, usTrailing, 0.001)
        // Confirmed Aug 2026 deduplicates the projected Aug 2026, leaving 4 quarterly payouts total
        assertEquals(4, usUpcoming.size)
        assertTrue(usUpcoming.first().isConfirmed)
        assertEquals(0.26 * 10.0 * 35.0, usUpcoming.first().estimatedPayoutThb, 0.01)
    }

    @Test
    fun knownCatalogSearch_filtersStrictlyByCategory_andStockColorsAreDistinct() {
        val thaiResults = com.example.data.KnownAssetsCatalog.search("กสิกร", AssetCategory.THAI_STOCK)
        assertTrue(thaiResults.isNotEmpty())
        assertTrue(thaiResults.all { it.category == AssetCategory.THAI_STOCK })
        assertTrue(thaiResults.any { it.symbol == "KBANK" })

        val fundResults = com.example.data.KnownAssetsCatalog.search("กสิกร", AssetCategory.MUTUAL_FUND)
        assertTrue(fundResults.isNotEmpty())
        assertTrue(fundResults.all { it.category == AssetCategory.MUTUAL_FUND })
        assertTrue(fundResults.any { it.symbol.startsWith("K-") })

        val thaiAccent = com.example.ui.theme.categoryAccentColor(AssetCategory.THAI_STOCK)
        val globalAccent = com.example.ui.theme.categoryAccentColor(AssetCategory.GLOBAL_STOCK)
        assertTrue(thaiAccent != globalAccent)
    }
}
