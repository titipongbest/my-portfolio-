package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzedHolding
import com.example.ui.FormatUtils
import com.example.ui.PortfolioUiState
import com.example.ui.components.DonutSlice
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleLarge
import com.example.ui.theme.NumericStyleMedium
import com.example.ui.theme.NumericStyleSmall

@Composable
fun DashboardTab(
    uiState: PortfolioUiState,
    onRefreshPrices: () -> Unit,
    onAddHoldingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategory = uiState.selectedCategory
    val summary = uiState.categorySummary
    var selectedDonutSliceId by remember(activeCategory) {
        mutableStateOf<String?>(null)
    }

    val displayedHoldings = uiState.filteredHoldings
    val displayedValue = summary.totalValueThb
    val displayedCost = summary.totalCostThb
    val displayedPnl = summary.totalProfitLossThb
    val displayedPnlPct = summary.totalProfitLossPercent

    val donutSlices = remember(displayedHoldings, displayedValue, uiState.managedSectors) {
        if (displayedHoldings.isEmpty() || displayedValue <= 0.0) {
            emptyList()
        } else {
            val top = displayedHoldings.take(5)
            val rest = displayedHoldings.drop(5)
            val slices = top.mapIndexed { index, item ->
                val pct = (item.currentValueThb / displayedValue) * 100.0
                DonutSlice(
                    id = "${item.holding.id}_${item.holding.symbol}_$index",
                    label = item.holding.symbol,
                    sublabel = buildString {
                        append(item.holding.assetCategory.shortLabelTh)
                        if (item.holding.assetCategory.isStock) {
                            append(" • ${item.holding.resolveSectorLabel(uiState.managedSectors)}")
                        }
                    },
                    valueThb = item.currentValueThb,
                    percent = pct,
                    color = ChartPalette[index % ChartPalette.size]
                )
            }.toMutableList()
            if (rest.isNotEmpty()) {
                val restVal = rest.sumOf { it.currentValueThb }
                val restPct = (restVal / displayedValue) * 100.0
                slices.add(
                    DonutSlice(
                        id = "others_group",
                        label = "อื่นๆ (${rest.size})",
                        sublabel = rest.joinToString(", ") { it.holding.symbol },
                        valueThb = restVal,
                        percent = restPct,
                        color = ChartPalette[8 % ChartPalette.size]
                    )
                )
            }
            slices
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Institutional Navy Hero Band + Floating White Summary & Allocation Card
        item {
            NavyOverlapHeroSection(
                categoryShortLabel = activeCategory.shortLabelTh,
                totalValueThb = displayedValue,
                totalCostThb = displayedCost,
                profitLossThb = displayedPnl,
                profitLossPercent = displayedPnlPct,
                usdToThbRate = summary.usdToThbRate,
                isLiveFxRate = summary.isLiveFxRate,
                lastUpdatedMillis = summary.lastUpdatedMillis,
                isRefreshing = uiState.isRefreshingPrices,
                onRefreshClick = onRefreshPrices,
                donutSlices = donutSlices,
                selectedDonutSliceId = selectedDonutSliceId,
                onSelectDonutSlice = { selectedDonutSliceId = it }
            )
        }

        if (uiState.allHoldings.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyPortfolioPrompt(
                        categoryLabel = activeCategory.labelTh,
                        onAddHoldingClick = onAddHoldingClick
                    )
                }
            }
        } else if (displayedHoldings.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ยังไม่มีสินทรัพย์ในหมวด ${activeCategory.labelTh}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "กดเพิ่มสินทรัพย์เพื่อเริ่มติดตามราคาและสัดส่วนในหมวดนี้",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onAddHoldingClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("empty_category_add_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("เพิ่ม${activeCategory.shortLabelTh}")
                            }
                        }
                    }
                }
            }
        } else {
            // 2. Section Header: Asset Summary Rows (matching institutional breakdown rows)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "รายละเอียด${activeCategory.shortLabelTh} (${displayedHoldings.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "เรียงตามมูลค่า",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalPortfolioColors.current.mutedSlate
                    )
                }
            }

            items(
                count = displayedHoldings.size,
                key = { idx ->
                    val h = displayedHoldings[idx].holding
                    if (h.id > 0L) h.id else -idx - 1L
                }
            ) { idx ->
                val item = displayedHoldings[idx]
                val stripeColor = ChartPalette[idx % ChartPalette.size]
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InstitutionalHoldingSummaryRow(
                        item = item,
                        sectorLabel = item.holding.resolveSectorLabel(uiState.managedSectors),
                        stripeColor = stripeColor,
                        isSelected = selectedDonutSliceId == "${item.holding.id}_${item.holding.symbol}_$idx",
                        onClick = {
                            if (idx < 5) {
                                val sliceId = "${item.holding.id}_${item.holding.symbol}_$idx"
                                selectedDonutSliceId = if (selectedDonutSliceId == sliceId) null else sliceId
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavyOverlapHeroSection(
    categoryShortLabel: String,
    totalValueThb: Double,
    totalCostThb: Double,
    profitLossThb: Double,
    profitLossPercent: Double,
    usdToThbRate: Double,
    isLiveFxRate: Boolean,
    lastUpdatedMillis: Long?,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    donutSlices: List<DonutSlice>,
    selectedDonutSliceId: String?,
    onSelectDonutSlice: (String?) -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val isGain = profitLossThb >= 0.0
    val pnlColor = if (isGain) portfolioColors.profit else portfolioColors.loss

    Box(modifier = Modifier.fillMaxWidth()) {
        // Deep Navy Top Gradient Backdrop (extends behind the upper edge of the white summary card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            portfolioColors.navyHeroDeep,
                            portfolioColors.navyHeroLight
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Utility Bar inside Hero: FX Rate + Sync Timestamp + Refresh Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "1 USD = ฿${String.format(java.util.Locale.US, "%.2f", usdToThbRate)}${if (isLiveFxRate) "" else "*"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.94f),
                        softWrap = false
                    )
                    Text(
                        text = FormatUtils.formatUpdatedTime(lastUpdatedMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isRefreshing) { onRefreshClick() }
                        .testTag("refresh_prices_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "ดึงราคาปัจจุบัน",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isRefreshing) "กำลังดึง..." else "อัปเดตราคา",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Centered Institutional Portfolio Valuation Header
            Text(
                text = "มูลค่าพอร์ต$categoryShortLabel",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.84f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = FormatUtils.formatNumber(totalValueThb, 2),
                    style = NumericStyleLarge.copy(fontSize = 30.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "บาท",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating White Institutional Summary Card (overlaps bottom of Navy Backdrop)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // 2x2 Institutional KPI Grid (Value | P&L THB, Cost | P&L %)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiMetricCell(
                            label = "มูลค่าพอร์ต",
                            value = FormatUtils.formatNumber(totalValueThb, 2),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCell(
                            label = "กำไร/ขาดทุน",
                            value = FormatUtils.formatNumber(profitLossThb, 2, showSign = true),
                            valueColor = pnlColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiMetricCell(
                            label = "เงินลงทุน (ต้นทุน)",
                            value = FormatUtils.formatNumber(totalCostThb, 2),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCell(
                            label = "%กำไร/ขาดทุน",
                            value = FormatUtils.formatPercent(profitLossPercent, decimals = 2, showSign = true),
                            valueColor = pnlColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (donutSlices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(14.dp))

                        InteractiveDonutChart(
                            slices = donutSlices,
                            centerTitle = categoryShortLabel,
                            centerValueText = FormatUtils.formatThbCompact(totalValueThb),
                            selectedSliceId = selectedDonutSliceId,
                            onSelectSlice = onSelectDonutSlice,
                            compactSideBySide = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalPortfolioColors.current.mutedSlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = NumericStyleMedium.copy(
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            ),
            softWrap = true,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InstitutionalHoldingSummaryRow(
    item: AnalyzedHolding,
    sectorLabel: String,
    stripeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val isGain = item.profitLossThb >= 0.0
    val pnlColor = if (isGain) portfolioColors.profit else portfolioColors.loss
    val h = item.holding

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) stripeColor else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = stripeColor,
                        topLeft = Offset.Zero,
                        size = Size(5.dp.toPx(), size.height)
                    )
                }
                .padding(start = 17.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Column(modifier = Modifier.weight(1.15f)) {
                    Text(
                        text = if (h.assetCategory.isStock) sectorLabel else h.assetCategory.shortLabelTh,
                        style = MaterialTheme.typography.labelSmall,
                        color = portfolioColors.mutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = h.symbol,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "มูลค่า (บาท)",
                        style = MaterialTheme.typography.labelSmall,
                        color = portfolioColors.mutedSlate,
                        maxLines = 1
                    )
                    Text(
                        text = FormatUtils.formatNumber(item.currentValueThb, 2),
                        style = NumericStyleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                }

                Column(
                    modifier = Modifier.weight(0.85f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "%กำไร/ขาดทุน",
                        style = MaterialTheme.typography.labelSmall,
                        color = portfolioColors.mutedSlate,
                        maxLines = 1
                    )
                    Text(
                        text = FormatUtils.formatPercent(item.profitLossPercent, decimals = 2, showSign = true),
                        style = NumericStyleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = pnlColor
                        ),
                        maxLines = 1
                    )
                }
            }
        }
}

@Composable
private fun EmptyPortfolioPrompt(
    categoryLabel: String,
    onAddHoldingClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = "พอร์ตว่าง",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "เริ่มต้นสร้างพอร์ต$categoryLabel",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "เลือกชนิดสินทรัพย์ด้านบน แล้วกดเพิ่มชื่อสินทรัพย์ จำนวนหน่วย และต้นทุน ระบบจะดึงราคาตลาดและวิเคราะห์สัดส่วนให้อัตโนมัติ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAddHoldingClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_add_holding_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "เพิ่มสินทรัพย์ตัวแรก",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
