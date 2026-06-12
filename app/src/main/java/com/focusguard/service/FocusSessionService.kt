package com.focusguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusguard.MainActivity
import kotlinx.coroutines.*

// Foreground Service que hostea la sesión activa.
// Sin esto, Android mata el proceso cuando la app está en background
// y la sesión muere en silencio (bug #1 de la v1).
class FocusSessionService : Service() {

    companion object {
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_DURATION = "duration"
        const val CHANNEL_ID = "focus_session"
        const val NOTIF_ID = 1001

        fun start(context: Context, category: String, durationMinutes: Int) {
            val intent = Intent(context, FocusSessionService::class.java).apply {
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_DURATION, durationMinutes)
            }
            context.startForegroundService(intent)
        }

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Sesión de foco",
                        NotificationManager.IMPORTANCE_LOW   // sin sonido, persistente
                    ).apply {
                        description = "Muestra el tiempo restante de la sesión activa"
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastShownMinute = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val category = intent?.getStringExtra(EXTRA_CATEGORY) ?: "otro"
        val duration = intent?.getIntExtra(EXTRA_DURATION, 25) ?: 25

        ensureChannel(this)
        startForeground(NOTIF_ID, buildNotification(category, duration * 60))

        scope.launch {
            SessionManager.startSession(applicationContext, category, duration)
            SessionManager.state.collect { state ->
                when (state) {
                    is SessionManager.State.Active -> {
                        // Actualizar notificación solo cuando cambia el minuto
                        val minute = state.remainingSeconds / 60
                        if (minute != lastShownMinute) {
                            lastShownMinute = minute
                            getSystemService(NotificationManager::class.java)
                                .notify(NOTIF_ID, buildNotification(state.category, state.remainingSeconds))
                        }
                    }
                    is SessionManager.State.Finished -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    else -> { /* Idle: nada */ }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(category: String, remainingSeconds: Int): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60

        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Sesión de $category en curso")
            .setContentText("%02d:%02d restantes".format(minutes, seconds))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tapIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
