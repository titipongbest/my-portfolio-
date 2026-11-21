package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzedHolding
import com.example.model.AssetCategory
import com.example.model.CategorySlice
import com.example.model.ManagedSector
import com.example.ui.FormatUtils
import com.example.ui.PortfolioUiState
import com.example.ui.components.DonutSlice
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleLarge
import com.example.ui.theme.NumericStyleMedium
import com.example.ui.theme.NumericStyleSmall
import com.example.ui.theme.categoryAccentColor
import com.example.ui.theme.categoryContainerColor
import com.example.ui.theme.categoryOnContainerColor
import java.util.Locale

private data class OverviewCategoryRowModel(
    val category: AssetCategory,
    val slice: CategorySlice?,
    val holdings: List<AnalyzedHolding>,
    val targetPercent: Double,
    val note: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewTab(
    uiState: PortfolioUiState,
    onRefreshPrices: () -> Unit,
    onToggleFilterCategory: (AssetCategory) -> Unit,
    onSelectAllFilters: () -> Unit,
    onOpenCategory: (AssetCategory) -> Unit,
    onOpenPlanTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val portfolioColors = LocalPortfolioColors.current
    val summary = uiState.overviewSummary
    val activeFilters = uiState.overviewFilterCategories
    val isSubsetFiltered = activeFilters.size < AssetCategory.entries.size
    val plan = uiState.portfolioPlan

    var selectedSliceId by remember(activeFilters) { mutableStateOf<String?>(null) }
    var expandedCategories by remember { mutableStateOf<Set<AssetCategory>>(emptySet()) }

    val sliceByCategory = remember(summary.categorySlices) {
        summary.categorySlices.associateBy { it.category }
    }
    val holdingsByCategory = remember(summary.analyzedHoldings) {
        summary.analyzedHoldings.groupBy { it.holding.assetCategory }
    }

    val donutSlices = remember(summary.categorySlices) {
        summary.categorySlices
            .filter { it.totalValueThb > 0.0 }
            .map { slice ->
                DonutSlice(
                    id = slice.category.name,
                    label = slice.category.shortLabelTh,
                    sublabel = "${slice.holdingCount} รายการ",
                    valueThb = slice.totalValueThb,
                    percent = slice.percentOfPortfolio,
                    color = categoryAccentColor(slice.category)
                )
            }
    }

    val categoryRows = remember(activeFilters, sliceByCategory, holdingsByCategory, plan) {
        AssetCategory.entries
            .filter { it in activeFilters }
            .map { cat ->
                OverviewCategoryRowModel(
                    category = cat,
                    slice = sliceByCategory[cat],
                    holdings = holdingsByCategory[cat].orEmpty(),
                    targetPercent = plan.targetPercentFor(cat),
                    note = plan.noteFor(cat)
                )
            }
            .sortedWith(
                compareByDescending<OverviewCategoryRowModel> { it.slice?.totalValueThb ?: 0.0 }
                    .thenByDescending { it.targetPercent }
                    .thenBy { it.category.ordinal }
            )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("overview_lazy_column"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Executive Slate Hero + Floating Combined Summary & Allocation Donut Card
        item {
            OverviewHeroSection(
                isSubsetFiltered = isSubsetFiltered,
                selectedFilterCount = activeFilters.size,
                totalValueThb = summary.totalValueThb,
                totalCostThb = summary.totalCostThb,
                profitLossThb = summary.totalProfitLossThb,
                profitLossPercent = summary.totalProfitLossPercent,
                usdToThbRate = summary.usdToThbRate,
                isLiveFxRate = summary.isLiveFxRate,
                lastUpdatedMillis = summary.lastUpdatedMillis,
                isRefreshing = uiState.isRefreshingPrices,
                onRefreshClick = onRefreshPrices,
                donutSlices = donutSlices,
                selectedDonutSliceId = selectedSliceId,
                onSelectDonutSlice = { clickedId ->
                    selectedSliceId = clickedId
                    if (clickedId != null) {
                        AssetCategory.entries.find { it.name == clickedId }?.let { cat ->
                            expandedCategories = expandedCategories + cat
                        }
                    }
                }
            )
        }

        // 2. Asset Category Filter Card
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "ตัวกรองชนิดสินทรัพย์",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ตัวกรองชนิดสินทรัพย์ (${activeFilters.size}/${AssetCategory.entries.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSubsetFiltered) {
                                TextButton(
                                    onClick = onSelectAllFilters,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.testTag("overview_filter_all")
                                ) {
                                    Text(
                                        text = "เลือกทั้งหมด",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssetCategory.entries.forEach { cat ->
                                val isSelected = cat in activeFilters
                                val count = uiState.allHoldings.count { it.assetCategory == cat }
                                val catAccent = categoryAccentColor(cat)
                                val catContainer = categoryContainerColor(cat)
                                val catOnContainer = categoryOnContainerColor(cat)

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleFilterCategory(cat) },
                                    label = {
                                        Text(
                                            text = "${cat.shortLabelTh} ($count)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(catAccent.copy(alpha = 0.55f))
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = catContainer,
                                        selectedLabelColor = catOnContainer,
                                        selectedLeadingIconColor = catAccent,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = portfolioColors.mutedSlate
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = catAccent.copy(alpha = 0.65f)
                                    ),
                                    modifier = Modifier.testTag("overview_filter_${cat.name}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Section Header: Asset Category Allocation & Details
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "สัดส่วนแต่ละชนิดสินทรัพย์",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "แตะเพื่อดูรายละเอียด",
                    style = MaterialTheme.typography.labelSmall,
                    color = portfolioColors.mutedSlate
                )
            }
        }

        if (uiState.allHoldings.isEmpty()) {
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
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ยังไม่มีสินทรัพย์ในพอร์ต",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "เลือกชนิดสินทรัพย์ด้านบนเพื่อเริ่มเพิ่มรายการ หรือตั้งสัดส่วนเป้าหมายล่วงหน้าได้ที่หน้าแผน",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onOpenPlanTab,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ตั้งแผนการลงทุน")
                                }
                                Button(
                                    onClick = { onOpenCategory(AssetCategory.THAI_STOCK) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("ไปหน้าหุ้นไทย")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = categoryRows,
                key = { it.category.name }
            ) { rowModel ->
                val isExpanded = rowModel.category in expandedCategories
                val isHighlighted = selectedSliceId == rowModel.category.name
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OverviewCategoryBreakdownCard(
                        model = rowModel,
                        managedSectors = uiState.managedSectors,
                        isExpanded = isExpanded,
                        isHighlighted = isHighlighted,
                        onToggleExpand = {
                            expandedCategories = if (isExpanded) {
                                expandedCategories - rowModel.category
                            } else {
                                expandedCategories + rowModel.category
                            }
                            selectedSliceId = if (isExpanded) null else rowModel.category.name
                        },
                        onOpenCategory = { onOpenCategory(rowModel.category) }
                    )
                }
            }
        }

        // 4. Overall Plan Note Quick Preview (if user has saved a plan note)
        if (plan.overallNote.isNotBlank()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenPlanTab() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "โน้ตแผนการลงทุน",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "แก้ไขแผน",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = plan.overallNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewHeroSection(
    isSubsetFiltered: Boolean,
    selectedFilterCount: Int,
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
                        text = "1 USD = ฿${String.format(Locale.US, "%.2f", usdToThbRate)}${if (isLiveFxRate) "" else "*"}",
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
                        .testTag("overview_refresh_prices_button")
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

            Text(
                text = if (isSubsetFiltered) {
                    "มูลค่าพอร์ตที่กรอง ($selectedFilterCount/${AssetCategory.entries.size} ชนิดสินทรัพย์)"
                } else {
                    "มูลค่าพอร์ตรวมทุกสินทรัพย์"
                },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OverviewKpiCell(
                            label = "มูลค่าพอร์ต",
                            value = FormatUtils.formatNumber(totalValueThb, 2),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewKpiCell(
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
                        OverviewKpiCell(
                            label = "เงินลงทุน (ต้นทุน)",
                            value = FormatUtils.formatNumber(totalCostThb, 2),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewKpiCell(
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
                            centerTitle = if (isSubsetFiltered) "ที่กรอง" else "รวมพอร์ต",
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
private fun OverviewKpiCell(
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
private fun OverviewCategoryBreakdownCard(
    model: OverviewCategoryRowModel,
    managedSectors: List<ManagedSector>,
    isExpanded: Boolean,
    isHighlighted: Boolean,
    onToggleExpand: () -> Unit,
    onOpenCategory: () -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val cat = model.category
    val catAccent = categoryAccentColor(cat)
    val catContainer = categoryContainerColor(cat)
    val catOnContainer = categoryOnContainerColor(cat)
    val slice = model.slice
    val valueThb = slice?.totalValueThb ?: 0.0
    val costThb = slice?.totalCostThb ?: 0.0
    val pnlThb = slice?.profitLossThb ?: 0.0
    val pnlPct = slice?.profitLossPercent ?: 0.0
    val actualPct = slice?.percentOfPortfolio ?: 0.0
    val count = slice?.holdingCount ?: 0
    val isGain = pnlThb >= 0.0
    val pnlColor = if (count == 0) portfolioColors.mutedSlate else if (isGain) portfolioColors.profit else portfolioColors.loss

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isExpanded || isHighlighted) 3.dp else 1.dp,
        border = BorderStroke(
            width = if (isExpanded || isHighlighted) 1.5.dp else 1.dp,
            color = if (isExpanded || isHighlighted) catAccent else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleExpand() }
            .testTag("overview_category_card_${cat.name}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = catAccent,
                        topLeft = Offset.Zero,
                        size = Size(5.dp.toPx(), size.height)
                    )
                }
                .padding(start = 17.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(catContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = overviewCategoryIcon(cat),
                        contentDescription = cat.labelTh,
                        tint = catOnContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cat.labelTh,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (model.targetPercent > 0.0) {
                            "$count รายการ • เป้า ${FormatUtils.formatPercent(model.targetPercent, decimals = 1)}"
                        } else {
                            "$count รายการ"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = portfolioColors.mutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = catContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatPercent(actualPct, decimals = 1),
                            style = NumericStyleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = catOnContainer
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatThb(valueThb, decimals = 0),
                        style = NumericStyleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "ย่อรายละเอียด" else "แสดงรายละเอียด",
                    tint = portfolioColors.mutedSlate,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Proportional Allocation Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val fillFraction = (actualPct / 100.0).toFloat().coerceIn(0f, 1f)
                    if (fillFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fillFraction)
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(catAccent)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (count > 0) {
                            "กำไร/ขาดทุน ${FormatUtils.formatNumber(pnlThb, 2, showSign = true)} (${FormatUtils.formatPercent(pnlPct, decimals = 2, showSign = true)})"
                        } else {
                            "ยังไม่มีสินทรัพย์ในหมวดนี้"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = pnlColor,
                        softWrap = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    if (model.targetPercent > 0.0) {
                        val diff = actualPct - model.targetPercent
                        val diffColor = when {
                            kotlin.math.abs(diff) <= 0.5 -> portfolioColors.profit
                            diff < 0 -> MaterialTheme.colorScheme.primary
                            else -> portfolioColors.loss
                        }
                        Text(
                            text = "ต่าง ${FormatUtils.formatPercent(diff, decimals = 1, showSign = true)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = diffColor,
                            softWrap = false
                        )
                    }
                }
            }

            // Expandable Asset Category Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overview_category_detail_${cat.name}"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // Category Summary Metrics Box
                    Surface(
                        color = portfolioColors.costBoxBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, portfolioColors.costBoxBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ต้นทุนรวม",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = portfolioColors.costBoxLabel
                                )
                                Text(
                                    text = FormatUtils.formatThb(costThb, decimals = 2),
                                    style = NumericStyleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "มูลค่าปัจจุบัน",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = portfolioColors.costBoxLabel
                                )
                                Text(
                                    text = FormatUtils.formatThb(valueThb, decimals = 2),
                                    style = NumericStyleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (model.note.isNotBlank()) {
                        Surface(
                            color = catContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = catOnContainer,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "โน้ตแผน: ${model.note}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catOnContainer
                                )
                            }
                        }
                    }

                    if (model.holdings.isEmpty()) {
                        Text(
                            text = "ยังไม่มีรายการถือครองในหมวด ${cat.labelTh}",
                            style = MaterialTheme.typography.bodySmall,
                            color = portfolioColors.mutedSlate
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "รายการสินทรัพย์ในหมวด (${model.holdings.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            model.holdings.forEach { item ->
                                OverviewHoldingDetailRow(
                                    item = item,
                                    categoryTotalValue = valueThb,
                                    sectorLabel = item.holding.resolveSectorLabel(managedSectors),
                                    accentColor = catAccent
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenCategory,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, catAccent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("overview_open_category_${cat.name}")
                    ) {
                        Text(
                            text = "เปิดจัดการหน้า${cat.shortLabelTh}",
                            color = catAccent,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = catAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewHoldingDetailRow(
    item: AnalyzedHolding,
    categoryTotalValue: Double,
    sectorLabel: String,
    accentColor: Color
) {
    val portfolioColors = LocalPortfolioColors.current
    var showExtra by remember(item.holding.id, item.holding.symbol) { mutableStateOf(false) }
    val h = item.holding
    val isGain = item.profitLossThb >= 0.0
    val pnlColor = if (isGain) portfolioColors.profit else portfolioColors.loss
    val shareInCategory = if (categoryTotalValue > 0.0) {
        (item.currentValueThb / categoryTotalValue) * 100.0
    } else 0.0

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showExtra = !showExtra }
            .testTag("overview_holding_row_${h.symbol}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = h.symbol,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(h.displayName)
                            if (h.assetCategory.isStock && sectorLabel != "-") {
                                append(" • ")
                                append(sectorLabel)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = portfolioColors.mutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(0.95f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = FormatUtils.formatThb(item.currentValueThb, decimals = 2),
                        style = NumericStyleSmall.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "${FormatUtils.formatPercent(item.profitLossPercent, decimals = 2, showSign = true)} • ${FormatUtils.formatPercent(item.allocationPercentOfTotal, decimals = 1)}",
                        style = NumericStyleSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pnlColor
                        ),
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(visible = showExtra) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "จำนวน: ${FormatUtils.formatQuantity(h.quantity)} หน่วย",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "สัดส่วนในหมวด: ${FormatUtils.formatPercent(shareInCategory, decimals = 1)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ต้นทุน: ${FormatUtils.formatThb(item.costBasisThb, decimals = 2)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "กำไร/ขาดทุน: ${FormatUtils.formatNumber(item.profitLossThb, 2, showSign = true)} ฿",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = pnlColor
                        )
                    }
                }
            }
        }
    }
}

internal fun overviewCategoryIcon(category: AssetCategory): ImageVector = when (category) {
    AssetCategory.THAI_STOCK -> Icons.AutoMirrored.Filled.ShowChart
    AssetCategory.GLOBAL_STOCK -> Icons.Default.Public
    AssetCategory.MUTUAL_FUND -> Icons.Default.AccountBalance
    AssetCategory.GOLD -> Icons.Default.Diamond
    AssetCategory.CRYPTO -> Icons.Default.CurrencyBitcoin
}
