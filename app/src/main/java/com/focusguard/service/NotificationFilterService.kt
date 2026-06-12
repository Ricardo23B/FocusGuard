package com.focusguard.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationFilterService : NotificationListenerService() {

    // Paquetes de llamadas / sistema que NUNCA se filtran
    private val systemAllowed = setOf(
        "com.android.phone",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.samsung.android.incallui",
        "com.android.server.telecom"
    )

    private val whatsappPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!SessionManager.isSessionActive()) return

        val pkg = sbn.packageName

        // Llamadas y sistema siempre pasan
        if (pkg in systemAllowed) return

        // WhatsApp: pasa solo si el remitente es VIP
        if (pkg in whatsappPackages) {
            if (!isFromVipContact(sbn)) cancelNotification(sbn.key)
            return
        }

        // Resto: se silencia durante la sesión
        cancelNotification(sbn.key)
    }

    // WhatsApp pone el NOMBRE del contacto (como está agendado) en android.title.
    // Por eso comparamos contra los nombres de los VIPs, no contra el número.
    // SessionManager carga vipNames al iniciar la sesión.
    private fun isFromVipContact(sbn: StatusBarNotification): Boolean {
        val title = sbn.notification?.extras?.getString("android.title")?.trim() ?: return false

        // Ignorar notificaciones de grupo/resumen ("3 mensajes nuevos", etc.)
        if (title.matches(Regex(".*\\d+ (mensaje|message|chat).*", RegexOption.IGNORE_CASE))) {
            return false
        }

        return SessionManager.vipNames.any { vipName ->
            title.equals(vipName, ignoreCase = true) ||
            title.contains(vipName, ignoreCase = true)
        }
    }
}
