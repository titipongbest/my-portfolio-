package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.StockDividendInfo
import com.example.model.Holding
import com.example.ui.FormatUtils
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleMedium
import com.example.ui.theme.NumericStyleSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface DividendUiState {
    data class Loading(val holding: Holding) : DividendUiState
    data class Ready(val holding: Holding, val info: StockDividendInfo) : DividendUiState
}

@Composable
fun DividendInfoDialog(
    state: DividendUiState,
    onRetry: (Holding) -> Unit,
    onDismiss: () -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val holding = when (state) {
        is DividendUiState.Loading -> state.holding
        is DividendUiState.Ready -> state.holding
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .testTag("dividend_info_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = "ข้อมูลปันผล",
                                tint = portfolioColors.accentHighlight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ปันผลในอนาคต • ${holding.symbol}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${holding.displayName} • ถือ ${FormatUtils.formatQuantity(holding.quantity)} หุ้น",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_dividend_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ปิด",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(14.dp))

                when (state) {
                    is DividendUiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "กำลังดึงข้อมูลประวัติและรอบจ่ายปันผลของ ${holding.symbol}...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is DividendUiState.Ready -> {
                        val info = state.info
                        if (info.errorMessage != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = info.errorMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = portfolioColors.loss
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onRetry(holding) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ลองใหม่")
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 440.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Summary Card
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "คาดการณ์ปันผลรวม 12 เดือนข้างหน้า",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (info.trailingYieldPercent != null) {
                                                    Surface(
                                                        color = portfolioColors.profitContainer,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "Yield ${FormatUtils.formatPercent(info.trailingYieldPercent, decimals = 2)}",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = portfolioColors.profit,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = FormatUtils.formatThb(info.estimatedAnnualIncomeThb),
                                                style = NumericStyleMedium.copy(
                                                    color = portfolioColors.profit,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )

                                            if (info.trailingAnnualDividendPerShare > 0.0) {
                                                Text(
                                                    text = "ปันผลต่อหุ้นปีล่าสุด: ${FormatUtils.formatOriginalCurrency(info.trailingAnnualDividendPerShare, info.currency)} / หุ้น",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Upcoming Payouts Header
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = portfolioColors.accentHighlight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "กำหนดการปันผลที่คาดว่าจะจ่ายในอนาคต",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (info.upcomingDividends.isEmpty()) {
                                    item {
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                        ) {
                                            Text(
                                                text = "ไม่พบประวัติการจ่ายปันผลในช่วง 2 ปีที่ผ่านมา จึงยังไม่มีรอบปันผลคาดการณ์ในอนาคต",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }
                                } else {
                                    items(info.upcomingDividends) { proj ->
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.2.dp,
                                                    if (proj.isConfirmed) portfolioColors.profit else MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(14.dp)
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "ประมาณ XD: ${formatThaiDate(proj.expectedExDateMillis)}",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${proj.note} (${FormatUtils.formatOriginalCurrency(proj.estimatedAmountPerShare, info.currency)} / หุ้น)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "คาดว่ารับ",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = FormatUtils.formatThb(proj.estimatedPayoutThb),
                                                        style = NumericStyleSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = portfolioColors.profit
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Recent Historical Payouts
                                if (info.historicalDividends.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ประวัติปันผลจริงย้อนหลัง (อ้างอิง)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    items(info.historicalDividends.take(6)) { hist ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "XD: ${formatThaiDate(hist.exDateMillis)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${FormatUtils.formatOriginalCurrency(hist.amountPerShare, info.currency)} / หุ้น",
                                                style = NumericStyleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        text = "* ${info.sourceNote} — เป็นการประมาณการตามรอบการจ่ายจริงย้อนหลัง อาจเปลี่ยนแปลงตามมติบริษัท",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_dividend_done_button")
                ) {
                    Text("ปิด", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun formatThaiDate(millis: Long): String {
    val sdf = SimpleDateFormat("d MMM yyyy", Locale("th", "TH"))
    return sdf.format(Date(millis))
}
