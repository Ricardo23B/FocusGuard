package com.focusguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.data.db.AppDatabase
import com.focusguard.data.models.AppRule
import com.focusguard.ui.theme.FG
import com.focusguard.utils.InstalledApp
import com.focusguard.utils.InstalledAppsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }

    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    val allowed by db.appRuleDao().getAllowedApps().collectAsState(initial = emptyList())
    val allowedSet = allowed.map { it.packageName }.toSet()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsHelper.getLaunchableApps(context) }
    }

    val filtered = apps.filter { it.label.contains(query, ignoreCase = true) }

    Column(Modifier.fillMaxSize().background(FG.Bg).padding(top = 48.dp)) {
        ScreenHeader("Apps permitidas", "Durante una sesión solo se abren estas", onBack)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar app", color = FG.TextLow) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FG.Ember,
                unfocusedBorderColor = FG.Border,
                focusedTextColor = FG.TextHi,
                unfocusedTextColor = FG.TextHi,
                cursorColor = FG.Ember
            ),
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.packageName }) { app ->
                val isOn = app.packageName in allowedSet
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FG.Surface)
                        .clickable {
                            scope.launch {
                                if (isOn) db.appRuleDao().delete(AppRule(app.packageName, app.label))
                                else db.appRuleDao().insert(AppRule(app.packageName, app.label, true))
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, color = FG.TextHi, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isOn,
                        onCheckedChange = { checked ->
                            scope.launch {
                                if (checked) db.appRuleDao().insert(AppRule(app.packageName, app.label, true))
                                else db.appRuleDao().delete(AppRule(app.packageName, app.label))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FG.Bg,
                            checkedTrackColor = FG.Success,
                            uncheckedThumbColor = FG.TextLow,
                            uncheckedTrackColor = FG.SurfaceHigh,
                            uncheckedBorderColor = FG.Border
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FG.Surface)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) { Text("←", color = FG.TextHi, fontSize = 20.sp) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = FG.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FG.TextMid, fontSize = 13.sp)
        }
    }
}
