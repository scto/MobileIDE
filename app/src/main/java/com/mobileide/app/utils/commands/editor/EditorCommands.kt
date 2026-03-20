package com.mobileide.app.utils.commands.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mobileide.app.ui.icons.AppIconType
import com.mobileide.app.utils.commands.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

// ── Copy ─────────────────────────────────────────────────────────────────────
class CopyCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.copy"
    override fun getLabel() = "Copy"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.ContentCopy)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_C, ctrl = true)
    override fun action(editorActionContext: EditorActionContext) {
        editorActionContext.editor.copyText()
    }
}

// ── Cut ──────────────────────────────────────────────────────────────────────
class CutCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.cut"
    override fun getLabel() = "Cut"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.ContentCut)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_X, ctrl = true)
    override fun isEnabled(vm: IDEViewModel) = !vm.editorSettings.value.wordWrap // cut only if editable
    override fun action(editorActionContext: EditorActionContext) {
        editorActionContext.editor.cutText()
    }
}

// ── Paste ─────────────────────────────────────────────────────────────────────
class PasteCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.paste"
    override fun getLabel() = "Paste"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.ContentPaste)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_V, ctrl = true)
    override fun action(editorActionContext: EditorActionContext) {
        editorActionContext.editor.pasteText()
    }
}

// ── Select All ────────────────────────────────────────────────────────────────
class SelectAllCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.select_all"
    override fun getLabel() = "Select All"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.SelectAll)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_A, ctrl = true)
    override fun action(editorActionContext: EditorActionContext) {
        editorActionContext.editor.selectAll()
    }
}

// ── Select Word ───────────────────────────────────────────────────────────────
class SelectWordCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.select_word"
    override fun getLabel() = "Select Word"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.TextFields)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_W, ctrl = true)
    override fun action(editorActionContext: EditorActionContext) {
        editorActionContext.editor.selectCurrentWord()
    }
}

// ── Undo ──────────────────────────────────────────────────────────────────────
class UndoCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.undo"
    override fun getLabel() = "Undo"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Undo)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_Z, ctrl = true)
    override fun isEnabled(vm: IDEViewModel) = vm.activeTab.value != null
    override fun action(editorActionContext: EditorActionContext) {
        val e = editorActionContext.editor
        if (e.canUndo()) e.undo()
    }
}

// ── Redo ──────────────────────────────────────────────────────────────────────
class RedoCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.redo"
    override fun getLabel() = "Redo"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Redo)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_Y, ctrl = true)
    override fun isEnabled(vm: IDEViewModel) = vm.activeTab.value != null
    override fun action(editorActionContext: EditorActionContext) {
        val e = editorActionContext.editor
        if (e.canRedo()) e.redo()
    }
}

// ── Save ──────────────────────────────────────────────────────────────────────
class SaveCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.save"
    override fun getLabel() = "Save File"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Save)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_S, ctrl = true)
    override fun action(editorActionContext: EditorActionContext) {
        ctx.vm.saveCurrentFile()
    }
}

// ── Save All ──────────────────────────────────────────────────────────────────
class SaveAllCommand(ctx: CommandContext) : GlobalCommand(ctx) {
    override val id = "global.save_all"
    override fun getLabel() = "Save All Files"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.SaveAlt)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_S, ctrl = true, shift = true)
    override fun action(actionContext: ActionContext) {
        ctx.vm.saveAllFiles()
    }
}

// ── Toggle Word Wrap ──────────────────────────────────────────────────────────
class ToggleWordWrapCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.toggle_word_wrap"
    override fun getLabel() = "Toggle Word Wrap"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.WrapText)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_Z, alt = true)
    override fun action(editorActionContext: EditorActionContext) {
        val e = editorActionContext.editor
        e.isWordwrap = !e.isWordwrap
    }
}

// ── Duplicate Line ────────────────────────────────────────────────────────────
class DuplicateLineCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.duplicate_line"
    override fun getLabel() = "Duplicate Line"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.ContentCopy)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_D, ctrl = true)
    override fun action(editorActionContext: EditorActionContext) {
        editorActionContext.editor.duplicateLine()
    }
}

// ── Upper Case ────────────────────────────────────────────────────────────────
class UpperCaseCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.uppercase"
    override fun getLabel() = "Transform to UPPERCASE"
    override fun getIcon() = AppIconType.TextIcon("AA")
    override fun action(editorActionContext: EditorActionContext) {
        val e = editorActionContext.editor
        if (!e.isTextSelected) return
        val s = e.cursorRange.startIndex; val en = e.cursorRange.endIndex
        e.text.replace(s, en, e.text.substring(s, en).uppercase())
    }
}

// ── Lower Case ────────────────────────────────────────────────────────────────
class LowerCaseCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.lowercase"
    override fun getLabel() = "Transform to lowercase"
    override fun getIcon() = AppIconType.TextIcon("aa")
    override fun action(editorActionContext: EditorActionContext) {
        val e = editorActionContext.editor
        if (!e.isTextSelected) return
        val s = e.cursorRange.startIndex; val en = e.cursorRange.endIndex
        e.text.replace(s, en, e.text.substring(s, en).lowercase())
    }
}

// ── Format Document ───────────────────────────────────────────────────────────
class FormatDocumentCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.format"
    override fun getLabel() = "Format Document"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.AutoFixHigh)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_F, ctrl = true, shift = true)
    override fun action(editorActionContext: EditorActionContext) {
        ctx.vm.formatCurrentFile()
    }
}

// ── Share File ────────────────────────────────────────────────────────────────
class ShareFileCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.share"
    override fun getLabel() = "Share File"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.Share)
    override fun action(editorActionContext: EditorActionContext) {
        val tab  = ctx.vm.activeTab.value ?: return
        val file = tab.file
        val uri  = FileProvider.getUriForFile(
            editorActionContext.activity,
            "${editorActionContext.activity.packageName}.fileprovider",
            file,
        )
        editorActionContext.activity.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = editorActionContext.activity.contentResolver.getType(uri) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                },
                "Share ${file.name}"
            )
        )
    }
}

// ── Jump to Line ──────────────────────────────────────────────────────────────
class JumpToLineCommand(ctx: CommandContext) : EditorCommand(ctx) {
    override val id = "editor.jump_to_line"
    override fun getLabel() = "Jump to Line"
    override fun getIcon() = AppIconType.VectorIcon(Icons.Default.ArrowForward)
    override val defaultKeybinds = KeyCombination(KeyEvent.KEYCODE_G, ctrl = true)
    override fun action(editorActionContext: EditorActionContext) {
        // Trigger the jump-to-line dialog via ViewModel
        ctx.vm.showJumpToLine()
    }
}
