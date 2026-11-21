package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AssetCategory(
    val labelTh: String,
    val shortLabelTh: String,
    val defaultCurrency: String
) {
    THAI_STOCK("หุ้นไทย (SET)", "หุ้นไทย", "THB"),
    GLOBAL_STOCK("หุ้นต่างประเทศ", "หุ้นนอก", "THB"),
    MUTUAL_FUND("กองทุนรวม / ETF", "กองทุน", "THB"),
    GOLD("ทองคำ", "ทอง", "THB"),
    CRYPTO("คริปโตเคอร์เรนซี", "คริปโต", "THB");

    val isStock: Boolean
        get() = this == THAI_STOCK || this == GLOBAL_STOCK
}

enum class StockSector(
    val labelTh: String,
    val shortLabelTh: String
) {
    ENERGY("พลังงานและสาธารณูปโภค", "พลังงาน"),
    PROPERTY("อสังหาริมทรัพย์และที่อยู่อาศัย", "ที่อยู่อาศัย"),
    INFRASTRUCTURE("โครงสร้างพื้นฐานและขนส่ง", "โครงสร้างพื้นฐาน"),
    FINANCE("ธนาคารและการเงิน", "การเงิน"),
    TECH_ICT("เทคโนโลยีและสื่อสาร", "เทคโนโลยี/ICT"),
    COMMERCE("พาณิชย์และค้าปลีก", "ค้าปลีก"),
    HEALTHCARE("การแพทย์และสุขภาพ", "การแพทย์"),
    FOOD("อาหารและเครื่องดื่ม", "อาหาร"),
    OTHER("อุตสาหกรรมและอื่นๆ", "อื่นๆ"),
    NONE("ไม่ระบุกลุ่ม", "-");

    companion object {
        fun defaultManagedSectors(): List<ManagedSector> = entries
            .filter { it != NONE }
            .map { ManagedSector(id = it.name, label = it.shortLabelTh, fullLabel = it.labelTh, isCustom = false) }
    }
}

data class ManagedSector(
    val id: String,
    val label: String,
    val fullLabel: String = label,
    val isCustom: Boolean = true
)

@Entity(tableName = "holdings")
data class Holding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val symbol: String = "",
    val displayName: String = "",
    val category: String = AssetCategory.THAI_STOCK.name,
    val sector: String = StockSector.ENERGY.name,
    val quantity: Double = 0.0,
    val buyPricePerUnit: Double = 0.0,
    val currency: String = "THB",
    val manualCurrentPrice: Double? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val assetCategory: AssetCategory
        get() = AssetCategory.entries.find { it.name == category } ?: AssetCategory.THAI_STOCK

    val stockSector: StockSector
        get() = StockSector.entries.find { it.name == sector } ?: StockSector.OTHER

    fun resolveSectorLabel(managedSectors: List<ManagedSector>): String {
        if (!assetCategory.isStock) return "-"
        managedSectors.firstOrNull { it.id == sector }?.let { return it.label }
        StockSector.entries.find { it.name == sector && it != StockSector.NONE }?.let { return it.shortLabelTh }
        return sector.takeIf { it.isNotBlank() && it != StockSector.NONE.name && !it.startsWith("CUSTOM_") }
            ?: StockSector.OTHER.shortLabelTh
    }
}

data class PortfolioPlan(
    val targetPercents: Map<AssetCategory, Double> = emptyMap(),
    val categoryNotes: Map<AssetCategory, String> = emptyMap(),
    val overallNote: String = "",
    val updatedAtMillis: Long? = null
) {
    fun targetPercentFor(category: AssetCategory): Double =
        targetPercents[category]?.takeIf { it.isFinite() && it >= 0.0 }?.coerceIn(0.0, 100.0) ?: 0.0

    fun noteFor(category: AssetCategory): String =
        categoryNotes[category].orEmpty()

    val totalTargetPercent: Double
        get() = AssetCategory.entries.sumOf { targetPercentFor(it) }

    val hasContent: Boolean
        get() = AssetCategory.entries.any { targetPercentFor(it) > 0.0 || noteFor(it).isNotBlank() } ||
            overallNote.isNotBlank()

    fun sanitized(timestampMillis: Long? = updatedAtMillis): PortfolioPlan {
        val cleanTargets = buildMap {
            for (cat in AssetCategory.entries) {
                val pct = targetPercentFor(cat)
                if (pct > 0.0) {
                    put(cat, Math.round(pct * 100.0) / 100.0)
                }
            }
        }
        val cleanNotes = buildMap {
            for (cat in AssetCategory.entries) {
                val trimmed = noteFor(cat).trim().take(MAX_CATEGORY_NOTE_CHARS)
                if (trimmed.isNotEmpty()) {
                    put(cat, trimmed)
                }
            }
        }
        val cleanOverall = overallNote.trim().take(MAX_OVERALL_NOTE_CHARS)
        return PortfolioPlan(
            targetPercents = cleanTargets,
            categoryNotes = cleanNotes,
            overallNote = cleanOverall,
            updatedAtMillis = timestampMillis
        )
    }

    companion object {
        const val MAX_CATEGORY_NOTE_CHARS = 500
        const val MAX_OVERALL_NOTE_CHARS = 2000
    }
}
