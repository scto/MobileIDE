package com.mobileide.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*

data class SymbolKey(val label: String, val insert: String = label)

val SYMBOL_ROWS = listOf(
    // Row 1 – brackets & operators
    listOf(
        SymbolKey("Tab", "    "),
        SymbolKey("{"), SymbolKey("}"),
        SymbolKey("("), SymbolKey(")"),
        SymbolKey("["), SymbolKey("]"),
        SymbolKey("<"), SymbolKey(">"),
        SymbolKey(";"), SymbolKey(","),
        SymbolKey("."), SymbolKey(":"),
    ),
    // Row 2 – common Kotlin ops
    listOf(
        SymbolKey("="), SymbolKey("!="),
        SymbolKey("=="), SymbolKey("->"),
        SymbolKey("=>", "=>"), SymbolKey("?."),
        SymbolKey("!!"), SymbolKey("?:"),
        SymbolKey("&&"), SymbolKey("||"),
        SymbolKey("!"), SymbolKey("@"),
        SymbolKey("#"), SymbolKey("$"),
    ),
    // Row 3 – snippets
    listOf(
        SymbolKey("fun", "fun name() {\n    \n}"),
        SymbolKey("val", "val name = "),
        SymbolKey("var", "var name = "),
        SymbolKey("if", "if (condition) {\n    \n}"),
        SymbolKey("for", "for (item in list) {\n    \n}"),
        SymbolKey("when", "when (value) {\n    else -> {}\n}"),
        SymbolKey("class", "class Name(\n    \n) {\n    \n}"),
        SymbolKey("@Comp", "@Composable\nfun Name() {\n    \n}"),
        SymbolKey("λ", "{ it }"),
        SymbolKey("TODO", "TODO(\"Not yet implemented\")"),
    )
)

@Composable
fun SmartKeyboardBar(
    textValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRow by remember { mutableStateOf(0) }

    Column(modifier = modifier.background(IDESurfaceVariant)) {

        // Row selector tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IDESurface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("{ }", "ops", "snip").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedRow = index }
                        .background(
                            if (selectedRow == index) IDEPrimary.copy(alpha = 0.2f)
                            else IDESurface
                        )
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (selectedRow == index) IDEPrimary else IDEOnSurface
                        )
                    )
                }
            }

            // Undo / redo placeholders
            IconButton(onClick = { /* TODO: undo */ }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp), tint = IDEOnSurface)
            }
            IconButton(onClick = { /* TODO: redo */ }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), tint = IDEOnSurface)
            }
        }

        // Symbol keys
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SYMBOL_ROWS[selectedRow].forEach { key ->
                SymbolKeyButton(key) {
                    val inserted = insertText(textValue, key.insert)
                    onValueChange(inserted)
                }
            }
        }
    }
}

private fun insertText(current: TextFieldValue, text: String): TextFieldValue {
    val sel    = current.selection
    val before = current.text.substring(0, sel.start)
    val after  = current.text.substring(sel.end)
    val newText = before + text + after
    val newCursor = sel.start + text.length
    return current.copy(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(newCursor)
    )
}

@Composable
private fun SymbolKeyButton(key: SymbolKey, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(IDESurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            key.label,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = IDEOnBackground
            )
        )
    }
}
