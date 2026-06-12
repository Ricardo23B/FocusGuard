package com.focusguard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.focusguard.data.db.AppDatabase
import com.focusguard.data.models.VipContact
import com.focusguard.ui.theme.FG
import com.focusguard.ui.theme.categoryColor
import com.focusguard.utils.ContactsHelper
import com.focusguard.utils.PhoneContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    val vips by db.vipContactDao().getAll().collectAsState(initial = emptyList())
    val vipIds = vips.map { it.contactId }.toSet()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = withContext(Dispatchers.IO) {
                ContactsHelper.getContactsWithPhone(context)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(FG.Bg).padding(top = 48.dp)) {
        ScreenHeader("Contactos VIP", "Solo estos te avisan durante una sesión", onBack)

        if (!hasPermission) {
            PermissionRequest(onGrant = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) })
            return@Column
        }

        // Resumen de VIPs activos
        if (vips.isNotEmpty()) {
            Text(
                "${vips.size} ${if (vips.size == 1) "contacto puede" else "contactos pueden"} interrumpirte",
                color = FG.Success,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar contacto", color = FG.TextLow) },
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

        val filtered = contacts.filter {
            it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
        }

        if (contacts.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No se encontraron contactos con número.", color = FG.TextLow, fontSize = 14.sp)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { contact ->
                val isVip = contact.id in vipIds
                ContactRow(
                    name = contact.name,
                    phone = contact.phone,
                    isVip = isVip,
                    onToggle = {
                        scope.launch {
                            if (isVip) {
                                db.vipContactDao().delete(
                                    VipContact(contact.id, contact.name, contact.phone)
                                )
                            } else {
                                db.vipContactDao().insert(
                                    VipContact(contact.id, contact.name, contact.phone)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ContactRow(name: String, phone: String, isVip: Boolean, onToggle: () -> Unit) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    val avatarColor = categoryColor(listOf("código", "lectura", "diseño", "estudio", "otro")[
        (name.hashCode() and 0x7fffffff) % 5
    ])
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isVip) FG.Success.copy(alpha = 0.10f) else FG.Surface)
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = avatarColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = FG.TextHi, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(phone, color = FG.TextLow, fontSize = 12.sp)
        }
        if (isVip) {
            Box(
                Modifier.clip(CircleShape).background(FG.Success).size(24.dp),
                contentAlignment = Alignment.Center
            ) { Text("✓", color = FG.Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        } else {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) { Text("+", color = FG.TextLow, fontSize = 20.sp) }
        }
    }
}

@Composable
private fun PermissionRequest(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(FG.Ember.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Text("☎", color = FG.Ember, fontSize = 32.sp) }
        Text(
            "Acceso a contactos",
            color = FG.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
        Text(
            "Para elegir quién puede interrumpirte, FocusGuard necesita leer tu lista de contactos. No se sube a ningún lado: queda en tu teléfono.",
            color = FG.TextMid, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FG.Ember),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Permitir acceso", color = FG.Bg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
