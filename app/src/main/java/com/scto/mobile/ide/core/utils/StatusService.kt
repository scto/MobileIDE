package com.scto.mobile.ide.core.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.scto.mobile.ide.MainActivity
import com.scto.mobile.ide.R

class StatusService : Service() {
    companion object {
        const val CHANNEL_ID = "status_service_channel"
        const val NOTIFICATION_ID = 9999
        const val ACTION_EXIT = "com.scto.mobile.ide.ACTION_EXIT"
        const val ACTION_TOGGLE_WAKELOCK = "com.scto.mobile.ide.ACTION_TOGGLE_WAKELOCK"
        
        var isRunning = false
            private set
            
        fun start(context: Context) {
            if (!isRunning) {
                val intent = Intent(context, StatusService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var isWakeLockAcquired = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_EXIT) {
            releaseWakeLock()
            stopSelf()
            // Terminate the application process completely
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        } else if (action == ACTION_TOGGLE_WAKELOCK) {
            toggleWakeLock()
        }

        updateNotification()
        return START_STICKY
    }

    private fun toggleWakeLock() {
        if (isWakeLockAcquired) {
            releaseWakeLock()
        } else {
            acquireWakeLock()
        }
    }

    private fun acquireWakeLock() {
        if (!isWakeLockAcquired) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileIDE::StatusServiceWakeLock").apply {
                acquire()
            }
            isWakeLockAcquired = true
            LogCatcher.i("StatusService", "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        if (isWakeLockAcquired) {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
            isWakeLockAcquired = false
            LogCatcher.i("StatusService", "WakeLock released")
        }
    }

    private fun updateNotification() {
        val exitIntent = Intent(this, StatusService::class.java).apply {
            this.action = ACTION_EXIT
        }
        val exitPendingIntent = PendingIntent.getService(
            this,
            0,
            exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeLockIntent = Intent(this, StatusService::class.java).apply {
            this.action = ACTION_TOGGLE_WAKELOCK
        }
        val wakeLockPendingIntent = PendingIntent.getService(
            this,
            1,
            wakeLockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this,
            2,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeLockBtnText = if (isWakeLockAcquired) "Release WakeLock" else "Acquire WakeLock"
        val wakeLockStatusText = if (isWakeLockAcquired) "WakeLock: Active" else "WakeLock: Inactive"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobileIDE")
            .setContentText("App is running | $wakeLockStatusText")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(mainActivityPendingIntent)
            .setOngoing(true)
            .addAction(0, "Exit", exitPendingIntent)
            .addAction(0, wakeLockBtnText, wakeLockPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
