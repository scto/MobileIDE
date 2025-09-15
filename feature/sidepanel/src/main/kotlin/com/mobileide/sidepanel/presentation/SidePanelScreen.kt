package com.mobileide.sidepanel.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afigaliyev.treeview.TreeView

@Composable
fun SidePanelScreen(
    viewModel: SidePanelViewModel = hiltViewModel(),
    onFileSelected: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxHeight().width(280.dp).padding(end = 8.dp)) {
        ViewSwitcher(
            currentView = state.currentView,
            onViewChange = { viewModel.onEvent(SidePanelEvent.ViewChanged(it)) }
        )
        Divider()

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            TreeView(
                items = state.treeItems,
                onItemClick = { item ->
                     if(item.type == ItemType.FILE) {
                         onFileSelected(item.path)
                     } else {
                         viewModel.onEvent(SidePanelEvent.ItemToggled(item.id))
                     }
                }
            ) { item ->
                TreeItemRow(item = item)
            }
        }
    }
}

@Composable
fun ViewSwitcher(currentView: SidePanelView, onViewChange: (SidePanelView) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(onClick = { onViewChange(SidePanelView.FILE_EXPLORER) }) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "File Explorer",
                tint = if (currentView == SidePanelView.FILE_EXPLORER) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
        IconButton(onClick = { onViewChange(SidePanelView.PROJECT_STRUCTURE) }) {
            Icon(
                Icons.Default.AccountTree,
                contentDescription = "Project Structure",
                tint = if (currentView == SidePanelView.PROJECT_STRUCTURE) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
        IconButton(onClick = { onViewChange(SidePanelView.GIT_CHANGES) }) {
            Icon(
                Icons.Default.Source,
                contentDescription = "Git Changes",
                 tint = if (currentView == SidePanelView.GIT_CHANGES) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
    }
}


@Composable
fun TreeItemRow(item: TreeItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.level * 16).dp) 
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isExpandable = item.type != ItemType.FILE

        val expansionIcon = when {
            !isExpandable -> null
            item.isExpanded -> Icons.Default.KeyboardArrowDown
            else -> Icons.Default.KeyboardArrowRight
        }

        if (expansionIcon != null) {
            Icon(
                imageVector = expansionIcon,
                contentDescription = "Toggle",
                modifier = Modifier.size(20.dp),
                tint = LocalContentColor.current.copy(alpha = 0.6f)
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }

        Icon(
            imageVector = item.type.toIcon(item.isExpanded),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = item.gitStatus.toColor()
        )
    }
}

fun ItemType.toIcon(isExpanded: Boolean): ImageVector {
    return when (this) {
        ItemType.FILE -> Icons.Default.Description
        ItemType.DIRECTORY -> if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
        ItemType.PROJECT -> Icons.Default.AccountTree
        ItemType.PACKAGE -> Icons.Default.Folder
        ItemType.GIT_BRANCH -> Icons.Default.Source
    }
}

@Composable
fun GitStatus.toColor(): Color {
    return when(this) {
        GitStatus.MODIFIED -> Color(0xFFE2C087) // Yellowish
        GitStatus.NEW -> Color(0xFF81C784) // Greenish
        GitStatus.CONFLICT -> Color(0xFFE57373) // Reddish
        else -> LocalContentColor.current
    }
}

