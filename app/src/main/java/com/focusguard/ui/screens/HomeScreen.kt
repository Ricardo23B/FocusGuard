package com.focusguard.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.service.FocusSessionService
import com.focusguard.service.SessionManager
import com.focusguard.ui.theme.FG
import com.focusguard.ui.theme.categoryColor
import com.focusguard.utils.PermissionsHelper
import kotlinx.coroutines.launch

val CATEGORIES = listOf(
    "código"  to "‹/›",
    "lectura" to "❖",
    "diseño"  to "◑",
    "estudio" to "✦",
    "otro"    to "⬡"
)

val DURATIONS = listOf(25, 45, 60, 90)

@Composable
fun HomeScreen(
    onOpenApps: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenContacts: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionState by SessionManager.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1726), FG.Bg)
                )
            )
    ) {
        when (val state = sessionState) {
            is SessionManager.State.Active   -> ActiveSessionView(state)
            is SessionManager.State.Finished -> FinishedView(state)
            is SessionManager.State.Idle     -> IdleView(
                onStart = { cat, dur -> FocusSessionService.start(context, cat, dur) },
                onOpenApps = onOpenApps,
                onOpenStats = onOpenStats,
                onOpenContacts = onOpenContacts
            )
        }
    }
}

@Composable
private fun IdleView(
    onStart: (String, Int) -> Unit,
    onOpenApps: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenContacts: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(CATEGORIES[0]) }
    var selectedDuration by remember { mutableIntStateOf(25) }
    val accent = categoryColor(selectedCategory.first)

    // Recalcular estado de permisos cada recomposición de entrada
    var accessOk by remember { mutableStateOf(PermissionsHelper.isAccessibilityEnabled(context)) }
    var notifOk by remember { mutableStateOf(PermissionsHelper.isNotificationListenerEnabled(context)) }
    LaunchedEffect(Unit) {
        accessOk = PermissionsHelper.isAccessibilityEnabled(context)
        notifOk = PermissionsHelper.isNotificationListenerEnabled(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ── Header con accesos a otras pantallas ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "FocusGuard",
                    color = FG.TextHi,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "¿En qué vas a trabajar?",
                    color = FG.TextMid,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconPill("◴", onOpenStats)
                IconPill("☎", onOpenContacts)
                IconPill("☰", onOpenApps)
            }
        }

        // ── Banner de permisos (solo si falta alguno) ──
        if (!accessOk || !notifOk) {
            PermissionBanner(
                accessOk = accessOk,
                notifOk = notifOk,
                onFixAccess = { context.startActivity(PermissionsHelper.accessibilitySettingsIntent()) },
                onFixNotif = { context.startActivity(PermissionsHelper.notificationListenerSettingsIntent()) }
            )
        }

        // ── Selector de categoría ──
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("CATEGORÍA")
            CATEGORIES.chunked(3).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { cat ->
                        val sel = cat == selectedCategory
                        val c = categoryColor(cat.first)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) c.copy(alpha = 0.16f) else FG.Surface)
                                .border(
                                    1.5.dp,
                                    if (sel) c else FG.Border,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(cat.second, fontSize = 22.sp, color = if (sel) c else FG.TextMid)
                                Text(
                                    cat.first,
                                    color = if (sel) c else FG.TextLow,
                                    fontSize = 12.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                    // Rellenar huecos para mantener grilla pareja
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ── Selector de duración ──
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("DURACIÓN")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DURATIONS.forEach { dur ->
                    val sel = dur == selectedDuration
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) accent.copy(alpha = 0.16f) else FG.Surface)
                            .border(1.5.dp, if (sel) accent else FG.Border, RoundedCornerShape(14.dp))
                            .clickable { selectedDuration = dur }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${dur}",
                            color = if (sel) accent else FG.TextMid,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            Text("minutos", color = FG.TextLow, fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))

        // ── Botón iniciar ──
        Button(
            onClick = { onStart(selectedCategory.first, selectedDuration) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Empezar a enfocar",
                color = Color(0xFF14121F),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
private fun IconPill(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(FG.Surface)
            .border(1.dp, FG.Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = FG.TextMid, fontSize = 18.sp)
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = FG.TextLow, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PermissionBanner(
    accessOk: Boolean,
    notifOk: Boolean,
    onFixAccess: () -> Unit,
    onFixNotif: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FG.Amber.copy(alpha = 0.10f))
            .border(1.dp, FG.Amber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Falta activar permisos",
            color = FG.Amber,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            "Sin esto, el bloqueo y el filtro de notificaciones no funcionan.",
            color = FG.TextMid,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        if (!accessOk) {
            PermFixRow("Bloqueo de apps", onFixAccess)
        }
        if (!notifOk) {
            PermFixRow("Filtro de notificaciones", onFixNotif)
        }
    }
}

@Composable
private fun PermFixRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = FG.TextHi, fontSize = 14.sp)
        Text("Activar →", color = FG.Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────
//  SESIÓN ACTIVA — anillo custom dibujado con Canvas
// ─────────────────────────────────────────────────────────────
@Composable
fun ActiveSessionView(state: SessionManager.State.Active) {
    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val progress = 1f - (state.remainingSeconds.toFloat() / (state.durationMinutes * 60))
    val accent = categoryColor(state.category)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.size(280.dp)) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    // pista
                    drawArc(
                        color = FG.Border,
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // progreso
                    drawArc(
                        brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.5f), accent)),
                        startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(minutes, seconds),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = FG.TextHi,
                        letterSpacing = 1.sp
                    )
                    Text(
                        state.category.uppercase(),
                        color = accent,
                        fontSize = 13.sp,
                        letterSpacing = 5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
            Text(
                "Dejá el teléfono. Yo me encargo del resto.",
                color = FG.TextLow,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FinishedView(state: SessionManager.State.Finished) {
    val accent = if (state.completed) categoryColor(state.category) else FG.Danger
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (state.completed) "✓" else "✕", fontSize = 44.sp, color = accent)
            }
            Text(
                if (state.completed) "Sesión completada" else "Sesión abandonada",
                color = FG.TextHi, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
            if (state.completed) {
                Text(
                    "${state.durationMinutes} minutos de ${state.category}",
                    color = FG.TextMid, fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { SessionManager.reset() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FG.Surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Nueva sesión", color = FG.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
