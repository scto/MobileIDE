package com.mobileide.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import java.io.File

/**
 * Full-screen folder picker dialog.
 *
 * Opens at [startPath].  The user navigates into subdirectories and
 * taps "Select this folder" (or any visible folder) to confirm.
 *
 * @param title          Dialog title shown in the top bar.
 * @param startPath      Initial directory to show (created if it doesn't exist).
 * @param confirmLabel   Label on the confirm button ("Open", "Import", …).
 * @param onDismiss      Called when the user cancels.
 * @param onConfirm      Called with the absolute path of the selected folder.
 * @param showFiles      Whether to also show regular files (grayed out, not selectable).
 * @param filterExtension If non-null only show files with this extension (e.g. "zip").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
    title: String                = "Select Folder",
    startPath: String            = "/storage/emulated/0/MobileIDEProjects",
    confirmLabel: String         = "Select",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    showFiles: Boolean           = false,
    filterExtension: String?     = null
) {
    // Ensure start directory exists
    val startDir = File(startPath).also { it.mkdirs() }

    var currentDir  by remember { mutableStateOf(startDir) }
    var entries     by remember { mutableStateOf(listDir(currentDir, showFiles, filterExtension)) }

    // Breadcrumb path segments from root to current
    val breadcrumbs: List<File> = remember(currentDir) {
        val segments = mutableListOf<File>()
        var f: File? = currentDir
        while (f != null) { segments.add(0, f); f = f.parentFile }
        // Only show from startDir's parent up to root within a reasonable depth
        segments.dropWhile { it.absolutePath.length < startDir.parent!!.length }
    }

    fun navigate(dir: File) {
        currentDir = dir
        entries    = listDir(dir, showFiles, filterExtension)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(20.dp),
            color    = IDESurface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
        ) {
            Column {
                // ── Title bar ───────────────────────────────────────────────
                Surface(color = IDESurfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, null,
                            Modifier.size(22.dp), tint = IDEPrimary)
                        Spacer(Modifier.width(10.dp))
                        Text(title, fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = IDEOnBackground,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null,
                                Modifier.size(18.dp), tint = IDEOnSurface)
                        }
                    }
                }

                // ── Breadcrumb bar ───────────────────────────────────────────
                Surface(color = IDEBackground) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Up button
                        if (currentDir.absolutePath != startDir.parent) {
                            IconButton(
                                onClick = { currentDir.parentFile?.let { navigate(it) } },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, null,
                                    Modifier.size(16.dp), tint = IDEPrimary)
                            }
                            Spacer(Modifier.width(4.dp))
                        }

                        breadcrumbs.forEachIndexed { i, seg ->
                            if (i > 0) {
                                Text("/", fontSize = 11.sp, color = IDEOutline,
                                    modifier = Modifier.padding(horizontal = 2.dp))
                            }
                            val isLast = i == breadcrumbs.lastIndex
                            Text(
                                text = if (seg == startDir) "MobileIDEProjects" else seg.name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isLast) IDEPrimary else IDEOnSurface,
                                modifier = Modifier.clickable(enabled = !isLast) { navigate(seg) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = IDEOutline)

                // ── Current directory label ──────────────────────────────────
                Surface(color = IDEPrimary.copy(alpha = 0.07f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null,
                            Modifier.size(14.dp), tint = IDEPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            currentDir.absolutePath,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = IDEPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── File / folder list ───────────────────────────────────────
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, null,
                                Modifier.size(48.dp), tint = IDEOutline)
                            Spacer(Modifier.height(8.dp))
                            Text("Empty folder", color = IDEOnSurface, fontSize = 13.sp)
                            if (currentDir == startDir) {
                                Text("No projects here yet", color = IDEOutline, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(entries, key = { it.absolutePath }) { file ->
                            FilePickerRow(
                                file     = file,
                                onClick  = {
                                    if (file.isDirectory) navigate(file)
                                    // files are greyed / non-clickable unless showFiles
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = IDEOutline)

                // ── Footer ───────────────────────────────────────────────────
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Current selection summary
                    Surface(
                        shape  = RoundedCornerShape(8.dp),
                        color  = IDESecondary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, IDESecondary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                Modifier.size(14.dp), tint = IDESecondary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                currentDir.absolutePath,
                                fontSize  = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color     = IDESecondary,
                                maxLines  = 2,
                                overflow  = TextOverflow.Ellipsis,
                                modifier  = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }

                        Button(
                            onClick  = { onConfirm(currentDir.absolutePath) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = IDEPrimary)
                        ) {
                            Icon(Icons.Default.FolderOpen, null,
                                Modifier.size(16.dp), tint = IDEBackground)
                            Spacer(Modifier.width(6.dp))
                            Text(confirmLabel, color = IDEBackground, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Single row in the list ─────────────────────────────────────────────────────

@Composable
private fun FilePickerRow(file: File, onClick: () -> Unit) {
    val isDir  = file.isDirectory
    val tint   = when {
        !isDir -> IDEOutline
        isAndroidProject(file) -> IDESecondary
        else -> IDEOnSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDir) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = when {
                isAndroidProject(file) -> Icons.Default.Android
                isDir                  -> Icons.Default.Folder
                file.extension == "zip"-> Icons.Default.Archive
                else                   -> Icons.Default.InsertDriveFile
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint     = tint
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = file.name,
                fontWeight = if (isAndroidProject(file)) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (isDir) IDEOnBackground else IDEOutline,
                fontSize   = 14.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (isAndroidProject(file)) {
                Text("Android project", fontSize = 10.sp, color = IDESecondary)
            } else if (isDir) {
                val count = file.listFiles()?.size ?: 0
                Text("$count items", fontSize = 10.sp, color = IDEOutline)
            } else {
                val kb = file.length() / 1024
                Text(if (kb > 0) "${kb} KB" else "empty", fontSize = 10.sp, color = IDEOutline)
            }
        }

        if (isDir) {
            Icon(Icons.Default.ChevronRight, null,
                Modifier.size(16.dp), tint = IDEOutline)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun listDir(
    dir: File,
    showFiles: Boolean,
    filterExt: String?
): List<File> {
    return try {
        dir.listFiles()
            ?.filter { f ->
                when {
                    f.name.startsWith(".") -> false
                    f.isDirectory          -> true
                    !showFiles             -> false
                    filterExt != null      -> f.extension.equals(filterExt, ignoreCase = true)
                    else                   -> true
                }
            }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    } catch (_: Exception) { emptyList() }
}

private fun isAndroidProject(dir: File): Boolean {
    if (!dir.isDirectory) return false
    return File(dir, "settings.gradle.kts").exists()
        || File(dir, "settings.gradle").exists()
        || File(dir, "build.gradle.kts").exists()
        || File(dir, "build.gradle").exists()
}
