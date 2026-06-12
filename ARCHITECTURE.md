# FocusGuard — Architecture

## Stack
- Kotlin + Jetpack Compose
- Room (local DB, sin backend)
- WorkManager (background tasks)
- Accessibility Service (bloqueo de apps)
- NotificationListenerService (filtro de notificaciones)

## Módulos

```
app/
├── data/
│   ├── db/         AppDatabase.kt
│   ├── models/     Session, AppRule, Contact, DailyStats
│   └── dao/        SessionDao, AppRuleDao, StatsDao
├── service/
│   ├── FocusAccessibilityService.kt   ← bloqueo de apps
│   ├── NotificationFilterService.kt   ← filtro notificaciones
│   └── SessionManager.kt              ← estado global de sesión
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt              ← timer + start session
│   │   ├── AppsScreen.kt              ← whitelist de apps
│   │   ├── ContactsScreen.kt          ← VIP contacts
│   │   └── StatsScreen.kt             ← métricas
│   ├── components/
│   │   ├── TimerRing.kt
│   │   └── SessionCard.kt
│   └── theme/
│       └── Theme.kt
└── utils/
    └── InstalledAppsHelper.kt
```

## Flujo principal

1. Usuario configura whitelist de apps + contactos VIP (una sola vez)
2. Inicia sesión: elige categoría + duración
3. SessionManager emite estado ACTIVE
4. AccessibilityService detecta foreground app → si no está en whitelist → lanza LockScreen
5. NotificationFilterService cancela notificaciones de apps no-VIP
6. Al terminar: guarda stats, muestra resumen

## Permisos requeridos
- BIND_ACCESSIBILITY_SERVICE
- BIND_NOTIFICATION_LISTENER_SERVICE  
- READ_CONTACTS
- PACKAGE_USAGE_STATS (UsageStatsManager)
- SCHEDULE_EXACT_ALARM
- FOREGROUND_SERVICE

## Anti-evasión
- La LockScreen no tiene botón de cierre
- Para salir de sesión: hold 3 segundos + confirmar → registra como sesión abandonada
- El Accessibility Service se declara como crítico; si el usuario lo deshabilita, la próxima sesión muestra warning prominente
