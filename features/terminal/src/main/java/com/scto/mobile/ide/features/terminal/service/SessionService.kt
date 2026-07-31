package com.scto.mobile.ide.features.terminal.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.scto.mobile.ide.core.terminal.resources.drawables
import com.scto.mobile.ide.core.terminal.resources.strings
import com.scto.mobile.ide.core.terminal.settings.Settings
import com.scto.mobile.ide.features.terminal.ui.terminal.MkSession
import com.scto.mobile.ide.core.terminal.model.WorkingMode
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

class SessionService : Service() {
    private val sessions = linkedMapOf<String, TerminalSession>()
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockHeld = false
    // Ordered list of session IDs for UI display
    val sessionOrder = mutableStateListOf<String>()
    // Map for storing workingMode per session
    val sessionList = mutableMapOf<String, Int>()
    // Observable map for terminal titles - triggers UI recomposition
    val sessionTitles = androidx.compose.runtime.mutableStateMapOf<String, String>()
    // Observable map for custom names - triggers UI recomposition
    val sessionCustomNames = androidx.compose.runtime.mutableStateMapOf<String, String>()
    var currentSession = mutableStateOf(Pair("main",com.scto.mobile.ide.core.terminal.settings.Settings.working_Mode))

    /**
     * Resolve display title with priority chain:
     * 1. User custom name (persisted)
     * 2. Shell title (from ANSI escape codes)
     * 3. Default fallback ("android", "alpine", etc.)
     */
    fun getDisplayTitle(sessionId: String): String {
        return sessionCustomNames[sessionId]
            ?: sessionTitles[sessionId]?.takeIf { it.isNotBlank() }
            ?: getDefaultSessionName(sessionId)
    }

    private fun getDefaultSessionName(sessionId: String): String {
        val modeName = when (sessionList[sessionId]) {
            WorkingMode.ALPINE -> "alpine"
            WorkingMode.ANDROID -> "android"
            WorkingMode.ALPINE_ROOT -> "alpine (root)"
            WorkingMode.UBUNTU -> "ubuntu"
            WorkingMode.UBUNTU_ROOT -> "ubuntu (root)"
            else -> sessionId
        }
        return modeName
    }

    fun getWorkingMode(sessionId: String): Int? {
        return sessionList[sessionId]
    }

    fun updateTerminalTitle(sessionId: String, title: String) {
        sessionTitles[sessionId] = title
    }

    fun setCustomName(sessionId: String, name: String) {
        if (name.isBlank()) {
            sessionCustomNames.remove(sessionId)
            com.scto.mobile.ide.core.terminal.settings.Settings.removeCustomSessionName(sessionId)
        } else {
            sessionCustomNames[sessionId] = name
            com.scto.mobile.ide.core.terminal.settings.Settings.setCustomSessionName(sessionId, name)
        }
    }
    private fun cleanupSessionTemp(sessionId: String) {
        runCatching {
            val tmpDir = java.io.File(cacheDir, "tmp").resolve(sessionId)
            if (tmpDir.exists()) {
                tmpDir.deleteRecursively()
            }
        }.onFailure { it.printStackTrace() }
    }

    inner class SessionBinder : Binder() {
        fun getService():SessionService{
            return this@SessionService
        }
        fun terminateAllSessions(){
            sessions.values.forEach{
                it.finishIfRunning()
            }
            sessions.keys.toList().forEach { cleanupSessionTemp(it) }
            sessions.clear()
            sessionOrder.clear()
            sessionList.clear()
            sessionTitles.clear()
            sessionCustomNames.clear()
            updateNotification()
        }
        fun createSession(id: String, client: TerminalSessionClient, activity: Activity, workingMode: Int): TerminalSession {
            return MkSession.createSession(activity, client, id, workingMode = workingMode).also {
                sessions[id] = it
                sessionOrder.add(id)
                sessionList[id] = workingMode
                sessionTitles[id] = ""
                // Restore persisted custom name if exists
                com.scto.mobile.ide.core.terminal.settings.Settings.getCustomSessionName(id)?.let { name ->
                    sessionCustomNames[id] = name
                }
                updateNotification()
            }
        }
        fun getSession(id: String): TerminalSession? {
            return sessions[id]
        }
        fun terminateSession(id: String) {
            runCatching {
                //crash is here
                sessions[id]?.apply {
                    if (emulator != null){
                        sessions[id]?.finishIfRunning()
                    }
                }

                sessions.remove(id)
                sessionOrder.remove(id)
                sessionList.remove(id)
                sessionTitles.remove(id)
                sessionCustomNames.remove(id)
                com.scto.mobile.ide.core.terminal.settings.Settings.removeCustomSessionName(id)
                cleanupSessionTemp(id)
                if (sessions.isEmpty()) {
                    stopSelf()
                } else {
                    updateNotification()
                }
            }.onFailure { it.printStackTrace() }

        }

        fun getSessionId(session: TerminalSession): String? {
            return sessions.entries.firstOrNull { it.value === session }?.key
        }
    }

    private val binder = SessionBinder()
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        releaseWakeLock()
        sessions.keys.toList().forEach { cleanupSessionTemp(it) }
        sessions.forEach { s -> s.value.finishIfRunning() }
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_EXIT" -> {
                releaseWakeLock()
                sessions.forEach { s -> s.value.finishIfRunning() }
                stopSelf()
            }
            ACTION_WAKE_LOCK -> {
                if (wakeLockHeld) releaseWakeLock() else acquireWakeLock()
                updateNotification()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification {
        val mainActivityClass = Class.forName("com.scto.mobile.ide.core.terminal.ui.activities.terminal.MainActivity")
        val intent = Intent(this, mainActivityClass)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val exitIntent = Intent(this, SessionService::class.java).apply {
            action = "ACTION_EXIT"
        }
        val exitPendingIntent = PendingIntent.getService(
            this, 1, exitIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wakeLockIntent = Intent(this, SessionService::class.java).apply {
            action = ACTION_WAKE_LOCK
        }
        val wakeLockPendingIntent = PendingIntent.getService(
            this, 2, wakeLockIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val wakeLockLabel = if (wakeLockHeld) "🔓 Release Wake Lock" else "🔒 Acquire Wake Lock"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobileIDE Terminal")
            .setContentText(getNotificationContentText())
            .setSmallIcon(drawables.terminal)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    wakeLockLabel,
                    wakeLockPendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "EXIT",
                    exitPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "session_service_channel"
    private val ACTION_WAKE_LOCK = "ACTION_WAKE_LOCK"
    private val WAKE_LOCK_TAG = "MobileIDE::TerminalWakeLock"

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Session Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for Terminal Service"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        val notification = createNotification()
        notificationManager.notify(1, notification)
    }

    private fun getNotificationContentText(): String {
        val count = sessions.size
        val wakeLockStatus = if (wakeLockHeld) " \u2022 Wake Lock" else ""
        if (count == 1){
            return "1 session running$wakeLockStatus"
        }
        return "$count sessions running$wakeLockStatus"
    }

    private fun acquireWakeLock() {
        if (wakeLockHeld) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            acquire()
        }
        wakeLockHeld = true
    }

    private fun releaseWakeLock() {
        if (!wakeLockHeld) return
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        wakeLockHeld = false
    }
}
