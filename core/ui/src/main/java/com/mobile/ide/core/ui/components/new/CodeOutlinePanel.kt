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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mobileide.app.data.OutlineSymbol
import com.mobileide.app.data.SymbolKind
import com.mobileide.app.ui.theme.*

@Composable
fun CodeOutlineDialog(
    symbols: List<OutlineSymbol>,
    onDismiss: () -> Unit,
    onNavigateTo: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, symbols) {
        if (query.isEmpty()) symbols
        else symbols.filter { it.name.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = IDESurface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().background(IDESurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, null, tint = IDEPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("File Outline", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = IDEOnSurface)
                    }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Filter symbols…", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                        focusedContainerColor = IDEBackground, unfocusedContainerColor = IDEBackground)
                )
                HorizontalDivider(color = IDEOutline)
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("No symbols found", color = IDEOnSurface)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(filtered) { symbol ->
                            OutlineSymbolRow(symbol) { onNavigateTo(symbol.line); onDismiss() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineSymbolRow(symbol: OutlineSymbol, onClick: () -> Unit) {
    val (icon, color) = symbolStyle(symbol.kind)
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(15.dp), tint = color)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(symbol.name, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, color = IDEOnBackground)
            Text(symbol.kind.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 10.sp, color = IDEOnSurface)
        }
        Text("L${symbol.line}", style = TextStyle(fontFamily = FontFamily.Monospace,
            fontSize = 10.sp, color = IDEOutline))
    }
}

private fun symbolStyle(kind: SymbolKind): Pair<ImageVector, Color> = when (kind) {
    SymbolKind.CLASS     -> Icons.Default.Category to Color(0xFF89DCEB)
    SymbolKind.FUNCTION  -> Icons.Default.Functions to Color(0xFF89B4FA)
    SymbolKind.PROPERTY  -> Icons.Default.Circle to Color(0xFFC3E88D)
    SymbolKind.VARIABLE  -> Icons.Default.Edit to Color(0xFFFAB387)
    SymbolKind.OBJECT    -> Icons.Default.DataObject to Color(0xFFCBA6F7)
    SymbolKind.INTERFACE -> Icons.Default.Layers to Color(0xFFF38BA8)
    SymbolKind.ENUM      -> Icons.Default.List to Color(0xFFF1FA8C)
    SymbolKind.COMPANION -> Icons.Default.People to Color(0xFF82AAFF)
}
