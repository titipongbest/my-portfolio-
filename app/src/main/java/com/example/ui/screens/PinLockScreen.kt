package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSecurityConfig
import com.example.data.PinVerifyResult
import com.example.data.PortfolioSettingsStore
import com.example.ui.security.BiometricAuthHelper
import com.example.ui.security.BiometricAuthHelper.findFragmentActivity
import com.example.ui.security.BiometricAvailability
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.NumericStyleLarge
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    securityConfig: AppSecurityConfig,
    onVerifyPin: (String) -> PinVerifyResult,
    onBiometricUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricAvailability = remember(context) { BiometricAuthHelper.checkAvailability(context) }
    val canUseBiometric = securityConfig.isBiometricEnabled &&
        biometricAvailability == BiometricAvailability.AVAILABLE &&
        activity != null

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorState by remember { mutableStateOf(false) }
    var lockoutSeconds by remember(securityConfig.lockoutUntilMillis) {
        val rem = ((securityConfig.lockoutUntilMillis - System.currentTimeMillis() + 999L) / 1000L)
            .toInt()
            .coerceAtLeast(0)
        mutableIntStateOf(rem)
    }

    // Consume back press so locked screen cannot be bypassed
    BackHandler(enabled = true) {}

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = (lockoutSeconds - 1).coerceAtLeast(0)
            if (lockoutSeconds == 0) {
                errorMessage = null
                isErrorState = false
            }
        }
    }

    fun triggerBiometric() {
        val hostActivity = activity ?: return
        BiometricAuthHelper.showPrompt(
            activity = hostActivity,
            title = "ปลดล็อก PortPulse",
            subtitle = "สแกนลายนิ้วมือหรือใบหน้าเพื่อเข้าสู่พอร์ตของคุณ",
            negativeButtonText = "ใช้รหัส PIN แทน",
            onSuccess = {
                enteredPin = ""
                errorMessage = null
                isErrorState = false
                onBiometricUnlockSuccess()
            },
            onError = { msg ->
                errorMessage = "ไม่สามารถยืนยันตัวตนได้: $msg"
                isErrorState = true
            },
            onCancel = {
                // User chose PIN fallback — keep PIN screen ready
            }
        )
    }

    // Auto-trigger biometric prompt once when lock screen opens if enabled & available
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric && lockoutSeconds == 0) {
            delay(180L)
            triggerBiometric()
        }
    }

    fun handlePinComplete(candidate: String) {
        when (val res = onVerifyPin(candidate)) {
            PinVerifyResult.Success,
            PinVerifyResult.NotConfigured -> {
                enteredPin = ""
                errorMessage = null
                isErrorState = false
            }
            is PinVerifyResult.WrongPin -> {
                enteredPin = ""
                isErrorState = true
                errorMessage = "รหัส PIN ไม่ถูกต้อง (เหลือโอกาสอีก ${res.remainingAttempts} ครั้ง)"
            }
            is PinVerifyResult.LockedOut -> {
                enteredPin = ""
                isErrorState = true
                lockoutSeconds = res.remainingSeconds
                errorMessage = "ใส่รหัสผิดเกินกำหนด กรุณารอ ${res.remainingSeconds} วินาที"
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("pin_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // App Security Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "ล็อกแอป PortPulse",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "PortPulse",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ใส่รหัส PIN ${PortfolioSettingsStore.PIN_LENGTH} หลัก เพื่อเข้าใช้งานพอร์ต",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                PinPadSection(
                    enteredLength = enteredPin.length,
                    pinLength = PortfolioSettingsStore.PIN_LENGTH,
                    isError = isErrorState,
                    statusMessage = if (lockoutSeconds > 0) {
                        "ใส่รหัสผิดหลายครั้ง กรุณารอ $lockoutSeconds วินาที"
                    } else {
                        errorMessage
                    },
                    enabled = lockoutSeconds == 0,
                    showBiometricKey = canUseBiometric,
                    onDigitClick = { digit ->
                        if (enteredPin.length < PortfolioSettingsStore.PIN_LENGTH && lockoutSeconds == 0) {
                            isErrorState = false
                            errorMessage = null
                            val next = enteredPin + digit
                            enteredPin = next
                            if (next.length == PortfolioSettingsStore.PIN_LENGTH) {
                                handlePinComplete(next)
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (enteredPin.isNotEmpty() && lockoutSeconds == 0) {
                            isErrorState = false
                            enteredPin = enteredPin.dropLast(1)
                        }
                    },
                    onBiometricClick = { triggerBiometric() }
                )

                if (canUseBiometric) {
                    OutlinedButton(
                        onClick = { triggerBiometric() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pin_lock_biometric_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = LocalPortfolioColors.current.accentHighlight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ปลดล็อกด้วยลายนิ้วมือ",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinPadSection(
    enteredLength: Int,
    pinLength: Int = PortfolioSettingsStore.PIN_LENGTH,
    isError: Boolean = false,
    statusMessage: String? = null,
    enabled: Boolean = true,
    showBiometricKey: Boolean = false,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PIN Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("pin_dots_row")
        ) {
            repeat(pinLength) { idx ->
                val isFilled = idx < enteredLength
                val dotSize by animateDpAsState(
                    targetValue = if (isFilled) 18.dp else 15.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "pinDotSize"
                )
                val dotColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.error
                        isFilled -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surface
                    },
                    label = "pinDotColor"
                )
                val borderColor = when {
                    isError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(2.dp, borderColor, CircleShape)
                )
            }
        }

        // Feedback / Error Text
        Box(
            modifier = Modifier.height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3x4 Numeric Keypad
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { rowDigits ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowDigits.forEach { digit ->
                        PinDigitButton(
                            digit = digit,
                            enabled = enabled,
                            onClick = { onDigitClick(digit) }
                        )
                    }
                }
            }

            // Bottom Row: Biometric (or blank) | 0 | Backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBiometricKey) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable(enabled = enabled) { onBiometricClick() }
                            .testTag("pin_key_biometric")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "สแกนลายนิ้วมือ",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(68.dp))
                }

                PinDigitButton(
                    digit = "0",
                    enabled = enabled,
                    onClick = { onDigitClick("0") }
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable(enabled = enabled) { onBackspaceClick() }
                        .testTag("pin_key_backspace")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "ลบตัวเลข",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDigitButton(
    digit: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        shadowElevation = 1.dp,
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .border(1.2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("pin_key_$digit")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = NumericStyleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
    }
}
