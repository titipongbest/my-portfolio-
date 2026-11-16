package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.KnownAsset
import com.example.data.KnownAssetsCatalog
import com.example.data.LiveQuote
import com.example.data.QuoteSource
import com.example.model.AssetCategory
import com.example.model.Holding
import com.example.model.ManagedSector
import com.example.model.StockSector
import com.example.ui.FormatUtils
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleMedium
import com.example.ui.theme.categoryAccentColor
import com.example.ui.theme.categoryContainerColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHoldingSheet(
    existingHolding: Holding?,
    activeCategory: AssetCategory,
    managedSectors: List<ManagedSector>,
    newlyCreatedSectorId: String? = null,
    isSaving: Boolean,
    onManageSectorsClick: () -> Unit,
    onSearchAssets: suspend (String, AssetCategory) -> List<KnownAsset>,
    onFetchPreviewQuote: suspend (String, AssetCategory, Double?, String) -> LiveQuote?,
    onDismiss: () -> Unit,
    onSave: (
        symbol: String,
        displayName: String,
        category: AssetCategory,
        sectorId: String,
        quantity: Double,
        buyPricePerUnit: Double,
        currency: String,
        manualCurrentPrice: Double?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val portfolioColors = LocalPortfolioColors.current

    val selectedCategory = existingHolding?.assetCategory ?: activeCategory

    var symbol by remember(existingHolding) {
        mutableStateOf(existingHolding?.symbol ?: "")
    }
    var displayName by remember(existingHolding) {
        mutableStateOf(existingHolding?.displayName ?: "")
    }
    val defaultSectorId = managedSectors.firstOrNull()?.id ?: StockSector.ENERGY.name
    var selectedSectorId by remember(existingHolding) {
        mutableStateOf(
            existingHolding?.sector?.takeIf {
                it.isNotBlank() && it != StockSector.NONE.name && managedSectors.any { m -> m.id == it }
            } ?: defaultSectorId
        )
    }

    LaunchedEffect(newlyCreatedSectorId) {
        if (!newlyCreatedSectorId.isNullOrBlank()) {
            selectedSectorId = newlyCreatedSectorId
        }
    }

    LaunchedEffect(managedSectors) {
        if (managedSectors.isNotEmpty() && managedSectors.none { it.id == selectedSectorId }) {
            selectedSectorId = managedSectors.first().id
        }
    }

    var quantityText by remember(existingHolding) {
        mutableStateOf(
            existingHolding?.let {
                if (it.quantity % 1.0 == 0.0) it.quantity.toLong().toString()
                else it.quantity.toString()
            } ?: ""
        )
    }
    var buyPriceText by remember(existingHolding) {
        mutableStateOf(
            existingHolding?.buyPricePerUnit?.toString() ?: ""
        )
    }
    var currency by remember(existingHolding, selectedCategory) {
        mutableStateOf(existingHolding?.currency ?: selectedCategory.defaultCurrency)
    }
    var manualPriceText by remember(existingHolding) {
        mutableStateOf(existingHolding?.manualCurrentPrice?.toString() ?: "")
    }
    var isManualPriceExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var previewQuote by remember { mutableStateOf<LiveQuote?>(null) }
    var isFetchingPreview by remember { mutableStateOf(false) }

    var searchQuery by remember(existingHolding) { mutableStateOf("") }
    var showFilteredOptions by remember(existingHolding) { mutableStateOf(false) }
    var filteredAssets by remember(selectedCategory) {
        mutableStateOf(KnownAssetsCatalog.search("", selectedCategory, limit = 10))
    }
    var isSearchingOnline by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, selectedCategory, showFilteredOptions) {
        val q = searchQuery.trim()
        filteredAssets = KnownAssetsCatalog.search(q, selectedCategory, limit = 10)
        if (showFilteredOptions &&
            q.length >= 2 &&
            (selectedCategory.isStock || selectedCategory == AssetCategory.MUTUAL_FUND)
        ) {
            delay(280L)
            isSearchingOnline = true
            val remoteAndLocal = runCatching { onSearchAssets(q, selectedCategory) }.getOrNull()
            if (remoteAndLocal != null) {
                filteredAssets = remoteAndLocal
            }
            isSearchingOnline = false
        } else {
            isSearchingOnline = false
        }
    }

    fun selectAssetSuggestion(asset: KnownAsset) {
        symbol = asset.symbol
        displayName = asset.displayName
        currency = asset.defaultCurrency
        if (asset.sector != StockSector.NONE &&
            managedSectors.any { it.id == asset.sector.name }
        ) {
            selectedSectorId = asset.sector.name
        }
        previewQuote = null
        showFilteredOptions = false
    }

    val costStepNumber = if (selectedCategory.isStock) 3 else 2

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color(0xFF94A3B8),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = portfolioColors.accentHighlight,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = Color(0xFF64748B),
        unfocusedPlaceholderColor = Color(0xFF64748B)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Title Header + Active Category Badge
            val catAccent = categoryAccentColor(selectedCategory)
            val catBg = categoryContainerColor(selectedCategory)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (existingHolding == null) "เพิ่มสินทรัพย์เข้าพอร์ต" else "แก้ไขข้อมูลสินทรัพย์",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = catBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(1.dp, catAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("sheet_active_category_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = categoryIcon(selectedCategory),
                                contentDescription = selectedCategory.labelTh,
                                tint = catAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "หมวดหมู่: ${selectedCategory.labelTh}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = catAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "กรอกข้อมูลทีละข้อด้านล่าง (ช่องที่มี * จำเป็นต้องกรอก)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ปิดหน้าต่าง",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ข้อ 1: Symbol & Name
            FormStepCard(
                stepNumber = 1,
                title = "รหัสและชื่อสินทรัพย์",
                subtitle = "พิมพ์รหัสหรือชื่อสินทรัพย์ แล้วกด \"เช็คราคา\" เพื่อดูราคาตลาด"
            ) {
                Text(
                    text = "รหัส / ชื่อย่อสินทรัพย์ (พิมพ์เพื่อกรองตัวเลือก) *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { input ->
                            symbol = input
                            searchQuery = input
                            showFilteredOptions = input.isNotBlank()
                            previewQuote = null
                            val match = KnownAssetsCatalog.findMatch(input, selectedCategory)
                            if (match != null) {
                                displayName = match.displayName
                                if (match.sector != StockSector.NONE &&
                                    managedSectors.any { it.id == match.sector.name }
                                ) {
                                    selectedSectorId = match.sector.name
                                }
                                currency = match.defaultCurrency
                            }
                        },
                        placeholder = {
                            Text(
                                when (selectedCategory) {
                                    AssetCategory.THAI_STOCK -> "เช่น PTT, AOT, กสิกร"
                                    AssetCategory.GLOBAL_STOCK -> "เช่น NVDA, AAPL, Apple"
                                    AssetCategory.MUTUAL_FUND -> "เช่น K-USA, SCB, VOO"
                                    AssetCategory.GOLD -> "เช่น XAUUSD, PAXG, GLD"
                                    AssetCategory.CRYPTO -> "เช่น BTC, ETH, SOL"
                                }
                            )
                        },
                        singleLine = true,
                        colors = fieldColors,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("symbol_input")
                    )

                    OutlinedButton(
                        onClick = {
                            val s = symbol.trim()
                            if (s.isNotBlank()) {
                                scope.launch {
                                    isFetchingPreview = true
                                    previewQuote = onFetchPreviewQuote(
                                        s,
                                        selectedCategory,
                                        manualPriceText.toDoubleOrNull(),
                                        currency
                                    )
                                    isFetchingPreview = false
                                }
                            }
                        },
                        enabled = symbol.isNotBlank() && !isFetchingPreview,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("fetch_preview_price_button")
                    ) {
                        if (isFetchingPreview) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "เช็คราคาปัจจุบัน",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "เช็คราคา",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // Category-filtered clickable options when typing (especially Stocks & Mutual Funds)
                AnimatedVisibility(
                    visible = showFilteredOptions && (filteredAssets.isNotEmpty() || isSearchingOnline)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .border(1.dp, catAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .testTag("filtered_asset_options_list")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ตัวเลือกในหมวด${selectedCategory.shortLabelTh} (แตะเพื่อเลือก)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = catAccent
                                )
                                if (isSearchingOnline) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = catAccent
                                    )
                                }
                            }
                            filteredAssets.take(10).forEachIndexed { idx, asset ->
                                if (idx > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectAssetSuggestion(asset) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("filtered_option_${asset.symbol}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = catBg,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = asset.symbol,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = catAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = asset.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (asset.category.isStock && asset.sector != StockSector.NONE) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = asset.sector.shortLabelTh,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (previewQuote != null) {
                    val q = previewQuote!!
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = if (q.source != QuoteSource.UNAVAILABLE) {
                            portfolioColors.profitContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (q.source != QuoteSource.UNAVAILABLE) portfolioColors.profit else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (q.source != QuoteSource.UNAVAILABLE) {
                                    val primaryPriceText = if (currency == "THB" && q.currentPriceThb > 0) {
                                        FormatUtils.formatThb(q.currentPriceThb)
                                    } else {
                                        FormatUtils.formatOriginalCurrency(q.currentPriceOriginal, q.originalCurrency)
                                    }
                                    "${q.source.labelTh}: $primaryPriceText"
                                } else {
                                    "ไม่พบราคาตลาดอัตโนมัติ (กดเปิดราคาปัจจุบันสำรองด้านล่าง)"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (q.originalCurrency == "USD" && q.currentPriceThb > 0) {
                                Text(
                                    text = if (currency == "THB") {
                                        "≈ ${FormatUtils.formatOriginalCurrency(q.currentPriceOriginal, "USD")}"
                                    } else {
                                        "≈ ${FormatUtils.formatThb(q.currentPriceThb)}"
                                    },
                                    style = NumericStyleMedium,
                                    color = portfolioColors.profit
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ชื่อเต็ม / ชื่อเรียกเพิ่มเติม (พิมพ์ค้นหาชื่อไทย/อังกฤษได้)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { input ->
                        displayName = input
                        if (symbol.isBlank() || showFilteredOptions) {
                            searchQuery = input
                            showFilteredOptions = input.isNotBlank()
                        }
                    },
                    placeholder = { Text("เช่น บมจ. ปตท. หรือ กองทุนเปิดเค ยูเอสเอ") },
                    singleLine = true,
                    colors = fieldColors,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input")
                )
            }

            // ข้อ 2 (Stocks Only): Managed Sector Picker
            AnimatedVisibility(visible = selectedCategory.isStock) {
                FormStepCard(
                    stepNumber = 2,
                    title = "เลือกกลุ่มอุตสาหกรรมของหุ้น",
                    subtitle = "ใช้คำนวณสัดส่วนกลุ่มธุรกิจในหมวด${selectedCategory.shortLabelTh}",
                    headerAction = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onManageSectorsClick() }
                                .testTag("sheet_manage_sectors_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    text = "เพิ่ม / ลบกลุ่ม",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        managedSectors.forEach { sector ->
                            val isSelected = selectedSectorId == sector.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSectorId = sector.id },
                                label = {
                                    Text(
                                        text = sector.label,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF94A3B8),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("sector_chip_${sector.id}")
                            )
                        }
                    }
                }
            }

            // ข้อ 2 or ข้อ 3: Quantity & Cost Basis
            FormStepCard(
                stepNumber = costStepNumber,
                title = "จำนวนและราคาที่ได้มา (ต้นทุน)",
                subtitle = "ระบุจำนวนที่ถือครองและราคาเฉลี่ยต่อหน่วยที่ซื้อมา"
            ) {
                Text(
                    text = "จำนวนหุ้น / หน่วยที่ถือครอง *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    placeholder = { Text("เช่น 1000 หรือ 0.5") },
                    suffix = {
                        Text(
                            text = if (selectedCategory.isStock) "หุ้น" else "หน่วย",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    textStyle = NumericStyleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quantity_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ราคาที่ได้มาต่อหน่วย ($currency) *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = buyPriceText,
                    onValueChange = { buyPriceText = it },
                    placeholder = { Text("เช่น 33.50 หรือ 4500") },
                    prefix = {
                        Text(
                            text = if (currency == "USD") "$ " else "฿ ",
                            style = NumericStyleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    textStyle = NumericStyleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("buy_price_input")
                )

                if (selectedCategory != AssetCategory.THAI_STOCK) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "สกุลเงินของราคาทุน:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("THB" to "บาท (฿)", "USD" to "ดอลลาร์ ($)").forEach { (code, label) ->
                            val selected = currency == code
                            FilterChip(
                                selected = selected,
                                onClick = { currency = code },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = Color(0xFF94A3B8),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("currency_chip_$code")
                            )
                        }
                    }
                }

                val parsedQty = quantityText.trim().toDoubleOrNull()
                val parsedBuy = buyPriceText.trim().toDoubleOrNull()
                if (parsedQty != null && parsedQty > 0.0 && parsedBuy != null && parsedBuy >= 0.0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "รวมมูลค่าต้นทุน:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatOriginalCurrency(parsedQty * parsedBuy, currency),
                                style = NumericStyleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Collapsible Dropdown Card for Manual Current Price Backup (collapsed by default)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isManualPriceExpanded = !isManualPriceExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("manual_price_dropdown_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ราคาปัจจุบันสำรอง (เพิ่มเติม)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (manualPriceText.isNotBlank()) {
                                    "กำหนดไว้: $manualPriceText $currency • แตะเพื่อ${if (isManualPriceExpanded) "ซ่อน" else "แก้ไข"}"
                                } else {
                                    "แตะเพื่อเปิดกรอกกรณีดึงราคาตลาดอัตโนมัติไม่ได้"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isManualPriceExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = if (isManualPriceExpanded) "ซ่อนราคาปัจจุบันสำรอง" else "เปิดราคาปัจจุบันสำรอง",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    AnimatedVisibility(visible = isManualPriceExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Text(
                                text = "ราคาปัจจุบันต่อหน่วย ($currency)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = manualPriceText,
                                onValueChange = { manualPriceText = it },
                                placeholder = { Text("เว้นว่างไว้เพื่อใช้ราคาตลาดอัตโนมัติ") },
                                prefix = {
                                    Text(
                                        text = if (currency == "USD") "$ " else "฿ ",
                                        style = NumericStyleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                colors = fieldColors,
                                textStyle = NumericStyleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_price_input")
                            )
                        }
                    }
                }
            }

            if (validationError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Button(
                onClick = {
                    val cleanSym = symbol.trim().uppercase()
                    val qty = quantityText.trim().toDoubleOrNull()
                    val buyPrice = buyPriceText.trim().toDoubleOrNull()
                    val manualPrice = manualPriceText.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()

                    when {
                        cleanSym.isBlank() -> validationError = "ข้อ 1: กรุณาระบุชื่อย่อหรือรหัสสินทรัพย์"
                        cleanSym.length > 32 -> validationError = "ข้อ 1: รหัสสินทรัพย์ต้องไม่เกิน 32 ตัวอักษร"
                        qty == null || qty <= 0.0 -> validationError = "ข้อ $costStepNumber: กรุณากรอกจำนวนหุ้น/หน่วยให้มากกว่า 0"
                        buyPrice == null || buyPrice < 0.0 -> validationError = "ข้อ $costStepNumber: กรุณากรอกราคาที่ได้มาต่อหน่วยให้ถูกต้อง"
                        manualPriceText.isNotBlank() && (manualPrice == null || manualPrice < 0.0) -> {
                            isManualPriceExpanded = true
                            validationError = "ราคาปัจจุบันสำรองต้องเป็นตัวเลขที่ไม่ติดลบ"
                        }
                        else -> {
                            validationError = null
                            onSave(
                                cleanSym,
                                displayName.trim().ifBlank { cleanSym },
                                selectedCategory,
                                if (selectedCategory.isStock) selectedSectorId else StockSector.NONE.name,
                                qty,
                                buyPrice,
                                currency,
                                manualPrice
                            )
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_holding_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (existingHolding == null) "บันทึกเข้าพอร์ต" else "บันทึกการแก้ไข",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun FormStepCard(
    stepNumber: Int,
    title: String,
    subtitle: String? = null,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
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
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$stepNumber",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ข้อ $stepNumber: $title",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (headerAction != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    headerAction()
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

private fun categoryIcon(category: AssetCategory) = when (category) {
    AssetCategory.THAI_STOCK -> Icons.AutoMirrored.Filled.ShowChart
    AssetCategory.GLOBAL_STOCK -> Icons.Default.Public
    AssetCategory.MUTUAL_FUND -> Icons.Default.AccountBalance
    AssetCategory.GOLD -> Icons.Default.Diamond
    AssetCategory.CRYPTO -> Icons.Default.CurrencyBitcoin
}
