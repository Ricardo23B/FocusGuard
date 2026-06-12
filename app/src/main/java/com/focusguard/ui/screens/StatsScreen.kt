package com.focusguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.data.db.AppDatabase
import com.focusguard.data.models.DailyStats
import com.focusguard.data.models.Session
import com.focusguard.ui.theme.FG
import com.focusguard.ui.theme.categoryColor
import java.time.LocalDate

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    val daily by db.dailyStatsDao().getLast30Days().collectAsState(initial = emptyList())
    val sessions by db.sessionDao().getAllSessions().collectAsState(initial = emptyList())

    val totalMinutes = daily.sumOf { it.totalMinutes }
    val totalCompleted = daily.sumOf { it.sessionsCompleted }
    val totalAbandoned = daily.sumOf { it.sessionsAbandoned }
    val streak = computeStreak(daily)

    val byCategory = sessions
        .filter { it.completed }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
        .toList()
        .sortedByDescending { it.second }

    Column(
        Modifier.fillMaxSize().background(FG.Bg).padding(top = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Tu progreso", "Últimos 30 días", onBack)

        // Racha destacada
        Box(
            Modifier.fillMaxWidth().padding(20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(FG.Ember.copy(alpha = 0.12f))
                .padding(24.dp)
        ) {
            Column {
                Text("RACHA ACTUAL", color = FG.Ember, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$streak", color = FG.TextHi, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (streak == 1) " día" else " días",
                        color = FG.TextMid, fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }

        // Tres números clave
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "${totalMinutes / 60}h ${totalMinutes % 60}m", "enfocado")
            StatCard(Modifier.weight(1f), "$totalCompleted", "completadas")
            StatCard(Modifier.weight(1f), "$totalAbandoned", "abandonadas")
        }

        Spacer(Modifier.height(24.dp))

        // Desglose por categoría
        if (byCategory.isNotEmpty()) {
            Text(
                "POR CATEGORÍA",
                color = FG.TextLow, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))
            val maxMin = byCategory.maxOf { it.second }.coerceAtLeast(1)
            byCategory.forEach { (cat, min) ->
                CategoryBar(cat, min, maxMin)
            }
        } else {
            Text(
                "Completá tu primera sesión para ver estadísticas.",
                color = FG.TextLow, fontSize = 14.sp,
                modifier = Modifier.padding(20.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(FG.Surface)
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = FG.TextHi, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = FG.TextMid, fontSize = 12.sp)
    }
}

@Composable
private fun CategoryBar(category: String, minutes: Int, maxMinutes: Int) {
    val c = categoryColor(category)
    val frac = (minutes.toFloat() / maxMinutes).coerceIn(0.05f, 1f)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category, color = FG.TextHi, fontSize = 14.sp)
            Text("${minutes / 60}h ${minutes % 60}m", color = FG.TextMid, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(FG.SurfaceHigh)
        ) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(c))
        }
    }
}

// Racha = días consecutivos hasta hoy con al menos una sesión completada
private fun computeStreak(daily: List<DailyStats>): Int {
    val byDate = daily.filter { it.sessionsCompleted > 0 }.map { it.date }.toSet()
    var streak = 0
    var day = LocalDate.now()
    while (byDate.contains(day.toString())) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}
