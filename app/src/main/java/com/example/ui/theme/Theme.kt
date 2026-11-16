package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AssetCategory

data class PortfolioSemanticColors(
    val profit: Color,
    val profitContainer: Color,
    val loss: Color,
    val lossContainer: Color,
    val accentHighlight: Color,
    val costBoxBg: Color,
    val costBoxBorder: Color,
    val costBoxLabel: Color
)

val LocalPortfolioColors = staticCompositionLocalOf {
    PortfolioSemanticColors(
        profit = ProfitGreenLight,
        profitContainer = ProfitGreenContainerLight,
        loss = LossRedLight,
        lossContainer = LossRedContainerLight,
        accentHighlight = GoldAccentTextLight,
        costBoxBg = CostBoxBg,
        costBoxBorder = CostBoxBorder,
        costBoxLabel = CostBoxLabelColor
    )
}

private val StockOrangeScheme = lightColorScheme(
    primary = StockOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = StockOrangeContainer,
    onPrimaryContainer = StockOrangeOnContainer,
    secondary = StockOrangeHighlight,
    onSecondary = Color.White,
    secondaryContainer = StockOrangeContainer,
    onSecondaryContainer = StockOrangeOnContainer,
    tertiary = SlateBlueTertiary,
    onTertiary = WhiteOnTertiary,
    tertiaryContainer = PaleSkyContainer,
    onTertiaryContainer = DeepSkyOnContainer,
    background = StockOrangeBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = StockOrangeSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = StockOrangeBorder,
    outlineVariant = StockOrangeBorder,
    error = LossRedLight,
    errorContainer = LossRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val GlobalStockPurpleScheme = lightColorScheme(
    primary = GlobalPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = GlobalPurpleContainer,
    onPrimaryContainer = GlobalPurpleOnContainer,
    secondary = GlobalPurpleHighlight,
    onSecondary = Color.White,
    secondaryContainer = GlobalPurpleContainer,
    onSecondaryContainer = GlobalPurpleOnContainer,
    tertiary = SlateBlueTertiary,
    onTertiary = WhiteOnTertiary,
    tertiaryContainer = PaleSkyContainer,
    onTertiaryContainer = DeepSkyOnContainer,
    background = GlobalPurpleBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = GlobalPurpleSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = GlobalPurpleBorder,
    outlineVariant = GlobalPurpleBorder,
    error = LossRedLight,
    errorContainer = LossRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val CryptoYellowScheme = lightColorScheme(
    primary = CryptoYellowPrimary,
    onPrimary = DarkInkOnYellow,
    primaryContainer = CryptoYellowContainer,
    onPrimaryContainer = CryptoYellowOnContainer,
    secondary = CryptoYellowHighlight,
    onSecondary = Color.White,
    secondaryContainer = CryptoYellowContainer,
    onSecondaryContainer = CryptoYellowOnContainer,
    tertiary = SlateBlueTertiary,
    onTertiary = WhiteOnTertiary,
    tertiaryContainer = PaleSkyContainer,
    onTertiaryContainer = DeepSkyOnContainer,
    background = CryptoYellowBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CryptoYellowSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = CryptoYellowBorder,
    outlineVariant = CryptoYellowBorder,
    error = LossRedLight,
    errorContainer = LossRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val GoldBlueScheme = lightColorScheme(
    primary = GoldBluePrimary,
    onPrimary = Color.White,
    primaryContainer = GoldBlueContainer,
    onPrimaryContainer = GoldBlueOnContainer,
    secondary = GoldBlueHighlight,
    onSecondary = Color.White,
    secondaryContainer = GoldBlueContainer,
    onSecondaryContainer = GoldBlueOnContainer,
    tertiary = WarmGoldSecondary,
    onTertiary = Color.White,
    tertiaryContainer = SoftYellowContainer,
    onTertiaryContainer = DeepAmberOnContainer,
    background = GoldBlueBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = GoldBlueSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = GoldBlueBorder,
    outlineVariant = GoldBlueBorder,
    error = LossRedLight,
    errorContainer = LossRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val FundGreenScheme = lightColorScheme(
    primary = FundGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = FundGreenContainer,
    onPrimaryContainer = FundGreenOnContainer,
    secondary = FundGreenHighlight,
    onSecondary = Color.White,
    secondaryContainer = FundGreenContainer,
    onSecondaryContainer = FundGreenOnContainer,
    tertiary = SlateBlueTertiary,
    onTertiary = WhiteOnTertiary,
    tertiaryContainer = PaleSkyContainer,
    onTertiaryContainer = DeepSkyOnContainer,
    background = FundGreenBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = FundGreenSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = FundGreenBorder,
    outlineVariant = FundGreenBorder,
    error = LossRedLight,
    errorContainer = LossRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val animSpec = tween<Color>(durationMillis = 280)
    val primary by animateColorAsState(target.primary, animSpec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, animSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, animSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, animSpec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, animSpec, label = "secondary")
    val background by animateColorAsState(target.background, animSpec, label = "background")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, animSpec, label = "surfaceVariant")
    val outline by animateColorAsState(target.outline, animSpec, label = "outline")
    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        background = background,
        surfaceVariant = surfaceVariant,
        outline = outline,
        outlineVariant = outline
    )
}

@Composable
fun PortPulseTheme(
    selectedCategory: AssetCategory = AssetCategory.THAI_STOCK,
    content: @Composable () -> Unit
) {
    val targetScheme = when (selectedCategory) {
        AssetCategory.THAI_STOCK -> StockOrangeScheme
        AssetCategory.GLOBAL_STOCK -> GlobalStockPurpleScheme
        AssetCategory.CRYPTO -> CryptoYellowScheme
        AssetCategory.GOLD -> GoldBlueScheme
        AssetCategory.MUTUAL_FUND -> FundGreenScheme
    }

    val targetHighlight = when (selectedCategory) {
        AssetCategory.THAI_STOCK -> StockOrangeHighlight
        AssetCategory.GLOBAL_STOCK -> GlobalPurpleHighlight
        AssetCategory.CRYPTO -> CryptoYellowHighlight
        AssetCategory.GOLD -> GoldBlueHighlight
        AssetCategory.MUTUAL_FUND -> FundGreenHighlight
    }

    val colorScheme = animateColorScheme(targetScheme)
    val animatedHighlight by animateColorAsState(
        targetValue = targetHighlight,
        animationSpec = tween(durationMillis = 280),
        label = "accentHighlight"
    )

    val semanticColors = PortfolioSemanticColors(
        profit = ProfitGreenLight,
        profitContainer = ProfitGreenContainerLight,
        loss = LossRedLight,
        lossContainer = LossRedContainerLight,
        accentHighlight = animatedHighlight,
        costBoxBg = CostBoxBg,
        costBoxBorder = CostBoxBorder,
        costBoxLabel = CostBoxLabelColor
    )

    CompositionLocalProvider(LocalPortfolioColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
