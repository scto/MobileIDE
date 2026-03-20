package com.mobileide.app.editor.intelligent

import androidx.compose.runtime.mutableStateListOf
import com.mobileide.app.editor.Editor
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.KeyBindingEvent

/**
 * Central registry for all intelligent editor features.
 *
 * Built-in features ([AutoCloseTag], [BulletContinuation]) are always present.
 * External features (plugins / future extensions) can be registered at runtime
 * via [registerFeature].
 */
object IntelligentFeatureRegistry {

    /** Features that ship with MobileIDE. */
    val builtInFeatures: List<IntelligentFeature> = listOf(AutoCloseTag, BulletContinuation)

    private val _extensionFeatures = mutableStateListOf<IntelligentFeature>()

    /** Runtime-registered features (observable for UI). */
    val extensionFeatures: List<IntelligentFeature>
        get() = _extensionFeatures.toList()

    /** All features: built-in + extensions. */
    val allFeatures: List<IntelligentFeature>
        get() = builtInFeatures + _extensionFeatures

    /** Add an external feature. No-op if already registered. */
    fun registerFeature(feature: IntelligentFeature) {
        if (!_extensionFeatures.contains(feature)) _extensionFeatures.add(feature)
    }

    /** Remove a previously registered external feature. */
    fun unregisterFeature(feature: IntelligentFeature) {
        _extensionFeatures.remove(feature)
    }

    /**
     * Dispatch a character-insertion event to all enabled features whose
     * [IntelligentFeature.supportedExtensions] contains [fileExtension].
     */
    fun dispatchInsertChar(char: Char, fileExtension: String, editor: Editor) {
        allFeatures
            .filter { it.isEnabled() && it.supportsExtension(fileExtension) }
            .filter { char in it.triggerCharacters }
            .forEach { it.handleInsertChar(char, editor) }
    }

    /** Dispatch an Enter / Delete event to all matching features. */
    fun dispatchInsert(fileExtension: String, editor: Editor) {
        allFeatures
            .filter { it.isEnabled() && it.supportsExtension(fileExtension) }
            .forEach { it.handleInsert(editor) }
    }

    fun dispatchDelete(fileExtension: String, editor: Editor) {
        allFeatures
            .filter { it.isEnabled() && it.supportsExtension(fileExtension) }
            .forEach { it.handleDelete(editor) }
    }

    fun dispatchKeyEvent(event: EditorKeyEvent, fileExtension: String, editor: Editor) {
        allFeatures
            .filter { it.isEnabled() && it.supportsExtension(fileExtension) }
            .forEach { it.handleKeyEvent(event, editor) }
    }

    fun dispatchKeyBindingEvent(event: KeyBindingEvent, fileExtension: String, editor: Editor) {
        allFeatures
            .filter { it.isEnabled() && it.supportsExtension(fileExtension) }
            .forEach { it.handleKeyBindingEvent(event, editor) }
    }
}

/**
 * Base class for an intelligent editing feature.
 *
 * Implement at minimum [id] and [supportedExtensions].  Override only the
 * handlers relevant to your feature.
 *
 * Example — auto-close brackets for Kotlin:
 * ```kotlin
 * object KotlinAutoClose : IntelligentFeature() {
 *     override val id = "kotlin.auto_close"
 *     override val supportedExtensions = listOf("kt", "kts")
 *     override val triggerCharacters = listOf('(', '{', '[')
 *     override fun handleInsertChar(triggerCharacter: Char, editor: Editor) { … }
 * }
 * ```
 */
abstract class IntelligentFeature {

    /** Stable unique identifier, e.g. `"html.auto_close_tag"`. */
    abstract val id: String

    /**
     * File extensions this feature operates on (without the leading dot),
     * e.g. `listOf("html", "htm", "htmx")`.
     */
    abstract val supportedExtensions: List<String>

    /**
     * Characters whose insertion should trigger [handleInsertChar].
     * Empty by default — override if your feature reacts to specific chars.
     */
    open val triggerCharacters: List<Char> = emptyList()

    /** Called after a [triggerCharacter] is inserted at the cursor. */
    open fun handleInsertChar(triggerCharacter: Char, editor: Editor) {}

    /** Called after a character is deleted at the cursor. */
    open fun handleDeleteChar(triggerCharacter: Char, editor: Editor) {}

    /** Called after any text is inserted (not triggered by a specific char). */
    open fun handleInsert(editor: Editor) {}

    /** Called after any text is deleted. */
    open fun handleDelete(editor: Editor) {}

    /** Called on a raw key event (ACTION_DOWN / UP). */
    open fun handleKeyEvent(event: EditorKeyEvent, editor: Editor) {}

    /** Called on a key-binding event (Ctrl+X, etc.). */
    open fun handleKeyBindingEvent(event: KeyBindingEvent, editor: Editor) {}

    /**
     * Whether this feature is currently active.  Check user preferences here.
     * Defaults to true so features work out-of-the-box.
     */
    open fun isEnabled(): Boolean = true

    /** Internal helper — true when [ext] is in [supportedExtensions]. */
    internal fun supportsExtension(ext: String): Boolean =
        supportedExtensions.any { it.equals(ext, ignoreCase = true) }
}
