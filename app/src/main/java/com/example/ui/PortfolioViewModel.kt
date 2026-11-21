package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSecurityConfig
import com.example.data.AutoBackupConfig
import com.example.data.AutoBackupWriter
import com.example.data.ImportMode
import com.example.data.LiveQuote
import com.example.data.MarketPriceService
import com.example.data.MarketSnapshot
import com.example.data.PinVerifyResult
import com.example.data.PortfolioJsonCodec
import com.example.data.PortfolioRepository
import com.example.data.PortfolioSettingsStore
import com.example.data.QuoteSource
import com.example.model.AnalyzedHolding
import com.example.model.AssetCategory
import com.example.model.Holding
import com.example.model.ManagedSector
import com.example.model.PortfolioAnalyzer
import com.example.model.PortfolioPlan
import com.example.model.PortfolioSummary
import com.example.model.StockSector
import com.example.ui.screens.DividendUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HoldingsLoadState {
    data object Loading : HoldingsLoadState
    data class Success(val holdings: List<Holding>) : HoldingsLoadState
    data class Error(val message: String) : HoldingsLoadState
}

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val allHoldings: List<Holding> = emptyList(),
    val selectedCategory: AssetCategory = AssetCategory.THAI_STOCK,
    val isOverviewSelected: Boolean = false,
    val overviewFilterCategories: Set<AssetCategory> = AssetCategory.entries.toSet(),
    val categorySummary: PortfolioSummary = PortfolioAnalyzer.analyze(emptyList(), MarketSnapshot()),
    val overviewSummary: PortfolioSummary = PortfolioAnalyzer.analyze(emptyList(), MarketSnapshot()),
    val fullPortfolioSummary: PortfolioSummary = PortfolioAnalyzer.analyze(emptyList(), MarketSnapshot()),
    val filteredHoldings: List<AnalyzedHolding> = emptyList(),
    val managedSectors: List<ManagedSector> = StockSector.defaultManagedSectors(),
    val portfolioPlan: PortfolioPlan = PortfolioPlan(),
    val autoBackupConfig: AutoBackupConfig = AutoBackupConfig(),
    val securityConfig: AppSecurityConfig = AppSecurityConfig(),
    val isRefreshingPrices: Boolean = false,
    val priceStatusMessage: String? = null,
    val actionMessage: String? = null,
    val isSaving: Boolean = false
) {
    val activeThemeCategory: AssetCategory?
        get() = if (isOverviewSelected) null else selectedCategory
}

