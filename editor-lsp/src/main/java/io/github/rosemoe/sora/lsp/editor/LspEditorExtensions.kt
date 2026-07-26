package io.github.rosemoe.sora.lsp.editor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either

suspend fun LspEditor.requestDefinitionAt(line: Int, column: Int): List<Location> = withContext(Dispatchers.IO) {
    try {
        val params = TextDocumentPositionParams(
            TextDocumentIdentifier(uri.toString()),
            Position(line, column)
        )
        val result = requestManager.definition(params).await() ?: return@withContext emptyList()
        when {
            result.isLeft -> result.left ?: emptyList()
            result.isRight -> result.right?.map { Location(it.targetUri, it.targetRange) } ?: emptyList()
            else -> emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun LspEditor.requestReferencesAt(line: Int, column: Int): List<Location> = withContext(Dispatchers.IO) {
    try {
        val params = ReferenceParams(
            TextDocumentIdentifier(uri.toString()),
            Position(line, column),
            ReferenceContext(true)
        )
        requestManager.references(params).await() ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun LspEditor.requestRenameAt(line: Int, column: Int, newName: String): WorkspaceEdit? = withContext(Dispatchers.IO) {
    try {
        val params = RenameParams(
            TextDocumentIdentifier(uri.toString()),
            Position(line, column),
            newName
        )
        requestManager.rename(params).await()
    } catch (e: Exception) {
        null
    }
}

suspend fun LspEditor.requestHoverAt(line: Int, column: Int): Hover? = withContext(Dispatchers.IO) {
    try {
        val params = HoverParams(
            TextDocumentIdentifier(uri.toString()),
            Position(line, column)
        )
        requestManager.hover(params).await()
    } catch (e: Exception) {
        null
    }
}

suspend fun LspEditor.requestSignatureHelpAt(line: Int, column: Int): SignatureHelp? = withContext(Dispatchers.IO) {
    try {
        val params = SignatureHelpParams(
            TextDocumentIdentifier(uri.toString()),
            Position(line, column)
        )
        requestManager.signatureHelp(params).await()
    } catch (e: Exception) {
        null
    }
}
