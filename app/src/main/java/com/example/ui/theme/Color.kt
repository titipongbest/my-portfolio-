package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.AssetCategory

// Base High-Contrast Text Colors on Light Backgrounds
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF334155)

// Shared Warm Amber / Sky Helpers
val DarkInkOnYellow = Color(0xFF1A1200)
val SoftYellowContainer = Color(0xFFFEF0B8)
val DeepAmberOnContainer = Color(0xFF422C00)
val WarmGoldSecondary = Color(0xFFB45309)
val SlateBlueTertiary = Color(0xFF0369A1)
val WhiteOnTertiary = Color(0xFFFFFFFF)
val PaleSkyContainer = Color(0xFFE0F2FE)
val DeepSkyOnContainer = Color(0xFF0C4A6E)

// Mode 1: Thai Stocks (หุ้นไทย) -> Orange (สีส้ม)
val StockOrangeBg = Color(0xFFFFF8F2)
val StockOrangeSurfaceVariant = Color(0xFFFFEDE0)
val StockOrangeBorder = Color(0xFFFDBA74)
val StockOrangePrimary = Color(0xFFC2410C)
val StockOrangeContainer = Color(0xFFFFEDD5)
val StockOrangeOnContainer = Color(0xFF7C2D12)
val StockOrangeHighlight = Color(0xFF9A3412)

// Mode 2: Global Stocks (หุ้นต่างประเทศ) -> Royal Purple / Violet (สีม่วง)
val GlobalPurpleBg = Color(0xFFF9F6FF)
val GlobalPurpleSurfaceVariant = Color(0xFFF1E8FF)
val GlobalPurpleBorder = Color(0xFFC4B5FD)
val GlobalPurplePrimary = Color(0xFF6D28D9)
val GlobalPurpleContainer = Color(0xFFEDE9FE)
val GlobalPurpleOnContainer = Color(0xFF4C1D95)
val GlobalPurpleHighlight = Color(0xFF5B21B6)

// Mode 3: Crypto (คริปโต) -> Yellow (สีเหลือง)
val CryptoYellowBg = Color(0xFFFFFCF0)
val CryptoYellowSurfaceVariant = Color(0xFFFFF7CC)
val CryptoYellowBorder = Color(0xFFEAB308)
val CryptoYellowPrimary = Color(0xFFF5B700)
val CryptoYellowContainer = Color(0xFFFEF08A)
val CryptoYellowOnContainer = Color(0xFF422C00)
val CryptoYellowHighlight = Color(0xFF92400E)

// Mode 4: Gold (ทอง) -> Blue (สีน้ำเงิน)
val GoldBlueBg = Color(0xFFF4F8FF)
val GoldBlueSurfaceVariant = Color(0xFFE6F0FF)
val GoldBlueBorder = Color(0xFF93C5FD)
val GoldBluePrimary = Color(0xFF1D4ED8)
val GoldBlueContainer = Color(0xFFDBEAFE)
val GoldBlueOnContainer = Color(0xFF1E3A8A)
val GoldBlueHighlight = Color(0xFF1D4ED8)

// Mode 5: Mutual Funds (กองทุน) -> Green (สีเขียว)
val FundGreenBg = Color(0xFFF3FCF6)
val FundGreenSurfaceVariant = Color(0xFFE3F9EC)
val FundGreenBorder = Color(0xFF6EE7B7)
val FundGreenPrimary = Color(0xFF15803D)
val FundGreenContainer = Color(0xFFDCFCE7)
val FundGreenOnContainer = Color(0xFF14532D)
val FundGreenHighlight = Color(0xFF15803D)

val PureWhiteSurface = Color(0xFFFFFFFF)

// Institutional Wealth / Banking Palette (inspired by Streaming Fund+ aesthetic)
// 0. Portfolio Overview (Executive Obsidian Slate - distinct from all 5 asset categories)
val OverviewSlateDeep = Color(0xFF1E293B)
val OverviewSlatePrimary = Color(0xFF334155)
val OverviewSlateBright = Color(0xFF475569)
val OverviewSlateContainer = Color(0xFFE2E8F0)
val OverviewSlateOnContainer = Color(0xFF0F172A)

// 1. Thai Stocks (สีเดิม: Deep Institutional Navy)
val NavyHeaderDeep = Color(0xFF0F2B4F)
val NavyPrimary = Color(0xFF194276)
val NavyBright = Color(0xFF235596)
val NavyContainer = Color(0xFFE8F0FA)
val NavyOnContainer = Color(0xFF0F2B4F)

// 2. Global Stocks (Royal Executive Indigo)
val GlobalIndigoDeep = Color(0xFF24144D)
val GlobalIndigoPrimary = Color(0xFF432687)
val GlobalIndigoBright = Color(0xFF5635A6)
val GlobalIndigoContainer = Color(0xFFEDE9FE)
val GlobalIndigoOnContainer = Color(0xFF2E1065)

// 3. Mutual Funds / ETF (Private Bank Forest Emerald)
val FundEmeraldDeep = Color(0xFF0A3624)
val FundEmeraldPrimary = Color(0xFF136342)
val FundEmeraldBright = Color(0xFF197D54)
val FundEmeraldContainer = Color(0xFFE1F5EC)
val FundEmeraldOnContainer = Color(0xFF0A3624)