class PortfolioViewModel(
    private val repository: PortfolioRepository,
    private val marketPriceService: MarketPriceService,
    private val settingsStore: PortfolioSettingsStore,
    private val autoBackupWriter: AutoBackupWriter? = null
) : ViewModel() {

    private val _marketSnapshot = MutableStateFlow(MarketSnapshot())
    private val _selectedCategory = MutableStateFlow(AssetCategory.THAI_STOCK)
    private val _isOverviewSelected = MutableStateFlow(false)
    private val _overviewFilterCategories = MutableStateFlow(AssetCategory.entries.toSet())
    private val _isRefreshingPrices = MutableStateFlow(false)
    private val _priceStatusMessage = MutableStateFlow<String?>(null)
    private val _actionMessage = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _isAppLocked = MutableStateFlow(settingsStore.securityConfig.value.isPinEnabled)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()
    private val _dividendDialogState = MutableStateFlow<DividendUiState?>(null)
    val dividendDialogState: StateFlow<DividendUiState?> = _dividendDialogState.asStateFlow()

    private var lastFetchedSignature: String? = null
    private var lastDataSignature: String? = null
    private var refreshJob: Job? = null
    private var autoBackupJob: Job? = null
    private var dividendJob: Job? = null
    private var pendingExportJson: String? = null

    private val holdingsState: StateFlow<HoldingsLoadState> = repository.observeHoldings()
        .map<List<Holding>, HoldingsLoadState> { HoldingsLoadState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing local holdings", error)
            emit(HoldingsLoadState.Error(error.message ?: "ไม่สามารถโหลดข้อมูลพอร์ตจากเครื่องได้"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HoldingsLoadState.Loading
        )

    private data class ScopeSelection(
        val category: AssetCategory,
        val isOverview: Boolean,
        val overviewFilters: Set<AssetCategory>
    )

    private data class RefreshControls(
        val scope: ScopeSelection,
        val isRefreshing: Boolean,
        val priceStatus: String?,
        val actionMsg: String?,
        val isSaving: Boolean
    )

    private data class SettingsBundle(
        val sectors: List<ManagedSector>,
        val backupConfig: AutoBackupConfig,
        val securityConfig: AppSecurityConfig,
        val plan: PortfolioPlan
    )

    private val scopeSelectionFlow = combine(
        _selectedCategory,
        _isOverviewSelected,
        _overviewFilterCategories
    ) { category, isOverview, filters ->
        ScopeSelection(category, isOverview, filters)
    }

    private val controlsFlow = combine(
        scopeSelectionFlow,
        _isRefreshingPrices,
        _priceStatusMessage,
        _actionMessage,
        _isSaving
    ) { scope, isRefreshing, priceStatus, actionMsg, isSaving ->
        RefreshControls(scope, isRefreshing, priceStatus, actionMsg, isSaving)
    }

    private val settingsBundleFlow = combine(
        settingsStore.managedSectors,
        settingsStore.autoBackupConfig,
        settingsStore.securityConfig,
        settingsStore.portfolioPlan
    ) { sectors, backupConfig, securityConfig, plan ->
        SettingsBundle(sectors, backupConfig, securityConfig, plan)
    }

    val uiState: StateFlow<PortfolioUiState> = combine(
        holdingsState,
        _marketSnapshot,
        settingsBundleFlow,
        controlsFlow
    ) { loadState, snapshot, settings, controls ->
        val (sectors, backupConfig, securityConfig, plan) = settings
        val scope = controls.scope
        when (loadState) {
            HoldingsLoadState.Loading -> PortfolioUiState(
                isLoading = true,
                selectedCategory = scope.category,
                isOverviewSelected = scope.isOverview,
                overviewFilterCategories = scope.overviewFilters,
                managedSectors = sectors,
                portfolioPlan = plan,
                autoBackupConfig = backupConfig,
                securityConfig = securityConfig,
                isRefreshingPrices = controls.isRefreshing,
                isSaving = controls.isSaving
            )
            is HoldingsLoadState.Error -> PortfolioUiState(
                isLoading = false,
                errorMessage = loadState.message,
                selectedCategory = scope.category,
                isOverviewSelected = scope.isOverview,
                overviewFilterCategories = scope.overviewFilters,
                managedSectors = sectors,
                portfolioPlan = plan,
                autoBackupConfig = backupConfig,
                securityConfig = securityConfig,
                isRefreshingPrices = controls.isRefreshing,
                isSaving = controls.isSaving
            )
            is HoldingsLoadState.Success -> {
                val categoryHoldings = loadState.holdings.filter {
                    it.assetCategory == scope.category
                }
                val categorySummary = PortfolioAnalyzer.analyze(categoryHoldings, snapshot, sectors)
                val fullPortfolioSummary = PortfolioAnalyzer.analyze(loadState.holdings, snapshot, sectors)
                val overviewSummary = if (scope.overviewFilters.size == AssetCategory.entries.size) {
                    fullPortfolioSummary
                } else {
                    val filteredForOverview = loadState.holdings.filter {
                        it.assetCategory in scope.overviewFilters
                    }
                    PortfolioAnalyzer.analyze(filteredForOverview, snapshot, sectors)
                }
                PortfolioUiState(
                    isLoading = false,
                    allHoldings = loadState.holdings,
                    selectedCategory = scope.category,
                    isOverviewSelected = scope.isOverview,
                    overviewFilterCategories = scope.overviewFilters,
                    categorySummary = categorySummary,
                    overviewSummary = overviewSummary,
                    fullPortfolioSummary = fullPortfolioSummary,
                    filteredHoldings = categorySummary.analyzedHoldings,
                    managedSectors = sectors,
                    portfolioPlan = plan,
                    autoBackupConfig = backupConfig,
                    securityConfig = securityConfig,
                    isRefreshingPrices = controls.isRefreshing,
                    priceStatusMessage = controls.priceStatus,
                    actionMessage = controls.actionMsg,
                    isSaving = controls.isSaving
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PortfolioUiState(
            isLoading = true,
            managedSectors = settingsStore.managedSectors.value,
            portfolioPlan = settingsStore.portfolioPlan.value,
            autoBackupConfig = settingsStore.autoBackupConfig.value,
            securityConfig = settingsStore.securityConfig.value
        )
    )

    init {
        viewModelScope.launch {
            holdingsState.collect { state ->
                if (state is HoldingsLoadState.Success) {
                    val sig = state.holdings
                        .map { "${it.category}:${it.symbol.uppercase()}" }
                        .sorted()
                        .joinToString("|")
                    if (sig != lastFetchedSignature) {
                        lastFetchedSignature = sig
                        refreshPricesInternal(state.holdings, userInitiated = false)
                    }
                }
            }
        }

        // Observe holdings, managed sectors, and portfolio plan to trigger automatic file backup on any update
        viewModelScope.launch {
            combine(
                holdingsState,
                settingsStore.managedSectors,
                settingsStore.portfolioPlan
            ) { state, sectors, plan ->
                Triple(state, sectors, plan)
            }.collect { (state, sectors, plan) ->
                if (state is HoldingsLoadState.Success) {
                    val dataSig = buildDataSignature(state.holdings, sectors, plan)
                    val previousSig = lastDataSignature
                    lastDataSignature = dataSig
                    if (previousSig != null && previousSig != dataSig) {
                        scheduleAutoBackup(state.holdings, sectors, plan)
                    }
                }
            }
        }
    }

    private fun buildDataSignature(
        holdings: List<Holding>,
        sectors: List<ManagedSector>,
        plan: PortfolioPlan
    ): String {
        val hSig = holdings.joinToString(";") { h ->
            "${h.id}|${h.symbol}|${h.displayName}|${h.category}|${h.sector}|${h.quantity}|${h.buyPricePerUnit}|${h.currency}|${h.manualCurrentPrice}|${h.updatedAtMillis}"
        }
        val sSig = sectors.joinToString(",") { "${it.id}:${it.label}" }
        val pSig = AssetCategory.entries.joinToString(",") { cat ->
            "${cat.name}=${plan.targetPercentFor(cat)}:${plan.noteFor(cat)}"
        } + "|${plan.overallNote}"
        return "$hSig#$sSig#$pSig"
    }

    private fun scheduleAutoBackup(
        holdings: List<Holding>,
        sectors: List<ManagedSector>,
        plan: PortfolioPlan
    ) {
        val config = settingsStore.autoBackupConfig.value
        val writer = autoBackupWriter ?: return
        val uri = config.uriString ?: return
        if (!config.isEnabled) return

        autoBackupJob?.cancel()
        autoBackupJob = viewModelScope.launch {
            delay(250L)
            val res = runCatching {
                val json = PortfolioJsonCodec.encode(holdings, sectors, plan)
                writer.writeJsonToUri(uri, json).getOrThrow()
            }
            if (res.isSuccess) {
                settingsStore.recordAutoBackupResult(success = true)
            } else {
                val err = res.exceptionOrNull()
                if (err is CancellationException) throw err
                settingsStore.recordAutoBackupResult(
                    success = false,
                    errorMessage = err?.localizedMessage ?: "ไม่สามารถเขียนทับไฟล์สำรองอัตโนมัติได้"
                )
            }
        }
    }

    fun selectOverview() {
        _isOverviewSelected.value = true
    }

    fun selectCategory(category: AssetCategory) {
        _isOverviewSelected.value = false
        _selectedCategory.value = category
    }

    fun toggleOverviewFilterCategory(category: AssetCategory) {
        val current = _overviewFilterCategories.value
        if (category in current) {
            if (current.size <= 1) {
                _actionMessage.value = "ต้องเลือกอย่างน้อย 1 ชนิดสินทรัพย์ในการกรอง"
                return
            }
            _overviewFilterCategories.value = current - category
        } else {
            _overviewFilterCategories.value = current + category
        }
    }

    fun selectAllOverviewFilterCategories() {
        _overviewFilterCategories.value = AssetCategory.entries.toSet()
    }

    fun savePortfolioPlan(plan: PortfolioPlan) {
        settingsStore.savePortfolioPlan(plan)
        _actionMessage.value = "บันทึกแผนการลงทุนเรียบร้อยแล้ว"
    }

    fun refreshPrices() {
        val current = (holdingsState.value as? HoldingsLoadState.Success)?.holdings ?: emptyList()
        refreshPricesInternal(current, userInitiated = true)
    }

    private fun refreshPricesInternal(holdings: List<Holding>, userInitiated: Boolean) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isRefreshingPrices.value = true
            _priceStatusMessage.value = null
            try {
                val snapshot = marketPriceService.fetchMarketSnapshot(holdings)
                _marketSnapshot.value = snapshot
                if (userInitiated && holdings.isNotEmpty()) {
                    val liveCount = snapshot.quotes.values.count {
                        it.source != QuoteSource.UNAVAILABLE && it.source != QuoteSource.MANUAL
                    }
                    _priceStatusMessage.value = if (liveCount > 0) {
                        "อัปเดตราคาตลาดล่าสุดแล้ว ($liveCount รายการ)"
                    } else {
                        "ไม่สามารถเชื่อมต่อราคาตลาดได้ กำลังแสดงราคาอ้างอิงล่าสุด"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Price refresh failed", e)
                if (userInitiated) {
                    _priceStatusMessage.value = "อัปเดตราคาไม่สำเร็จ กรุณาลองใหม่"
                }
            } finally {
                _isRefreshingPrices.value = false
            }
        }
    }

    fun openDividendDialog(holding: Holding) {
        dividendJob?.cancel()
        _dividendDialogState.value = DividendUiState.Loading(holding)
        dividendJob = viewModelScope.launch {
            val info = marketPriceService.fetchStockDividendInfo(holding)
            _dividendDialogState.value = DividendUiState.Ready(holding, info)
        }
    }

    fun closeDividendDialog() {
        dividendJob?.cancel()
        _dividendDialogState.value = null
    }

    fun saveHolding(
        existingHolding: Holding?,
        symbol: String,
        displayName: String,
        category: AssetCategory,
        sectorId: String,
        quantity: Double,
        buyPricePerUnit: Double,
        currency: String,
        manualCurrentPrice: Double?,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val resolvedSector = if (category.isStock) {
                sectorId.trim().ifBlank {
                    settingsStore.managedSectors.value.firstOrNull()?.id ?: StockSector.OTHER.name
                }
            } else {
                StockSector.NONE.name
            }
            val result = if (existingHolding == null) {
                repository.addHolding(
                    Holding(
                        symbol = symbol.trim().uppercase(),
                        displayName = displayName.trim().ifBlank { symbol.trim().uppercase() },
                        category = category.name,
                        sector = resolvedSector,
                        quantity = quantity,
                        buyPricePerUnit = buyPricePerUnit,
                        currency = currency,
                        manualCurrentPrice = manualCurrentPrice
                    )
                )
            } else {
                repository.updateHolding(
                    existingHolding.copy(
                        symbol = symbol.trim().uppercase(),
                        displayName = displayName.trim().ifBlank { symbol.trim().uppercase() },
                        category = category.name,
                        sector = resolvedSector,
                        quantity = quantity,
                        buyPricePerUnit = buyPricePerUnit,
                        currency = currency,
                        manualCurrentPrice = manualCurrentPrice
                    )
                )
            }
            _isSaving.value = false
            if (result.isSuccess) {
                _actionMessage.value = if (existingHolding == null) {
                    "เพิ่ม ${symbol.trim().uppercase()} เข้าพอร์ตแล้ว"
                } else {
                    "อัปเดต ${symbol.trim().uppercase()} เรียบร้อย"
                }
                onSaved()
            } else {
                _actionMessage.value = "บันทึกไม่สำเร็จ: ${result.exceptionOrNull()?.localizedMessage ?: "เกิดข้อผิดพลาด"}"
            }
        }
    }

    fun deleteHolding(holding: Holding) {
        viewModelScope.launch {
            val result = repository.deleteHolding(holding.id)
            if (result.isSuccess) {
                _actionMessage.value = "ลบ ${holding.symbol} ออกจากพอร์ตแล้ว"
            } else {
                _actionMessage.value = "ลบไม่สำเร็จ: ${result.exceptionOrNull()?.localizedMessage ?: "เกิดข้อผิดพลาด"}"
            }
        }
    }

    fun addSector(label: String): Result<ManagedSector> {
        val result = settingsStore.addSector(label)
        result.onSuccess { added ->
            _actionMessage.value = "เพิ่มกลุ่ม \"${added.label}\" แล้ว"
        }
        return result
    }

    fun deleteSector(sectorId: String): Result<Unit> {
        val res = settingsStore.deleteSector(sectorId)
        res.onFailure { err ->
            _actionMessage.value = err.message ?: "ไม่สามารถลบกลุ่มอุตสาหกรรมได้"
            return Result.failure(err)
        }
        val fallback = res.getOrThrow()
        viewModelScope.launch {
            repository.reassignSector(sectorId, fallback.id)
            _actionMessage.value = "ลบกลุ่มอุตสาหกรรมแล้ว (ย้ายหุ้นที่เกี่ยวข้องไปยัง \"${fallback.label}\")"
        }
        return Result.success(Unit)
    }

    fun resetDefaultSectors() {
        settingsStore.resetDefaultSectors()
        val validIds = settingsStore.managedSectors.value.map { it.id }.toSet()
        viewModelScope.launch {
            val orphanedIds = repository.getHoldingsOnce()
                .filter { it.assetCategory.isStock && it.sector !in validIds }
                .map { it.sector }
                .distinct()
            orphanedIds.forEach { oldId ->
                repository.reassignSector(oldId, StockSector.OTHER.name)
            }
        }
        _actionMessage.value = "คืนค่ากลุ่มอุตสาหกรรมเริ่มต้นแล้ว"
    }

    fun setPendingExportJson(json: String?) {
        pendingExportJson = json
    }

    fun consumePendingExportJson(): String? {
        val current = pendingExportJson
        pendingExportJson = null
        return current
    }

    fun configureAutoBackupDestination(uriString: String, fileName: String) {
        settingsStore.setAutoBackupDestination(uriString, fileName, enabled = true)
        viewModelScope.launch {
            val writer = autoBackupWriter ?: return@launch
            val res = runCatching {
                val items = repository.getHoldingsOnce()
                val sectors = settingsStore.managedSectors.value
                val plan = settingsStore.portfolioPlan.value
                val json = PortfolioJsonCodec.encode(items, sectors, plan)
                writer.writeJsonToUri(uriString, json).getOrThrow()
            }
            if (res.isSuccess) {
                settingsStore.recordAutoBackupResult(success = true)
                _actionMessage.value = "ตั้งค่าและสำรองข้อมูลลง \"$fileName\" แล้ว"
            } else {
                val errObj = res.exceptionOrNull()
                if (errObj is CancellationException) throw errObj
                val err = errObj?.localizedMessage ?: "ไม่สามารถเขียนไฟล์ได้"
                settingsStore.recordAutoBackupResult(success = false, errorMessage = err)
                _actionMessage.value = "ตั้งค่าตำแหน่งแล้ว แต่เขียนไฟล์ไม่สำเร็จ: $err"
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        settingsStore.setAutoBackupEnabled(enabled)
        if (enabled) {
            runAutoBackupNow(showToast = true)
        } else {
            _actionMessage.value = "ปิดการสำรองข้อมูลอัตโนมัติแล้ว"
        }
    }

    fun runAutoBackupNow(showToast: Boolean = true) {
        val config = settingsStore.autoBackupConfig.value
        val uri = config.uriString
        val writer = autoBackupWriter
        if (uri.isNullOrBlank() || writer == null) {
            if (showToast) {
                _actionMessage.value = "กรุณาเลือกตำแหน่งไฟล์สำรองอัตโนมัติก่อน"
            }
            return
        }
        viewModelScope.launch {
            val res = runCatching {
                val items = repository.getHoldingsOnce()
                val sectors = settingsStore.managedSectors.value
                val plan = settingsStore.portfolioPlan.value
                val json = PortfolioJsonCodec.encode(items, sectors, plan)
                writer.writeJsonToUri(uri, json).getOrThrow()
            }
            if (res.isSuccess) {
                settingsStore.recordAutoBackupResult(success = true)
                if (showToast) {
                    _actionMessage.value = "บันทึกข้อมูลทับไฟล์ \"${config.fileName ?: "สำรอง"}\" เรียบร้อย"
                }
            } else {
                val errObj = res.exceptionOrNull()
                if (errObj is CancellationException) throw errObj
                val err = errObj?.localizedMessage ?: "เขียนไฟล์ไม่สำเร็จ"
                settingsStore.recordAutoBackupResult(success = false, errorMessage = err)
                if (showToast) {
                    _actionMessage.value = "สำรองข้อมูลไม่สำเร็จ: $err"
                }
            }
        }
    }

    suspend fun exportPortfolioJson(): Result<String> {
        return repository.exportToJson(
            managedSectors = settingsStore.managedSectors.value,
            portfolioPlan = settingsStore.portfolioPlan.value
        )
    }

    fun importHoldings(
        holdings: List<Holding>,
        importedSectors: List<ManagedSector>,
        mode: ImportMode,
        importedPlan: PortfolioPlan? = null
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            settingsStore.mergeImportedSectors(importedSectors)
            settingsStore.ensureSectorsPresent(
                holdings.filter { it.assetCategory.isStock }.map { it.sector }
            )
            val result = if (holdings.isEmpty() && mode == ImportMode.MERGE) {
                Result.success(0)
            } else {
                repository.importHoldings(holdings, mode)
            }
            if (result.isSuccess && importedPlan != null) {
                settingsStore.mergeImportedPlan(importedPlan, replace = mode == ImportMode.REPLACE)
            }
            _isSaving.value = false
            if (result.isSuccess) {
                val count = result.getOrDefault(holdings.size)
                val planSuffix = if (importedPlan?.hasContent == true) " พร้อมแผนการลงทุน" else ""
                _actionMessage.value = when (mode) {
                    ImportMode.MERGE -> "นำเข้าและเพิ่ม $count รายการเข้าพอร์ตแล้ว$planSuffix"
                    ImportMode.REPLACE -> "นำเข้าและแทนที่พอร์ตด้วย $count รายการแล้ว$planSuffix"
                }
            } else {
                _actionMessage.value = "นำเข้าข้อมูลไม่สำเร็จ: ${result.exceptionOrNull()?.localizedMessage ?: "เกิดข้อผิดพลาด"}"
            }
        }
    }

    fun notifyMessage(message: String) {
        _actionMessage.value = message
    }

    suspend fun searchAssets(
        query: String,
        category: AssetCategory
    ): List<com.example.data.KnownAsset> {
        return marketPriceService.searchAssets(query, category)
    }

    suspend fun fetchPreviewQuote(
        symbol: String,
        category: AssetCategory,
        manualPrice: Double? = null,
        currency: String = "THB"
    ): LiveQuote? {
        return marketPriceService.fetchPreviewQuote(symbol, category, manualPrice, currency)
    }

    fun unlockWithPin(pin: String): PinVerifyResult {
        val result = settingsStore.verifyPin(pin)
        if (result is PinVerifyResult.Success || result is PinVerifyResult.NotConfigured) {
            _isAppLocked.value = false
        }
        return result
    }

    fun unlockWithBiometric() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (settingsStore.securityConfig.value.isPinEnabled) {
            _isAppLocked.value = true
        }
    }

    fun setupPin(pin: String, enableBiometric: Boolean? = null): Result<Unit> {
        val res = settingsStore.setPin(pin, enableBiometric)
        if (res.isSuccess) {
            _isAppLocked.value = false
            _actionMessage.value = "ตั้งรหัส PIN ล็อกแอปเรียบร้อยแล้ว"
        }
        return res
    }

    fun verifyCurrentPin(pin: String): PinVerifyResult {
        return settingsStore.verifyPin(pin)
    }

    fun toggleBiometricLock(enabled: Boolean) {
        settingsStore.setBiometricEnabled(enabled)
        _actionMessage.value = if (enabled) {
            "เปิดใช้งานการปลดล็อกด้วยลายนิ้วมือแล้ว"
        } else {
            "ปิดใช้งานการปลดล็อกด้วยลายนิ้วมือแล้ว"
        }
    }

    fun disablePinLock() {
        settingsStore.disablePinLock()
        _isAppLocked.value = false
        _actionMessage.value = "ปิดการล็อกแอปด้วย PIN แล้ว"
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearPriceStatusMessage() {
        _priceStatusMessage.value = null
    }

    private companion object {
        const val TAG = "PortfolioVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
