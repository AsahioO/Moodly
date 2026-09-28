# AGENTS.md — Moodly (Android nativo, Kotlin + Compose)

Single-module: solo `:app` (`settings.gradle.kts:23`). Paquete `com.asahioo.moodly`,
minSdk 26 / compile+target 36 / Java 17. Versiones en `gradle/libs.versions.toml`
(Kotlin 2.2.20, AGP 8.13.0, Compose BOM 2025.10.01).

## Comandos

- `./gradlew :app:testDebugUnitTest` — tests JVM puros (`app/src/test`).
- Un solo test: `./gradlew :app:testDebugUnitTest --tests "com.asahioo.moodly.domain.DomainTest"`.
- Medir fluidez real solo en variante **release** (R8 + minify; Compose en debug es
  mucho más lento). Release está firmada con la llave debug solo para pruebas locales
  (`app/build.gradle.kts:23`); reemplazar antes de publicar.
- SDK vía `local.properties:sdk.dir` (gitignored). Sin SDK Gradle no sincroniza:
  crear `local.properties` con `sdk.dir=<ruta>` en vez de tocar los `*.kts`.

## Arquitectura

- MVVM unidireccional: ViewModels con `StateFlow` inmutable; pantallas sin estado de negocio.
- Los ViewModels solo hablan con la interfaz `MoodRepository`
  (`data/repository/MoodRepository.kt:20`); implementación `DefaultMoodRepository`.
- DI manual en `di/AppContainer.kt` (no Hilt). Nuevos ViewModels: usar
  `containerViewModelFactory { c -> MiViewModel(c.repository, ...) }`
  (`ui/ViewModelFactory.kt`). Solo migrar a Hilt si `AppContainer` se queda chico.
- `data/` y `domain/` sin dependencias Android: textos en `strings.xml` mapeados en
  `ui/MoodUi.kt`. No importar `R` ni `Context` en datos/dominio.
- Persistencia: un solo documento `AppData` JSON en DataStore Preferences
  (key `app_state_v1`, `data/local/MoodLocalDataSource.kt`). Escrituras atómicas vía
  `update{}`; decode en `Dispatchers.Default`; documento corrupto → `AppData()` vacío.
  `Json { ignoreUnknownKeys = true, encodeDefaults = true }` (`di/AppContainer.kt:21`).

## Restricciones del modelo (`data/model/Models.kt`)

- `PresetTag` se serializa **por nombre**: nunca renombrar ni reordenar, solo agregar al final.
- El orden de `Mood` define el orden de chips y leyenda; no reordenar sin motivo UI.
- Claves de día ISO `yyyy-MM-dd`, compartidas entre `moods` y `days`.
- Límites (`AppData` companion): nota 140, nombre 30, etiqueta 20 chars / máx 20 customs,
  sueño 240–720 min (defecto 440), recordatorio en minutos-desde-medianoche (defecto 21:00).
- `setMood(date, null)` borra ánimo+nota+etiquetas pero **conserva el sueño**;
  `saveDay` escribe día completo de una vez. `DayContext` vacío se elimina del mapa
  (no acumular entradas vacías). Historial de `CustomTag` referenciado por id estable.

## UI / recursos

- Español `es-MX` (`MoodUi.kt:152`); fechas con `monthName/monthShort/shortDate`, nunca hardcodear meses.
- `MoodGlyphs.icon()` está **duplicado** en `res/drawable/ic_mood_*.xml` para notificación
  y widget (RemoteViews no usa Compose): cambiar ambos lados (`MoodGlyphs.kt:14`).
- Animaciones continuas en fase de dibujo (`graphicsLayer`/`drawBehind`/`offset{}`),
  no recomponer por cuadro; entradas escalonadas con un solo reloj `Stagger`.
- Haptics con `HapticFeedbackConstants`, respeta ajustes (haptics / reduceMotion).

## Recordatorio y widget (`quicklog/`)

- `AlarmManager.setWindow` con ventana de 10 min, sin permiso de alarmas exactas; cada
  disparo programa el siguiente. No suena si el día ya está registrado. Se reprograma en
  reboot, cambio de hora y update de app. En Doze profundo puede diferirse (ver comentario
  en `Reminder.kt:29` antes de pedir exactitud al minuto).
- Caras de notificación/widget son los vectores `ic_mood_*` (ver duplicación arriba).

## graphify

Grafo en `graphify-out/` (code-only, offline). Para preguntas de código, primero
`graphify query "<pregunta>"`; `graphify path "<A>" "<B>"` para relaciones,
`graphify explain "<concepto>"` para un foco. `GRAPH_REPORT.md` solo para revisión
amplia de arquitectura. Tras modificar código: `graphify update .`. Tras `git pull`:
`graphify update .` también. Archivos `graphify-out/` sucios tras hooks son normales.
