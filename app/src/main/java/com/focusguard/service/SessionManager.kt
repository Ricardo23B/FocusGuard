package com.focusguard.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.focusguard.data.db.AppDatabase
import com.focusguard.data.models.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

// Estado global de sesión — singleton accedido por servicios y UI.
// El timer vive acá, pero FocusSessionService (foreground) mantiene
// el proceso vivo para que Android no lo mate en background.
object SessionManager {

    sealed class State {
        object Idle : State()
        data class Active(
            val sessionId: Long,
            val category: String,
            val durationMinutes: Int,
            val startTime: Long,
            val remainingSeconds: Int
        ) : State()
        data class Finished(
            val sessionId: Long,
            val category: String,
            val durationMinutes: Int,
            val completed: Boolean
        ) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var allowedPackages: Set<String> = emptySet()
        private set

    var vipPhones: Set<String> = emptySet()
        private set

    var vipNames: Set<String> = emptySet()
        private set

    suspend fun startSession(
        context: Context,
        category: String,
        durationMinutes: Int
    ) {
        if (_state.value is State.Active) return  // ya hay una corriendo

        val db = AppDatabase.get(context)

        // Whitelist en memoria + apps de sistema imprescindibles
        allowedPackages = db.appRuleDao().getAllSync()
            .filter { it.isAllowed }
            .map { it.packageName }
            .toSet()
            .plus(systemEssentials(context))

        val vips = db.vipContactDao().getAllSync()
        vipPhones = vips.map { it.phone }.toSet()
        vipNames = vips.map { it.name }.toSet()

        val session = Session(
            category = category,
            durationMinutes = durationMinutes,
            startTime = System.currentTimeMillis()
        )
        val sessionId = db.sessionDao().insert(session)

        _state.value = State.Active(
            sessionId = sessionId,
            category = category,
            durationMinutes = durationMinutes,
            startTime = session.startTime,
            remainingSeconds = durationMinutes * 60
        )

        startTimer(context, sessionId)
    }

    // Apps que SIEMPRE quedan permitidas: nosotros, el launcher real,
    // systemui (notificaciones, quick settings), teléfono y settings.
    private fun systemEssentials(context: Context): Set<String> {
        val essentials = mutableSetOf(
            context.packageName,
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.incallui",
            "com.google.android.dialer"
        )
        // Launcher por defecto del dispositivo (no hardcodeado)
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager
            .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.let { essentials.add(it) }
        return essentials
    }

    private fun startTimer(context: Context, sessionId: Long) {
        timerJob?.cancel()
        val appContext = context.applicationContext
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current !is State.Active) break

                val newRemaining = current.remainingSeconds - 1
                if (newRemaining <= 0) {
                    completeSession(appContext, sessionId)
                    break
                } else {
                    _state.value = current.copy(remainingSeconds = newRemaining)
                }
            }
        }
    }

    suspend fun abandonSession(context: Context) {
        val current = _state.value as? State.Active ?: return
        timerJob?.cancel()

        val db = AppDatabase.get(context)
        db.sessionDao().getById(current.sessionId)?.let { session ->
            db.sessionDao().update(
                session.copy(
                    endTime = System.currentTimeMillis(),
                    completed = false,
                    abandonedAt = System.currentTimeMillis()
                )
            )
        }
        updateDailyStats(context, durationMinutes = 0, completed = false)
        _state.value = State.Finished(
            current.sessionId, current.category, current.durationMinutes, completed = false
        )
    }

    private suspend fun completeSession(context: Context, sessionId: Long) {
        val current = _state.value as? State.Active ?: return
        val db = AppDatabase.get(context)
        db.sessionDao().getById(sessionId)?.let { session ->
            db.sessionDao().update(
                session.copy(endTime = System.currentTimeMillis(), completed = true)
            )
        }
        updateDailyStats(context, durationMinutes = current.durationMinutes, completed = true)
        _state.value = State.Finished(
            sessionId, current.category, current.durationMinutes, completed = true
        )
    }

    // Vuelve a Idle después de una sesión terminada (fix: antes no existía)
    fun reset() {
        if (_state.value is State.Finished) {
            _state.value = State.Idle
        }
    }

    private suspend fun updateDailyStats(context: Context, durationMinutes: Int, completed: Boolean) {
        val db = AppDatabase.get(context)
        val today = LocalDate.now().toString()
        val existing = db.dailyStatsDao().getByDate(today)
        val updated = (existing ?: com.focusguard.data.models.DailyStats(date = today)).copy(
            totalMinutes = (existing?.totalMinutes ?: 0) + durationMinutes,
            sessionsCompleted = (existing?.sessionsCompleted ?: 0) + if (completed) 1 else 0,
            sessionsAbandoned = (existing?.sessionsAbandoned ?: 0) + if (!completed) 1 else 0
        )
        db.dailyStatsDao().upsert(updated)
    }

    fun isSessionActive() = _state.value is State.Active
}
