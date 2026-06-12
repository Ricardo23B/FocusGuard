package com.focusguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focusguard.ui.screens.LockActivity

// Este servicio corre en background y detecta qué app está en foreground.
// Si hay sesión activa y la app no está en la whitelist → lanza LockActivity.
class FocusAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!SessionManager.isSessionActive()) return

        val pkg = event.packageName?.toString() ?: return

        // No bloquear el propio lock screen (loop infinito)
        if (pkg == packageName) return

        if (pkg !in SessionManager.allowedPackages) {
            // Solo lanzar si cambiamos de app bloqueada (evitar spam de intents)
            if (pkg != lastBlockedPackage) {
                lastBlockedPackage = pkg
                val intent = Intent(this, LockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("BLOCKED_PACKAGE", pkg)
                }
                startActivity(intent)
            }
        } else {
            lastBlockedPackage = null
        }
    }

    override fun onInterrupt() {}
}
