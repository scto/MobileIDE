package com.scto.mobile.ide.features.terminal.ui

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
import com.scto.mobile.ide.core.terminal.resources.R
import timber.log.Timber

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
            Timber.tag("TerminalService").i("startService requested")
            val intent = Intent(context, TerminalService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    Timber.tag("TerminalService").e(e, "Failed to start foreground service")
                    try {
                        context.startService(intent)
                    } catch (e2: Exception) {
                        Timber.tag("TerminalService").e(e2, "Failed to start service fallback")
                    }
                }
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            Timber.tag("TerminalService").i("stopService requested")
            val intent = Intent(context, TerminalService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun toggleWakeLock(context: Context) {
            Timber.tag("TerminalService").i("toggleWakeLock requested")
            val intent = Intent(context, TerminalService::class.java).apply { action = ACTION_TOGGLE_WAKE_LOCK }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag("TerminalService").i("TerminalService onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Timber.tag("TerminalService").i("onStartCommand action: $action")

        when (action) {
            ACTION_START -> {
                showNotification()
            }
            ACTION_STOP -> {
                Timber.tag("TerminalService").i("Action STOP received, stopping foreground service.")
                releaseWakeLock()
                stopForeground(true)
                stopSelf()
            }
            ACTION_TOGGLE_WAKE_LOCK -> {
                if (isWakeLockAcquired) {
                    releaseWakeLock()
                } else {
                    acquireWakeLock()
                }
                showNotification()
            }
            else -> {
                showNotification()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("TerminalService").i("TerminalService onDestroy")
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileIDE::TerminalServiceWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
            isWakeLockAcquired = true
            Timber.tag("TerminalService").i("acquireWakeLock: CPU wake lock acquired.")
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        isWakeLockAcquired = false
        Timber.tag("TerminalService").i("releaseWakeLock: CPU wake lock released.")
    }

    private fun showNotification() {
        val mainIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
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
                .setSmallIcon(R.drawable.ic_code)
                .setContentIntent(mainPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", exitPendingIntent)
                .addAction(android.R.drawable.ic_lock_lock, wakeLockActionText, wakeLockPendingIntent)
                .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Timber.tag("TerminalService").e(e, "Failed to startForeground in showNotification")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Terminal Service",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Hält den Terminal-Dienst im Hintergrund aktiv"
                }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
