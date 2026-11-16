package com.example.data

import com.example.model.AssetCategory
import com.example.model.Holding
import com.example.model.ManagedSector
import com.example.model.StockSector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

data class PortfolioBackupPayload(
    val holdings: List<Holding>,
    val sectors: List<ManagedSector> = emptyList()
)

object PortfolioJsonCodec {
    private const val CURRENT_SCHEMA_VERSION = 1

    fun encode(
        holdings: List<Holding>,
        managedSectors: List<ManagedSector> = emptyList()
    ): String {
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val holdingsArray = JSONArray()
        for (h in holdings) {
            val obj = JSONObject().apply {
                put("symbol", h.symbol)
                put("displayName", h.displayName)
                put("category", h.category)
                put("sector", h.sector)
                put("quantity", h.quantity)
                put("buyPricePerUnit", h.buyPricePerUnit)
                put("currency", h.currency)
                if (h.manualCurrentPrice != null) {
                    put("manualCurrentPrice", h.manualCurrentPrice)
                }
                put("createdAtMillis", h.createdAtMillis)
                put("updatedAtMillis", h.updatedAtMillis)
            }
            holdingsArray.put(obj)
        }

        val sectorsArray = JSONArray()
        for (s in managedSectors) {
            sectorsArray.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("label", s.label)
                    put("fullLabel", s.fullLabel)
                    put("isCustom", s.isCustom)
                }
            )
        }

        val root = JSONObject().apply {
            put("app", "PortPulse")
            put("version", CURRENT_SCHEMA_VERSION)
            put("exportedAt", isoFormatter.format(Date()))
            put("count", holdings.size)
            put("holdings", holdingsArray)
            if (managedSectors.isNotEmpty()) {
                put("sectors", sectorsArray)
            }
        }
        return root.toString(2)
    }

    fun decode(jsonString: String): Result<List<Holding>> =
        decodeWithSectors(jsonString).map { it.holdings }

    fun decodeWithSectors(jsonString: String): Result<PortfolioBackupPayload> {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("ไฟล์ JSON ว่างเปล่า"))
            }

            var rootObj: JSONObject? = null
            val array: JSONArray = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    rootObj = root
                    root.optJSONArray("holdings")
                        ?: root.optJSONArray("items")
                        ?: return Result.failure(
                            IllegalArgumentException("ไม่พบรายการ 'holdings' ในไฟล์ JSON")
                        )
                }
                else -> return Result.failure(
                    IllegalArgumentException("รูปแบบไฟล์ JSON ไม่ถูกต้อง")
                )
            }

            val parsedSectors = mutableListOf<ManagedSector>()
            rootObj?.optJSONArray("sectors")?.let { secArr ->
                for (i in 0 until secArr.length()) {
                    val secObj = secArr.optJSONObject(i) ?: continue
                    val id = secObj.optString("id", "").trim()
                    val label = secObj.optString("label", "").trim()
                    val fullLabel = secObj.optString("fullLabel", label).trim().ifBlank { label }
                    val isCustom = secObj.optBoolean("isCustom", true)
                    if (id.isNotBlank() && label.isNotBlank()) {
                        parsedSectors.add(
                            ManagedSector(
                                id = id,
                                label = label,
                                fullLabel = fullLabel,
                                isCustom = isCustom
                            )
                        )
                    }
                }
            }

            val now = System.currentTimeMillis()
            val parsed = mutableListOf<Holding>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                    ?: return Result.failure(
                        IllegalArgumentException("รายการลำดับที่ ${i + 1} ไม่ใช่ JSON Object")
                    )

                val rawSymbol = obj.optString("symbol", "").trim().uppercase()
                if (rawSymbol.isBlank()) {
                    return Result.failure(
                        IllegalArgumentException("รายการลำดับที่ ${i + 1} ไม่มีรหัสสินทรัพย์ (symbol)")
                    )
                }

                val rawCategory = obj.optString("category", AssetCategory.THAI_STOCK.name).trim()
                val category = AssetCategory.entries.find {
                    it.name.equals(rawCategory, ignoreCase = true)
                } ?: AssetCategory.THAI_STOCK

                val rawSector = obj.optString("sector", "").trim()
                val sector = if (category.isStock) {
                    if (rawSector.isNotBlank() && rawSector != StockSector.NONE.name) {
                        rawSector
                    } else {
                        KnownAssetsCatalog.inferSector(rawSymbol, category).name
                    }
                } else {
                    StockSector.NONE.name
                }

                val quantity = obj.optDouble("quantity", Double.NaN)
                if (quantity.isNaN() || quantity <= 0.0) {
                    return Result.failure(
                        IllegalArgumentException("รายการ $rawSymbol มีจำนวนหุ้น/หน่วย (quantity) ไม่ถูกต้อง")
                    )
                }

                val buyPrice = obj.optDouble("buyPricePerUnit", Double.NaN)
                if (buyPrice.isNaN() || buyPrice < 0.0) {
                    return Result.failure(
                        IllegalArgumentException("รายการ $rawSymbol มีราคาที่ได้มา (buyPricePerUnit) ไม่ถูกต้อง")
                    )
                }

                val rawCurrency = obj.optString("currency", category.defaultCurrency).trim().uppercase()
                val currency = if (rawCurrency == "USD") "USD" else "THB"

                val displayName = obj.optString("displayName", rawSymbol)
                    .trim()
                    .ifBlank { rawSymbol }
                    .take(80)

                val manualPrice = if (obj.has("manualCurrentPrice") && !obj.isNull("manualCurrentPrice")) {
                    obj.optDouble("manualCurrentPrice", Double.NaN)
                        .takeIf { !it.isNaN() && it >= 0.0 }
                } else {
                    null
                }

                val createdAt = obj.optLong("createdAtMillis", now).takeIf { it > 0L } ?: now
                val updatedAt = obj.optLong("updatedAtMillis", now).takeIf { it > 0L } ?: now

                parsed.add(
                    Holding(
                        id = 0L,
                        symbol = rawSymbol.take(32),
                        displayName = displayName,
                        category = category.name,
                        sector = sector,
                        quantity = quantity,
                        buyPricePerUnit = buyPrice,
                        currency = currency,
                        manualCurrentPrice = manualPrice,
                        createdAtMillis = createdAt,
                        updatedAtMillis = updatedAt
                    )
                )
            }

            if (parsed.isEmpty()) {
                return Result.failure(IllegalArgumentException("ไม่พบรายการสินทรัพย์ในไฟล์ JSON"))
            }

            Result.success(PortfolioBackupPayload(holdings = parsed, sectors = parsedSectors))
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("ไม่สามารถอ่านไฟล์ JSON ได้: ${e.localizedMessage ?: "รูปแบบไม่ถูกต้อง"}")
            )
        }
    }
}
