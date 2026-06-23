/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.scto.mobile.ide.ui.settings.debugOptions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat

import com.rk.components.StyledTextField
import com.rk.utils.application
import com.rk.xededitor.BuildConfig

@Composable
fun AppLogs() {
    var logLevel by remember { mutableStateOf(LogLevel.INFO) }
    val logText = buildLogs(logLevel)

    LogScreen(logText, "App Logs Report", "app_logs") { LogLevelDropdown(logLevel, { logLevel = it }) }
}

private fun buildLogs(logLevel: LogLevel): String {
    val entries =
        LogCollector.logs
            .filter { it.level.ordinal <= logLevel.ordinal }
            .joinToString("\n") { "[${it.level.name.uppercase()}] ${it.message}" }

    val packageInfo = with(application!!) { packageManager.getPackageInfo(packageName, 0) }
    val versionName = packageInfo.versionName
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

    return buildString {
        append("[REPORT] App version: ").append(versionName).appendLine()
        append("[REPORT] Version code: ").append(versionCode).appendLine()
        append("[REPORT] Commit hash: ").append(BuildConfig.GIT_COMMIT_HASH.substring(0, 8)).appendLine()

        appendLine()

        append(entries)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.LogLevelDropdown(logLevel: LogLevel, onLogLevelChange: (LogLevel) -> Unit) {
    var dropdownMenuExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = dropdownMenuExpanded,
        onExpandedChange = { dropdownMenuExpanded = !dropdownMenuExpanded },
        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
    ) {
        StyledTextField(
            value = logLevel.label,
            onValueChange = {},
            shape = RoundedCornerShape(8.dp),
            maxLines = 1,
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownMenuExpanded) },
            modifier =
                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().height(42.dp),
        )

        ExposedDropdownMenu(expanded = dropdownMenuExpanded, onDismissRequest = { dropdownMenuExpanded = false }) {
            LogLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(text = level.label) },
                    onClick = {
                        onLogLevelChange(level)
                        dropdownMenuExpanded = false
                    },
                )
            }
        }
    }
}
