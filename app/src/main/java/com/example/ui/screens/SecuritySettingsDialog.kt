package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppSecurityConfig
import com.example.data.PinVerifyResult
import com.example.data.PortfolioSettingsStore
import com.example.ui.security.BiometricAuthHelper
import com.example.ui.security.BiometricAuthHelper.findFragmentActivity
import com.example.ui.security.BiometricAvailability
import com.example.ui.theme.LocalPortfolioColors

private enum class SecurityFlowStep {
    OVERVIEW,
    CREATE_PIN,
    CONFIRM_PIN,
    VERIFY_CURRENT_FOR_CHANGE,
    VERIFY_CURRENT_FOR_DISABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsDialog(
    securityConfig: AppSecurityConfig,
    onSetupPin: (pin: String, enableBiometric: Boolean?) -> Result<Unit>,
    onVerifyCurrentPin: (String) -> PinVerifyResult,
    onToggleBiometric: (Boolean) -> Unit,
    onDisablePinLock: () -> Unit,
    onLockNow: () -> Unit,
    onLaunchExternalIntent: () -> Unit,
    onNotifyMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val portfolioColors = LocalPortfolioColors.current
    var biometricAvailability by remember {
        mutableStateOf(BiometricAuthHelper.checkAvailability(context))
    }

    var flowStep by remember(securityConfig.isPinEnabled) {
        mutableStateOf(
            if (securityConfig.isPinEnabled) SecurityFlowStep.OVERVIEW else SecurityFlowStep.CREATE_PIN
        )
    }
    var pendingNewPin by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var enableBioOnSetup by remember(biometricAvailability) {
        mutableStateOf(
            securityConfig.isBiometricEnabled || biometricAvailability == BiometricAvailability.AVAILABLE
        )
    }

    fun resetPinInput(targetStep: SecurityFlowStep) {
        currentPinInput = ""
        pinError = null
        flowStep = targetStep
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            if (flowStep != SecurityFlowStep.OVERVIEW && securityConfig.isPinEnabled) {
                pendingNewPin = ""
                resetPinInput(SecurityFlowStep.OVERVIEW)
            } else {
                onDismiss()
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (flowStep != SecurityFlowStep.OVERVIEW && securityConfig.isPinEnabled) {
                                    pendingNewPin = ""
                                    resetPinInput(SecurityFlowStep.OVERVIEW)
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("security_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "กลับ"
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = portfolioColors.accentHighlight,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ความปลอดภัยและล็อกแอป",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (securityConfig.isPinEnabled) {
                                        "เปิดใช้งานล็อกด้วย PIN ${PortfolioSettingsStore.PIN_LENGTH} หลักแล้ว"
                                    } else {
                                        "ตั้งรหัส PIN ${PortfolioSettingsStore.PIN_LENGTH} หลัก เพื่อปกป้องข้อมูลพอร์ตของคุณ"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (flowStep != SecurityFlowStep.OVERVIEW) {
                val (stepTitle, stepSubtitle) = when (flowStep) {
                    SecurityFlowStep.CREATE_PIN -> "ตั้งรหัส PIN ${PortfolioSettingsStore.PIN_LENGTH} หลักใหม่" to "รหัสนี้จะใช้ปลดล็อกทุกครั้งที่เปิดหรือกลับเข้าแอป"
                    SecurityFlowStep.CONFIRM_PIN -> "ยืนยันรหัส PIN อีกครั้ง" to "ใส่รหัส PIN ${PortfolioSettingsStore.PIN_LENGTH} หลักเดิมเพื่อยืนยันการตั้งค่า"
                    SecurityFlowStep.VERIFY_CURRENT_FOR_CHANGE -> "ยืนยันรหัส PIN เดิม" to "กรอกรหัส PIN ปัจจุบันก่อนเปลี่ยนรหัสใหม่"
                    SecurityFlowStep.VERIFY_CURRENT_FOR_DISABLE -> "ยืนยันเพื่อปิดการล็อกแอป" to "กรอกรหัส PIN ปัจจุบันเพื่อปิดการล็อก"
                    SecurityFlowStep.OVERVIEW -> "" to ""
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stepSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (flowStep == SecurityFlowStep.CREATE_PIN && biometricAvailability == BiometricAvailability.AVAILABLE) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = portfolioColors.accentHighlight,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "เปิดใช้สแกนลายนิ้วมือร่วมด้วย",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = enableBioOnSetup,
                                    onCheckedChange = { enableBioOnSetup = it },
                                    modifier = Modifier.testTag("setup_biometric_switch")
                                )
                            }
                        }
                    }

                    PinPadSection(
                        enteredLength = currentPinInput.length,
                        pinLength = PortfolioSettingsStore.PIN_LENGTH,
                        isError = pinError != null,
                        statusMessage = pinError,
                        enabled = true,
                        showBiometricKey = false,
                        onDigitClick = { digit ->
                            if (currentPinInput.length < PortfolioSettingsStore.PIN_LENGTH) {
                                pinError = null
                                val next = currentPinInput + digit
                                currentPinInput = next
                                if (next.length == PortfolioSettingsStore.PIN_LENGTH) {
                                    when (flowStep) {
                                        SecurityFlowStep.CREATE_PIN -> {
                                            pendingNewPin = next
                                            resetPinInput(SecurityFlowStep.CONFIRM_PIN)
                                        }
                                        SecurityFlowStep.CONFIRM_PIN -> {
                                            if (next == pendingNewPin) {
                                                val bioFlag = if (biometricAvailability == BiometricAvailability.AVAILABLE) {
                                                    enableBioOnSetup
                                                } else {
                                                    false
                                                }
                                                onSetupPin(next, bioFlag)
                                                pendingNewPin = ""
                                                resetPinInput(SecurityFlowStep.OVERVIEW)
                                            } else {
                                                currentPinInput = ""
                                                pinError = "รหัส PIN ไม่ตรงกัน กรุณาลองใหม่"
                                            }
                                        }
                                        SecurityFlowStep.VERIFY_CURRENT_FOR_CHANGE -> {
                                            when (val res = onVerifyCurrentPin(next)) {
                                                PinVerifyResult.Success -> resetPinInput(SecurityFlowStep.CREATE_PIN)
                                                is PinVerifyResult.WrongPin -> {
                                                    currentPinInput = ""
                                                    pinError = "รหัสเดิมไม่ถูกต้อง (เหลือโอกาส ${res.remainingAttempts} ครั้ง)"
                                                }
                                                is PinVerifyResult.LockedOut -> {
                                                    currentPinInput = ""
                                                    pinError = "กรุณารอ ${res.remainingSeconds} วินาที"
                                                }
                                                PinVerifyResult.NotConfigured -> resetPinInput(SecurityFlowStep.CREATE_PIN)
                                            }
                                        }
                                        SecurityFlowStep.VERIFY_CURRENT_FOR_DISABLE -> {
                                            when (val res = onVerifyCurrentPin(next)) {
                                                PinVerifyResult.Success -> {
                                                    onDisablePinLock()
                                                    onDismiss()
                                                }
                                                is PinVerifyResult.WrongPin -> {
                                                    currentPinInput = ""
                                                    pinError = "รหัส PIN ไม่ถูกต้อง (เหลือโอกาส ${res.remainingAttempts} ครั้ง)"
                                                }
                                                is PinVerifyResult.LockedOut -> {
                                                    currentPinInput = ""
                                                    pinError = "กรุณารอ ${res.remainingSeconds} วินาที"
                                                }
                                                PinVerifyResult.NotConfigured -> {
                                                    onDisablePinLock()
                                                    onDismiss()
                                                }
                                            }
                                        }
                                        SecurityFlowStep.OVERVIEW -> Unit
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (currentPinInput.isNotEmpty()) {
                                pinError = null
                                currentPinInput = currentPinInput.dropLast(1)
                            }
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. PIN Status Card
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(portfolioColors.profitContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = portfolioColors.profit,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "เปิดใช้งานรหัส PIN ล็อกแอปแล้ว",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "แอปจะล็อกอัตโนมัติเมื่อปิดแอปหรือสลับออกจากแอป",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onLockNow()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("lock_app_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ล็อกแอปทันที",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Biometric / Fingerprint Card
                    item {
                        biometricAvailability = BiometricAuthHelper.checkAvailability(context)
                        val bioStatusDesc = when (biometricAvailability) {
                            BiometricAvailability.AVAILABLE -> "ปลดล็อกรวดเร็วด้วยลายนิ้วมือหรือใบหน้าที่ลงทะเบียนไว้ในเครื่อง"
                            BiometricAvailability.NOT_ENROLLED -> "เครื่องรองรับแต่ยังไม่ได้ตั้งค่าลายนิ้วมือในระบบ Android"
                            BiometricAvailability.NO_HARDWARE -> "อุปกรณ์นี้ไม่มีเซ็นเซอร์สแกนลายนิ้วมือหรือชีวมิติ"
                            BiometricAvailability.UNAVAILABLE -> "เซ็นเซอร์ชีวมิติไม่พร้อมใช้งานชั่วคราว"
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
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
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "ลายนิ้วมือ",
                                                tint = portfolioColors.accentHighlight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "ใช้ลายนิ้วมือปลดล็อก",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = bioStatusDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = securityConfig.isBiometricEnabled &&
                                            biometricAvailability == BiometricAvailability.AVAILABLE,
                                        enabled = biometricAvailability == BiometricAvailability.AVAILABLE,
                                        onCheckedChange = { checked ->
                                            if (!checked) {
                                                onToggleBiometric(false)
                                            } else {
                                                val hostActivity = activity
                                                if (hostActivity != null) {
                                                    BiometricAuthHelper.showPrompt(
                                                        activity = hostActivity,
                                                        title = "ยืนยันลายนิ้วมือ",
                                                        subtitle = "สแกนลายนิ้วมือเพื่อเปิดใช้งานการปลดล็อกด่วน",
                                                        negativeButtonText = "ยกเลิก",
                                                        onSuccess = { onToggleBiometric(true) },
                                                        onError = { msg ->
                                                            onNotifyMessage("ไม่สามารถเปิดใช้ลายนิ้วมือได้: $msg")
                                                        },
                                                        onCancel = {}
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("toggle_biometric_switch")
                                    )
                                }

                                if (biometricAvailability == BiometricAvailability.NOT_ENROLLED) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            onLaunchExternalIntent()
                                            runCatching {
                                                context.startActivity(BiometricAuthHelper.buildEnrollIntent())
                                            }.onFailure {
                                                onNotifyMessage("ไม่สามารถเปิดหน้าตั้งค่าลายนิ้วมือได้")
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text("ไปตั้งค่าลายนิ้วมือในระบบ Android")
                                    }
                                }
                            }
                        }
                    }

                    // 3. Change / Disable PIN Card
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "จัดการรหัส PIN",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedButton(
                                    onClick = { resetPinInput(SecurityFlowStep.VERIFY_CURRENT_FOR_CHANGE) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("change_pin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("เปลี่ยนรหัส PIN ใหม่")
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                                OutlinedButton(
                                    onClick = { resetPinInput(SecurityFlowStep.VERIFY_CURRENT_FOR_DISABLE) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("disable_pin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ปิดการล็อกแอปด้วย PIN")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
