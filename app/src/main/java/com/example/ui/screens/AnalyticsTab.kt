package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SectorSlice
import com.example.ui.FormatUtils
import com.example.ui.PortfolioUiState
import com.example.ui.components.DonutSlice
import com.example.ui.components.HorizontalStackedAllocationBar
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleSmall

@Composable
fun AnalyticsTab(
    uiState: PortfolioUiState,
    onManageSectorsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategory = uiState.selectedCategory
    val summary = uiState.categorySummary
    val portfolioColors = LocalPortfolioColors.current
    var selectedSliceId by remember(activeCategory) { mutableStateOf<String?>(null) }
    var expandedSectorId by remember(activeCategory) { mutableStateOf<String?>(null) }

    val sectorDonutSlices = remember(summary.stockSectorSlices) {
        summary.stockSectorSlices.mapIndexed { idx, s ->
            DonutSlice(
                id = s.sectorId,
                label = s.shortLabelTh,
                sublabel = "${s.holdings.size} ตัว • ${FormatUtils.formatThbCompact(s.totalValueThb)}",
                valueThb = s.totalValueThb,
                percent = s.percentOfTotalPortfolio,
                color = ChartPalette[idx % ChartPalette.size]
            )
        }
    }

    val holdingDonutSlices = remember(summary.analyzedHoldings, summary.totalValueThb) {
        val total = summary.totalValueThb
        if (summary.analyzedHoldings.isEmpty() || total <= 0.0) {
            emptyList()
        } else {
            val top = summary.analyzedHoldings.take(8)
            val rest = summary.analyzedHoldings.drop(8)
            val slices = top.mapIndexed { idx, item ->
                DonutSlice(
                    id = "${item.holding.id}_${item.holding.symbol}_$idx",
                    label = item.holding.symbol,
                    sublabel = item.holding.displayName,
                    valueThb = item.currentValueThb,
                    percent = item.allocationPercentOfTotal,
                    color = ChartPalette[idx % ChartPalette.size]
                )
            }.toMutableList()
            if (rest.isNotEmpty()) {
                val restVal = rest.sumOf { it.currentValueThb }
                slices.add(
                    DonutSlice(
                        id = "others_category_group",
                        label = "อื่นๆ (${rest.size} รายการ)",
                        sublabel = rest.joinToString(", ") { it.holding.symbol },
                        valueThb = restVal,
                        percent = (restVal / total) * 100.0,
                        color = ChartPalette[8 % ChartPalette.size]
                    )
                )
            }
            slices
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (summary.analyzedHoldings.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "ไม่มีข้อมูลวิเคราะห์",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ยังไม่มีข้อมูลในหมวด${activeCategory.labelTh}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeCategory.isStock) {
                                "เพิ่ม${activeCategory.shortLabelTh} เพื่อดูกราฟสัดส่วนกลุ่มอุตสาหกรรมในหมวดนี้"
                            } else {
                                "เพิ่มสินทรัพย์ในหมวด${activeCategory.shortLabelTh} เพื่อดูกราฟสัดส่วนรายตัวในหมวดนี้"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activeCategory.isStock) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onManageSectorsClick() }
                                    .testTag("manage_sectors_empty_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "จัดการกลุ่มอุตสาหกรรม",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "จัดการกลุ่มอุตสาหกรรม (${uiState.managedSectors.size} กลุ่ม)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 1. Category-Scoped Proportion Chart
            if (activeCategory.isStock) {
                // Stock Sector Breakdown strictly within active stock category (sums to 100%)
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = "กลุ่มอุตสาหกรรมหุ้น",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "กลุ่มอุตสาหกรรมใน${activeCategory.shortLabelTh}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "พบ ${summary.stockSectorSlices.size} กลุ่มในหมวดนี้ (ตั้งค่าไว้ ${uiState.managedSectors.size} กลุ่ม)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onManageSectorsClick() }
                                        .testTag("manage_sectors_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "จัดการกลุ่มอุตสาหกรรม",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "จัดการกลุ่ม",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalStackedAllocationBar(
                                segments = summary.stockSectorSlices.mapIndexed { idx, s ->
                                    ChartPalette[idx % ChartPalette.size] to s.totalValueThb
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            InteractiveDonutChart(
                                slices = sectorDonutSlices,
                                centerTitle = activeCategory.shortLabelTh,
                                centerValueText = FormatUtils.formatThbCompact(summary.totalValueThb),
                                centerSubText = "${summary.stockSectorSlices.size} กลุ่มอุตสาหกรรม",
                                selectedSliceId = selectedSliceId,
                                onSelectSlice = { id ->
                                    selectedSliceId = id
                                    expandedSectorId = id
                                },
                                showLegendList = false
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "กลุ่มธุรกิจในหมวดนี้ (แตะเพื่อดูชื่อหุ้นในกลุ่ม)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            summary.stockSectorSlices.forEachIndexed { idx, sectorSlice ->
                                SectorDetailExpandableCard(
                                    slice = sectorSlice,
                                    categoryShortLabel = activeCategory.shortLabelTh,
                                    accentColor = ChartPalette[idx % ChartPalette.size],
                                    isExpanded = expandedSectorId == sectorSlice.sectorId,
                                    onToggleExpand = {
                                        val next = if (expandedSectorId == sectorSlice.sectorId) {
                                            null
                                        } else {
                                            sectorSlice.sectorId
                                        }
                                        expandedSectorId = next
                                        selectedSliceId = next
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } else {
                // Non-stock modes (Mutual Fund, Gold, Crypto): Proportion Chart strictly inside this category
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = "สัดส่วนหมวด${activeCategory.shortLabelTh}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "สัดส่วนภายในหมวด${activeCategory.shortLabelTh}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "วิเคราะห์สัดส่วนสินทรัพย์ ${summary.analyzedHoldings.size} รายการในหมวดนี้ (100%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalStackedAllocationBar(
                                segments = holdingDonutSlices.map { slice ->
                                    slice.color to slice.valueThb
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            InteractiveDonutChart(
                                slices = holdingDonutSlices,
                                centerTitle = activeCategory.shortLabelTh,
                                centerValueText = FormatUtils.formatThbCompact(summary.totalValueThb),
                                centerSubText = "${summary.analyzedHoldings.size} รายการ",
                                selectedSliceId = selectedSliceId,
                                onSelectSlice = { selectedSliceId = it }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun SectorDetailExpandableCard(
    slice: SectorSlice,
    categoryShortLabel: String,
    accentColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val isGain = slice.profitLossThb >= 0.0
    val pnlColor = if (isGain) portfolioColors.profit else portfolioColors.loss

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isExpanded) 1.5.dp else 1.dp,
                color = if (isExpanded) accentColor else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggleExpand() }
            .padding(12.dp)
            .testTag("sector_card_${slice.sectorId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slice.labelTh,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${slice.holdings.size} หุ้น • มูลค่า ${FormatUtils.formatThbCompact(slice.totalValueThb)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatPercent(slice.percentOfTotalPortfolio, decimals = 1),
                    style = NumericStyleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
                Text(
                    text = FormatUtils.formatPercent(slice.profitLossPercent, showSign = true),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = pnlColor
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "ย่อ" else "ขยาย",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                slice.holdings.forEach { item ->
                    val itemGain = item.profitLossThb >= 0.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.holding.symbol} — ${item.holding.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${FormatUtils.formatQuantity(item.holding.quantity)} หุ้น • สัดส่วน ${FormatUtils.formatPercent(item.allocationPercentOfTotal, decimals = 1)} ของ$categoryShortLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatThbCompact(item.currentValueThb),
                                style = NumericStyleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = FormatUtils.formatThb(item.profitLossThb, showSign = true),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (itemGain) portfolioColors.profit else portfolioColors.loss
                            )
                        }
                    }
                }
            }
        }
    }
}
