package com.example.data

import android.util.Log
import com.example.model.AssetCategory
import com.example.model.Holding
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

enum class QuoteSource(val labelTh: String) {
    LIVE_YAHOO("ราคาตลาดจริง"),
    LIVE_COINGECKO("ราคา CoinGecko"),
    LIVE_FUND("ราคา NAV กองทุน"),
    MANUAL("ราคาที่กรอกเอง"),
    UNAVAILABLE("ไม่พบราคาอัตโนมัติ")
}

data class LiveQuote(
    val key: String,
    val symbol: String,
    val currentPriceOriginal: Double,
    val originalCurrency: String,
    val currentPriceThb: Double,
    val changePercent24h: Double? = null,
    val source: QuoteSource,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

data class MarketSnapshot(
    val quotes: Map<String, LiveQuote> = emptyMap(),
    val usdToThbRate: Double = DEFAULT_USD_THB_RATE,
    val isLiveFxRate: Boolean = false,
    val lastUpdatedMillis: Long? = null
) {
    companion object {
        const val DEFAULT_USD_THB_RATE = 34.50
    }
}

data class DividendEvent(
    val exDateMillis: Long,
    val amountPerShare: Double
)

data class UpcomingDividendProjection(
    val expectedExDateMillis: Long,
    val estimatedAmountPerShare: Double,
    val estimatedPayoutThb: Double,
    val isConfirmed: Boolean = false,
    val note: String = ""
)

data class StockDividendInfo(
    val symbol: String,
    val displayName: String,
    val currency: String,
    val currentPrice: Double,
    val usdToThbRate: Double,
    val heldQuantity: Double,
    val trailingAnnualDividendPerShare: Double,
    val trailingYieldPercent: Double?,
    val estimatedAnnualIncomeThb: Double,
    val upcomingDividends: List<UpcomingDividendProjection>,
    val historicalDividends: List<DividendEvent>,
    val sourceNote: String,
    val errorMessage: String? = null
)

data class FinnomenaFundEntry(
    val fundId: String,
    val shortCode: String,
    val nameTh: String
)

class MarketPriceService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val directoryLock = Any()

    @Volatile
    private var cachedFundDirectory: List<FinnomenaFundEntry> = emptyList()

    @Volatile
    private var cachedFundDirectoryAtMillis: Long = 0L
    suspend fun fetchMarketSnapshot(holdings: List<Holding>): MarketSnapshot = withContext(Dispatchers.IO) {
        val fxPair = fetchUsdThbRate()
        val usdToThb = fxPair.first
        val isLiveFx = fxPair.second

        if (holdings.isEmpty()) {
            return@withContext MarketSnapshot(
                quotes = emptyMap(),
                usdToThbRate = usdToThb,
                isLiveFxRate = isLiveFx,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        }

        val coinGeckoTargets = holdings.filter {
            it.assetCategory == AssetCategory.CRYPTO || it.assetCategory == AssetCategory.GOLD
        }
        val coinGeckoQuotes = if (coinGeckoTargets.isNotEmpty()) {
            fetchCoinGeckoQuotes(coinGeckoTargets, usdToThb)
        } else {
            emptyMap()
        }

        val otherHoldings = holdings.filter { holding ->
            val key = quoteKey(holding)
            !coinGeckoQuotes.containsKey(key)
        }

        val fetchedOthers = coroutineScope {
            otherHoldings
                .distinctBy { quoteKey(it) }
                .map { holding ->
                    async {
                        quoteKey(holding) to fetchSingleNonCoinGeckoQuote(holding, usdToThb)
                    }
                }
                .awaitAll()
                .toMap()
        }

        val combined = coinGeckoQuotes + fetchedOthers
        MarketSnapshot(
            quotes = combined,
            usdToThbRate = usdToThb,
            isLiveFxRate = isLiveFx,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    suspend fun fetchPreviewQuote(
        symbol: String,
        category: AssetCategory,
        manualFallback: Double? = null,
        currency: String = "THB"
    ): LiveQuote? = withContext(Dispatchers.IO) {
        val clean = symbol.trim().uppercase()
        if (clean.isBlank()) return@withContext null
        val fx = fetchUsdThbRate().first
        val tempHolding = Holding(
            symbol = clean,
            displayName = clean,
            category = category.name,
            currency = currency,
            manualCurrentPrice = manualFallback
        )
        if (category == AssetCategory.CRYPTO || category == AssetCategory.GOLD) {
            val cg = fetchCoinGeckoQuotes(listOf(tempHolding), fx)
            cg[quoteKey(tempHolding)] ?: fetchSingleNonCoinGeckoQuote(tempHolding, fx)
        } else {
            fetchSingleNonCoinGeckoQuote(tempHolding, fx)
        }
    }

    suspend fun searchAssets(
        query: String,
        category: AssetCategory
    ): List<KnownAsset> = withContext(Dispatchers.IO) {
        val localMatches = KnownAssetsCatalog.search(query, category, limit = 12)
        val q = query.trim()
        if (q.length < 2) return@withContext localMatches

        val remoteMatches = when (category) {
            AssetCategory.THAI_STOCK -> searchYahooFinance(q, AssetCategory.THAI_STOCK)
            AssetCategory.GLOBAL_STOCK -> searchYahooFinance(q, AssetCategory.GLOBAL_STOCK)
            AssetCategory.MUTUAL_FUND -> {
                val funds = searchFinnomenaFunds(q)
                val etfs = searchYahooFinance(q, AssetCategory.MUTUAL_FUND)
                funds + etfs
            }
            AssetCategory.GOLD, AssetCategory.CRYPTO -> emptyList()
        }

        val combined = LinkedHashMap<String, KnownAsset>()
        localMatches.forEach { combined[it.symbol.uppercase()] = it }
        remoteMatches.forEach { item ->
            val key = item.symbol.uppercase()
            if (!combined.containsKey(key)) {
                combined[key] = item
            }
        }
        combined.values.take(15)
    }

    private fun getOrFetchFinnomenaDirectory(): List<FinnomenaFundEntry> {
        val now = System.currentTimeMillis()
        val current = cachedFundDirectory
        if (current.isNotEmpty() && now - cachedFundDirectoryAtMillis < DIRECTORY_CACHE_TTL_MILLIS) {
            return current
        }
        synchronized(directoryLock) {
            val fresh = cachedFundDirectory
            val freshNow = System.currentTimeMillis()
            if (fresh.isNotEmpty() && freshNow - cachedFundDirectoryAtMillis < DIRECTORY_CACHE_TTL_MILLIS) {
                return fresh
            }
            return try {
                val req = Request.Builder()
                    .url("https://www.finnomena.com/fn3/api/fund/public/list")
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return fresh
                    val body = resp.body?.string() ?: return fresh
                    val parsed = parseFinnomenaFundListJson(body)
                    if (parsed.isNotEmpty()) {
                        cachedFundDirectory = parsed
                        cachedFundDirectoryAtMillis = System.currentTimeMillis()
                        parsed
                    } else {
                        fresh
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Finnomena directory fetch failed: ${e.message}")
                fresh
            }
        }
    }

    private fun searchFinnomenaFunds(query: String): List<KnownAsset> {
        val directory = getOrFetchFinnomenaDirectory()
        if (directory.isNotEmpty()) {
            val hits = filterFinnomenaDirectory(directory, query, limit = 15)
            if (hits.isNotEmpty()) return hits
        }
        // Fallback: direct single-fund lookup by exact short code via v2 endpoint
        val cleanUpper = query.trim().uppercase()
        if (cleanUpper.length < 3) return emptyList()
        val direct = fetchFinnomenaFundDetailByCode(cleanUpper) ?: return emptyList()
        return listOf(
            KnownAsset(
                symbol = direct.shortCode,
                displayName = direct.nameTh.ifBlank { direct.shortCode },
                category = AssetCategory.MUTUAL_FUND,
                defaultCurrency = "THB"
            )
        )
    }

    private fun fetchFinnomenaFundDetailByCode(shortCode: String): FinnomenaFundEntry? {
        return try {
            val encoded = URLEncoder.encode(shortCode.trim().uppercase(), "UTF-8")
            val url = "https://www.finnomena.com/fn3/api/fund/v2/public/funds/$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                if (!root.optBoolean("status", false)) return null
                val data = root.optJSONObject("data") ?: return null
                val fundId = data.optString("fund_id", "").trim()
                val sc = data.optString("short_code", "").trim().uppercase()
                val nameTh = data.optString("name_th", "").trim()
                    .ifBlank { data.optString("name_en", "").trim() }
                if (fundId.isBlank() || sc.isBlank()) null
                else FinnomenaFundEntry(fundId = fundId, shortCode = sc, nameTh = nameTh.ifBlank { sc })
            }
        } catch (e: Exception) {
            Log.d(TAG, "Finnomena v2 detail failed for $shortCode: ${e.message}")
            null
        }
    }

    private fun searchYahooFinance(query: String, category: AssetCategory): List<KnownAsset> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v1/finance/search?q=$encoded&quotesCount=12&newsCount=0"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val root = JSONObject(body)
                val quotes = root.optJSONArray("quotes") ?: return emptyList()
                val out = mutableListOf<KnownAsset>()
                for (i in 0 until quotes.length()) {
                    val qObj = quotes.optJSONObject(i) ?: continue
                    val rawSym = qObj.optString("symbol", "").trim().uppercase()
                    val quoteType = qObj.optString("quoteType", "").uppercase()
                    val exchange = qObj.optString("exchange", "").uppercase()
                    if (rawSym.isBlank()) continue

                    val isThaiTicker = rawSym.endsWith(".BK") || exchange == "SET"
                    val accepted = when (category) {
                        AssetCategory.THAI_STOCK -> isThaiTicker && (quoteType == "EQUITY" || quoteType == "ETF")
                        AssetCategory.GLOBAL_STOCK -> !isThaiTicker && quoteType == "EQUITY"
                        AssetCategory.MUTUAL_FUND -> quoteType == "ETF" || quoteType == "MUTUALFUND"
                        else -> false
                    }
                    if (!accepted) continue

                    val displaySym = if (category == AssetCategory.THAI_STOCK) rawSym.removeSuffix(".BK") else rawSym
                    val name = qObj.optString("shortname", "").ifBlank {
                        qObj.optString("longname", "")
                    }.ifBlank { displaySym }
                    val inferredSector = if (category.isStock) {
                        KnownAssetsCatalog.inferSector(displaySym, category)
                    } else {
                        com.example.model.StockSector.NONE
                    }

                    out.add(
                        KnownAsset(
                            symbol = displaySym,
                            displayName = name,
                            category = category,
                            sector = inferredSector,
                            defaultCurrency = "THB",
                            yahooTicker = rawSym
                        )
                    )
                }
                out
            }
        } catch (e: Exception) {
            Log.d(TAG, "Yahoo search failed for $query: ${e.message}")
            emptyList()
        }
    }

    private fun fetchUsdThbRate(): Pair<Double, Boolean> {
        val yahooFx = queryYahooChart("THB=X")
        if (yahooFx != null && yahooFx.price > 10.0) {
            return yahooFx.price to true
        }
        // Secondary FX source via CoinGecko USDT in THB
        try {
            val req = Request.Builder()
                .url("https://api.coingecko.com/api/v3/simple/price?ids=tether&vs_currencies=thb")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val thb = json.optJSONObject("tether")?.optDouble("thb", 0.0) ?: 0.0
                    if (thb > 10.0) return thb to true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "CoinGecko FX fallback failed: ${e.message}")
        }
        return MarketSnapshot.DEFAULT_USD_THB_RATE to false
    }

    private fun fetchCoinGeckoQuotes(
        holdings: List<Holding>,
        usdToThb: Double
    ): Map<String, LiveQuote> {
        val idToHoldings = mutableMapOf<String, MutableList<Holding>>()
        for (h in holdings) {
            val cleanSym = h.symbol.trim().uppercase().removeSuffix("-USD")
            val knownId = KnownAssetsCatalog.findMatch(cleanSym, h.assetCategory)?.coinGeckoId
                ?: if (h.assetCategory == AssetCategory.CRYPTO) CRYPTO_FALLBACK_IDS[cleanSym] else GOLD_COINGECKO_IDS[cleanSym]
            if (knownId != null) {
                idToHoldings.getOrPut(knownId) { mutableListOf() }.add(h)
            }
        }
        if (idToHoldings.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, LiveQuote>()
        try {
            val idsParam = URLEncoder.encode(idToHoldings.keys.joinToString(","), "UTF-8")
            val url = "https://api.coingecko.com/api/v3/simple/price?ids=$idsParam&vs_currencies=thb,usd&include_24hr_change=true"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    for ((cgId, list) in idToHoldings) {
                        val obj = json.optJSONObject(cgId) ?: continue
                        val priceThb = obj.optDouble("thb", Double.NaN)
                        val priceUsd = obj.optDouble("usd", Double.NaN)
                        val change24h = obj.optDouble("thb_24h_change", Double.NaN)
                            .takeIf { !it.isNaN() }
                            ?: obj.optDouble("usd_24h_change", Double.NaN).takeIf { !it.isNaN() }

                        if (!priceThb.isNaN() && priceThb > 0.0) {
                            for (h in list) {
                                val origPrice = if (h.currency == "USD" && !priceUsd.isNaN() && priceUsd > 0.0) {
                                    priceUsd
                                } else if (h.currency == "USD") {
                                    priceThb / usdToThb
                                } else {
                                    priceThb
                                }
                                result[quoteKey(h)] = LiveQuote(
                                    key = quoteKey(h),
                                    symbol = h.symbol,
                                    currentPriceOriginal = origPrice,
                                    originalCurrency = h.currency,
                                    currentPriceThb = priceThb,
                                    changePercent24h = change24h,
                                    source = QuoteSource.LIVE_COINGECKO
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "CoinGecko fetch failed: ${e.message}")
        }
        return result
    }

    private fun fetchSingleNonCoinGeckoQuote(
        holding: Holding,
        usdToThb: Double
    ): LiveQuote {
        val key = quoteKey(holding)
        val clean = holding.symbol.trim().uppercase()
        val known = KnownAssetsCatalog.findMatch(clean, holding.assetCategory)

        val candidates: List<String> = when (holding.assetCategory) {
            AssetCategory.THAI_STOCK -> {
                val base = clean.removeSuffix(".BK")
                listOfNotNull(known?.yahooTicker, "$base.BK", clean)
            }
            AssetCategory.GLOBAL_STOCK -> {
                listOfNotNull(known?.yahooTicker, clean)
            }
            AssetCategory.MUTUAL_FUND -> {
                val base = clean.removeSuffix(".BK")
                if (known?.yahooTicker != null) {
                    listOf(known.yahooTicker)
                } else {
                    listOf(clean, "$base.BK")
                }
            }
            AssetCategory.GOLD -> {
                when {
                    known?.yahooTicker != null -> listOf(known.yahooTicker)
                    clean in setOf("XAU", "XAUUSD", "GOLD") -> listOf("GC=F")
                    else -> listOf(clean, "$clean-USD")
                }
            }
            AssetCategory.CRYPTO -> {
                val base = clean.removeSuffix("-USD")
                listOfNotNull(known?.yahooTicker, "$base-USD", clean)
            }
        }.distinct()

        // For known Thai Mutual Funds (without a Yahoo ticker) or Thai-style fund codes (e.g. K-USA, SCBNDQ(A)), try Finnomena NAV first
        val looksLikeThaiFundCode = (known != null && known.yahooTicker == null) ||
            clean.contains("-") ||
            clean.contains("(")
        if (holding.assetCategory == AssetCategory.MUTUAL_FUND && looksLikeThaiFundCode) {
            val fundQuote = queryFinnomenaFundNav(clean)
            if (fundQuote != null && fundQuote.price > 0.0) {
                return LiveQuote(
                    key = key,
                    symbol = holding.symbol,
                    currentPriceOriginal = fundQuote.price,
                    originalCurrency = "THB",
                    currentPriceThb = fundQuote.price,
                    changePercent24h = fundQuote.changePercent,
                    source = QuoteSource.LIVE_FUND
                )
            }
        }

        for (ticker in candidates) {
            val chartQuote = queryYahooChart(ticker)
            if (chartQuote != null && chartQuote.price > 0.0) {
                val quoteCurrency = chartQuote.currency.uppercase()
                val priceInThb = when (quoteCurrency) {
                    "THB" -> chartQuote.price
                    "USD" -> chartQuote.price * usdToThb
                    else -> if (holding.currency == "USD") chartQuote.price * usdToThb else chartQuote.price
                }
                return LiveQuote(
                    key = key,
                    symbol = holding.symbol,
                    currentPriceOriginal = chartQuote.price,
                    originalCurrency = quoteCurrency.ifBlank { holding.currency },
                    currentPriceThb = priceInThb,
                    changePercent24h = chartQuote.changePercent,
                    source = QuoteSource.LIVE_YAHOO
                )
            }
        }

        // For Mutual Funds (fallback if yahooTicker lookup failed) or Thai Gold Funds, check Finnomena public fund API
        if (holding.assetCategory == AssetCategory.MUTUAL_FUND || holding.assetCategory == AssetCategory.GOLD) {
            val fundQuote = queryFinnomenaFundNav(clean)
            if (fundQuote != null && fundQuote.price > 0.0) {
                return LiveQuote(
                    key = key,
                    symbol = holding.symbol,
                    currentPriceOriginal = fundQuote.price,
                    originalCurrency = "THB",
                    currentPriceThb = fundQuote.price,
                    changePercent24h = fundQuote.changePercent,
                    source = QuoteSource.LIVE_FUND
                )
            }
        }

        // Manual fallback if user supplied current price/NAV
        val manual = holding.manualCurrentPrice
        if (manual != null && manual > 0.0) {
            val priceThb = if (holding.currency == "USD") manual * usdToThb else manual
            return LiveQuote(
                key = key,
                symbol = holding.symbol,
                currentPriceOriginal = manual,
                originalCurrency = holding.currency,
                currentPriceThb = priceThb,
                changePercent24h = null,
                source = QuoteSource.MANUAL
            )
        }

        return LiveQuote(
            key = key,
            symbol = holding.symbol,
            currentPriceOriginal = 0.0,
            originalCurrency = holding.currency,
            currentPriceThb = 0.0,
            changePercent24h = null,
            source = QuoteSource.UNAVAILABLE
        )
    }

    internal data class RawChartQuote(
        val price: Double,
        val previousClose: Double?,
        val currency: String
    ) {
        val changePercent: Double?
            get() = if (previousClose != null && previousClose > 0.0) {
                ((price - previousClose) / previousClose) * 100.0
            } else null
    }

    private fun queryYahooChart(yahooSymbol: String): RawChartQuote? {
        return try {
            val encoded = URLEncoder.encode(yahooSymbol, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=5d"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val chart = root.optJSONObject("chart") ?: return null
                val results = chart.optJSONArray("result") ?: return null
                if (results.length() == 0) return null
                val meta = results.getJSONObject(0).optJSONObject("meta") ?: return null
                val price = meta.optDouble("regularMarketPrice", Double.NaN)
                if (price.isNaN() || price <= 0.0) return null
                val prevClose = meta.optDouble("chartPreviousClose", Double.NaN)
                    .takeIf { !it.isNaN() && it > 0.0 }
                    ?: meta.optDouble("previousClose", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
                val currency = meta.optString("currency", "THB")
                RawChartQuote(price = price, previousClose = prevClose, currency = currency)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Yahoo chart lookup failed for $yahooSymbol: ${e.message}")
            null
        }
    }

    private fun queryFinnomenaFundNav(symbol: String): RawChartQuote? {
        return try {
            val clean = symbol.trim().uppercase()
            if (clean.isBlank()) return null

            val directory = getOrFetchFinnomenaDirectory()
            val matchedFund = resolveBestFinnomenaMatch(directory, clean)
                ?: fetchFinnomenaFundDetailByCode(clean)
                ?: return null

            val encodedId = URLEncoder.encode(matchedFund.fundId, "UTF-8")
            val url = "https://www.finnomena.com/fn3/api/fund/v2/public/funds/nav/q?funds[]=$encodedId&range=1M"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                parseFinnomenaNavChartJson(body)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Finnomena fund NAV lookup failed for $symbol: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "MarketPriceService"
        private const val DIRECTORY_CACHE_TTL_MILLIS = 6L * 60L * 60L * 1000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

        fun quoteKey(holding: Holding): String =
            "${holding.category}:${holding.symbol.trim().uppercase()}"

        private fun normalizeFundCode(raw: String): String =
            raw.trim().uppercase().replace("-", "").replace("(", "").replace(")", "").replace(" ", "")

        internal fun parseFinnomenaFundListJson(jsonBody: String): List<FinnomenaFundEntry> {
            return try {
                val arr = JSONArray(jsonBody)
                val out = ArrayList<FinnomenaFundEntry>(arr.length())
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").trim()
                    val shortCode = obj.optString("short_code", "").trim().uppercase()
                    val nameTh = obj.optString("name_th", "").trim()
                    if (id.isNotBlank() && shortCode.isNotBlank()) {
                        out.add(
                            FinnomenaFundEntry(
                                fundId = id,
                                shortCode = shortCode,
                                nameTh = nameTh.ifBlank { shortCode }
                            )
                        )
                    }
                }
                out
            } catch (_: Exception) {
                emptyList()
            }
        }

        internal fun filterFinnomenaDirectory(
            entries: List<FinnomenaFundEntry>,
            query: String,
            limit: Int = 15
        ): List<KnownAsset> {
            val q = query.trim().lowercase()
            if (q.isBlank()) return emptyList()
            val normQ = normalizeFundCode(query).lowercase()

            data class Scored(val rank: Int, val entry: FinnomenaFundEntry)

            val matches = mutableListOf<Scored>()
            for (e in entries) {
                val scLower = e.shortCode.lowercase()
                val nameLower = e.nameTh.lowercase()
                val scNorm = normalizeFundCode(e.shortCode).lowercase()

                val rank = when {
                    scLower == q -> 0
                    scLower.startsWith(q) -> 1
                    normQ.isNotEmpty() && scNorm == normQ -> 2
                    normQ.isNotEmpty() && scNorm.startsWith(normQ) -> 3
                    scLower.contains(q) -> 4
                    normQ.isNotEmpty() && scNorm.contains(normQ) -> 5
                    nameLower.startsWith(q) -> 6
                    nameLower.contains(q) -> 7
                    else -> -1
                }
                if (rank >= 0) {
                    matches.add(Scored(rank, e))
                }
            }

            return matches
                .sortedWith(
                    compareBy<Scored>(
                        { it.rank },
                        { it.entry.shortCode.length },
                        { it.entry.shortCode }
                    )
                )
                .take(limit)
                .map {
                    KnownAsset(
                        symbol = it.entry.shortCode,
                        displayName = it.entry.nameTh,
                        category = AssetCategory.MUTUAL_FUND,
                        defaultCurrency = "THB"
                    )
                }
        }

        internal fun resolveBestFinnomenaMatch(
            entries: List<FinnomenaFundEntry>,
            symbol: String
        ): FinnomenaFundEntry? {
            if (entries.isEmpty()) return null
            val clean = symbol.trim().uppercase()
            val norm = normalizeFundCode(clean)
            return entries.firstOrNull { it.shortCode.equals(clean, ignoreCase = true) }
                ?: entries.firstOrNull { normalizeFundCode(it.shortCode) == norm }
                ?: entries.firstOrNull { it.shortCode.equals("${clean}A", ignoreCase = true) }
                ?: entries.firstOrNull { it.shortCode.equals("${clean}(A)", ignoreCase = true) }
                ?: entries.firstOrNull { it.shortCode.equals("${clean}-A", ignoreCase = true) }
                ?: entries.firstOrNull {
                    it.shortCode.startsWith("${clean}-", ignoreCase = true) ||
                        it.shortCode.startsWith("${clean}(", ignoreCase = true) ||
                        it.shortCode.startsWith("${clean} ", ignoreCase = true)
                }
                ?: if (clean.length >= 5) {
                    entries.firstOrNull { it.shortCode.startsWith(clean, ignoreCase = true) }
                } else {
                    null
                }
        }

        internal fun parseFinnomenaNavChartJson(jsonBody: String): RawChartQuote? {
            return try {
                val root = JSONObject(jsonBody)
                if (!root.optBoolean("status", false)) return null
                val dataArr = root.optJSONArray("data") ?: return null
                if (dataArr.length() == 0) return null
                val fundObj = dataArr.optJSONObject(0) ?: return null
                val navs = fundObj.optJSONArray("navs") ?: return null
                if (navs.length() == 0) return null

                var latestNav: Double? = null
                var prevNav: Double? = null
                for (i in navs.length() - 1 downTo 0) {
                    val point = navs.optJSONObject(i) ?: continue
                    val v = point.optDouble("value", Double.NaN)
                    if (!v.isNaN() && v > 0.0) {
                        if (latestNav == null) {
                            latestNav = v
                        } else {
                            prevNav = v
                            break
                        }
                    }
                }
                val finalLatest = latestNav ?: return null
                RawChartQuote(
                    price = finalLatest,
                    previousClose = prevNav,
                    currency = "THB"
                )
            } catch (_: Exception) {
                null
            }
        }

        private val CRYPTO_FALLBACK_IDS = mapOf(
            "BTC" to "bitcoin",
            "ETH" to "ethereum",
            "SOL" to "solana",
            "BNB" to "binancecoin",
            "XRP" to "ripple",
            "ADA" to "cardano",
            "DOGE" to "dogecoin",
            "AVAX" to "avalanche-2",
            "DOT" to "polkadot",
            "MATIC" to "matic-network",
            "POL" to "matic-network",
            "LINK" to "chainlink",
            "TON" to "the-open-network",
            "SUI" to "sui",
            "USDT" to "tether",
            "USDC" to "usd-coin"
        )

        private val GOLD_COINGECKO_IDS = mapOf(
            "PAXG" to "pax-gold",
            "XAUT" to "tether-gold"
        )

        fun buildUpcomingDividendProjections(
            historicalEvents: List<DividendEvent>,
            heldQuantity: Double,
            currency: String,
            usdToThbRate: Double,
            confirmedExDateMillis: Long? = null,
            confirmedDividendPerShare: Double? = null,
            nowMillis: Long = System.currentTimeMillis()
        ): Pair<Double, List<UpcomingDividendProjection>> {
            val sortedAsc = historicalEvents
                .filter { it.amountPerShare > 0.0 && it.exDateMillis > 0L }
                .sortedBy { it.exDateMillis }

            val fxMultiplier = if (currency.uppercase() == "USD") usdToThbRate else 1.0
            val oneYearMillis = 365L * 24L * 60L * 60L * 1000L
            val trailingCutoff = nowMillis - (380L * 24L * 60L * 60L * 1000L)

            val recentCycle = sortedAsc.filter { it.exDateMillis >= trailingCutoff }
            val trailingPerShare = recentCycle.sumOf { it.amountPerShare }

            val projections = mutableListOf<UpcomingDividendProjection>()

            if (confirmedExDateMillis != null && confirmedExDateMillis >= nowMillis - (24L * 60L * 60L * 1000L)) {
                val estAmt = confirmedDividendPerShare?.takeIf { it > 0.0 }
                    ?: sortedAsc.lastOrNull()?.amountPerShare
                    ?: 0.0
                if (estAmt > 0.0) {
                    projections.add(
                        UpcomingDividendProjection(
                            expectedExDateMillis = confirmedExDateMillis,
                            estimatedAmountPerShare = estAmt,
                            estimatedPayoutThb = estAmt * heldQuantity * fxMultiplier,
                            isConfirmed = true,
                            note = "วัน XD ตามประกาศตลาด"
                        )
                    )
                }
            }

            // Project next 12 months by shifting each payout from the trailing cycle forward by 1 year
            val cycleSource = if (recentCycle.isNotEmpty()) recentCycle else sortedAsc.takeLast(2)
            for (event in cycleSource) {
                val cal = Calendar.getInstance(Locale.US).apply {
                    timeInMillis = event.exDateMillis
                }
                var safety = 0
                while (cal.timeInMillis <= nowMillis && safety < 5) {
                    cal.add(Calendar.YEAR, 1)
                    safety++
                }
                val projectedMillis = cal.timeInMillis
                if (projectedMillis > nowMillis && projectedMillis <= nowMillis + oneYearMillis + (30L * 24L * 60L * 60L * 1000L)) {
                    val alreadyCoveredByConfirmed = projections.any { existing ->
                        kotlin.math.abs(existing.expectedExDateMillis - projectedMillis) < (25L * 24L * 60L * 60L * 1000L)
                    }
                    if (!alreadyCoveredByConfirmed) {
                        projections.add(
                            UpcomingDividendProjection(
                                expectedExDateMillis = projectedMillis,
                                estimatedAmountPerShare = event.amountPerShare,
                                estimatedPayoutThb = event.amountPerShare * heldQuantity * fxMultiplier,
                                isConfirmed = false,
                                note = "คาดการณ์จากรอบปันผลปีที่ผ่านมา"
                            )
                        )
                    }
                }
            }

            return trailingPerShare to projections.sortedBy { it.expectedExDateMillis }
        }
    }

    suspend fun fetchStockDividendInfo(holding: Holding): StockDividendInfo = withContext(Dispatchers.IO) {
        val fx = fetchUsdThbRate().first
        val clean = holding.symbol.trim().uppercase()
        val known = KnownAssetsCatalog.findMatch(clean, holding.assetCategory)

        val candidates: List<String> = when (holding.assetCategory) {
            AssetCategory.THAI_STOCK -> {
                val base = clean.removeSuffix(".BK")
                listOfNotNull(known?.yahooTicker, "$base.BK", clean)
            }
            AssetCategory.GLOBAL_STOCK -> listOfNotNull(known?.yahooTicker, clean)
            else -> listOfNotNull(known?.yahooTicker, clean)
        }.distinct()

        for (ticker in candidates) {
            val result = queryYahooDividendChart(ticker)
            if (result != null) {
                val calendarConfirmed = queryYahooCalendarExDate(ticker)
                val (trailingPerShare, upcoming) = buildUpcomingDividendProjections(
                    historicalEvents = result.dividends,
                    heldQuantity = holding.quantity,
                    currency = result.currency,
                    usdToThbRate = fx,
                    confirmedExDateMillis = calendarConfirmed?.first,
                    confirmedDividendPerShare = calendarConfirmed?.second
                )
                val fxMult = if (result.currency.uppercase() == "USD") fx else 1.0
                val yieldPct = if (result.price > 0.0 && trailingPerShare > 0.0) {
                    (trailingPerShare / result.price) * 100.0
                } else null
                val annualIncomeThb = if (upcoming.isNotEmpty()) {
                    upcoming.sumOf { it.estimatedPayoutThb }
                } else {
                    trailingPerShare * holding.quantity * fxMult
                }

                return@withContext StockDividendInfo(
                    symbol = holding.symbol,
                    displayName = holding.displayName,
                    currency = result.currency,
                    currentPrice = result.price,
                    usdToThbRate = fx,
                    heldQuantity = holding.quantity,
                    trailingAnnualDividendPerShare = trailingPerShare,
                    trailingYieldPercent = yieldPct,
                    estimatedAnnualIncomeThb = annualIncomeThb,
                    upcomingDividends = upcoming,
                    historicalDividends = result.dividends.sortedByDescending { it.exDateMillis },
                    sourceNote = "ข้อมูลปันผลจริงจาก Yahoo Finance ($ticker)"
                )
            }
        }

        StockDividendInfo(
            symbol = holding.symbol,
            displayName = holding.displayName,
            currency = holding.currency,
            currentPrice = 0.0,
            usdToThbRate = fx,
            heldQuantity = holding.quantity,
            trailingAnnualDividendPerShare = 0.0,
            trailingYieldPercent = null,
            estimatedAnnualIncomeThb = 0.0,
            upcomingDividends = emptyList(),
            historicalDividends = emptyList(),
            sourceNote = "ไม่สามารถเชื่อมต่อข้อมูลปันผลได้",
            errorMessage = "ไม่สามารถดึงข้อมูลเงินปันผลของ ${holding.symbol} ได้ในขณะนี้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
        )
    }

    private data class RawDividendChartResult(
        val price: Double,
        val currency: String,
        val dividends: List<DividendEvent>
    )

    private fun queryYahooDividendChart(yahooSymbol: String): RawDividendChartResult? {
        return try {
            val encoded = URLEncoder.encode(yahooSymbol, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=2y&events=div"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val chart = root.optJSONObject("chart") ?: return null
                val results = chart.optJSONArray("result") ?: return null
                if (results.length() == 0) return null
                val first = results.getJSONObject(0)
                val meta = first.optJSONObject("meta") ?: return null
                val price = meta.optDouble("regularMarketPrice", 0.0)
                val currency = meta.optString("currency", "THB").uppercase()

                val dividendsList = mutableListOf<DividendEvent>()
                val divObj = first.optJSONObject("events")?.optJSONObject("dividends")
                if (divObj != null) {
                    val keys = divObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val entry = divObj.optJSONObject(key) ?: continue
                        val amount = entry.optDouble("amount", Double.NaN)
                        val dateSec = entry.optLong("date", 0L)
                        if (!amount.isNaN() && amount > 0.0 && dateSec > 0L) {
                            dividendsList.add(
                                DividendEvent(
                                    exDateMillis = dateSec * 1000L,
                                    amountPerShare = amount
                                )
                            )
                        }
                    }
                }
                RawDividendChartResult(
                    price = if (price.isNaN()) 0.0 else price,
                    currency = currency.ifBlank { "THB" },
                    dividends = dividendsList
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Yahoo dividend chart lookup failed for $yahooSymbol: ${e.message}")
            null
        }
    }

    private fun queryYahooCalendarExDate(yahooSymbol: String): Pair<Long?, Double?>? {
        return try {
            val encoded = URLEncoder.encode(yahooSymbol, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/$encoded?modules=calendarEvents,summaryDetail"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val resArr = root.optJSONObject("quoteSummary")?.optJSONArray("result") ?: return null
                if (resArr.length() == 0) return null
                val first = resArr.getJSONObject(0)
                val exSec = first.optJSONObject("calendarEvents")
                    ?.optJSONObject("exDividendDate")
                    ?.optLong("raw", 0L)
                    ?.takeIf { it > 0L }
                val divRate = first.optJSONObject("summaryDetail")
                    ?.optJSONObject("lastDividendValue")
                    ?.optDouble("raw", Double.NaN)
                    ?.takeIf { !it.isNaN() && it > 0.0 }
                if (exSec != null) (exSec * 1000L) to divRate else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
