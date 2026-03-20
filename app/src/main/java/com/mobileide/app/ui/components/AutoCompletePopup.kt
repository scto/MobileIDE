package com.mobileide.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.utils.AutoCompleteEngine
import com.mobileide.app.utils.Completion
import com.mobileide.app.utils.CompletionKind

@Composable
fun AutoCompletePopup(
    textValue: TextFieldValue,
    fileSymbols: List<String>,
    onApply: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val completions = remember(textValue.selection.start) {
        AutoCompleteEngine.getCompletions(textValue, fileSymbols)
    }

    AnimatedVisibility(
        visible = completions.isNotEmpty(),
        enter = slideInVertically { it } + fadeIn(),
        exit  = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = IDESurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, IDEOutline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                completions.forEach { completion ->
                    CompletionChip(completion) {
                        onApply(AutoCompleteEngine.applyCompletion(textValue, completion))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionChip(completion: Completion, onClick: () -> Unit) {
    val (bgColor, textColor, icon) = completionStyle(completion.kind)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(11.dp), tint = textColor.copy(alpha = 0.8f))
            Text(
                completion.label,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            )
            if (completion.detail.isNotEmpty()) {
                Text(
                    completion.detail,
                    style = TextStyle(fontSize = 9.sp, color = textColor.copy(alpha = 0.5f))
                )
            }
        }
    }
}

private fun completionStyle(kind: CompletionKind): Triple<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color, ImageVector> =
    when (kind) {
        CompletionKind.KEYWORD  -> Triple(IDESurface, SyntaxKeyword, Icons.Default.Code)
        CompletionKind.FUNCTION -> Triple(IDESurfaceVariant, SyntaxFunction, Icons.Default.Functions)
        CompletionKind.CLASS    -> Triple(IDESurfaceVariant, SyntaxType, Icons.Default.Category)
        CompletionKind.PROPERTY -> Triple(IDESurface, SyntaxAnnotation, Icons.Default.Tune)
        CompletionKind.SNIPPET  -> Triple(IDEPrimary.copy(alpha = 0.1f), IDEPrimary, Icons.Default.AutoAwesome)
        CompletionKind.IMPORT   -> Triple(IDESurface, IDEOnSurface, Icons.Default.Input)
        CompletionKind.VARIABLE -> Triple(IDESurface, SyntaxString, Icons.Default.DataObject)
    }
