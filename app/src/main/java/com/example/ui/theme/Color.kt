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

// Financial Semantic Colors (High-contrast on Light White/Cream)
val ProfitGreenLight = Color(0xFF047857)
val ProfitGreenContainerLight = Color(0xFFD1FAE5)
val LossRedLight = Color(0xFFB91C1C)
val LossRedContainerLight = Color(0xFFFEE2E2)
val GoldAccentTextLight = Color(0xFF92400E)

// Distinct Cost-Basis Box Colors (Cool Slate so Cost vs Current Value stands out clearly)
val CostBoxBg = Color(0xFFF1F5F9)
val CostBoxBorder = Color(0xFF94A3B8)
val CostBoxLabelColor = Color(0xFF475569)

// High-contrast colorblind-friendly chart palette (every color is readable on white!)
val ChartPalette = listOf(
    Color(0xFFEA580C), // Deep Orange
    Color(0xFF7C3AED), // Royal Violet
    Color(0xFF1D4ED8), // Deep Royal Blue
    Color(0xFF15803D), // Deep Emerald Green
    Color(0xFFB45309), // Rich Amber Gold
    Color(0xFFBE123C), // Deep Rose
    Color(0xFF0F766E), // Deep Teal
    Color(0xFF4338CA), // Deep Indigo
    Color(0xFF92400E), // Dark Honey
    Color(0xFF4D7C0F)  // Deep Olive
)

fun categoryAccentColor(category: AssetCategory?): Color = when (category) {
    null -> GoldAccentTextLight
    AssetCategory.THAI_STOCK -> StockOrangePrimary
    AssetCategory.GLOBAL_STOCK -> GlobalPurplePrimary
    AssetCategory.CRYPTO -> CryptoYellowHighlight
    AssetCategory.GOLD -> GoldBluePrimary
    AssetCategory.MUTUAL_FUND -> FundGreenPrimary
}

fun categoryContainerColor(category: AssetCategory?): Color = when (category) {
    null -> SoftYellowContainer
    AssetCategory.THAI_STOCK -> StockOrangeContainer
    AssetCategory.GLOBAL_STOCK -> GlobalPurpleContainer
    AssetCategory.CRYPTO -> CryptoYellowContainer
    AssetCategory.GOLD -> GoldBlueContainer
    AssetCategory.MUTUAL_FUND -> FundGreenContainer
}

fun categoryOnContainerColor(category: AssetCategory?): Color = when (category) {
    null -> DeepAmberOnContainer
    AssetCategory.THAI_STOCK -> StockOrangeOnContainer
    AssetCategory.GLOBAL_STOCK -> GlobalPurpleOnContainer
    AssetCategory.CRYPTO -> CryptoYellowOnContainer
    AssetCategory.GOLD -> GoldBlueOnContainer
    AssetCategory.MUTUAL_FUND -> FundGreenOnContainer
}
