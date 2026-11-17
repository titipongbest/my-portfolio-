package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AutoBackupConfig
import com.example.model.AssetCategory
import com.example.model.Holding
import com.example.ui.FormatUtils
import com.example.ui.theme.LocalPortfolioColors
import com.example.ui.theme.categoryContainerColor
import com.example.ui.theme.categoryOnContainerColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackupScreenDialog(
    allHoldings: List<Holding>,
    autoBackupConfig: AutoBackupConfig,
    actionMessage: String?,
    onConsumeActionMessage: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onSelectAutoBackupLocation: () -> Unit,
    onRunAutoBackupNow: () -> Unit,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val portfolioColors = LocalPortfolioColors.current
    val hasSelectedFile = !autoBackupConfig.uriString.isNullOrBlank()
    val holdingsCount = allHoldings.size
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        if (!actionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionMessage)
            onConsumeActionMessage()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Keep a passive pointer node on the full-screen surface so taps on empty space
                        // do not fall through to MainPortfolioScreen, without consuming Move/Up events
                        // that child buttons need during real finger taps.
                        awaitPointerEvent(PointerEventPass.Final)
                    }
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        BackHandler { onDismiss() }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("backup_back_button")
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
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = portfolioColors.accentHighlight,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "สำรองและกู้คืนข้อมูล",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "รวมทุกหมวดหมู่ทั้งหมด $holdingsCount รายการในไฟล์เดียว",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Automatic Backup Card (Save & Overwrite Same File on Update)
                item {
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
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Autorenew,
                                            contentDescription = "สำรองข้อมูลอัตโนมัติ",
                                            tint = portfolioColors.accentHighlight,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "สำรองข้อมูลอัตโนมัติ",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "บันทึกทับไฟล์เดิมทันทีเมื่อมีการเพิ่ม แก้ไข หรือลบข้อมูล",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = autoBackupConfig.isEnabled && hasSelectedFile,
                                    onCheckedChange = { checked ->
                                        if (checked && !hasSelectedFile) {
                                            onSelectAutoBackupLocation()
                                        } else {
                                            onToggleAutoBackup(checked)
                                        }
                                    },
                                    modifier = Modifier.testTag("auto_backup_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(14.dp))

                            if (hasSelectedFile) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (autoBackupConfig.lastError == null) {
                                                    Icons.Default.CheckCircle
                                                } else {
                                                    Icons.Default.WarningAmber
                                                },
                                                contentDescription = null,
                                                tint = if (autoBackupConfig.lastError == null) {
                                                    portfolioColors.profit
                                                } else {
                                                    portfolioColors.loss
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ไฟล์ที่ผูกไว้: ${autoBackupConfig.fileName ?: "portpulse_auto_backup.json"}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = if (autoBackupConfig.lastError != null) {
                                                "สถานะ: ${autoBackupConfig.lastError}"
                                            } else if (autoBackupConfig.lastBackupMillis != null) {
                                                "บันทึกล่าสุด: ${FormatUtils.formatUpdatedTime(autoBackupConfig.lastBackupMillis)}"
                                            } else {
                                                "พร้อมบันทึกทับอัตโนมัติเมื่อมีการอัปเดตข้อมูล"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (autoBackupConfig.lastError != null) {
                                                portfolioColors.loss
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSelectAutoBackupLocation,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("change_auto_backup_location_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "เปลี่ยนตำแหน่ง",
                                            softWrap = true
                                        )
                                    }

                                    Button(
                                        onClick = onRunAutoBackupNow,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("run_auto_backup_now_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "สำรองทันที",
                                            softWrap = true
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "ยังไม่ได้กำหนดไฟล์สำหรับสำรองอัตโนมัติ กดปุ่มด้านล่างเพื่อเปิด File Explorer ของ Android เลือกโฟลเดอร์และไฟล์ปลายทาง",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onSelectAutoBackupLocation,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("select_auto_backup_location_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "เลือกตำแหน่งผ่าน File Explorer",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        softWrap = true
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Manual Export Card (All Categories Unified in One File)
                item {
                    BackupActionCard(
                        icon = Icons.Default.Download,
                        title = "ส่งออกข้อมูลรวมไฟล์เดียว (Export JSON)",
                        subtitle = "เลือกตำแหน่งบันทึกผ่าน File Explorer ของ Android รวมสินทรัพย์ทุกหมวดหมู่และกลุ่มอุตสาหกรรมไว้ในไฟล์ .json เดียว",
                        buttonLabel = "ส่งออกไฟล์ JSON (File Explorer)",
                        buttonTag = "backup_screen_export_button",
                        isPrimaryButton = false,
                        onClick = onExportJsonClick,
                        extraContent = {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssetCategory.entries.forEach { cat ->
                                    val count = allHoldings.count { it.assetCategory == cat }
                                    Surface(
                                        color = categoryContainerColor(cat),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${cat.shortLabelTh}: $count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = categoryOnContainerColor(cat),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                // 3. Manual Import Card (Restores All Categories from Single File)
                item {
                    BackupActionCard(
                        icon = Icons.Default.UploadFile,
                        title = "นำเข้าข้อมูลจากไฟล์เดียว (Import JSON)",
                        subtitle = "เปิด File Explorer ของ Android เพื่อเลือกไฟล์สำรอง .json และนำเข้าสินทรัพย์ทุกหมวดหมู่",
                        buttonLabel = "นำเข้าไฟล์ JSON (File Explorer)",
                        buttonTag = "backup_screen_import_button",
                        isPrimaryButton = false,
                        onClick = onImportJsonClick
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun BackupActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    buttonTag: String,
    isPrimaryButton: Boolean,
    onClick: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    val portfolioColors = LocalPortfolioColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = portfolioColors.accentHighlight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(12.dp))
                extraContent()
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isPrimaryButton) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(buttonTag)
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        softWrap = true
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(buttonTag)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        softWrap = true
                    )
                }
            }
        }
    }
}
