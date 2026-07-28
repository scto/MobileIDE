package com.scto.mobile.ide.features.extensions

import com.scto.mobile.ide.features.extensions.events.Event

/** Events related to extension lifecycle and state. */
sealed interface ExtensionEvent : Event {

    /** Event triggered when an extension is installed. */
    data class Installed(val extension: Extension) : ExtensionEvent

    /** Event triggered when an extension has been successfully loaded. */
    data class Loaded(val extension: LocalExtension) : ExtensionEvent

    /** Event triggered when an extension has crashed and been disabled. */
    data class Crashed(val extension: Extension) : ExtensionEvent

    /**
     * Event triggered when an extension is uninstalled.
     *
     * This also includes temporary uninstallation during an update.
     *
     * @param isUpdate Whether the extension was temporarily uninstalled as a result of an update.
     */
    data class Uninstalled(val extension: Extension, val isUpdate: Boolean) : ExtensionEvent
}
