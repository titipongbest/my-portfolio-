package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.model.ManagedSector
import com.example.model.StockSector
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AutoBackupConfig(
    val isEnabled: Boolean = false,
    val uriString: String? = null,
    val fileName: String? = null,
    val lastBackupMillis: Long? = null,
    val lastError: String? = null
)

data class AppSecurityConfig(
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L
)

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class WrongPin(val remainingAttempts: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
    data object NotConfigured : PinVerifyResult
}

class PortfolioSettingsStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _managedSectors = MutableStateFlow(loadSectorsFromPrefs())
    val managedSectors: StateFlow<List<ManagedSector>> = _managedSectors.asStateFlow()

    private val _autoBackupConfig = MutableStateFlow(loadAutoBackupConfig())
    val autoBackupConfig: StateFlow<AutoBackupConfig> = _autoBackupConfig.asStateFlow()

    private val _securityConfig = MutableStateFlow(loadSecurityConfig())
    val securityConfig: StateFlow<AppSecurityConfig> = _securityConfig.asStateFlow()

    fun addSector(labelInput: String): Result<ManagedSector> {
        val clean = labelInput.trim().take(40)
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("กรุณาระบุชื่อกลุ่มอุตสาหกรรม"))
        }
        val current = _managedSectors.value
        if (current.any { it.label.equals(clean, ignoreCase = true) || it.fullLabel.equals(clean, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("มีกลุ่มอุตสาหกรรม \"$clean\" อยู่แล้ว"))
        }
        val newSector = ManagedSector(
            id = "CUSTOM_${System.currentTimeMillis()}",
            label = clean,
            fullLabel = clean,
            isCustom = true
        )
        val updated = current + newSector
        saveSectorsToPrefs(updated)
        _managedSectors.value = updated
        return Result.success(newSector)
    }

    fun deleteSector(sectorId: String): Result<ManagedSector> {
        val current = _managedSectors.value
        if (current.size <= 1) {
            return Result.failure(IllegalArgumentException("ต้องเหลือกลุ่มอุตสาหกรรมอย่างน้อย 1 กลุ่ม"))
        }
        if (current.none { it.id == sectorId }) {
            return Result.failure(IllegalArgumentException("ไม่พบกลุ่มอุตสาหกรรมที่ต้องการลบ"))
        }
        val updated = current.filterNot { it.id == sectorId }
        saveSectorsToPrefs(updated)
        _managedSectors.value = updated
        val fallback = updated.firstOrNull { it.id == StockSector.OTHER.name } ?: updated.first()
        return Result.success(fallback)
    }

    fun resetDefaultSectors() {
        val defaults = StockSector.defaultManagedSectors()
        saveSectorsToPrefs(defaults)
        _managedSectors.value = defaults
    }

    fun ensureSectorsPresent(sectorIds: Collection<String>) {
        val current = _managedSectors.value.toMutableList()
        var changed = false
        for (rawId in sectorIds) {
            val id = rawId.trim()
            if (id.isBlank() || id == StockSector.NONE.name) continue
            if (current.none { it.id == id }) {
                val knownEnum = StockSector.entries.find { it.name == id && it != StockSector.NONE }
                current.add(
                    if (knownEnum != null) {
                        ManagedSector(knownEnum.name, knownEnum.shortLabelTh, knownEnum.labelTh, isCustom = false)
                    } else {
                        ManagedSector(id = id, label = id, fullLabel = id, isCustom = true)
                    }
                )
                changed = true
            }
        }
        if (changed) {
            saveSectorsToPrefs(current)
            _managedSectors.value = current
        }
    }

    fun mergeImportedSectors(imported: List<ManagedSector>) {
        if (imported.isEmpty()) return
        val current = _managedSectors.value.toMutableList()
        var changed = false
        for (sec in imported) {
            if (sec.id.isBlank() || sec.id == StockSector.NONE.name) continue
            if (current.none { it.id == sec.id }) {
                current.add(sec)
                changed = true
            }
        }
        if (changed) {
            saveSectorsToPrefs(current)
            _managedSectors.value = current
        }
    }

    fun setAutoBackupDestination(uriString: String, fileName: String, enabled: Boolean = true) {
        prefs.edit()
            .putString(KEY_BACKUP_URI, uriString)
            .putString(KEY_BACKUP_NAME, fileName)
            .putBoolean(KEY_BACKUP_ENABLED, enabled)
            .remove(KEY_BACKUP_ERROR)
            .apply()
        _autoBackupConfig.value = loadAutoBackupConfig()
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BACKUP_ENABLED, enabled)
            .apply()
        _autoBackupConfig.value = loadAutoBackupConfig()
    }

    fun recordAutoBackupResult(success: Boolean, errorMessage: String? = null) {
        val editor = prefs.edit()
        if (success) {
            editor.putLong(KEY_BACKUP_LAST_MILLIS, System.currentTimeMillis())
            editor.remove(KEY_BACKUP_ERROR)
        } else {
            editor.putString(KEY_BACKUP_ERROR, errorMessage ?: "บันทึกไฟล์อัตโนมัติไม่สำเร็จ")
        }
        editor.apply()
        _autoBackupConfig.value = loadAutoBackupConfig()
    }

    fun setPin(pin: String, enableBiometric: Boolean? = null): Result<Unit> {
        val clean = pin.trim()
        if (clean.length != PIN_LENGTH || !clean.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("รหัส PIN ต้องเป็นตัวเลข $PIN_LENGTH หลัก"))
        }
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = hashPin(clean, salt)
        val bio = enableBiometric ?: prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putBoolean(KEY_PIN_ENABLED, true)
            .putBoolean(KEY_BIOMETRIC_ENABLED, bio)
            .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
            .putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
            .apply()
        _securityConfig.value = loadSecurityConfig()
        return Result.success(Unit)
    }

    fun verifyPin(pin: String, nowMillis: Long = System.currentTimeMillis()): PinVerifyResult {
        val enabled = prefs.getBoolean(KEY_PIN_ENABLED, false)
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        val storedSalt = prefs.getString(KEY_PIN_SALT, null)
        if (!enabled || storedHash.isNullOrBlank() || storedSalt.isNullOrBlank()) {
            return PinVerifyResult.NotConfigured
        }

        val lockoutUntil = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        if (lockoutUntil > nowMillis) {
            val secs = ((lockoutUntil - nowMillis + 999L) / 1000L).toInt().coerceAtLeast(1)
            return PinVerifyResult.LockedOut(secs)
        }

        val candidateHash = hashPin(pin.trim(), storedSalt)
        val isMatch = MessageDigest.isEqual(
            candidateHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
        if (isMatch) {
            prefs.edit()
                .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
                .putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
                .apply()
            _securityConfig.value = loadSecurityConfig()
            return PinVerifyResult.Success
        }

        val prevAttempts = if (lockoutUntil in 1L..nowMillis) 0 else prefs.getInt(KEY_PIN_FAILED_ATTEMPTS, 0)
        val newAttempts = prevAttempts + 1
        return if (newAttempts >= MAX_PIN_ATTEMPTS) {
            val newLockoutUntil = nowMillis + LOCKOUT_DURATION_MILLIS
            prefs.edit()
                .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
                .putLong(KEY_PIN_LOCKOUT_UNTIL, newLockoutUntil)
                .apply()
            _securityConfig.value = loadSecurityConfig()
            PinVerifyResult.LockedOut((LOCKOUT_DURATION_MILLIS / 1000L).toInt())
        } else {
            prefs.edit()
                .putInt(KEY_PIN_FAILED_ATTEMPTS, newAttempts)
                .putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
                .apply()
            _securityConfig.value = loadSecurityConfig()
            PinVerifyResult.WrongPin(MAX_PIN_ATTEMPTS - newAttempts)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val pinEnabled = prefs.getBoolean(KEY_PIN_ENABLED, false)
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled && pinEnabled)
            .apply()
        _securityConfig.value = loadSecurityConfig()
    }

    fun disablePinLock() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_PIN_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
            .putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
            .apply()
        _securityConfig.value = loadSecurityConfig()
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun loadSecurityConfig(): AppSecurityConfig {
        val hasHash = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
        val pinEnabled = prefs.getBoolean(KEY_PIN_ENABLED, false) && hasHash
        return AppSecurityConfig(
            isPinEnabled = pinEnabled,
            isBiometricEnabled = pinEnabled && prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
            failedAttempts = prefs.getInt(KEY_PIN_FAILED_ATTEMPTS, 0),
            lockoutUntilMillis = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        )
    }

    private fun loadAutoBackupConfig(): AutoBackupConfig {
        val lastMillis = prefs.getLong(KEY_BACKUP_LAST_MILLIS, -1L).takeIf { it > 0L }
        return AutoBackupConfig(
            isEnabled = prefs.getBoolean(KEY_BACKUP_ENABLED, false),
            uriString = prefs.getString(KEY_BACKUP_URI, null),
            fileName = prefs.getString(KEY_BACKUP_NAME, null),
            lastBackupMillis = lastMillis,
            lastError = prefs.getString(KEY_BACKUP_ERROR, null)
        )
    }

    private fun loadSectorsFromPrefs(): List<ManagedSector> {
        val raw = prefs.getString(KEY_SECTORS_JSON, null)
        if (raw.isNullOrBlank()) {
            return StockSector.defaultManagedSectors()
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ManagedSector>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val label = obj.optString("label", "").trim()
                val fullLabel = obj.optString("fullLabel", label).trim().ifBlank { label }
                val isCustom = obj.optBoolean("isCustom", true)
                if (id.isNotBlank() && label.isNotBlank()) {
                    list.add(ManagedSector(id = id, label = label, fullLabel = fullLabel, isCustom = isCustom))
                }
            }
            list.ifEmpty { StockSector.defaultManagedSectors() }
        } catch (_: Exception) {
            StockSector.defaultManagedSectors()
        }
    }

    private fun saveSectorsToPrefs(sectors: List<ManagedSector>) {
        val arr = JSONArray()
        for (s in sectors) {
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("label", s.label)
                    put("fullLabel", s.fullLabel)
                    put("isCustom", s.isCustom)
                }
            )
        }
        prefs.edit().putString(KEY_SECTORS_JSON, arr.toString()).apply()
    }

    companion object {
        const val PIN_LENGTH = 4
        const val MAX_PIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MILLIS = 30_000L

        private const val PREFS_NAME = "portpulse_settings"
        private const val KEY_SECTORS_JSON = "managed_sectors_json"
        private const val KEY_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_BACKUP_URI = "auto_backup_uri"
        private const val KEY_BACKUP_NAME = "auto_backup_file_name"
        private const val KEY_BACKUP_LAST_MILLIS = "auto_backup_last_millis"
        private const val KEY_BACKUP_ERROR = "auto_backup_error"
        private const val KEY_PIN_ENABLED = "security_pin_enabled"
        private const val KEY_PIN_HASH = "security_pin_hash"
        private const val KEY_PIN_SALT = "security_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "security_biometric_enabled"
        private const val KEY_PIN_FAILED_ATTEMPTS = "security_pin_failed_attempts"
        private const val KEY_PIN_LOCKOUT_UNTIL = "security_pin_lockout_until"
    }
}
