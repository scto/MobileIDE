package com.mobileide.core.logger

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kotlin port of jkas.androidpe.logger.LogMsg  (author: JKas)
 *
 * Fields [src], [level], [message] are identical to the Java original.
 * [resColor] is replaced by [color] (Compose Color) since we have no R.color here.
 */
class LogMsg(
    val src: String,
    val level: String,
    vararg listMsg: String
) {
    // ── Identical to Java original ─────────────────────────────────────────────
    val message: String = buildString { listMsg.forEach { append(it).append('\n') } }

    // replaces:  public int resColor = R.color.info;
    val color: Color = when (level) {
        Logger.Level.ERROR.name   -> COLOR_ERROR
        Logger.Level.INFO.name    -> COLOR_INFO
        Logger.Level.SUCCESS.name -> COLOR_SUCCESS
        Logger.Level.WARNING.name -> COLOR_WARNING
        else                      -> COLOR_INFO
    }

    // ── Extra Kotlin helpers (not in Java original) ────────────────────────────
    val timestamp: Long  = System.currentTimeMillis()
    val thread: String   = Thread.currentThread().name

    val formattedTime: String
        get() = TIME_FMT.format(Date(timestamp))

    val formattedTimeFull: String
        get() = TIME_FMT_FULL.format(Date(timestamp))

    /** One-line logcat representation: time  L/tag   message */
    val logcatLine: String
        get() = "$formattedTime  ${level.take(1)}/${src.padEnd(23).take(23)}  $message".trimEnd()

    override fun toString() = logcatLine

    companion object {
        val COLOR_INFO    = Color(0xFF89DCEB)   // Catppuccin Sky
        val COLOR_SUCCESS = Color(0xFFA6E3A1)   // Catppuccin Green
        val COLOR_WARNING = Color(0xFFFAB387)   // Catppuccin Peach
        val COLOR_ERROR   = Color(0xFFFF9CAC)   // Catppuccin Red

        private val TIME_FMT      = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        private val TIME_FMT_FULL = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }
}
