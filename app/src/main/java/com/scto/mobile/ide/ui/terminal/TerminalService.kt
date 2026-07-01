package com.scto.mobile.ide.ui.terminal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.scto.mobile.ide.MainActivity
import com.scto.mobile.ide.R
import com.scto.mobile.ide.core.utils.LogCatcher

class TerminalService : Service() {

    companion object {
        const val CHANNEL_ID = "terminal_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_WAKE_LOCK = "ACTION_TOGGLE_WAKE_LOCK"

        private var wakeLock: PowerManager.WakeLock? = null
        var isWakeLockAcquired = false
            private set

        fun startService(context: Context) {
            LogCatcher.i("TerminalService", "startService requested")
            val intent = Intent(context, TerminalService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            LogCatcher.i("TerminalService", "stopService requested")
            val intent = Intent(context, TerminalService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        LogCatcher.i("TerminalService", "onCreate called")
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        LogCatcher.i("TerminalService", "onStartCommand action=$action")
        when (action) {
            ACTION_START -> {
                showNotification()
            }
            ACTION_STOP -> {
                LogCatcher.i(
                    "TerminalService",
                    "ACTION_STOP action received. Releasing wake lock and clearing sessions.",
                )
                releaseWakeLock()
                val list = ArrayList(SessionManager.sessions)
                list.forEach { SessionManager.removeSession(it) }
                stopForeground(true)
                stopSelf()
            }
            ACTION_TOGGLE_WAKE_LOCK -> {
                LogCatcher.i("TerminalService", "ACTION_TOGGLE_WAKE_LOCK action received.")
                if (isWakeLockAcquired) {
                    releaseWakeLock()
                } else {
                    acquireWakeLock()
                }
                showNotification()
            }
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        LogCatcher.i("TerminalService", "acquireWakeLock: acquiring CPU wake lock...")
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileIDE::TerminalWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
            isWakeLockAcquired = true
            LogCatcher.i("TerminalService", "acquireWakeLock: CPU wake lock successfully acquired.")
        }
    }

    private fun releaseWakeLock() {
        LogCatcher.i("TerminalService", "releaseWakeLock: releasing CPU wake lock...")
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        isWakeLockAcquired = false
        LogCatcher.i("TerminalService", "releaseWakeLock: CPU wake lock released.")
    }

    private fun showNotification() {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        // Exit intent
        val exitIntent = Intent(this, TerminalService::class.java).apply { action = ACTION_STOP }
        val exitPendingIntent =
            PendingIntent.getService(
                this,
                1,
                exitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        // WakeLock toggle intent
        val wakeLockIntent = Intent(this, TerminalService::class.java).apply { action = ACTION_TOGGLE_WAKE_LOCK }
        val wakeLockPendingIntent =
            PendingIntent.getService(
                this,
                2,
                wakeLockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val wakeLockActionText =
            if (isWakeLockAcquired) {
                "Release Wake Lock"
            } else {
                "Acquire Wake Lock"
            }

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MobileIDE Terminal")
                .setContentText("Terminal läuft im Hintergrund")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(mainPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Left button: Exit
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", exitPendingIntent)
                // Right button: Acquire Wake Lock
                .addAction(android.R.drawable.ic_lock_lock, wakeLockActionText, wakeLockPendingIntent)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, "Terminal Foreground Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        LogCatcher.i("TerminalService", "onDestroy called")
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
