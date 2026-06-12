package com.focusguard.ui.screens

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.service.SessionManager
import com.focusguard.ui.theme.FG
import com.focusguard.ui.theme.FocusGuardTheme
import com.focusguard.ui.theme.categoryColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        setContent {
            FocusGuardTheme {
                LockScreen(onAbandoned = { finish() })
            }
        }
    }

    @Deprecated("Bloqueado a propósito")
    override fun onBackPressed() { /* no-op */ }
}

@Composable
fun LockScreen(onAbandoned: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val sessionState by SessionManager.state.collectAsState()
    val active = sessionState as? SessionManager.State.Active
    val accent = active?.let { categoryColor(it.category) } ?: FG.Ember

    // Si la sesión termina sola mientras estamos acá, cerrar
    LaunchedEffect(sessionState) {
        if (sessionState is SessionManager.State.Finished) onAbandoned()
    }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val steps = 60
            repeat(steps) {
                if (!isHolding) return@repeat
                holdProgress = (it + 1) / steps.toFloat()
                delay(50)
            }
            if (isHolding) showConfirm = true
        } else {
            holdProgress = 0f
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1726), FG.Bg))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (active != null) {
                val m = active.remainingSeconds / 60
                val s = active.remainingSeconds % 60
                Text(
                    "%02d:%02d".format(m, s),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    color = FG.TextHi
                )
                Text(
                    active.category.uppercase(),
                    fontSize = 13.sp,
                    color = accent,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .clip(CircleShape)
                    .background(FG.Surface)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    "Esta app está fuera de tu sesión",
                    color = FG.TextMid,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(36.dp))

            // Botón hold para abandonar
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(84.dp)) {
                    val w = 4.dp.toPx()
                    drawArc(
                        color = FG.Border,
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        style = Stroke(width = w, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = FG.Danger,
                        startAngle = -90f, sweepAngle = 360f * holdProgress, useCenter = false,
                        style = Stroke(width = w, cap = StrokeCap.Round)
                    )
                }
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FG.Surface)
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = FG.TextLow, fontSize = 20.sp)
                }
            }
            Text("Mantené para salir de la sesión", color = FG.TextLow, fontSize = 12.sp)
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false; holdProgress = 0f },
            containerColor = FG.Surface,
            titleContentColor = FG.TextHi,
            textContentColor = FG.TextMid,
            title = { Text("¿Abandonar la sesión?") },
            text = { Text("Se va a registrar como abandonada y romper tu racha del día.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        SessionManager.abandonSession(context.applicationContext)
                    }
                    showConfirm = false
                    onAbandoned()
                }) { Text("Abandonar", color = FG.Danger, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false; holdProgress = 0f }) {
                    Text("Seguir enfocado", color = FG.Success)
                }
            }
        )
    }
}
