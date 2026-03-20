package com.mobileide.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobileide.app.ui.icons.AppIcon
import com.mobileide.app.utils.commands.*
import com.mobileide.app.utils.commands.KeybindingsManager
import com.mobileide.app.viewmodel.IDEViewModel

/**
 * Command Palette — search and execute any registered [Command].
 *
 * Features:
 * - Fuzzy label search with match highlighting
 * - Sub-palette navigation (animated Left/Right slide)
 * - Keybind display next to each item
 * - Recently used command shown first
 * - Section dividers (sectionId groups)
 *
 * Triggered via [IDEViewModel.commandPaletteVisible].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPaletteScreen(vm: IDEViewModel) {
    val visible         by vm.commandPaletteVisible.collectAsState()
    val commands        by vm.commandPaletteCommands.collectAsState()
    val lastUsed        by vm.commandPaletteLastUsed.collectAsState()
    val initChildren    by vm.commandPaletteInitialChildren.collectAsState()
    val initPlaceholder by vm.commandPaletteInitialPlaceholder.collectAsState()

    if (!visible) return

    Dialog(
        onDismissRequest = { vm.dismissCommandPalette() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CommandPaletteContent(
            commands             = commands,
            lastUsedCommand      = lastUsed,
            initialChildCommands = initChildren,
            initialPlaceholder   = initPlaceholder,
            onDismiss            = { vm.dismissCommandPalette() },
        )
    }
}

@Composable
private fun CommandPaletteContent(
    commands: List<Command>,
    lastUsedCommand: Command?,
    initialChildCommands: List<Command>?,
    initialPlaceholder: String?,
    onDismiss: () -> Unit,
) {
    var query    by remember { mutableStateOf("") }
    val focusReq = remember { FocusRequester() }

    var childCmds       by remember { mutableStateOf(initialChildCommands) }
    var placeholderText by remember { mutableStateOf(initialPlaceholder) }

    val sortedRoot by remember(commands, lastUsedCommand) {
        derivedStateOf {
            buildList {
                lastUsedCommand?.let { add(it) }
                addAll(commands.filter { it != lastUsedCommand })
            }
        }
    }

    val visible  = childCmds ?: sortedRoot
    val filtered by remember(visible, query) {
        derivedStateOf {
            if (query.isBlank()) visible
            else visible.filter {
                it.getLabel().contains(query, ignoreCase = true) ||
                it.prefix?.contains(query, ignoreCase = true) == true
            }
        }
    }
    val grouped = filtered.groupBy { it.sectionId }

    BackHandler(enabled = childCmds != null && initialChildCommands == null) {
        childCmds = null; placeholderText = null; query = ""
    }

    Surface(
        modifier      = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.75f),
        shape         = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        color         = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            // ── Search field ──────────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = {
                    Text(
                        if (childCmds != null && placeholderText != null) placeholderText!!
                        else "Type a command…"
                    )
                },
                maxLines      = 1,
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .focusRequester(focusReq),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                leadingIcon   = {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                        tint = MaterialTheme.colorScheme.primary)
                },
            )

            LaunchedEffect(Unit) { focusReq.requestFocus() }

            HorizontalDivider()

            // ── Command list ──────────────────────────────────────────────────
            AnimatedContent(
                targetState = childCmds != null,
                transitionSpec = {
                    if (targetState)
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) togetherWith
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left)
                    else
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) togetherWith
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
                },
                label = "sub-palette",
            ) { isSubPage ->
                LazyColumn(
                    modifier       = Modifier.padding(vertical = 4.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    grouped.entries.forEachIndexed { idx, (_, cmds) ->
                        items(cmds, key = { it.id }) { cmd ->
                            CommandRow(
                                command         = cmd,
                                isRecentlyUsed  = cmd == lastUsedCommand,
                                searchQuery     = query,
                                isSubPage       = isSubPage,
                                onDismiss       = onDismiss,
                                onNavigateToSub = { placeholder, children ->
                                    childCmds       = children
                                    placeholderText = placeholder
                                    query           = ""
                                },
                            )
                        }
                        if (idx < grouped.size - 1) {
                            item { HorizontalDivider(Modifier.padding(vertical = 2.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandRow(
    command: Command,
    isRecentlyUsed: Boolean,
    searchQuery: String,
    isSubPage: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSub: (String?, List<Command>) -> Unit,
) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val enabled  by remember { derivedStateOf { command.isSupported() && command.isEnabled() } }
    val keybind  = remember(command.id) { KeybindingsManager.getForCommand(command.id) }

    val start = command.getLabel().indexOf(searchQuery, ignoreCase = true)
    val highlighted = remember(searchQuery) {
        buildAnnotatedString {
            append(command.getLabel())
            if (start >= 0 && searchQuery.isNotEmpty()) {
                addStyle(
                    SpanStyle(color = Color.Unspecified, fontWeight = FontWeight.Bold),
                    start, start + searchQuery.length
                )
            }
        }
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (command.childCommands.isNotEmpty()) {
                    onNavigateToSub(command.getChildSearchPlaceholder(), command.childCommands)
                } else {
                    onDismiss()
                    if (activity != null) {
                        command.action(ActionContext(activity))
                    }
                }
            },
        leadingContent = {
            AppIcon(
                icon               = command.getIcon(),
                modifier           = Modifier.size(20.dp),
                contentDescription = command.getLabel(),
                tint               = if (command is ToggleableCommand && command.isOn())
                    MaterialTheme.colorScheme.primary
                else
                    LocalContentColor.current,
            )
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                command.prefix?.let {
                    Text("$it: ", color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text     = highlighted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color    = if (!enabled)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        Color.Unspecified,
                )
                if (isRecentlyUsed) {
                    Text(
                        "recent",
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 10.sp,
                        color      = MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        supportingContent = if (!isSubPage) ({
            Text(
                text     = "Global",
                maxLines = 1,
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }) else null,
        trailingContent = {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                keybind?.let {
                    Text(
                        it.getDisplayName(),
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 11.sp,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }
                if (command.childCommands.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
