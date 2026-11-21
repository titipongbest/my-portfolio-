package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssetCategory
import com.example.model.PortfolioPlan
import com.example.ui.FormatUtils
import com.example.ui.PortfolioUiState
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleLarge
import com.example.ui.theme.NumericStyleSmall
import com.example.ui.theme.categoryAccentColor
import com.example.ui.theme.categoryContainerColor
import com.example.ui.theme.categoryOnContainerColor
import java.util.Locale
import kotlin.math.abs

@Composable
fun OverviewPlanTab(
    uiState: PortfolioUiState,
    onSavePlan: (PortfolioPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    val portfolioColors = LocalPortfolioColors.current
    val fullSummary = uiState.fullPortfolioSummary
    val savedPlan = uiState.portfolioPlan

    val targetInputs = remember { mutableStateMapOf<AssetCategory, String>() }
    val noteInputs = remember { mutableStateMapOf<AssetCategory, String>() }
    var overallNoteInput by remember { mutableStateOf("") }

    LaunchedEffect(savedPlan) {
        for (cat in AssetCategory.entries) {
            val pct = savedPlan.targetPercentFor(cat)
            targetInputs[cat] = formatEditablePercent(pct)
            noteInputs[cat] = savedPlan.noteFor(cat)
        }
        overallNoteInput = savedPlan.overallNote
    }

    val parsedTargets: Map<AssetCategory, Double> = AssetCategory.entries.associateWith { cat ->
        parsePercentInput(targetInputs[cat].orEmpty())
    }
    val totalTarget = parsedTargets.values.sum()
    val actualByCategory = remember(fullSummary.categorySlices) {
        fullSummary.categorySlices.associateBy { it.category }
    }

    fun buildDraftPlan(): PortfolioPlan {
        val cleanTargets = parsedTargets.filterValues { it > 0.0 }
        val cleanNotes = buildMap {
            for (cat in AssetCategory.entries) {
                val txt = noteInputs[cat].orEmpty().trim()
                if (txt.isNotEmpty()) put(cat, txt)
            }
        }
        return PortfolioPlan(
            targetPercents = cleanTargets,
            categoryNotes = cleanNotes,
            overallNote = overallNoteInput.trim(),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    val isExact100 = abs(totalTarget - 100.0) < 0.05
    val isOver100 = totalTarget > 100.05
    val statusColor = when {
        isExact100 -> portfolioColors.profit
        isOver100 -> portfolioColors.loss
        else -> MaterialTheme.colorScheme.primary
    }
    val statusContainer = when {
        isExact100 -> portfolioColors.profitContainer
        isOver100 -> portfolioColors.lossContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val statusText = when {
        isExact100 -> "ครบ 100% พอดี"
        isOver100 -> "เกิน 100% อยู่ ${FormatUtils.formatPercent(totalTarget - 100.0, decimals = 1)}"
        totalTarget == 0.0 -> "ยังไม่ได้กำหนดสัดส่วน"
        else -> "เหลืออีก ${FormatUtils.formatPercent(100.0 - totalTarget, decimals = 1)}"
    }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("overview_plan_lazy_column"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Plan Hero Banner + Floating Target Summary Card
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp)
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "แผนการจัดสรรพอร์ตเป้าหมาย",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatPercent(totalTarget, decimals = 1),
                        style = NumericStyleLarge.copy(fontSize = 30.sp),
                        color = Color.White,
                        modifier = Modifier.testTag("plan_total_target_text")
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "สถานะสัดส่วนเป้าหมาย",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = statusContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isExact100) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = statusColor
                                        )
                                    }
                                }
                            }

                            // Stacked Planned Allocation Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val barDenominator = maxOf(totalTarget, 100.0)
                                AssetCategory.entries.forEach { cat ->
                                    val pct = parsedTargets[cat] ?: 0.0
                                    if (pct > 0.0) {
                                        Box(
                                            modifier = Modifier
                                                .weight((pct / barDenominator).toFloat().coerceAtLeast(0.001f))
                                                .height(10.dp)
                                                .background(categoryAccentColor(cat))
                                        )
                                    }
                                }
                                if (totalTarget < 100.0) {
                                    Spacer(
                                        modifier = Modifier.weight(((100.0 - totalTarget) / 100.0).toFloat().coerceAtLeast(0.001f))
                                    )
                                }
                            }

                            // Quick Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        AssetCategory.entries.forEach { cat ->
                                            targetInputs[cat] = "20"
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("plan_equal_weight_button")
                                ) {
                                    Text(
                                        text = "แบ่งเท่ากัน (20%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        AssetCategory.entries.forEach { cat ->
                                            val actual = actualByCategory[cat]?.percentOfPortfolio ?: 0.0
                                            targetInputs[cat] = formatEditablePercent(actual)
                                        }
                                    },
                                    enabled = fullSummary.totalValueThb > 0.0,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("plan_copy_actual_button")
                                ) {
                                    Text(
                                        text = "ดึงจากพอร์ตจริง",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = { onSavePlan(buildDraftPlan()) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("plan_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "บันทึกแผนการลงทุน",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Section Header: Per-Category Target % & Notes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "กำหนดสัดส่วนเป้าหมายรายสินทรัพย์",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "รวม 100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = portfolioColors.mutedSlate
                )
            }
        }

        items(
            items = AssetCategory.entries,
            key = { it.name }
        ) { cat ->
            val catAccent = categoryAccentColor(cat)
            val catContainer = categoryContainerColor(cat)
            val catOnContainer = categoryOnContainerColor(cat)
            val actualSlice = actualByCategory[cat]
            val actualPct = actualSlice?.percentOfPortfolio ?: 0.0
            val actualValue = actualSlice?.totalValueThb ?: 0.0
            val targetPct = parsedTargets[cat] ?: 0.0
            val diffPct = targetPct - actualPct
            val targetValueThb = fullSummary.totalValueThb * (targetPct / 100.0)
            val rebalanceThb = targetValueThb - actualValue

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_category_card_${cat.name}")
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(catContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = overviewCategoryIcon(cat),
                                    contentDescription = cat.labelTh,
                                    tint = catOnContainer,
                                    modifier = Modifier.size(19.dp)
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
                                    text = "ปัจจุบัน ${FormatUtils.formatPercent(actualPct, decimals = 1)} (${FormatUtils.formatThb(actualValue, decimals = 0)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = portfolioColors.mutedSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedTextField(
                                value = targetInputs[cat].orEmpty(),
                                onValueChange = { raw ->
                                    val filtered = sanitizeDecimalInput(raw)
                                    targetInputs[cat] = filtered
                                },
                                label = { Text("เป้า %", style = MaterialTheme.typography.labelSmall) },
                                suffix = { Text("%", style = MaterialTheme.typography.labelMedium) },
                                singleLine = true,
                                colors = inputColors,
                                textStyle = NumericStyleSmall.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = catAccent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .width(108.dp)
                                    .testTag("plan_target_input_${cat.name}")
                            )
                        }

                        // Comparison Progress + Rebalance Guidance
                        if (fullSummary.totalValueThb > 0.0 || targetPct > 0.0) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    val targetFill = (targetPct / 100.0).toFloat().coerceIn(0f, 1f)
                                    val actualFill = (actualPct / 100.0).toFloat().coerceIn(0f, 1f)
                                    if (targetFill > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(targetFill)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(catAccent.copy(alpha = 0.25f))
                                        )
                                    }
                                    if (actualFill > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(actualFill)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(catAccent)
                                        )
                                    }
                                }

                                if (fullSummary.totalValueThb > 0.0 && targetPct > 0.0) {
                                    val guidanceColor = when {
                                        abs(diffPct) <= 0.5 -> portfolioColors.profit
                                        diffPct > 0.0 -> catAccent
                                        else -> portfolioColors.loss
                                    }
                                    val guidanceText = when {
                                        abs(diffPct) <= 0.5 -> "สัดส่วนใกล้เคียงเป้าหมายแล้ว"
                                        rebalanceThb > 0.0 -> "ควรเพิ่มอีก ${FormatUtils.formatThb(rebalanceThb, decimals = 0)} (${FormatUtils.formatPercent(diffPct, decimals = 1, showSign = true)})"
                                        else -> "เกินเป้าอยู่ ${FormatUtils.formatThb(abs(rebalanceThb), decimals = 0)} (${FormatUtils.formatPercent(diffPct, decimals = 1, showSign = true)})"
                                    }
                                    Text(
                                        text = guidanceText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = guidanceColor
                                    )
                                }
                            }
                        }

                        // Per-Category Note
                        OutlinedTextField(
                            value = noteInputs[cat].orEmpty(),
                            onValueChange = { text ->
                                noteInputs[cat] = text.take(PortfolioPlan.MAX_CATEGORY_NOTE_CHARS)
                            },
                            placeholder = {
                                Text(
                                    text = "โน้ตสำหรับ${cat.shortLabelTh} (เช่น กลยุทธ์ หรือสินทรัพย์ที่สนใจ)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = inputColors,
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("plan_category_note_${cat.name}")
                        )
                    }
                }
            }
        }

        // 3. Overall Investment Plan Note Card
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "โน้ตแผนการลงทุน",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "โน้ตแผนการลงทุนโดยรวม (Note)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "บันทึกกติกา DCA เป้าหมายผลตอบแทน หรือเงื่อนไขรีบาลานซ์",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = portfolioColors.mutedSlate
                                )
                            }
                        }

                        OutlinedTextField(
                            value = overallNoteInput,
                            onValueChange = { text ->
                                overallNoteInput = text.take(PortfolioPlan.MAX_OVERALL_NOTE_CHARS)
                            },
                            placeholder = {
                                Text(
                                    text = "เช่น เติมเงิน DCA ทุกวันที่ 25 ของเดือน, รีบาลานซ์ทุกไตรมาสเมื่อสัดส่วนคลาดเคลื่อนเกิน 5%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = inputColors,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(12.dp),
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("plan_overall_note_input")
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = buildString {
                                    append("สัดส่วนเป้าหมายและโน้ตจะถูกสำรองรวมอยู่ในไฟล์ Backup JSON ด้วย")
                                    savedPlan.updatedAtMillis?.let { ts ->
                                        append(" • บันทึกล่าสุด ${FormatUtils.formatUpdatedTime(ts)}")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = portfolioColors.mutedSlate
                            )
                        }

                        Button(
                            onClick = { onSavePlan(buildDraftPlan()) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("plan_save_bottom_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "บันทึกแผนและโน้ต",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sanitizeDecimalInput(raw: String): String {
    val sb = StringBuilder()
    var hasDot = false
    for (ch in raw) {
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !hasDot -> {
                hasDot = true
                sb.append(ch)
            }
        }
    }
    val str = sb.toString()
    val numeric = str.toDoubleOrNull()
    return if (numeric != null && numeric > 100.0) "100" else str.take(6)
}

private fun parsePercentInput(raw: String): Double {
    val parsed = raw.trim().toDoubleOrNull() ?: return 0.0
    return if (parsed.isFinite()) parsed.coerceIn(0.0, 100.0) else 0.0
}

private fun formatEditablePercent(value: Double): String {
    if (!value.isFinite() || value <= 0.0) return ""
    val rounded = Math.round(value * 100.0) / 100.0
    return if (abs(rounded - rounded.toInt()) < 1e-6) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}
