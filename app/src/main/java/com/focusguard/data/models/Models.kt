package com.focusguard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// Una sesión de foco
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,           // "code", "read", "design", "study", "other"
    val durationMinutes: Int,
    val startTime: Long,            // epoch ms
    val endTime: Long? = null,
    val completed: Boolean = false, // false = abandonada
    val abandonedAt: Long? = null
)

// App permitida durante sesión
@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isAllowed: Boolean = true,
    val iconBase64: String? = null
)

// Contacto VIP (puede enviar notificaciones durante sesión)
@Entity(tableName = "vip_contacts")
data class VipContact(
    @PrimaryKey val contactId: String,
    val name: String,
    val phone: String
)

// Stats diarias agregadas (para gráficos)
@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String,   // "2024-01-15"
    val totalMinutes: Int = 0,
    val sessionsCompleted: Int = 0,
    val sessionsAbandoned: Int = 0,
    val currentStreak: Int = 0
)
