package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface AutoBackupWriter {
    suspend fun writeJsonToUri(uriString: String, json: String): Result<Unit>
}

class AndroidAutoBackupWriter(context: Context) : AutoBackupWriter {
    private val appContext = context.applicationContext

    override suspend fun writeJsonToUri(uriString: String, json: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val bytes = json.toByteArray(Charsets.UTF_8)

                if (uri.scheme == "file") {
                    val path = uri.path?.takeIf { it.isNotBlank() }
                        ?: error("ไม่พบตำแหน่งไฟล์สำหรับบันทึก")
                    val targetFile = File(path)
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile, false).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                    return@runCatching
                }

                val resolver = appContext.contentResolver

                // Prefer "wt" (write + truncate) to overwrite existing content cleanly.
                val wroteWithWt = try {
                    resolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(bytes)
                        out.flush()
                    } != null
                } catch (_: UnsupportedOperationException) {
                    false
                } catch (_: IllegalArgumentException) {
                    false
                }

                if (!wroteWithWt) {
                    // Fallback: ParcelFileDescriptor with explicit channel truncate(0)
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
                        resolver.openOutputStream(uri, "w")?.use { out ->
                            out.write(bytes)
                            out.flush()
                        } ?: error("ไม่สามารถเปิดไฟล์เพื่อเขียนทับได้ กรุณาเลือกตำแหน่งไฟล์ใหม่")
                    }
                }
            }
        }

    companion object {
        fun persistWritePermission(context: Context, uri: Uri) {
            if (uri.scheme != "content") return
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }

        fun resolveDisplayName(context: Context, uri: Uri): String {
            if (uri.scheme == "file") {
                val fileName = uri.path?.let { File(it).name }?.ifBlank { null }
                if (fileName != null) return fileName
            }
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
            }
            return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
                ?: "portpulse_auto_backup.json"
        }
    }
}
