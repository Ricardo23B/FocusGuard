package com.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.focusguard.ui.screens.AppsScreen
import com.focusguard.ui.screens.ContactsScreen
import com.focusguard.ui.screens.HomeScreen
import com.focusguard.ui.screens.StatsScreen
import com.focusguard.ui.theme.FocusGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusGuardTheme {
                var screen by remember { mutableStateOf("home") }
                when (screen) {
                    "apps"     -> AppsScreen(onBack = { screen = "home" })
                    "stats"    -> StatsScreen(onBack = { screen = "home" })
                    "contacts" -> ContactsScreen(onBack = { screen = "home" })
                    else -> HomeScreen(
                        onOpenApps = { screen = "apps" },
                        onOpenStats = { screen = "stats" },
                        onOpenContacts = { screen = "contacts" }
                    )
                }
            }
        }
    }
}
