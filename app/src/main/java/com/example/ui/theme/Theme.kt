package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val costBoxLabel: Color,
    val navyHeroDeep: Color = NavyHeaderDeep,
    val navyHeroLight: Color = NavyPrimary,
    val mutedSlate: Color = WealthSlateMuted,
    val categoryAccent: Color = NavyPrimary,
    val categoryContainer: Color = NavyContainer,
    val categoryOnContainer: Color = NavyOnContainer
)

val LocalPortfolioColors = staticCompositionLocalOf {
    PortfolioSemanticColors(
        profit = ProfitGreenLight,
        profitContainer = ProfitGreenContainerLight,
        loss = LossRedLight,
        lossContainer = LossRedContainerLight,
        accentHighlight = NavyPrimary,
        costBoxBg = CostBoxBg,
        costBoxBorder = CostBoxBorder,
        costBoxLabel = CostBoxLabelColor,
        navyHeroDeep = NavyHeaderDeep,
        navyHeroLight = NavyPrimary,
        mutedSlate = WealthSlateMuted,
        categoryAccent = NavyPrimary,
        categoryContainer = NavyContainer,
        categoryOnContainer = NavyOnContainer
    )
}

private val InstitutionalWealthScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyContainer,
    onPrimaryContainer = NavyOnContainer,
    secondary = NavyBright,
    onSecondary = Color.White,
    secondaryContainer = NavyContainer,
    onSecondaryContainer = NavyOnContainer,
    tertiary = SlateBlueTertiary,
    onTertiary = WhiteOnTertiary,
    tertiaryContainer = PaleSkyContainer,
    onTertiaryContainer = DeepSkyOnContainer,
    background = WealthCanvasBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = WealthSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = WealthHairlineBorder,
    outlineVariant = WealthHairlineBorder,
    error = LossRedLight,
    errorContainer = LossRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PortPulseTheme(
    selectedCategory: AssetCategory = AssetCategory.THAI_STOCK,
    content: @Composable () -> Unit
) {
    val targetHeroDeep = categoryHeroDeepColor(selectedCategory)
    val targetHeroLight = categoryHeroLightColor(selectedCategory)
    val targetSecondary = categorySecondaryColor(selectedCategory)
    val targetCatAccent = categoryAccentColor(selectedCategory)
    val targetCatContainer = categoryContainerColor(selectedCategory)
    val targetCatOnContainer = categoryOnContainerColor(selectedCategory)

    val animSpec = tween<Color>(durationMillis = 260)
    val animatedHeroDeep by animateColorAsState(targetHeroDeep, animSpec, label = "heroDeep")
    val animatedHeroLight by animateColorAsState(targetHeroLight, animSpec, label = "heroLight")
    val animatedSecondary by animateColorAsState(targetSecondary, animSpec, label = "secondary")
    val animatedCatAccent by animateColorAsState(targetCatAccent, animSpec, label = "catAccent")
    val animatedCatContainer by animateColorAsState(targetCatContainer, animSpec, label = "catContainer")
    val animatedCatOnContainer by animateColorAsState(targetCatOnContainer, animSpec, label = "catOnContainer")

    val colorScheme = InstitutionalWealthScheme.copy(
        primary = animatedCatAccent,
        secondary = animatedSecondary,
        primaryContainer = animatedCatContainer,
        onPrimaryContainer = animatedCatOnContainer,
        secondaryContainer = animatedCatContainer,
        onSecondaryContainer = animatedCatOnContainer
    )

    val semanticColors = PortfolioSemanticColors(
        profit = ProfitGreenLight,
        profitContainer = ProfitGreenContainerLight,
        loss = LossRedLight,
        lossContainer = LossRedContainerLight,
        accentHighlight = animatedCatAccent,
        costBoxBg = CostBoxBg,
        costBoxBorder = CostBoxBorder,
        costBoxLabel = CostBoxLabelColor,
        navyHeroDeep = animatedHeroDeep,
        navyHeroLight = animatedHeroLight,
        mutedSlate = WealthSlateMuted,
        categoryAccent = animatedCatAccent,
        categoryContainer = animatedCatContainer,
        categoryOnContainer = animatedCatOnContainer
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
