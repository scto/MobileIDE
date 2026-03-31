package com.mobileide.feature.termux.shared

interface TerminalSession {
    fun write(data: String)
    fun write(data: ByteArray)
    fun resize(columns: Int, rows: Int)
    fun finish()

    interface SessionCallback {
        fun onTextChanged(session: TerminalSession)
        fun onSessionFinished(session: TerminalSession)
        fun onTitleChanged(session: TerminalSession)
    }
}
