package com.example.data

import com.example.model.AssetCategory
import com.example.model.Holding
import com.example.model.ManagedSector
import com.example.model.PortfolioPlan
import com.example.model.StockSector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

data class PortfolioBackupPayload(
    val holdings: List<Holding>,
    val sectors: List<ManagedSector> = emptyList(),
    val plan: PortfolioPlan? = null
)

object PortfolioJsonCodec {
    private const val CURRENT_SCHEMA_VERSION = 2
    const val MAX_IMPORT_JSON_BYTES = 5 * 1024 * 1024
    private const val MAX_IMPORT_JSON_CHARS = 5 * 1024 * 1024

    fun encodePlanToJsonObject(plan: PortfolioPlan): JSONObject {
        val clean = plan.sanitized()
        val targetsObj = JSONObject()
        for (cat in AssetCategory.entries) {
            val pct = clean.targetPercentFor(cat)
            if (pct > 0.0) {
                targetsObj.put(cat.name, pct)
            }
        }
        val notesObj = JSONObject()
        for (cat in AssetCategory.entries) {
            val note = clean.noteFor(cat)
            if (note.isNotBlank()) {
                notesObj.put(cat.name, note)
            }
        }
        return JSONObject().apply {
            put("targetPercents", targetsObj)
            put("categoryNotes", notesObj)
            put("overallNote", clean.overallNote)
            clean.updatedAtMillis?.takeIf { it > 0L }?.let { put("updatedAtMillis", it) }
        }
    }

    fun decodePlanFromJsonObject(obj: JSONObject?): PortfolioPlan? {
        if (obj == null) return null
        val targets = mutableMapOf<AssetCategory, Double>()
        obj.optJSONObject("targetPercents")?.let { tObj ->
            for (cat in AssetCategory.entries) {
                if (tObj.has(cat.name) && !tObj.isNull(cat.name)) {
                    val v = tObj.optDouble(cat.name, Double.NaN)
                    if (v.isFinite() && v >= 0.0) {
                        targets[cat] = v.coerceIn(0.0, 100.0)
                    }
                }
            }
        }
        val notes = mutableMapOf<AssetCategory, String>()
        obj.optJSONObject("categoryNotes")?.let { nObj ->
            for (cat in AssetCategory.entries) {
                if (nObj.has(cat.name) && !nObj.isNull(cat.name)) {
                    val text = nObj.optString(cat.name, "").trim()
                    if (text.isNotEmpty()) {
                        notes[cat] = text.take(PortfolioPlan.MAX_CATEGORY_NOTE_CHARS)
                    }
                }
            }
        }
        val overallNote = if (obj.has("overallNote") && !obj.isNull("overallNote")) {
            obj.optString("overallNote", "").trim().take(PortfolioPlan.MAX_OVERALL_NOTE_CHARS)
        } else {
            ""
        }
        val updatedAt = obj.optLong("updatedAtMillis", -1L).takeIf { it > 0L }
        return PortfolioPlan(
            targetPercents = targets,
            categoryNotes = notes,
            overallNote = overallNote,
            updatedAtMillis = updatedAt
        ).sanitized(updatedAt)
    }

    fun encode(
        holdings: List<Holding>,
        managedSectors: List<ManagedSector> = emptyList(),
        portfolioPlan: PortfolioPlan? = null
    ): String {
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val holdingsArray = JSONArray()
        for (h in holdings) {
            val safeQuantity = h.quantity.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
            val safeBuyPrice = h.buyPricePerUnit.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            val safeManualPrice = h.manualCurrentPrice?.takeIf { it.isFinite() && it >= 0.0 }
            val obj = JSONObject().apply {
                put("symbol", h.symbol)
                put("displayName", h.displayName)
                put("category", h.category)
                put("sector", h.sector)
                put("quantity", safeQuantity)
                put("buyPricePerUnit", safeBuyPrice)
                put("currency", h.currency)
                if (safeManualPrice != null) {
                    put("manualCurrentPrice", safeManualPrice)
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
            if (portfolioPlan != null && portfolioPlan.hasContent) {
                put("plan", encodePlanToJsonObject(portfolioPlan))
            }
        }
        return root.toString(2)
    }

    fun decode(jsonString: String): Result<List<Holding>> =
        decodeWithSectors(jsonString).map { it.holdings }

    fun decodeWithSectors(jsonString: String): Result<PortfolioBackupPayload> {
        return try {
            val trimmed = jsonString.trimStart('\uFEFF').trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("ไฟล์ JSON ว่างเปล่า"))
            }
            if (trimmed.length > MAX_IMPORT_JSON_CHARS) {
                return Result.failure(
                    IllegalArgumentException("ไฟล์ JSON มีขนาดใหญ่เกินไป (สูงสุด 5 MB)")
                )
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

            val parsedPlan = decodePlanFromJsonObject(rootObj?.optJSONObject("plan"))

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
                if (!quantity.isFinite() || quantity <= 0.0) {
                    return Result.failure(
                        IllegalArgumentException("รายการ $rawSymbol มีจำนวนหุ้น/หน่วย (quantity) ไม่ถูกต้อง")
                    )
                }

                val buyPrice = obj.optDouble("buyPricePerUnit", Double.NaN)
                if (!buyPrice.isFinite() || buyPrice < 0.0) {
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
                        .takeIf { it.isFinite() && it >= 0.0 }
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

            if (parsed.isEmpty() && parsedPlan?.hasContent != true) {
                return Result.failure(IllegalArgumentException("ไม่พบรายการสินทรัพย์ในไฟล์ JSON"))
            }

            Result.success(
                PortfolioBackupPayload(
                    holdings = parsed,
                    sectors = parsedSectors,
                    plan = parsedPlan
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("ไม่สามารถอ่านไฟล์ JSON ได้: ${e.localizedMessage ?: "รูปแบบไม่ถูกต้อง"}")
            )
        } catch (_: OutOfMemoryError) {
            Result.failure(
                IllegalArgumentException("ไฟล์ JSON มีขนาดใหญ่หรือซับซ้อนเกินไป ไม่สามารถอ่านได้")
            )
        }
    }
}
