package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuoteSource
import com.example.model.AnalyzedHolding
import com.example.model.Holding
import com.example.ui.FormatUtils
import com.example.ui.PortfolioUiState
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleMedium
import com.example.ui.theme.NumericStyleSmall
import com.example.ui.theme.categoryContainerColor
import com.example.ui.theme.categoryOnContainerColor

@Composable
fun HoldingsTab(
    uiState: PortfolioUiState,
    onEditHolding: (Holding) -> Unit,
    onDeleteHolding: (Holding) -> Unit,
    onViewDividends: (Holding) -> Unit,
    modifier: Modifier = Modifier
) {
    val portfolioColors = LocalPortfolioColors.current
    var searchQuery by remember { mutableStateOf("") }
    var holdingPendingDelete by remember { mutableStateOf<Holding?>(null) }

    val filtered = remember(uiState.filteredHoldings, searchQuery, uiState.managedSectors) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) uiState.filteredHoldings
        else uiState.filteredHoldings.filter {
            it.holding.symbol.lowercase().contains(q) ||
                it.holding.displayName.lowercase().contains(q) ||
                it.holding.resolveSectorLabel(uiState.managedSectors).lowercase().contains(q)
        }
    }

    if (holdingPendingDelete != null) {
        val target = holdingPendingDelete!!
        AlertDialog(
            onDismissRequest = { holdingPendingDelete = null },
            title = { Text("ลบ ${target.symbol}?") },
            text = { Text("ต้องการลบ ${target.displayName} (${FormatUtils.formatQuantity(target.quantity)} หน่วย) ออกจากพอร์ตหรือไม่?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        holdingPendingDelete = null
                        onDeleteHolding(target)
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("ลบรายการ", color = portfolioColors.loss)
                }
            },
            dismissButton = {
                TextButton(onClick = { holdingPendingDelete = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ค้นหาชื่อย่อ ชื่อสินทรัพย์ หรือกลุ่มหุ้น...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "ค้นหา",
                        tint = portfolioColors.mutedSlate
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("holdings_search_input")
            )
        }

        if (filtered.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "รายการ${uiState.selectedCategory.shortLabelTh} (${filtered.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "แตะที่รายการเพื่อดูรายละเอียดต้นทุน",
                        style = MaterialTheme.typography.labelSmall,
                        color = portfolioColors.mutedSlate
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.allHoldings.none { it.assetCategory == uiState.selectedCategory }) {
                            "ยังไม่มีสินทรัพย์ในหมวด ${uiState.selectedCategory.labelTh} กดปุ่ม + เพื่อเพิ่มรายการ"
                        } else {
                            "ไม่พบรายการที่ตรงกับคำค้นหา"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(
                count = filtered.size,
                key = { idx -> filtered[idx].holding.id.takeIf { it > 0L } ?: (-idx - 1L) }
            ) { idx ->
                HoldingDetailCard(
                    item = filtered[idx],
                    sectorLabel = filtered[idx].holding.resolveSectorLabel(uiState.managedSectors),
                    stripeColor = ChartPalette[idx % ChartPalette.size],
                    onEdit = { onEditHolding(filtered[idx].holding) },
                    onDelete = { holdingPendingDelete = filtered[idx].holding },
                    onViewDividends = { onViewDividends(filtered[idx].holding) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoldingDetailCard(
    item: AnalyzedHolding,
    sectorLabel: String,
    stripeColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewDividends: () -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val isGain = item.profitLossThb >= 0.0
    val pnlColor = if (isGain) portfolioColors.profit else portfolioColors.loss
    val pnlBg = if (isGain) portfolioColors.profitContainer else portfolioColors.lossContainer
    val h = item.holding

    var isExpanded by remember(h.id) { mutableStateOf(false) }

    val catContainer = categoryContainerColor(h.assetCategory)
    val catOnContainer = categoryOnContainerColor(h.assetCategory)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("holding_card_${h.symbol}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Signature Left Institutional Color Accent Bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                // Header Row: Symbol + Allocation Pill + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = h.symbol,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = FormatUtils.formatPercent(item.allocationPercentOfTotal, decimals = 1),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = h.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = portfolioColors.mutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (h.assetCategory.isStock) {
                            IconButton(
                                onClick = onViewDividends,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("dividend_info_button_${h.symbol}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PriorityHigh,
                                        contentDescription = "ดูข้อมูลปันผลในอนาคตของ ${h.symbol}",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("edit_holding_${h.symbol}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "แก้ไข ${h.symbol}",
                                tint = portfolioColors.mutedSlate,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_holding_${h.symbol}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "ลบ ${h.symbol}",
                                tint = portfolioColors.loss,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Clean Institutional 3-Column Metrics Strip (Cost | Value | P&L %)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "เงินลงทุน (บาท)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = portfolioColors.mutedSlate
                        )
                        Text(
                            text = FormatUtils.formatNumber(item.costBasisThb, 2),
                            style = NumericStyleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
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
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = portfolioColors.mutedSlate
                        )
                        Text(
                            text = FormatUtils.formatNumber(item.currentValueThb, 2),
                            style = NumericStyleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        modifier = Modifier.weight(0.95f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "%กำไร/ขาดทุน",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = portfolioColors.mutedSlate
                        )
                        Text(
                            text = FormatUtils.formatPercent(item.profitLossPercent, decimals = 2, showSign = true),
                            style = NumericStyleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TagChip(
                        text = h.assetCategory.shortLabelTh,
                        containerColor = catContainer,
                        textColor = catOnContainer
                    )
                    if (h.assetCategory.isStock) {
                        TagChip(text = sectorLabel)
                    }
                    TagChip(
                        text = when (item.priceSource) {
                            QuoteSource.UNAVAILABLE -> "รอราคาตลาด"
                            QuoteSource.MANUAL -> "ราคากรอกเอง"
                            else -> "ราคาตลาดจริง"
                        }
                    )
                }

                // Expandable Cost vs Current Breakdown
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricBox(
                                label = "จำนวน × ทุน/หน่วย",
                                value = "${FormatUtils.formatQuantity(h.quantity)} × ${FormatUtils.formatOriginalCurrency(h.buyPricePerUnit, h.currency)}",
                                containerColor = portfolioColors.costBoxBg,
                                borderColor = portfolioColors.costBoxBorder,
                                labelColor = portfolioColors.costBoxLabel,
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                label = "ราคาปัจจุบัน/หน่วย",
                                value = if (item.isEstimatedUsingCost) {
                                    "รอราคา (ใช้ทุน)"
                                } else {
                                    val displayPrice = if (h.currency == "THB") {
                                        FormatUtils.formatThb(item.effectiveCurrentPriceThb)
                                    } else {
                                        val origCurr = item.quote?.originalCurrency ?: h.currency
                                        FormatUtils.formatOriginalCurrency(item.effectiveCurrentPriceOriginal, origCurr)
                                    }
                                    buildString {
                                        append(displayPrice)
                                        item.dailyChangePercent?.let { chg ->
                                            append(" (${FormatUtils.formatPercent(chg, showSign = true, decimals = 1)})")
                                        }
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                labelColor = MaterialTheme.colorScheme.primary,
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricBox(
                                label = "มูลค่าทุนรวม",
                                value = FormatUtils.formatThb(item.costBasisThb),
                                containerColor = portfolioColors.costBoxBg,
                                borderColor = portfolioColors.costBoxBorder,
                                labelColor = portfolioColors.costBoxLabel,
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                isEmphasized = true,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                label = "มูลค่าปัจจุบัน",
                                value = FormatUtils.formatThb(item.currentValueThb),
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                labelColor = MaterialTheme.colorScheme.primary,
                                valueColor = MaterialTheme.colorScheme.primary,
                                isEmphasized = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact Institutional P&L Summary Bar
                Surface(
                    color = pnlBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, pnlColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .testTag("holding_pnl_bar_${h.symbol}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "ย่อรายละเอียด" else "ขยายดูทุนและมูลค่าปัจจุบัน",
                                tint = pnlColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isGain) "กำไรสุทธิ" else "ขาดทุนสุทธิ",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = pnlColor
                            )
                        }
                        Text(
                            text = "${FormatUtils.formatThb(item.profitLossThb, showSign = true)} (${FormatUtils.formatPercent(item.profitLossPercent, showSign = true)})",
                            style = NumericStyleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    containerColor: Color,
    borderColor: Color,
    labelColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
    isEmphasized: Boolean = false
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(10.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = if (isEmphasized) {
                    NumericStyleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    NumericStyleSmall.copy(fontWeight = FontWeight.SemiBold)
                },
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