// 4. Gold (Bullion Bronze / Executive Warm Gold)
val GoldBronzeDeep = Color(0xFF472504)
val GoldBronzePrimary = Color(0xFF8C4E0A)
val GoldBronzeBright = Color(0xFFA86010)
val GoldBronzeContainer = Color(0xFFFEF3C7)
val GoldBronzeOnContainer = Color(0xFF451A03)

// 5. Crypto (Deep Ocean Petrol Teal)
val CryptoTealDeep = Color(0xFF0A3542)
val CryptoTealPrimary = Color(0xFF0E6478)
val CryptoTealBright = Color(0xFF127C94)
val CryptoTealContainer = Color(0xFFE0F2FE)
val CryptoTealOnContainer = Color(0xFF083344)

val WealthCanvasBg = Color(0xFFF3F6FA)
val WealthSurfaceVariant = Color(0xFFECF1F7)
val WealthHairlineBorder = Color(0xFFDCE4EE)
val WealthSlateMuted = Color(0xFF64748B)

// Financial Semantic Colors (High-contrast on Pure White & Soft Containers)
val ProfitGreenLight = Color(0xFF047857)
val ProfitGreenContainerLight = Color(0xFFECFDF5)
val LossRedLight = Color(0xFFB91C1C)
val LossRedContainerLight = Color(0xFFFEF2F2)
val GoldAccentTextLight = Color(0xFF92400E)

// Distinct Cost-Basis Box Colors (Cool Slate so Cost vs Current Value stands out clearly)
val CostBoxBg = Color(0xFFF8FAFC)
val CostBoxBorder = Color(0xFFCBD5E1)
val CostBoxLabelColor = Color(0xFF475569)

// Institutional Chart Palette (crisp blues, teals, violets, and high-contrast accents on white)
val ChartPalette = listOf(
    Color(0xFF1D4ED8), // Cobalt Blue
    Color(0xFF0369A1), // Deep Sky Blue
    Color(0xFF0F766E), // Institutional Teal
    Color(0xFF6D28D9), // Royal Violet
    Color(0xFF1E3A8A), // Deep Indigo
    Color(0xFFB45309), // Amber Gold
    Color(0xFF047857), // Emerald Green
    Color(0xFFBE123C), // Crimson Rose
    Color(0xFF475569), // Institutional Slate
    Color(0xFFC2410C)  // Deep Rust
)

fun categoryHeroDeepColor(category: AssetCategory?): Color = when (category) {
    null -> OverviewSlateDeep
    AssetCategory.THAI_STOCK -> NavyHeaderDeep
    AssetCategory.GLOBAL_STOCK -> GlobalIndigoDeep
    AssetCategory.MUTUAL_FUND -> FundEmeraldDeep
    AssetCategory.GOLD -> GoldBronzeDeep
    AssetCategory.CRYPTO -> CryptoTealDeep
}

fun categoryHeroLightColor(category: AssetCategory?): Color = when (category) {
    null -> OverviewSlatePrimary
    AssetCategory.THAI_STOCK -> NavyPrimary
    AssetCategory.GLOBAL_STOCK -> GlobalIndigoPrimary
    AssetCategory.MUTUAL_FUND -> FundEmeraldPrimary
    AssetCategory.GOLD -> GoldBronzePrimary
    AssetCategory.CRYPTO -> CryptoTealPrimary
}

fun categorySecondaryColor(category: AssetCategory?): Color = when (category) {
    null -> OverviewSlateBright
    AssetCategory.THAI_STOCK -> NavyBright
    AssetCategory.GLOBAL_STOCK -> GlobalIndigoBright
    AssetCategory.MUTUAL_FUND -> FundEmeraldBright
    AssetCategory.GOLD -> GoldBronzeBright
    AssetCategory.CRYPTO -> CryptoTealBright
}

fun categoryAccentColor(category: AssetCategory?): Color = when (category) {
    null -> OverviewSlatePrimary
    AssetCategory.THAI_STOCK -> NavyPrimary
    AssetCategory.GLOBAL_STOCK -> GlobalIndigoPrimary
    AssetCategory.MUTUAL_FUND -> FundEmeraldPrimary
    AssetCategory.GOLD -> GoldBronzePrimary
    AssetCategory.CRYPTO -> CryptoTealPrimary
}

fun categoryContainerColor(category: AssetCategory?): Color = when (category) {
    null -> OverviewSlateContainer
    AssetCategory.THAI_STOCK -> NavyContainer
    AssetCategory.GLOBAL_STOCK -> GlobalIndigoContainer
    AssetCategory.MUTUAL_FUND -> FundEmeraldContainer
    AssetCategory.GOLD -> GoldBronzeContainer
    AssetCategory.CRYPTO -> CryptoTealContainer
}

fun categoryOnContainerColor(category: AssetCategory?): Color = when (category) {
    null -> OverviewSlateOnContainer
    AssetCategory.THAI_STOCK -> NavyOnContainer
    AssetCategory.GLOBAL_STOCK -> GlobalIndigoOnContainer
    AssetCategory.MUTUAL_FUND -> FundEmeraldOnContainer
    AssetCategory.GOLD -> GoldBronzeOnContainer
    AssetCategory.CRYPTO -> CryptoTealOnContainer
}
