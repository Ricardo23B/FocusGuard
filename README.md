# FocusGuard

App Android de foco y bloqueo. Sin backend, sin cuenta, todo local.

## Setup

1. Clonar en Android Studio (Electric Eel o posterior)
2. Sync Gradle
3. Correr en dispositivo físico (emulador no soporta AccessibilityService correctamente)

## Permisos que vas a tener que conceder manualmente

Estos no se pueden pedir con el dialog estándar — hay que mandar al usuario a Settings:

| Permiso | Dónde activarlo |
|---|---|
| Servicio de accesibilidad | Settings → Accesibilidad → FocusGuard |
| Acceso a notificaciones | Settings → Notificaciones → Acceso a notificaciones |
| Uso de apps (UsageStats) | Settings → Privacidad → Uso de apps |

La app tiene que guiar al usuario a cada uno en el onboarding.

## Estructura de archivos clave

```
service/
  SessionManager.kt          ← Estado global. Singleton.
  FocusAccessibilityService  ← Detecta foreground app y bloquea
  NotificationFilterService  ← Filtra notificaciones durante sesión

ui/screens/
  HomeScreen.kt              ← Timer + iniciar sesión
  LockActivity.kt            ← Pantalla de bloqueo (no cerrable)
  AppsScreen.kt              ← Whitelist (TODO)
  StatsScreen.kt             ← Métricas (TODO)

data/
  models/Models.kt           ← Entidades Room
  dao/Daos.kt                ← Queries
  db/AppDatabase.kt          ← Singleton DB
```

## TODOs para v1.0 completo

- [ ] AppsScreen: lista de apps instaladas con toggle
- [ ] ContactsScreen: picker de contactos VIP
- [ ] StatsScreen: gráfico de rachas y tiempo por categoría
- [ ] Onboarding: guiar al usuario a conceder los 3 permisos especiales
- [ ] Conectar `abandonSession` al diálogo de LockActivity (necesita Application context)
- [ ] Foreground Service notification durante sesión activa (obligatorio en Android 14+)
- [ ] Manejo del caso donde el usuario revoca AccessibilityService durante sesión

## Limitación conocida

En Android 13+ el usuario puede ir a Settings y revocar el Accessibility Service en cualquier momento.
No hay forma de impedirlo a nivel OS. La única defensa es fricción de UX (recordatorio prominente al
volver a la app) y el registro de sesión abandonada.
