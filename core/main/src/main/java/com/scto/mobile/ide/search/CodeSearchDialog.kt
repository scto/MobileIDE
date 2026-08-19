package com.scto.mobile.ide.search





import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.scto.mobile.ide.activities.main.MainViewModel
import com.scto.mobile.ide.components.SingleInputDialog
import com.scto.mobile.ide.components.XedDialog
import com.scto.mobile.ide.components.XedDropdownMenuItem
import com.scto.mobile.ide.components.compose.utils.addIf
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.filetree.FileIcon
import com.scto.mobile.ide.filetree.getAppropriateName
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.tabs.editor.EditorTab
import com.scto.mobile.ide.utils.rememberNumberFormatter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch











@OptIn(FlowPreview::class)
@Composable
fun CodeSearchDialog(
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    projectFile: FileObject,
    onFinish: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val viewportHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

    val editorTab = mainViewModel.currentTab as? EditorTab
    val textFieldSearchState =
        rememberTextFieldState(
            editorTab?.editorState?.editor?.get()?.getSelectedText() ?: searchViewModel.codeSearchQuery
        )
    LaunchedEffect(textFieldSearchState.text) { searchViewModel.codeSearchQuery = textFieldSearchState.text.toString() }

    val textFieldReplaceState = rememberTextFieldState(searchViewModel.codeReplaceQuery)
    LaunchedEffect(textFieldReplaceState.text) {
        searchViewModel.codeReplaceQuery = textFieldReplaceState.text.toString()
    }

    LaunchedEffect(
        searchViewModel.isIndexing(projectFile),
        searchViewModel.codeSearchQuery,
        searchViewModel.ignoreCase,
        searchViewModel.fileMaskText,
    ) {
        searchViewModel.launchCodeSearch(context, mainViewModel, projectFile)
    }

    if (searchViewModel.showFileMaskDialog) {
        ExcludeFilesDialog(searchViewModel)
    }

    fun replace(codeItem: CodeItem) {
        searchViewModel.viewModelScope.launch {
            searchViewModel.replaceIn(mainViewModel, codeItem)
            searchViewModel.launchCodeSearch(context, mainViewModel, projectFile)
        }
    }

    fun replaceAll(codeItems: List<CodeItem>) {
        searchViewModel.viewModelScope.launch {
            searchViewModel.replaceAllIn(mainViewModel, codeItems)
            searchViewModel.launchCodeSearch(context, mainViewModel, projectFile)
        }
    }

    XedDialog(onDismissRequest = onFinish, modifier = Modifier.imePadding()) {
        Column(modifier = Modifier.animateContentSize().height(viewportHeight * 0.8f)) {
            TextField(
                state = textFieldSearchState,
                lineLimits = TextFieldLineLimits.SingleLine,
                leadingIcon = {
                    IconButton(modifier = Modifier, onClick = { searchViewModel.toggleReplaceShown() }) {
                        Icon(
                            imageVector =
                                if (searchViewModel.isReplaceShown) {
                                    Icons.Outlined.KeyboardArrowUp
                                } else {
                                    Icons.Outlined.KeyboardArrowDown
                                },
                            null,
                        )
                    }
                },
                trailingIcon = {
                    Box {
                        IconButton(onClick = { searchViewModel.showOptionsMenu = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, stringResource(com.scto.mobile.ide.core.main.R.string.more))
                        }

                        DropdownMenu(
                            expanded = searchViewModel.showOptionsMenu,
                            onDismissRequest = { searchViewModel.showOptionsMenu = false },
                        ) {
                            XedDropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = searchViewModel.ignoreCase, onCheckedChange = null)
                                        Spacer(Modifier.width(12.dp))
                                        Text(stringResource(com.scto.mobile.ide.core.main.R.string.ignore_case))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                },
                                onClick = {
                                    searchViewModel.ignoreCase = !searchViewModel.ignoreCase
                                    searchViewModel.showOptionsMenu = false
                                },
                            )

                            XedDropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.edit), contentDescription = null)
                                        Spacer(Modifier.width(12.dp))
                                        Text(stringResource(com.scto.mobile.ide.core.main.R.string.file_mask))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                },
                                onClick = {
                                    searchViewModel.showFileMaskDialog = true
                                    searchViewModel.showOptionsMenu = false
                                },
                            )
                        }
                    }
                },
                keyboardOptions =
                    KeyboardOptions(
                        imeAction =
                            if (searchViewModel.isReplaceShown) {
                                ImeAction.Next
                            } else {
                                ImeAction.Search
                            }
                    ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(text = stringResource(com.scto.mobile.ide.core.main.R.string.search)) },
                supportingText =
                    if (!searchViewModel.isReplaceShown) {
                        {
                            Text(
                                text =
                                    stringResource(com.scto.mobile.ide.core.main.R.string.searching_in)
                                        .fillPlaceholders(projectFile.getAppropriateName()),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else null,
            )

            if (searchViewModel.isReplaceShown) {
                TextField(
                    state = textFieldReplaceState,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text(text = stringResource(com.scto.mobile.ide.core.main.R.string.replace)) },
                    shape = RectangleShape,
                    trailingIcon = {
                        IconButton(
                            enabled = searchViewModel.totalCodeSearchResults != 0,
                            onClick = { replaceAll(searchViewModel.codeSearchResults.values.flatten()) },
                        ) {
                            Icon(
                                painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.find_replace),
                                contentDescription = stringResource(com.scto.mobile.ide.core.main.R.string.replace),
                            )
                        }
                    },
                    supportingText = {
                        Text(
                            text =
                                stringResource(com.scto.mobile.ide.core.main.R.string.searching_in).fillPlaceholders(projectFile.getAppropriateName()),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 8.dp,
                    ),
            ) {
                if (searchViewModel.isIndexing(projectFile) || searchViewModel.isSearchingCode) {
                    CircularProgressIndicator(modifier = Modifier.size(9.dp), strokeWidth = 2.dp)
                }
                val numberFormatter = rememberNumberFormatter()
                val resultCount by remember {
                    derivedStateOf {
                        val amount = searchViewModel.totalCodeSearchResults
                        val suffix = if (amount == SearchViewModel.MAX_CODE_RESULTS) "+" else ""
                        numberFormatter.format(amount) + suffix
                    }
                }
                Text(
                    stringResource(
                            when {
                                searchViewModel.isIndexing(projectFile) -> com.scto.mobile.ide.core.main.R.string.indexing
                                searchViewModel.totalCodeSearchResults != 0 -> com.scto.mobile.ide.core.main.R.string.results
                                else -> com.scto.mobile.ide.core.main.R.string.no_results
                            }
                        )
                        .fillPlaceholders(resultCount)
                )
            }

            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            if (searchViewModel.codeSearchQuery.isNotEmpty()) {
                LazyColumn {
                    searchViewModel.codeSearchResultsOrder.forEachIndexed { _, fileObject ->
                        val codeItems = searchViewModel.codeSearchResults[fileObject] ?: return@forEachIndexed
                        val isCollapsed = searchViewModel.isCollapsed(fileObject)

                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.addIf(codeItems.first().isHidden) { alpha(0.5f) }
                                        .clickable { searchViewModel.toggleCollapsed(fileObject) }
                                        .padding(
                                            start = 16.dp,
                                            end = 8.dp,
                                            top = 8.dp,
                                            bottom = 8.dp,
                                        ),
                            ) {
                                Icon(
                                    imageVector =
                                        if (isCollapsed) Icons.AutoMirrored.Outlined.KeyboardArrowRight
                                        else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                FileIcon(file = fileObject, iconTint = MaterialTheme.colorScheme.primary)

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text =
                                        if (codeItems.first().isOpen) {
                                            stringResource(com.scto.mobile.ide.core.main.R.string.file_name_opened)
                                                .fillPlaceholders(fileObject.getName())
                                        } else {
                                            fileObject.getName()
                                        },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )

                                if (searchViewModel.isReplaceShown) {
                                    CompositionLocalProvider(
                                        LocalContentColor provides MaterialTheme.colorScheme.primary
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier =
                                                Modifier.clip(ButtonDefaults.shape)
                                                    .clickable { replaceAll(codeItems) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = stringResource(com.scto.mobile.ide.core.main.R.string.replace_all),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )

                                            Spacer(Modifier.width(4.dp))

                                            Icon(
                                                painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.arrow_downward),
                                                contentDescription = stringResource(com.scto.mobile.ide.core.main.R.string.replace),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!isCollapsed) {
                            items(items = codeItems) { codeItem ->
                                CodeItemRow(
                                    item = codeItem,
                                    leadingIcon =
                                        if (searchViewModel.isReplaceShown) {
                                            {
                                                Icon(
                                                    painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.find_replace),
                                                    contentDescription = stringResource(com.scto.mobile.ide.core.main.R.string.replace),
                                                    modifier =
                                                        Modifier.clip(RoundedCornerShape(8.dp))
                                                            .clickable(onClick = { replace(codeItem) }),
                                                )
                                            }
                                        } else null,
                                    onClick = {
                                        codeItem.onClick()
                                        onFinish()
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(com.scto.mobile.ide.core.main.R.string.enter_query_to_search),
                        modifier = Modifier.fillMaxWidth(0.5f),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun ExcludeFilesDialog(searchViewModel: SearchViewModel) {
    var fileMaskText by remember { mutableStateOf(searchViewModel.fileMaskText) }

    SingleInputDialog(
        title = stringResource(id = com.scto.mobile.ide.core.main.R.string.file_mask),
        inputLabel = stringResource(id = com.scto.mobile.ide.core.main.R.string.file_mask_hint),
        inputValue = fileMaskText,
        onInputValueChange = { fileMaskText = it },
        onConfirm = {
            searchViewModel.fileMaskText = fileMaskText
            Settings.file_mask = fileMaskText
        },
        onFinish = {
            searchViewModel.fileMaskText = Settings.file_mask
            searchViewModel.showFileMaskDialog = false
        },
    )
}
