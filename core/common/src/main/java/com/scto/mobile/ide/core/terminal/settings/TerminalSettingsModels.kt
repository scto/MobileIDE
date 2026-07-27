package com.scto.mobile.ide.core.terminal.settings

object InputMode {
    const val DEFAULT = 0
    const val TYPE_NULL = 1
    const val VISIBLE_PASSWORD = 2
}

object LayoutMode {
    const val CLASSIC = 0   // Original Material drawer + TopAppBar
    const val TAB_BAR = 1   // Horizontal tab bar mode
}

object CloseLastSessionBehavior {
    const val EXIT_APP = 0      // Exit the app when last session is closed
    const val NEW_SESSION = 1   // Create a new session instead of exiting
}

object ShellType {
    const val BASH = 0
    const val ASH = 1
    const val ZSH = 2
}
