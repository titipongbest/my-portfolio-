package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.FormatUtils
import com.example.ui.PortfolioUiState
import com.example.ui.components.DonutSlice
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleLarge
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
            val top = displayedHoldings.take(8)
            val rest = displayedHoldings.drop(8)
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
                        label = "อื่นๆ (${rest.size} รายการ)",
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Portfolio Value Card (Strictly Active Category)
        item {
            HeroPortfolioCard(
                filterLabel = activeCategory.labelTh,
                totalValueThb = displayedValue,
                totalCostThb = displayedCost,
                profitLossThb = displayedPnl,
                profitLossPercent = displayedPnlPct,
                usdToThbRate = summary.usdToThbRate,
                isLiveFxRate = summary.isLiveFxRate,
                lastUpdatedMillis = summary.lastUpdatedMillis,
                isRefreshing = uiState.isRefreshingPrices,
                onRefreshClick = onRefreshPrices
            )
        }

        if (uiState.allHoldings.isEmpty()) {
            item {
                EmptyPortfolioPrompt(
                    categoryLabel = activeCategory.labelTh,
                    onAddHoldingClick = onAddHoldingClick
                )
            }
        } else if (displayedHoldings.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ยังไม่มีสินทรัพย์ในหมวด ${activeCategory.labelTh}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "กดเพิ่มสินทรัพย์เพื่อเริ่มติดตามราคาและสัดส่วนในหมวดนี้",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddHoldingClick,
                            modifier = Modifier.testTag("empty_category_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("เพิ่ม${activeCategory.shortLabelTh}")
                        }
                    }
                }
            }
        } else {
            // 2. Category Allocation Donut Chart
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "สัดส่วนในหมวด${activeCategory.shortLabelTh}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "แตะที่กราฟหรือรายการเพื่อดูสัดส่วน % ในหมวดนี้",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        InteractiveDonutChart(
                            slices = donutSlices,
                            centerTitle = activeCategory.shortLabelTh,
                            centerValueText = FormatUtils.formatThbCompact(displayedValue),
                            centerSubText = "${displayedHoldings.size} รายการ",
                            selectedSliceId = selectedDonutSliceId,
                            onSelectSlice = { selectedDonutSliceId = it }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun HeroPortfolioCard(
    filterLabel: String,
    totalValueThb: Double,
    totalCostThb: Double,
    profitLossThb: Double,
    profitLossPercent: Double,
    usdToThbRate: Double,
    isLiveFxRate: Boolean,
    lastUpdatedMillis: Long?,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val isGain = profitLossThb >= 0.0
    val pnlColor = if (isGain) portfolioColors.profit else portfolioColors.loss
    val pnlBg = if (isGain) portfolioColors.profitContainer else portfolioColors.lossContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filterLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${FormatUtils.formatUpdatedTime(lastUpdatedMillis)} • 1 USD = ฿${String.format(java.util.Locale.US, "%.2f", usdToThbRate)}${if (isLiveFxRate) "" else "*"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isRefreshing) { onRefreshClick() }
                        .testTag("refresh_prices_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "ดึงราคาปัจจุบัน",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshing) "กำลังดึง..." else "อัปเดตราคา",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = FormatUtils.formatThb(totalValueThb),
                style = NumericStyleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = pnlBg,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGain) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = if (isGain) "กำไร" else "ขาดทุน",
                            tint = pnlColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${FormatUtils.formatThb(profitLossThb, showSign = true)} (${FormatUtils.formatPercent(profitLossPercent, showSign = true)})",
                            style = NumericStyleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ต้นทุนรวม",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatThb(totalCostThb),
                        style = NumericStyleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = "พอร์ตว่าง",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "เริ่มต้นสร้างพอร์ต$categoryLabel",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "สลับหมวดหมู่ด้านบนได้ตามต้องการ แล้วกดเพิ่มชื่อสินทรัพย์ จำนวนหน่วย และราคาที่ได้มา ระบบจะดึงราคาตลาดและวิเคราะห์สัดส่วนให้อัตโนมัติ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddHoldingClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
