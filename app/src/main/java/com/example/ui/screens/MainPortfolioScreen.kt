package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.categoryOnContainerColor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class GetContentChooserContract(
    private val chooserTitle: String
) : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        val pickIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = input
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        return Intent.createChooser(pickIntent, chooserTitle)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
    }
}

private data class InAppFileManagerRequest(
    val mode: InAppFileManagerMode,
    val suggestedFileName: String,
    val exportJson: String? = null
)

private enum class MainTab(
    val labelTh: String,
    val icon: ImageVector
) {
    DASHBOARD("ภาพรวม", Icons.Default.PieChart),
    ANALYTICS("วิเคราะห์กลุ่ม", Icons.Default.Analytics),
    HOLDINGS("สินทรัพย์", Icons.AutoMirrored.Filled.ListAlt)
}

private enum class OverviewTabMenu(
    val labelTh: String,
    val icon: ImageVector
) {
    OVERVIEW("ภาพรวม", Icons.Default.PieChart),
    PLAN("แผน", Icons.Default.TrackChanges)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPortfolioScreen(
    onCategoryChanged: (AssetCategory?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope { Dispatchers.Main.immediate }

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
    var selectedOverviewTab by remember { mutableStateOf(OverviewTabMenu.OVERVIEW) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingHolding by remember { mutableStateOf<Holding?>(null) }
    var showBackupScreen by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showManageSectorsDialog by remember { mutableStateOf(false) }
    var newlyCreatedSectorId by remember { mutableStateOf<String?>(null) }
    var pendingImportPayload by remember { mutableStateOf<PortfolioBackupPayload?>(null) }
    var fileManagerRequest by remember { mutableStateOf<InAppFileManagerRequest?>(null) }
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

    LaunchedEffect(uiState.activeThemeCategory) {
        onCategoryChanged(uiState.activeThemeCategory)
    }

    var pendingTreeExportName by remember { mutableStateOf("portpulse_backup.json") }

    fun createJsonFileInTree(treeUri: Uri, fileName: String): Result<Uri> = runCatching {
        AndroidAutoBackupWriter.persistWritePermission(context, treeUri)
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        DocumentsContract.createDocument(
            context.contentResolver,
            parentDocUri,
            "application/json",
            fileName
        ) ?: error("ไม่สามารถสร้างไฟล์ในโฟลเดอร์ที่เลือกได้")
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

    val exportTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) {
            viewModel.consumePendingExportJson()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val created = withContext(Dispatchers.IO) {
                createJsonFileInTree(treeUri, pendingTreeExportName)
            }
            created.onSuccess { docUri ->
                handleExportUriSelected(docUri)
            }.onFailure { err ->
                viewModel.consumePendingExportJson()
                viewModel.notifyMessage(
                    "ส่งออกไฟล์ไม่สำเร็จ: ${err.localizedMessage ?: "เกิดข้อผิดพลาด"}"
                )
            }
        }
    }

    val autoBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> handleAutoBackupUriSelected(uri) }

    val autoBackupAnyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri -> handleAutoBackupUriSelected(uri) }

    val autoBackupTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val created = withContext(Dispatchers.IO) {
                createJsonFileInTree(treeUri, "portpulse_auto_backup.json")
            }
            created.onSuccess { docUri ->
                handleAutoBackupUriSelected(docUri)
            }.onFailure { err ->
                viewModel.notifyMessage(
                    "ตั้งค่าตำแหน่งสำรองอัตโนมัติไม่สำเร็จ: ${err.localizedMessage ?: "เกิดข้อผิดพลาด"}"
                )
            }
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handleImportUriSelected(uri) }

    val importGetContentLauncher = rememberLauncherForActivityResult(
        contract = GetContentChooserContract("เลือกไฟล์สำรองจาก File Explorer")
    ) { uri -> handleImportUriSelected(uri) }

    fun triggerExportJson() {
        scope.launch {
            if (uiState.allHoldings.isEmpty() && !uiState.portfolioPlan.hasContent) {
                viewModel.notifyMessage("ยังไม่มีข้อมูลสินทรัพย์หรือแผนการลงทุนสำหรับส่งออก")
                return@launch
            }
            val res = viewModel.exportPortfolioJson()
            res.onSuccess { json ->
                viewModel.setPendingExportJson(json)
                val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                val suggestedName = "portpulse_backup_$dateStamp.json"
                pendingTreeExportName = suggestedName
                suppressNextAutoLock = true
                var lastErr: Exception? = null
                val launchedPrimary = try {
                    exportDocumentLauncher.launch(suggestedName)
                    true
                } catch (e: Exception) {
                    lastErr = e
                    android.util.Log.w("MainPortfolioScreen", "exportDocumentLauncher failed", e)
                    false
                }
                if (!launchedPrimary) {
                    val launchedAny = try {
                        exportAnyDocumentLauncher.launch(suggestedName)
                        true
                    } catch (e: Exception) {
                        lastErr = e
                        android.util.Log.w("MainPortfolioScreen", "exportAnyDocumentLauncher failed", e)
                        false
                    }
                    if (!launchedAny) {
                        val launchedTree = try {
                            exportTreeLauncher.launch(null)
                            true
                        } catch (e: Exception) {
                            lastErr = e
                            android.util.Log.w("MainPortfolioScreen", "exportTreeLauncher failed", e)
                            false
                        }
                        if (!launchedTree) {
                            suppressNextAutoLock = false
                            viewModel.consumePendingExportJson()
                            lastErr?.let { err ->
                                viewModel.notifyMessage("เปิด File Explorer ไม่สำเร็จ (${err::class.java.simpleName}: ${err.localizedMessage ?: err.message})")
                            }
                            fileManagerRequest = InAppFileManagerRequest(
                                mode = InAppFileManagerMode.EXPORT_JSON,
                                suggestedFileName = suggestedName,
                                exportJson = json
                            )
                        }
                    }
                }
            }.onFailure { err ->
                viewModel.notifyMessage("เตรียมไฟล์ส่งออกไม่สำเร็จ: ${err.localizedMessage ?: "เกิดข้อผิดพลาด"}")
            }
        }
    }

    fun triggerImportJson() {
        suppressNextAutoLock = true
        var lastErr: Exception? = null
        val launchedPrimary = try {
            importDocumentLauncher.launch(arrayOf("*/*"))
            true
        } catch (e: Exception) {
            lastErr = e
            android.util.Log.w("MainPortfolioScreen", "importDocumentLauncher failed", e)
            false
        }
        if (!launchedPrimary) {
            val launchedFallback = try {
                importGetContentLauncher.launch("*/*")
                true
            } catch (e: Exception) {
                lastErr = e
                android.util.Log.w("MainPortfolioScreen", "importGetContentLauncher failed", e)
                false
            }
            if (!launchedFallback) {
                suppressNextAutoLock = false
                lastErr?.let { err ->
                    viewModel.notifyMessage("เปิด File Explorer ไม่สำเร็จ (${err::class.java.simpleName}: ${err.localizedMessage ?: err.message})")
                }
                fileManagerRequest = InAppFileManagerRequest(
                    mode = InAppFileManagerMode.IMPORT_JSON,
                    suggestedFileName = ""
                )
            }
        }
    }

    fun triggerSelectAutoBackupLocation() {
        suppressNextAutoLock = true
        val fileName = "portpulse_auto_backup.json"
        var lastErr: Exception? = null
        val launchedPrimary = try {
            autoBackupFileLauncher.launch(fileName)
            true
        } catch (e: Exception) {
            lastErr = e
            android.util.Log.w("MainPortfolioScreen", "autoBackupFileLauncher failed", e)
            false
        }
        if (!launchedPrimary) {
            val launchedAny = try {
                autoBackupAnyFileLauncher.launch(fileName)
                true
            } catch (e: Exception) {
                lastErr = e
                android.util.Log.w("MainPortfolioScreen", "autoBackupAnyFileLauncher failed", e)
                false
            }
            if (!launchedAny) {
                val launchedTree = try {
                    autoBackupTreeLauncher.launch(null)
                    true
                } catch (e: Exception) {
                    lastErr = e
                    android.util.Log.w("MainPortfolioScreen", "autoBackupTreeLauncher failed", e)
                    false
                }
                if (!launchedTree) {
                    suppressNextAutoLock = false
                    lastErr?.let { err ->
                        viewModel.notifyMessage("เปิด File Explorer ไม่สำเร็จ (${err::class.java.simpleName}: ${err.localizedMessage ?: err.message})")
                    }
                    fileManagerRequest = InAppFileManagerRequest(
                        mode = InAppFileManagerMode.SELECT_AUTO_BACKUP,
                        suggestedFileName = fileName
                    )
                }
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

    BackHandler(
        enabled = if (uiState.isOverviewSelected) {
            selectedOverviewTab != OverviewTabMenu.OVERVIEW
        } else {
            selectedTab != MainTab.DASHBOARD
        }
    ) {
        if (uiState.isOverviewSelected) {
            selectedOverviewTab = OverviewTabMenu.OVERVIEW
        } else {
            selectedTab = MainTab.DASHBOARD
        }
    }

    val visibleMainTabs = remember(uiState.selectedCategory) {
        visibleMainTabsFor(uiState.selectedCategory)
    }
    val effectiveMainTab = if (selectedTab in visibleMainTabs) selectedTab else MainTab.DASHBOARD

    LaunchedEffect(uiState.selectedCategory) {
        if (selectedTab !in visibleMainTabsFor(uiState.selectedCategory)) {
            selectedTab = MainTab.DASHBOARD
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val portfolioColors = LocalPortfolioColors.current

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopCategoryDropdownBar(
                    isOverviewSelected = uiState.isOverviewSelected,
                    selectedCategory = uiState.selectedCategory,
                    allHoldings = uiState.allHoldings,
                    isPinEnabled = uiState.securityConfig.isPinEnabled,
                    onSelectOverview = {
                        viewModel.selectOverview()
                    },
                    onSelectCategory = viewModel::selectCategory,
                    onSecurityClick = { showSecurityDialog = true },
                    onBackupClick = {
                        viewModel.clearActionMessage()
                        showBackupScreen = true
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    SlimBottomNavBar(
                        isOverviewSelected = uiState.isOverviewSelected,
                        visibleMainTabs = visibleMainTabs,
                        selectedMainTab = effectiveMainTab,
                        selectedOverviewTab = selectedOverviewTab,
                        onSelectMainTab = { selectedTab = it },
                        onSelectOverviewTab = { selectedOverviewTab = it }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(if (useRail) Modifier.navigationBarsPadding() else Modifier)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            if (uiState.isOverviewSelected) {
                                OverviewTabMenu.entries.forEach { tab ->
                                    NavigationRailItem(
                                        selected = selectedOverviewTab == tab,
                                        onClick = { selectedOverviewTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.labelTh
                                            )
                                        },
                                        label = { Text(tab.labelTh) },
                                        colors = NavigationRailItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = portfolioColors.mutedSlate,
                                            unselectedTextColor = portfolioColors.mutedSlate
                                        ),
                                        modifier = Modifier.testTag("overview_rail_tab_${tab.name}")
                                    )
                                }
                            } else {
                                visibleMainTabs.forEach { tab ->
                                    NavigationRailItem(
                                        selected = effectiveMainTab == tab,
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
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = portfolioColors.mutedSlate,
                                            unselectedTextColor = portfolioColors.mutedSlate
                                        ),
                                        modifier = Modifier.testTag("rail_tab_${tab.name}")
                                    )
                                }
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
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                            uiState.isOverviewSelected -> {
                                when (selectedOverviewTab) {
                                    OverviewTabMenu.OVERVIEW -> OverviewTab(
                                        uiState = uiState,
                                        onRefreshPrices = viewModel::refreshPrices,
                                        onToggleFilterCategory = viewModel::toggleOverviewFilterCategory,
                                        onSelectAllFilters = viewModel::selectAllOverviewFilterCategories,
                                        onOpenCategory = { cat ->
                                            selectedTab = MainTab.DASHBOARD
                                            viewModel.selectCategory(cat)
                                        },
                                        onOpenPlanTab = {
                                            selectedOverviewTab = OverviewTabMenu.PLAN
                                        }
                                    )
                                    OverviewTabMenu.PLAN -> OverviewPlanTab(
                                        uiState = uiState,
                                        onSavePlan = viewModel::savePortfolioPlan
                                    )
                                }
                            }
                            else -> {
                                when (effectiveMainTab) {
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

                        if (!uiState.isOverviewSelected) {
                            FloatingActionButton(
                                onClick = {
                                    editingHolding = null
                                    showAddEditSheet = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .testTag("add_holding_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "เพิ่มสินทรัพย์"
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
            hasPortfolioPlan = uiState.portfolioPlan.hasContent,
            autoBackupConfig = uiState.autoBackupConfig,
            actionMessage = uiState.actionMessage,
            onConsumeActionMessage = viewModel::clearActionMessage,
            onToggleAutoBackup = viewModel::toggleAutoBackup,
            onSelectAutoBackupLocation = { triggerSelectAutoBackupLocation() },
            onRunAutoBackupNow = { viewModel.runAutoBackupNow(showToast = true) },
            onExportJsonClick = { triggerExportJson() },
            onImportJsonClick = { triggerImportJson() },
            onDismiss = {
                viewModel.clearActionMessage()
                showBackupScreen = false
            }
        )
    }

    if (fileManagerRequest != null) {
        val request = fileManagerRequest!!
        InAppFileManagerDialog(
            mode = request.mode,
            suggestedFileName = request.suggestedFileName,
            onSelect = { selection ->
                fileManagerRequest = null
                when (request.mode) {
                    InAppFileManagerMode.EXPORT_JSON -> {
                        val json = request.exportJson ?: return@InAppFileManagerDialog
                        scope.launch {
                            when (selection) {
                                is InAppFileSelection.SaveFile -> {
                                    val res = writeJsonToLocalFile(selection.targetFile, json)
                                    if (res.isSuccess) {
                                        viewModel.notifyMessage("ส่งออกข้อมูลพอร์ตเป็นไฟล์ ${selection.targetFile.name} เรียบร้อย")
                                    } else {
                                        viewModel.notifyMessage(
                                            "ส่งออกไฟล์ไม่สำเร็จ: ${res.exceptionOrNull()?.localizedMessage ?: "เกิดข้อผิดพลาด"}"
                                        )
                                    }
                                }
                                is InAppFileSelection.SaveMediaStore -> {
                                    val res = saveJsonToMediaStoreCollection(
                                        context = context,
                                        jsonPayload = json,
                                        collection = selection.collection,
                                        fileName = selection.fileName
                                    )
                                    if (res.isSuccess) {
                                        viewModel.notifyMessage("ส่งออกไฟล์ ${selection.fileName} ลงโฟลเดอร์ ${selection.collection.labelTh} เรียบร้อย")
                                    } else {
                                        viewModel.notifyMessage(
                                            "ส่งออกไฟล์ไม่สำเร็จ: ${res.exceptionOrNull()?.localizedMessage ?: "เกิดข้อผิดพลาด"}"
                                        )
                                    }
                                }
                                is InAppFileSelection.OpenUri -> Unit
                            }
                        }
                    }
                    InAppFileManagerMode.IMPORT_JSON -> {
                        if (selection is InAppFileSelection.OpenUri) {
                            handleImportUriSelected(selection.uri)
                        }
                    }
                    InAppFileManagerMode.SELECT_AUTO_BACKUP -> {
                        if (selection is InAppFileSelection.SaveFile) {
                            val fileUri = Uri.fromFile(selection.targetFile)
                            viewModel.configureAutoBackupDestination(fileUri.toString(), selection.targetFile.name)
                        }
                    }
                }
            },
            onDismiss = { fileManagerRequest = null }
        )
    }

    if (pendingImportPayload != null) {
        val payload = pendingImportPayload!!
        val parsed = payload.holdings
        val hasImportedPlan = payload.plan?.hasContent == true
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
                        text = if (parsed.isNotEmpty()) {
                            "พบสินทรัพย์ทั้งหมด ${parsed.size} รายการ ($previewSymbols)"
                        } else {
                            "พบข้อมูลแผนการลงทุนในไฟล์สำรอง"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (categoryBreakdown.isNotBlank()) {
                        Text(
                            text = "หมวดหมู่ในไฟล์: $categoryBreakdown",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (hasImportedPlan) {
                        Text(
                            text = "รวมแผนสัดส่วนเป้าหมายและโน้ตการลงทุนในไฟล์นี้ด้วย",
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
                            viewModel.importHoldings(parsed, payload.sectors, ImportMode.MERGE, payload.plan)
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
                            viewModel.importHoldings(parsed, payload.sectors, ImportMode.REPLACE, payload.plan)
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
}

@Composable
private fun TopCategoryDropdownBar(
    isOverviewSelected: Boolean,
    selectedCategory: AssetCategory,
    allHoldings: List<Holding>,
    isPinEnabled: Boolean,
    onSelectOverview: () -> Unit,
    onSelectCategory: (AssetCategory) -> Unit,
    onSecurityClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val portfolioColors = LocalPortfolioColors.current

    val activeCount = if (isOverviewSelected) {
        allHoldings.size
    } else {
        allHoldings.count { it.assetCategory == selectedCategory }
    }
    val activeLabel = if (isOverviewSelected) {
        "ภาพรวมทุกสินทรัพย์ (Overview)"
    } else {
        selectedCategory.labelTh
    }
    val activeIcon = if (isOverviewSelected) Icons.Default.Dashboard else categoryMenuIcon(selectedCategory)
    val themeCat: AssetCategory? = if (isOverviewSelected) null else selectedCategory
    val activeOnContainer = categoryOnContainerColor(themeCat)
    val activeContainer = categoryContainerColor(themeCat)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(portfolioColors.navyHeroDeep)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .testTag("top_category_dropdown")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = activeContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = activeIcon,
                                    contentDescription = activeLabel,
                                    tint = activeOnContainer,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = activeLabel,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                softWrap = true
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$activeCount รายการ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White.copy(alpha = 0.86f)
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                                Text(
                                    text = "เปลี่ยน",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "เปิดเมนูเลือกหมวดหมู่",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .widthIn(min = 280.dp)
                        .fillMaxWidth(0.92f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    CategoryDropdownRow(
                        label = "ภาพรวมทุกสินทรัพย์ (Overview)",
                        count = allHoldings.size,
                        category = null,
                        icon = Icons.Default.Dashboard,
                        isSelected = isOverviewSelected,
                        onClick = {
                            expanded = false
                            onSelectOverview()
                        },
                        testTag = "top_dropdown_OVERVIEW"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    )
                    AssetCategory.entries.forEach { cat ->
                        val count = allHoldings.count { it.assetCategory == cat }
                        CategoryDropdownRow(
                            label = cat.labelTh,
                            count = count,
                            category = cat,
                            icon = categoryMenuIcon(cat),
                            isSelected = !isOverviewSelected && selectedCategory == cat,
                            onClick = {
                                expanded = false
                                onSelectCategory(cat)
                            },
                            testTag = "top_dropdown_${cat.name}"
                        )
                    }
                }
            }

            IconButton(
                onClick = onSecurityClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("security_lock_button")
            ) {
                Icon(
                    imageVector = if (isPinEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "ล็อกแอปด้วย PIN และลายนิ้วมือ",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(
                onClick = onBackupClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("backup_screen_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = "สำรองข้อมูล",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

private fun visibleMainTabsFor(category: AssetCategory): List<MainTab> = when (category) {
    AssetCategory.GOLD, AssetCategory.CRYPTO -> listOf(MainTab.DASHBOARD, MainTab.HOLDINGS)
    else -> MainTab.entries
}

@Composable
private fun SlimBottomNavBar(
    isOverviewSelected: Boolean,
    visibleMainTabs: List<MainTab>,
    selectedMainTab: MainTab,
    selectedOverviewTab: OverviewTabMenu,
    onSelectMainTab: (MainTab) -> Unit,
    onSelectOverviewTab: (OverviewTabMenu) -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOverviewSelected) {
                OverviewTabMenu.entries.forEach { tab ->
                    SlimBottomNavItem(
                        label = tab.labelTh,
                        icon = tab.icon,
                        selected = selectedOverviewTab == tab,
                        mutedColor = portfolioColors.mutedSlate,
                        onClick = { onSelectOverviewTab(tab) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overview_nav_tab_${tab.name}")
                    )
                }
            } else {
                visibleMainTabs.forEach { tab ->
                    SlimBottomNavItem(
                        label = tab.labelTh,
                        icon = tab.icon,
                        selected = selectedMainTab == tab,
                        mutedColor = portfolioColors.mutedSlate,
                        onClick = { onSelectMainTab(tab) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_tab_${tab.name}")
                    )
                }
            }
        }
    }
}

@Composable
private fun SlimBottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    mutedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val contentColor = if (selected) activeColor else mutedColor

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryDropdownRow(
    label: String,
    count: Int,
    category: AssetCategory?,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val itemAccent = categoryAccentColor(category)
    val itemContainer = categoryContainerColor(category)
    val itemOnContainer = categoryOnContainerColor(category)

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
                    color = if (isSelected) itemAccent else MaterialTheme.colorScheme.onSurface,
                    softWrap = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Surface(
                    color = if (isSelected) itemAccent else itemContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else itemOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(itemContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = itemOnContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
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
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
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
        if (uri.scheme == "file") {
            val path = uri.path ?: error("ไม่พบตำแหน่งไฟล์ที่เลือก")
            return@withContext writeJsonToLocalFile(File(path), jsonPayload)
        }
        val resolver = context.contentResolver
        val bytes = jsonPayload.toByteArray(Charsets.UTF_8)
        val wroteTruncated = try {
            resolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } != null
        } catch (_: Exception) {
            false
        }
        if (!wroteTruncated) {
            val wroteWithPfd = try {
                resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { fos ->
                        fos.channel.truncate(0L)
                        fos.write(bytes)
                        fos.flush()
                    }
                } != null
            } catch (_: Exception) {
                false
            }
            if (!wroteWithPfd) {
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                } ?: error("ไม่สามารถเปิดไฟล์เพื่อบันทึกได้")
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun writeJsonToLocalFile(
    targetFile: File,
    jsonPayload: String
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile, false).use { stream ->
            stream.write(jsonPayload.toByteArray(Charsets.UTF_8))
            stream.flush()
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
            val stream = if (uri.scheme == "file") {
                val path = uri.path ?: error("ไม่พบตำแหน่งไฟล์ที่เลือก")
                File(path).inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
                    ?: error("ไม่สามารถอ่านไฟล์ที่เลือกได้")
            }
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

private suspend fun saveJsonToMediaStoreCollection(
    context: Context,
    jsonPayload: String,
    collection: MediaBackupCollection,
    fileName: String
): Result<String> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return@withContext Result.failure(
            UnsupportedOperationException("ไม่รองรับการบันทึกลง ${collection.labelTh} โดยตรงใน Android เวอร์ชันนี้")
        )
    }
    val resolver = context.contentResolver
    val targetUri = when (collection) {
        MediaBackupCollection.DOWNLOADS -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        MediaBackupCollection.DOCUMENTS -> MediaStore.Files.getContentUri("external")
    }
    var insertedUri: Uri? = null
    try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, collection.relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(targetUri, values)
            ?: error("ไม่สามารถสร้างไฟล์ในโฟลเดอร์ ${collection.labelTh} ได้")
        insertedUri = uri

        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(jsonPayload.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: error("ไม่สามารถเขียนข้อมูลลงโฟลเดอร์ ${collection.labelTh} ได้")

        val publishValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
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
