package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AndroidAutoBackupWriter
import com.example.data.ImportMode
import com.example.data.MarketPriceService
import com.example.data.PortfolioBackupPayload
import com.example.data.PortfolioJsonCodec
import com.example.data.PortfolioRepository
import com.example.data.PortfolioSettingsStore
import com.example.data.local.PortfolioDatabase
import com.example.model.AssetCategory
import com.example.model.Holding
import com.example.ui.PortfolioViewModel
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.categoryAccentColor
import com.example.ui.theme.categoryContainerColor
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MainTab(
    val labelTh: String,
    val icon: ImageVector
) {
    DASHBOARD("ภาพรวม", Icons.Default.PieChart),
    ANALYTICS("วิเคราะห์กลุ่ม", Icons.Default.Analytics),
    HOLDINGS("สินทรัพย์", Icons.AutoMirrored.Filled.ListAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPortfolioScreen(
    onCategoryChanged: (AssetCategory) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: PortfolioViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = PortfolioDatabase.getInstance(app)
                PortfolioViewModel(
                    repository = PortfolioRepository(db.holdingDao()),
                    marketPriceService = MarketPriceService(),
                    settingsStore = PortfolioSettingsStore(app),
                    autoBackupWriter = AndroidAutoBackupWriter(app)
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val dividendDialogState by viewModel.dividendDialogState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingHolding by remember { mutableStateOf<Holding?>(null) }
    var showBackupScreen by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showManageSectorsDialog by remember { mutableStateOf(false) }
    var newlyCreatedSectorId by remember { mutableStateOf<String?>(null) }
    var pendingImportPayload by remember { mutableStateOf<PortfolioBackupPayload?>(null) }
    var fallbackExportJson by remember { mutableStateOf<String?>(null) }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var pasteImportText by remember { mutableStateOf("") }
    var pasteImportError by remember { mutableStateOf<String?>(null) }
    var pasteImportHint by remember { mutableStateOf<String?>(null) }
    var suppressNextAutoLock by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, uiState.securityConfig.isPinEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (uiState.securityConfig.isPinEnabled && !suppressNextAutoLock) {
                        viewModel.lockApp()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    suppressNextAutoLock = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.selectedCategory) {
        onCategoryChanged(uiState.selectedCategory)
    }

    fun handleExportUriSelected(uri: Uri?) {
        val jsonPayload = viewModel.consumePendingExportJson()
        if (uri != null && jsonPayload != null) {
            scope.launch {
                val writeResult = writeJsonToUriSafely(context, uri, jsonPayload)
                if (writeResult.isSuccess) {
                    viewModel.notifyMessage("ส่งออกข้อมูลพอร์ตเป็นไฟล์ JSON เรียบร้อย")
                } else {
                    viewModel.notifyMessage(
                        "ส่งออกไฟล์ไม่สำเร็จ: ${writeResult.exceptionOrNull()?.localizedMessage ?: "เกิดข้อผิดพลาด"}"
                    )
                    fallbackExportJson = jsonPayload
                }
            }
        }
    }

    fun handleAutoBackupUriSelected(uri: Uri?) {
        if (uri != null) {
            AndroidAutoBackupWriter.persistWritePermission(context, uri)
            val fileName = AndroidAutoBackupWriter.resolveDisplayName(context, uri)
            viewModel.configureAutoBackupDestination(uri.toString(), fileName)
        }
    }

    fun handleImportUriSelected(uri: Uri?) {
        if (uri != null) {
            scope.launch {
                val decodedResult = readAndDecodeBackupUri(context, uri)
                decodedResult.onSuccess { payload ->
                    pendingImportPayload = payload
                }.onFailure { err ->
                    viewModel.notifyMessage(err.message ?: "ไม่สามารถอ่านไฟล์ JSON ได้")
                }
            }
        }
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> handleExportUriSelected(uri) }

    val exportAnyDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri -> handleExportUriSelected(uri) }

    val autoBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> handleAutoBackupUriSelected(uri) }

    val autoBackupAnyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri -> handleAutoBackupUriSelected(uri) }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handleImportUriSelected(uri) }

    val importGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> handleImportUriSelected(uri) }

    fun triggerExportJson() {
        scope.launch {
            if (uiState.allHoldings.isEmpty()) {
                viewModel.notifyMessage("ยังไม่มีข้อมูลสินทรัพย์ในพอร์ตสำหรับส่งออก")
                return@launch
            }
            val res = viewModel.exportPortfolioJson()
            res.onSuccess { json ->
                viewModel.setPendingExportJson(json)
                val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                val suggestedName = "portpulse_backup_$dateStamp.json"
                suppressNextAutoLock = true
                val launchedPrimary = try {
                    exportDocumentLauncher.launch(suggestedName)
                    true
                } catch (_: Exception) {
                    false
                }
                if (!launchedPrimary) {
                    val launchedFallback = try {
                        exportAnyDocumentLauncher.launch(suggestedName)
                        true
                    } catch (_: Exception) {
                        false
                    }
                    if (!launchedFallback) {
                        suppressNextAutoLock = false
                        viewModel.consumePendingExportJson()
                        fallbackExportJson = json
                        viewModel.notifyMessage("อุปกรณ์นี้ไม่มีตัวเลือกสร้างไฟล์ กรุณาคัดลอก แชร์ หรือบันทึกลง Downloads แทน")
                    }
                }
            }.onFailure { err ->
                viewModel.notifyMessage("เตรียมไฟล์ส่งออกไม่สำเร็จ: ${err.localizedMessage ?: "เกิดข้อผิดพลาด"}")
            }
        }
    }

    fun triggerExportCopyShare() {
        scope.launch {
            if (uiState.allHoldings.isEmpty()) {
                viewModel.notifyMessage("ยังไม่มีข้อมูลสินทรัพย์ในพอร์ตสำหรับส่งออก")
                return@launch
            }
            val res = viewModel.exportPortfolioJson()
            res.onSuccess { json ->
                fallbackExportJson = json
            }.onFailure { err ->
                viewModel.notifyMessage("เตรียมข้อมูลส่งออกไม่สำเร็จ: ${err.localizedMessage ?: "เกิดข้อผิดพลาด"}")
            }
        }
    }

    fun triggerOpenPasteImport(hint: String? = null) {
        pasteImportHint = hint
        pasteImportError = null
        showPasteImportDialog = true
    }

    fun triggerImportJson() {
        suppressNextAutoLock = true
        val launchedPrimary = try {
            importDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            true
        } catch (_: Exception) {
            false
        }
        if (!launchedPrimary) {
            val launchedFallback = try {
                importGetContentLauncher.launch("*/*")
                true
            } catch (_: Exception) {
                false
            }
            if (!launchedFallback) {
                suppressNextAutoLock = false
                triggerOpenPasteImport(
                    hint = "อุปกรณ์นี้ไม่มีตัวเลือกไฟล์ระบบ (DocumentsUI) กรุณาวางข้อความ JSON เพื่อนำเข้าแทน"
                )
            }
        }
    }

    fun triggerSelectAutoBackupLocation() {
        suppressNextAutoLock = true
        val fileName = "portpulse_auto_backup.json"
        val launchedPrimary = try {
            autoBackupFileLauncher.launch(fileName)
            true
        } catch (_: Exception) {
            false
        }
        if (!launchedPrimary) {
            val launchedFallback = try {
                autoBackupAnyFileLauncher.launch(fileName)
                true
            } catch (_: Exception) {
                false
            }
            if (!launchedFallback) {
                suppressNextAutoLock = false
                viewModel.notifyMessage(
                    "อุปกรณ์นี้ไม่มีตัวเลือกไฟล์ระบบ (DocumentsUI) กรุณาใช้คัดลอกหรือส่งออก JSON แทน"
                )
            }
        }
    }

    LaunchedEffect(uiState.actionMessage, showBackupScreen) {
        val msg = uiState.actionMessage
        if (!showBackupScreen && !msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    LaunchedEffect(uiState.priceStatusMessage) {
        val msg = uiState.priceStatusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearPriceStatusMessage()
        }
    }

    BackHandler(enabled = selectedTab != MainTab.DASHBOARD) {
        selectedTab = MainTab.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = "PortPulse",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "PortPulse",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = selectedTab.labelTh,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showSecurityDialog = true },
                                modifier = Modifier.testTag("security_lock_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.securityConfig.isPinEnabled) {
                                        Icons.Default.Lock
                                    } else {
                                        Icons.Default.LockOpen
                                    },
                                    contentDescription = "ล็อกแอปด้วย PIN และลายนิ้วมือ",
                                    tint = LocalPortfolioColors.current.accentHighlight
                                )
                            }
                            IconButton(
                                onClick = { showBackupScreen = true },
                                modifier = Modifier.testTag("backup_screen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = "สำรองข้อมูล",
                                    tint = LocalPortfolioColors.current.accentHighlight
                                )
                            }
                        }
                    )

                    // Category Dropdown pinned right at the top (specific AssetCategory only, no "all")
                    TopCategoryDropdownBar(
                        selectedCategory = uiState.selectedCategory,
                        allHoldings = uiState.allHoldings,
                        onSelectCategory = viewModel::selectCategory,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.labelTh
                                    )
                                },
                                label = { Text(tab.labelTh) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingHolding = null
                        showAddEditSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_holding_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "เพิ่มสินทรัพย์"
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.labelTh
                                    )
                                },
                                label = { Text(tab.labelTh) },
                                colors = NavigationRailItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "กำลังโหลดข้อมูลพอร์ต...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        uiState.errorMessage != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.errorMessage ?: "เกิดข้อผิดพลาด",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            when (selectedTab) {
                                MainTab.DASHBOARD -> DashboardTab(
                                    uiState = uiState,
                                    onRefreshPrices = viewModel::refreshPrices,
                                    onAddHoldingClick = {
                                        editingHolding = null
                                        showAddEditSheet = true
                                    }
                                )
                                MainTab.ANALYTICS -> AnalyticsTab(
                                    uiState = uiState,
                                    onManageSectorsClick = { showManageSectorsDialog = true }
                                )
                                MainTab.HOLDINGS -> HoldingsTab(
                                    uiState = uiState,
                                    onEditHolding = { holding ->
                                        editingHolding = holding
                                        showAddEditSheet = true
                                    },
                                    onDeleteHolding = viewModel::deleteHolding,
                                    onViewDividends = viewModel::openDividendDialog
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditSheet) {
        AddEditHoldingSheet(
            existingHolding = editingHolding,
            activeCategory = editingHolding?.assetCategory ?: uiState.selectedCategory,
            managedSectors = uiState.managedSectors,
            newlyCreatedSectorId = newlyCreatedSectorId,
            isSaving = uiState.isSaving,
            onManageSectorsClick = { showManageSectorsDialog = true },
            onSearchAssets = { query, cat ->
                viewModel.searchAssets(query, cat)
            },
            onFetchPreviewQuote = { sym, cat, manual, curr ->
                viewModel.fetchPreviewQuote(sym, cat, manual, curr)
            },
            onDismiss = {
                showAddEditSheet = false
                editingHolding = null
                newlyCreatedSectorId = null
            },
            onSave = { symbol, displayName, category, sectorId, quantity, buyPrice, currency, manualPrice ->
                viewModel.saveHolding(
                    existingHolding = editingHolding,
                    symbol = symbol,
                    displayName = displayName,
                    category = category,
                    sectorId = sectorId,
                    quantity = quantity,
                    buyPricePerUnit = buyPrice,
                    currency = currency,
                    manualCurrentPrice = manualPrice,
                    onSaved = {
                        showAddEditSheet = false
                        editingHolding = null
                        newlyCreatedSectorId = null
                    }
                )
            }
        )
    }

    if (showManageSectorsDialog) {
        ManageSectorsDialog(
            sectors = uiState.managedSectors,
            stockHoldings = uiState.categorySummary.analyzedHoldings.filter { it.holding.assetCategory.isStock },
            onAddSector = viewModel::addSector,
            onDeleteSector = viewModel::deleteSector,
            onResetDefaults = viewModel::resetDefaultSectors,
            onSectorCreated = { created ->
                newlyCreatedSectorId = created.id
            },
            onDismiss = { showManageSectorsDialog = false }
        )
    }

    if (showBackupScreen) {
        BackupScreenDialog(
            allHoldings = uiState.allHoldings,
            autoBackupConfig = uiState.autoBackupConfig,
            actionMessage = uiState.actionMessage,
            onConsumeActionMessage = viewModel::clearActionMessage,
            onToggleAutoBackup = viewModel::toggleAutoBackup,
            onSelectAutoBackupLocation = { triggerSelectAutoBackupLocation() },
            onRunAutoBackupNow = { viewModel.runAutoBackupNow(showToast = true) },
            onExportJsonClick = { triggerExportJson() },
            onExportCopyShareClick = { triggerExportCopyShare() },
            onImportJsonClick = { triggerImportJson() },
            onImportPasteJsonClick = { triggerOpenPasteImport() },
            onDismiss = { showBackupScreen = false }
        )
    }

    if (fallbackExportJson != null) {
        val exportJsonText = fallbackExportJson!!
        val dateStamp = remember(exportJsonText) {
            SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        }
        val previewSnippet = remember(exportJsonText) {
            if (exportJsonText.length <= 1200) exportJsonText else exportJsonText.take(1200) + "\n..."
        }
        AlertDialog(
            onDismissRequest = { fallbackExportJson = null },
            title = { Text("ส่งออกข้อมูลพอร์ต (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "สามารถคัดลอกข้อความ JSON ด้านล่าง แชร์ไปยังแอปอื่น หรือบันทึกไปยังโฟลเดอร์ Downloads ได้ทันที",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        Text(
                            text = previewSnippet,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                    Button(
                        onClick = {
                            if (copyTextToClipboard(context, "PortPulse Backup JSON", exportJsonText)) {
                                viewModel.notifyMessage("คัดลอกข้อมูล JSON ไปยังคลิปบอร์ดแล้ว")
                            } else {
                                viewModel.notifyMessage("ไม่สามารถคัดลอกไปยังคลิปบอร์ดได้")
                            }
                            fallbackExportJson = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_copy_clipboard_button")
                    ) {
                        Text("คัดลอก JSON ทั้งหมด")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val fileName = "portpulse_backup_$dateStamp.json"
                                    val saveRes = saveJsonToMediaStoreDownloads(context, exportJsonText, fileName)
                                    if (saveRes.isSuccess) {
                                        viewModel.notifyMessage("บันทึกไฟล์ $fileName ลงโฟลเดอร์ Downloads แล้ว")
                                    } else {
                                        viewModel.notifyMessage(
                                            "บันทึกลง Downloads ไม่สำเร็จ: ${saveRes.exceptionOrNull()?.localizedMessage ?: "เกิดข้อผิดพลาด"}"
                                        )
                                    }
                                    fallbackExportJson = null
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_save_downloads_button")
                        ) {
                            Text("บันทึกลงโฟลเดอร์ Downloads")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            suppressNextAutoLock = true
                            val shared = shareJsonText(context, exportJsonText)
                            if (!shared) {
                                suppressNextAutoLock = false
                                viewModel.notifyMessage("ไม่พบแอปสำหรับแชร์ข้อความในอุปกรณ์นี้")
                            }
                            fallbackExportJson = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_share_button")
                    ) {
                        Text("แชร์ข้อความ JSON")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { fallbackExportJson = null }) {
                    Text("ปิด")
                }
            }
        )
    }

    if (showPasteImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            title = { Text("วางข้อความ JSON เพื่อนำเข้า") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!pasteImportHint.isNullOrBlank()) {
                        Text(
                            text = pasteImportHint!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = pasteImportText,
                        onValueChange = {
                            pasteImportText = it
                            pasteImportError = null
                        },
                        label = { Text("ข้อมูล JSON สำรองพอร์ต") },
                        placeholder = { Text("วางเนื้อหา JSON ที่ขึ้นต้นด้วย { ... } หรือ [ ... ]") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_import_json_field")
                    )
                    if (!pasteImportError.isNullOrBlank()) {
                        Text(
                            text = pasteImportError!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val clipText = readTextFromClipboard(context)
                            if (!clipText.isNullOrBlank()) {
                                pasteImportText = clipText
                                pasteImportError = null
                            } else {
                                pasteImportError = "ไม่พบข้อความในคลิปบอร์ด"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("วางจากคลิปบอร์ด")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val decoded = PortfolioJsonCodec.decodeWithSectors(pasteImportText)
                        decoded.onSuccess { payload ->
                            showPasteImportDialog = false
                            pasteImportText = ""
                            pasteImportError = null
                            pendingImportPayload = payload
                        }.onFailure { err ->
                            pasteImportError = err.message ?: "รูปแบบ JSON ไม่ถูกต้อง"
                        }
                    },
                    modifier = Modifier.testTag("paste_import_confirm_button")
                ) {
                    Text("ตรวจสอบและนำเข้า")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasteImportDialog = false
                        pasteImportError = null
                    }
                ) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    if (pendingImportPayload != null) {
        val payload = pendingImportPayload!!
        val parsed = payload.holdings
        val categoryBreakdown = AssetCategory.entries.mapNotNull { cat ->
            val count = parsed.count { it.assetCategory == cat }
            if (count > 0) "${cat.shortLabelTh} $count" else null
        }.joinToString(" • ")
        val previewSymbols = parsed.take(5).joinToString(", ") { it.symbol } +
            if (parsed.size > 5) " และอีก ${parsed.size - 5} รายการ" else ""
        AlertDialog(
            onDismissRequest = { pendingImportPayload = null },
            title = { Text("นำเข้าข้อมูลพอร์ต (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "พบสินทรัพย์ทั้งหมด ${parsed.size} รายการ ($previewSymbols)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (categoryBreakdown.isNotBlank()) {
                        Text(
                            text = "หมวดหมู่ในไฟล์: $categoryBreakdown",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "เลือกรูปแบบการนำเข้าข้อมูล:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            pendingImportPayload = null
                            viewModel.importHoldings(parsed, payload.sectors, ImportMode.MERGE)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_merge_button")
                    ) {
                        Text("เพิ่มต่อจากพอร์ตเดิม")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingImportPayload = null
                            viewModel.importHoldings(parsed, payload.sectors, ImportMode.REPLACE)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_replace_button")
                    ) {
                        Text("แทนที่ข้อมูลพอร์ตทั้งหมด")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingImportPayload = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    if (showSecurityDialog) {
        SecuritySettingsDialog(
            securityConfig = uiState.securityConfig,
            onSetupPin = viewModel::setupPin,
            onVerifyCurrentPin = viewModel::verifyCurrentPin,
            onToggleBiometric = viewModel::toggleBiometricLock,
            onDisablePinLock = viewModel::disablePinLock,
            onLockNow = {
                showSecurityDialog = false
                viewModel.lockApp()
            },
            onLaunchExternalIntent = { suppressNextAutoLock = true },
            onNotifyMessage = viewModel::notifyMessage,
            onDismiss = { showSecurityDialog = false }
        )
    }

    if (dividendDialogState != null) {
        DividendInfoDialog(
            state = dividendDialogState!!,
            onRetry = viewModel::openDividendDialog,
            onDismiss = viewModel::closeDividendDialog
        )
    }

    if (isAppLocked && uiState.securityConfig.isPinEnabled) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            PinLockScreen(
                securityConfig = uiState.securityConfig,
                onVerifyPin = viewModel::unlockWithPin,
                onBiometricUnlockSuccess = viewModel::unlockWithBiometric
            )
        }
    }
}

@Composable
private fun TopCategoryDropdownBar(
    selectedCategory: AssetCategory,
    allHoldings: List<Holding>,
    onSelectCategory: (AssetCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val activeCount = allHoldings.count { it.assetCategory == selectedCategory }
    val activeLabel = selectedCategory.labelTh
    val activeIcon = categoryMenuIcon(selectedCategory)
    val activeAccent = categoryAccentColor(selectedCategory)
    val activeContainer = categoryContainerColor(selectedCategory)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("top_category_dropdown"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(activeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeIcon,
                        contentDescription = activeLabel,
                        tint = activeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "หมวดหมู่สินทรัพย์",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$activeCount รายการ",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "เปิดเมนูเลือกหมวดหมู่",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AssetCategory.entries.forEach { cat ->
                val count = allHoldings.count { it.assetCategory == cat }
                CategoryDropdownRow(
                    label = cat.labelTh,
                    count = count,
                    category = cat,
                    icon = categoryMenuIcon(cat),
                    isSelected = selectedCategory == cat,
                    onClick = {
                        expanded = false
                        onSelectCategory(cat)
                    },
                    testTag = "top_dropdown_${cat.name}"
                )
            }
        }
    }
}

@Composable
private fun CategoryDropdownRow(
    label: String,
    count: Int,
    category: AssetCategory,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val itemAccent = categoryAccentColor(category)
    val itemContainer = categoryContainerColor(category)

    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) itemAccent else MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (isSelected) itemAccent else itemContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else itemAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = itemAccent
            )
        },
        trailingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "กำลังเลือก",
                    tint = itemAccent
                )
            }
        } else null,
        onClick = onClick,
        modifier = Modifier
            .background(
                if (isSelected) itemContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surface
            )
            .testTag(testTag)
    )
}

private fun categoryMenuIcon(category: AssetCategory): ImageVector = when (category) {
    AssetCategory.THAI_STOCK -> Icons.AutoMirrored.Filled.ShowChart
    AssetCategory.GLOBAL_STOCK -> Icons.Default.Public
    AssetCategory.MUTUAL_FUND -> Icons.Default.AccountBalance
    AssetCategory.GOLD -> Icons.Default.Diamond
    AssetCategory.CRYPTO -> Icons.Default.CurrencyBitcoin
}

private suspend fun writeJsonToUriSafely(
    context: Context,
    uri: Uri,
    jsonPayload: String
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val bytes = jsonPayload.toByteArray(Charsets.UTF_8)
        val wroteTruncated = try {
            resolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } != null
        } catch (_: UnsupportedOperationException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
        if (!wroteTruncated) {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: error("ไม่สามารถเปิดไฟล์เพื่อบันทึกได้")
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun readAndDecodeBackupUri(
    context: Context,
    uri: Uri
): Result<PortfolioBackupPayload> {
    val readResult = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("ไม่สามารถอ่านไฟล์ที่เลือกได้")
            val rawText = stream.use { input ->
                val buffer = ByteArray(8192)
                val out = ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > PortfolioJsonCodec.MAX_IMPORT_JSON_BYTES) {
                        error("ไฟล์มีขนาดใหญ่เกิน 5 MB กรุณาเลือกไฟล์สำรอง JSON ที่ถูกต้อง")
                    }
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
            Result.success(rawText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("อ่านไฟล์ไม่สำเร็จ: ${e.localizedMessage ?: "เกิดข้อผิดพลาด"}")
            )
        } catch (_: OutOfMemoryError) {
            Result.failure(
                IllegalArgumentException("ไฟล์มีขนาดใหญ่เกินไป ไม่สามารถอ่านได้")
            )
        }
    }

    val rawJson = readResult.getOrElse { return Result.failure(it) }
    return withContext(Dispatchers.Default) {
        PortfolioJsonCodec.decodeWithSectors(rawJson)
    }
}

private suspend fun saveJsonToMediaStoreDownloads(
    context: Context,
    jsonPayload: String,
    fileName: String
): Result<String> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return@withContext Result.failure(UnsupportedOperationException("ไม่รองรับการบันทึกลง Downloads โดยตรงใน Android เวอร์ชันนี้"))
    }
    val resolver = context.contentResolver
    var insertedUri: Uri? = null
    try {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("ไม่สามารถสร้างไฟล์ในโฟลเดอร์ Downloads ได้")
        insertedUri = uri

        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(jsonPayload.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: error("ไม่สามารถเขียนข้อมูลลงไฟล์ Downloads ได้")

        val publishValues = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        resolver.update(uri, publishValues, null, null)
        Result.success(fileName)
    } catch (e: CancellationException) {
        insertedUri?.let { runCatching { resolver.delete(it, null, null) } }
        throw e
    } catch (e: Exception) {
        insertedUri?.let { runCatching { resolver.delete(it, null, null) } }
        Result.failure(e)
    }
}

private fun copyTextToClipboard(context: Context, label: String, text: String): Boolean {
    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        true
    }.getOrDefault(false)
}

private fun readTextFromClipboard(context: Context): String? {
    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount <= 0) return null
        clip.getItemAt(0).coerceToText(context)?.toString()
    }.getOrNull()
}

private fun shareJsonText(context: Context, json: String): Boolean {
    return runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PortPulse Backup JSON")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        val chooser = Intent.createChooser(sendIntent, "แชร์ข้อมูลสำรองพอร์ต (JSON)")
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}
