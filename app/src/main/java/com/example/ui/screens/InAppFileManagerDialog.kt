package com.example.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class InAppFileManagerMode {
    EXPORT_JSON,
    IMPORT_JSON,
    SELECT_AUTO_BACKUP
}

enum class MediaBackupCollection(val labelTh: String, val relativePath: String) {
    DOWNLOADS("Downloads", Environment.DIRECTORY_DOWNLOADS),
    DOCUMENTS("Documents", Environment.DIRECTORY_DOCUMENTS)
}

sealed interface InAppFileSelection {
    data class OpenUri(val uri: Uri, val displayName: String) : InAppFileSelection
    data class SaveFile(val targetFile: File) : InAppFileSelection
    data class SaveMediaStore(val collection: MediaBackupCollection, val fileName: String) : InAppFileSelection
}

private enum class FileManagerLocationTab(val labelTh: String) {
    APP_FOLDER("โฟลเดอร์แอป"),
    DOWNLOADS("Downloads"),
    DOCUMENTS("Documents")
}

private data class BrowserEntry(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModifiedMillis: Long = 0L,
    val file: File? = null,
    val contentUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppFileManagerDialog(
    mode: InAppFileManagerMode,
    suggestedFileName: String,
    onSelect: (InAppFileSelection) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appRootDir = remember(context) { resolveAppBackupRootDir(context) }

    val availableTabs = remember(mode) {
        if (mode == InAppFileManagerMode.SELECT_AUTO_BACKUP) {
            listOf(FileManagerLocationTab.APP_FOLDER)
        } else {
            listOf(
                FileManagerLocationTab.DOWNLOADS,
                FileManagerLocationTab.DOCUMENTS,
                FileManagerLocationTab.APP_FOLDER
            )
        }
    }

    var selectedTab by remember(mode) {
        mutableStateOf(
            if (mode == InAppFileManagerMode.SELECT_AUTO_BACKUP) {
                FileManagerLocationTab.APP_FOLDER
            } else {
                FileManagerLocationTab.DOWNLOADS
            }
        )
    }
    var currentDir by remember(appRootDir) { mutableStateOf(appRootDir) }
    var fileNameInput by remember(suggestedFileName) { mutableStateOf(suggestedFileName) }
    var entries by remember { mutableStateOf<List<BrowserEntry>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTab, currentDir, mode, refreshTrigger) {
        withContext(Dispatchers.Main.immediate) {
            entries = withContext(Dispatchers.IO) {
                loadDirectoryEntries(
                    context = context,
                    tab = selectedTab,
                    appDir = currentDir,
                    includeMediaStoreOnlyOnQ = true
                )
            }
        }
    }

    val isSaveMode = mode != InAppFileManagerMode.IMPORT_JSON
    val titleText = when (mode) {
        InAppFileManagerMode.EXPORT_JSON -> "เลือกตำแหน่งบันทึกไฟล์ (Export)"
        InAppFileManagerMode.IMPORT_JSON -> "เลือกไฟล์ JSON เพื่อนำเข้า"
        InAppFileManagerMode.SELECT_AUTO_BACKUP -> "เลือกตำแหน่งสำรองอัตโนมัติ"
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("สร้างโฟลเดอร์ใหม่") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = {
                            newFolderName = it
                            folderError = null
                        },
                        label = { Text("ชื่อโฟลเดอร์") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!folderError.isNullOrBlank()) {
                        Text(
                            text = folderError!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newFolderName.trim().replace("/", "").replace("\\", "")
                        if (clean.isBlank()) {
                            folderError = "กรุณาระบุชื่อโฟลเดอร์"
                            return@Button
                        }
                        val target = File(currentDir, clean)
                        val created = runCatching { target.exists() || target.mkdirs() }.getOrDefault(false)
                        if (created) {
                            currentDir = target
                            newFolderName = ""
                            showCreateFolderDialog = false
                            refreshTrigger++
                        } else {
                            folderError = "ไม่สามารถสร้างโฟลเดอร์ในตำแหน่งนี้ได้"
                        }
                    }
                ) {
                    Text("สร้าง")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            if (selectedTab == FileManagerLocationTab.APP_FOLDER && currentDir != appRootDir) {
                currentDir.parentFile?.let { parent ->
                    if (parent.absolutePath.startsWith(appRootDir.absolutePath)) {
                        currentDir = parent
                        return@BackHandler
                    }
                }
            }
            onDismiss()
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
                            onClick = onDismiss,
                            modifier = Modifier.testTag("file_manager_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "กลับ"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (selectedTab) {
                                    FileManagerLocationTab.APP_FOLDER -> currentDir.absolutePath
                                    FileManagerLocationTab.DOWNLOADS -> "โฟลเดอร์ Downloads"
                                    FileManagerLocationTab.DOCUMENTS -> "โฟลเดอร์ Documents"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (selectedTab == FileManagerLocationTab.APP_FOLDER && isSaveMode) {
                            IconButton(
                                onClick = {
                                    newFolderName = ""
                                    folderError = null
                                    showCreateFolderDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "สร้างโฟลเดอร์ใหม่"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (isSaveMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = fileNameInput,
                                onValueChange = { fileNameInput = it },
                                label = { Text("ชื่อไฟล์ (.json)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("file_manager_filename_input")
                            )
                            Button(
                                onClick = {
                                    val safeName = ensureJsonFileName(fileNameInput, suggestedFileName)
                                    val selection = when (selectedTab) {
                                        FileManagerLocationTab.APP_FOLDER -> {
                                            InAppFileSelection.SaveFile(File(currentDir, safeName))
                                        }
                                        FileManagerLocationTab.DOWNLOADS -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                InAppFileSelection.SaveMediaStore(MediaBackupCollection.DOWNLOADS, safeName)
                                            } else {
                                                val dlDir = resolvePublicDirectoryOrFallback(
                                                    Environment.DIRECTORY_DOWNLOADS,
                                                    appRootDir
                                                )
                                                InAppFileSelection.SaveFile(File(dlDir, safeName))
                                            }
                                        }
                                        FileManagerLocationTab.DOCUMENTS -> {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                InAppFileSelection.SaveMediaStore(MediaBackupCollection.DOCUMENTS, safeName)
                                            } else {
                                                val docDir = resolvePublicDirectoryOrFallback(
                                                    Environment.DIRECTORY_DOCUMENTS,
                                                    appRootDir
                                                )
                                                InAppFileSelection.SaveFile(File(docDir, safeName))
                                            }
                                        }
                                    }
                                    onSelect(selection)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("file_manager_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "บันทึกในตำแหน่งนี้",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableTabs.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.labelTh) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (tab) {
                                        FileManagerLocationTab.DOWNLOADS -> Icons.Default.Download
                                        FileManagerLocationTab.DOCUMENTS -> Icons.Default.FolderOpen
                                        FileManagerLocationTab.APP_FOLDER -> Icons.Default.Folder
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                if (selectedTab == FileManagerLocationTab.APP_FOLDER && currentDir != appRootDir) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                currentDir.parentFile?.let { parent ->
                                    if (parent.absolutePath.startsWith(appRootDir.absolutePath)) {
                                        currentDir = parent
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "ขึ้นไปโฟลเดอร์ก่อนหน้า",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ".. (ย้อนกลับไปโฟลเดอร์ก่อนหน้า)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSaveMode) {
                                "โฟลเดอร์นี้ยังไม่มีไฟล์ .json\nกด \"บันทึกในตำแหน่งนี้\" ด้านล่างเพื่อบันทึกไฟล์"
                            } else {
                                "ยังไม่มีไฟล์ .json ในตำแหน่งนี้\nลองสลับแท็บ Downloads / Documents / โฟลเดอร์แอป ด้านบน"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries, key = { "${it.isDirectory}:${it.name}:${it.contentUri ?: it.file?.absolutePath}" }) { item ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (item.isDirectory && item.file != null) {
                                            currentDir = item.file
                                        } else if (!item.isDirectory) {
                                            if (isSaveMode) {
                                                fileNameInput = item.name
                                            } else {
                                                val targetUri = item.contentUri ?: item.file?.let { Uri.fromFile(it) }
                                                if (targetUri != null) {
                                                    onSelect(InAppFileSelection.OpenUri(targetUri, item.name))
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!item.isDirectory) {
                                            val meta = formatFileMeta(item.sizeBytes, item.lastModifiedMillis)
                                            if (meta.isNotBlank()) {
                                                Text(
                                                    text = meta,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "โฟลเดอร์",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveAppBackupRootDir(context: Context): File {
    val externalDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    val base = externalDocs ?: File(context.filesDir, "backups")
    if (!base.exists()) {
        runCatching { base.mkdirs() }
    }
    return base
}

private fun resolvePublicDirectoryOrFallback(envType: String, fallbackDir: File): File {
    @Suppress("DEPRECATION")
    val publicDir = Environment.getExternalStoragePublicDirectory(envType)
    return if (publicDir != null && (publicDir.exists() || publicDir.mkdirs()) && publicDir.canWrite()) {
        publicDir
    } else {
        fallbackDir
    }
}

private fun ensureJsonFileName(raw: String, fallback: String): String {
    val cleaned = raw.trim()
        .replace("/", "_")
        .replace("\\", "_")
        .ifBlank { fallback }
    return if (cleaned.endsWith(".json", ignoreCase = true)) cleaned else "$cleaned.json"
}

private fun formatFileMeta(sizeBytes: Long, modifiedMillis: Long): String {
    val sizeText = when {
        sizeBytes <= 0L -> ""
        sizeBytes < 1024L -> "$sizeBytes B"
        else -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
    }
    val dateText = if (modifiedMillis > 0L) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(modifiedMillis))
    } else {
        ""
    }
    return listOf(sizeText, dateText).filter { it.isNotBlank() }.joinToString(" • ")
}

private fun loadDirectoryEntries(
    context: Context,
    tab: FileManagerLocationTab,
    appDir: File,
    includeMediaStoreOnlyOnQ: Boolean
): List<BrowserEntry> {
    if (tab == FileManagerLocationTab.APP_FOLDER) {
        return listLocalFileEntries(appDir)
    }

    if (includeMediaStoreOnlyOnQ && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val mediaItems = queryMediaStoreJsonFiles(context, tab)
        if (mediaItems.isNotEmpty()) return mediaItems
    }

    val envType = if (tab == FileManagerLocationTab.DOWNLOADS) {
        Environment.DIRECTORY_DOWNLOADS
    } else {
        Environment.DIRECTORY_DOCUMENTS
    }
    @Suppress("DEPRECATION")
    val publicDir = Environment.getExternalStoragePublicDirectory(envType)
    return if (publicDir != null && publicDir.exists() && publicDir.canRead()) {
        listLocalFileEntries(publicDir)
    } else {
        emptyList()
    }
}

private fun listLocalFileEntries(dir: File): List<BrowserEntry> {
    val children = runCatching { dir.listFiles()?.toList() }.getOrNull() ?: return emptyList()
    val dirs = children.filter { it.isDirectory && !it.isHidden }
        .sortedBy { it.name.lowercase(Locale.US) }
        .map {
            BrowserEntry(
                name = it.name,
                isDirectory = true,
                lastModifiedMillis = it.lastModified(),
                file = it
            )
        }
    val jsonFiles = children.filter { it.isFile && it.name.endsWith(".json", ignoreCase = true) }
        .sortedByDescending { it.lastModified() }
        .map {
            BrowserEntry(
                name = it.name,
                isDirectory = false,
                sizeBytes = it.length(),
                lastModifiedMillis = it.lastModified(),
                file = it
            )
        }
    return dirs + jsonFiles
}

private fun queryMediaStoreJsonFiles(
    context: Context,
    tab: FileManagerLocationTab
): List<BrowserEntry> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
    val collectionUri = if (tab == FileManagerLocationTab.DOWNLOADS) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Files.getContentUri("external")
    }
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.RELATIVE_PATH
    )
    val selection: String
    val selectionArgs: Array<String>
    if (tab == FileManagerLocationTab.DOWNLOADS) {
        selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        selectionArgs = arrayOf("%.json")
    } else {
        selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        selectionArgs = arrayOf("%.json", "${Environment.DIRECTORY_DOCUMENTS}%")
    }

    val results = mutableListOf<BrowserEntry>()
    runCatching {
        context.contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val modIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx) ?: continue
                val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                val modSec = if (modIdx >= 0) cursor.getLong(modIdx) else 0L
                val itemUri = ContentUris.withAppendedId(collectionUri, id)
                results.add(
                    BrowserEntry(
                        name = name,
                        isDirectory = false,
                        sizeBytes = size,
                        lastModifiedMillis = modSec * 1000L,
                        contentUri = itemUri
                    )
                )
            }
        }
    }
    return results
}
