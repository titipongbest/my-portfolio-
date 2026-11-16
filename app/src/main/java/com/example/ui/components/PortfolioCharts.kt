package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.FormatUtils
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleMedium
import com.example.ui.theme.NumericStyleSmall
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

data class DonutSlice(
    val id: String,
    val label: String,
    val sublabel: String,
    val valueThb: Double,
    val percent: Double,
    val color: Color
)

@Composable
fun InteractiveDonutChart(
    slices: List<DonutSlice>,
    centerTitle: String,
    centerValueText: String,
    centerSubText: String? = null,
    selectedSliceId: String?,
    onSelectSlice: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showLegendList: Boolean = true
) {
    val totalValue = slices.sumOf { it.valueThb }.coerceAtLeast(1e-9)
    val selected = slices.firstOrNull { it.id == selectedSliceId }

    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "donut_sweep"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .semantics { contentDescription = "$centerTitle $centerValueText" },
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices, selectedSliceId) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy)
                            val outerR = minOf(size.width, size.height) / 2f
                            val innerR = outerR * 0.52f
                            if (dist < innerR || dist > outerR) {
                                onSelectSlice(null)
                                return@detectTapGestures
                            }
                            var angle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI + 90.0)
                            if (angle < 0) angle += 360.0

                            var cumulative = 0.0
                            for (slice in slices) {
                                val sweep = (slice.valueThb / totalValue) * 360.0
                                if (angle >= cumulative && angle <= cumulative + sweep) {
                                    onSelectSlice(if (selectedSliceId == slice.id) null else slice.id)
                                    break
                                }
                                cumulative += sweep
                            }
                        }
                    }
            ) {
                val strokeBase = size.minDimension * 0.16f
                val padding = strokeBase / 2f + 12.dp.toPx()
                val diameter = size.minDimension - padding * 2f
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeBase)
                )

                var startAngle = -90f
                val gapDegrees = if (slices.size > 1) 2.2f else 0f

                slices.forEach { slice ->
                    val rawSweep = ((slice.valueThb / totalValue) * 360.0).toFloat() * sweepProgress
                    val drawSweep = (rawSweep - gapDegrees).coerceAtLeast(1.2f)
                    val isSelected = slice.id == selectedSliceId
                    val alpha = if (selectedSliceId == null || isSelected) 1f else 0.35f
                    val strokeWidth = if (isSelected) strokeBase * 1.24f else strokeBase

                    drawArc(
                        color = slice.color.copy(alpha = alpha),
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += rawSweep
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 34.dp)
            ) {
                Text(
                    text = selected?.label ?: centerTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (selected != null) {
                        FormatUtils.formatPercent(selected.percent, decimals = 1)
                    } else {
                        centerValueText
                    },
                    style = NumericStyleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = selected?.color ?: MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                val sub = selected?.let { FormatUtils.formatThbCompact(it.valueThb) } ?: centerSubText
                if (!sub.isNullOrBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showLegendList) {
            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Legend List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val isSelected = slice.id == selectedSliceId
                    val bg = if (isSelected) {
                        slice.color.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    }
                    val border = if (isSelected) slice.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(1.2.dp, border, RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectSlice(if (isSelected) null else slice.id)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("donut_legend_${slice.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = slice.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (slice.sublabel.isNotBlank()) {
                                Text(
                                    text = slice.sublabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatPercent(slice.percent, decimals = 1),
                                style = NumericStyleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = slice.color
                                )
                            )
                            Text(
                                text = FormatUtils.formatThbCompact(slice.valueThb),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalStackedAllocationBar(
    segments: List<Pair<Color, Double>>,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.second }.coerceAtLeast(1e-9)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(trackColor)
    ) {
        var currentX = 0f
        val gap = if (segments.size > 1) 2.dp.toPx() else 0f
        segments.forEachIndexed { index, (color, value) ->
            val ratio = (value / total).toFloat().coerceIn(0f, 1f)
            val width = (size.width * ratio - if (index < segments.lastIndex) gap else 0f)
                .coerceAtLeast(0f)
            if (width > 0f) {
                drawRect(
                    color = color,
                    topLeft = Offset(currentX, 0f),
                    size = Size(width, size.height)
                )
            }
            currentX += size.width * ratio
        }
    }
}

@Composable
fun ProfitLossBarRow(
    symbol: String,
    subtitle: String,
    profitLossThb: Double,
    profitLossPercent: Double,
    maxAbsPercent: Double,
    modifier: Modifier = Modifier
) {
    val portfolioColors = LocalPortfolioColors.current
    val isPositive = profitLossThb >= 0.0
    val barColor = if (isPositive) portfolioColors.profit else portfolioColors.loss
    val ratio = (kotlin.math.abs(profitLossPercent) / maxAbsPercent.coerceAtLeast(1.0))
        .toFloat()
        .coerceIn(0.04f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatPercent(profitLossPercent, showSign = true),
                    style = NumericStyleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                )
                Text(
                    text = FormatUtils.formatThb(profitLossThb, showSign = true),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = barColor
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}
