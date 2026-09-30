# Moodly

App nativa de Android para llevar el registro diario del ánimo, hecha con **Kotlin + Jetpack Compose**. Toda la interfaz está en español. Los datos viven solo en el teléfono: no hay cuenta, servidor ni analítica.

## Funciones

- **Registro diario** de 6 ánimos (Feliz, Enojado, Somnoliento, Aburrido, Tranquilo, Estresado) con caras vectoriales animadas.
- **Contexto del día**: nota corta, etiquetas incluidas y propias, y horas de sueño.
- **Estrés**: indicador y quiz corto que calcula un nivel bajo, medio o alto.
- **Calendario** de ánimo por mes y **Resumen** del mes (ánimo principal, disciplina de registro, dona por ánimo).
- **Racha** de días consecutivos con registro.
- **Patrones**: coincidencias entre tus etiquetas, sueño y pasos y tu ánimo.
- **Health Connect** (opcional): importa sueño y pasos, solo lectura.
- **Recordatorio diario**, con caras en la notificación para registrar sin abrir la app.
- **Widget** de pantalla de inicio redimensionable.

## Cómo correrlo

1. Abre la carpeta del proyecto en Android Studio (JDK 17).
2. Si no existe `local.properties`, créalo con `sdk.dir=<ruta a tu Android SDK>` (Android Studio lo genera solo). Está en `.gitignore`.
3. Deja que Gradle sincronice; la primera vez descarga Gradle 8.14.3 y las dependencias.
4. Ejecuta en un teléfono o emulador con **Run ▶**. Health Connect requiere Android 9 o superior con el proveedor instalado; sin él la sección se muestra deshabilitada.
5. Para juzgar la fluidez real usa la variante **release** (`Build Variants → release`). Compose en debug es mucho más lento. Release activa R8 y se firma con la llave de debug solo para pruebas locales: no sirve para Play Store.

> El proyecto se escribió sin poder compilarlo en el entorno donde se generó. Si aparece un error de compilación o sincronización, pégalo tal cual para corregirlo puntualmente.

## Comandos

| Qué | Comando |
|---|---|
| Pruebas unitarias | `./gradlew test` |
| Una clase de pruebas | `./gradlew test --tests "com.asahioo.moodly.domain.DomainTest"` |
| APK debug | `./gradlew assembleDebug` |
| APK release | `./gradlew assembleRelease` |

Solo hay pruebas JVM; no hay suite instrumentada ni de UI.

## Versiones

| Pieza | Versión |
|---|---|
| Android Gradle Plugin | 8.13.0 |
| Gradle (wrapper) | 8.14.3 |
| Kotlin | 2.2.20 |
| Compose BOM | 2025.10.01 |
| Health Connect client | 1.1.0 |
| DataStore Preferences | 1.1.4 |
| kotlinx.serialization JSON | 1.9.0 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| JVM | 17 |

## Arquitectura

Un solo módulo Gradle (`app`), paquete `com.asahioo.moodly`.

```
com.asahioo.moodly
├── data
│   ├── model        Modelos serializables (Mood, AppData, DayContext, Settings, PresetTag, CustomTag…)
│   ├── local        DataStore + kotlinx.serialization (un documento JSON, escrituras atómicas)
│   ├── repository   Interfaz MoodRepository + implementación (única fuente de verdad)
│   └── health       Lectura de sueño y pasos desde Health Connect
├── domain           Reglas puras y probadas: quiz de estrés, estadísticas y racha, patrones, fecha actual
├── quicklog         Recordatorio diario, caras en la notificación y widget (RemoteViews)
├── di               AppContainer (inyección manual, sin framework)
└── ui
    ├── theme        Paleta, tipografía Inter Tight, curvas de movimiento
    ├── components   Motor de glifos vectoriales, hoja inferior, aviso heads-up, tab bar, modificadores
    ├── home / calendar / insights / settings / onboarding / sheets
    └── MoodlyRoot   Navegación por pestañas, hojas y avisos
```

- **MVVM con flujo unidireccional**: cada pantalla tiene su ViewModel con un `StateFlow` inmutable; los composables no guardan estado de negocio.
- **Sin dependencias de Android en `data`/`domain`** (salvo `data/health`, que envuelve el SDK de Health Connect): los textos viven en `strings.xml` y se mapean en la UI (`MoodUi.kt`, `UiText`).
- **Persistencia**: `AppData` es el único documento persistido, en DataStore fuera del hilo principal; la decodificación JSON corre en `Dispatchers.Default`. Un documento corrupto se reemplaza por uno vacío.
- **`PresetTag` se guarda por nombre**: nunca renombres ni reordenes una entrada existente, solo agrega al final, o los datos guardados dejarán de leerse.

## Registro rápido

La app arranca vacía. El onboarding pide tu nombre y la hora del recordatorio.

- **Recordatorio diario** (Ajustes → Recordatorio): usa `AlarmManager.setWindow` con 10 minutos de margen, sin permiso de alarmas exactas; en reposo profundo puede tardar más. Se reprograma solo tras reiniciar, cambiar la hora o la zona, o actualizar la app, y no suena si ya registraste el día.
- **Caras en la notificación**: tocas una y queda guardado sin abrir la app.
- **Widget** (4×2 por defecto, redimensionable). El launcher elige el diseño según el alto:

  | Alto | Contenido |
  |---|---|
  | menos de 90 dp | Fila de 6 caras |
  | 90 dp o más | Saludo, racha y, si ya registraste, confirmación con "Cambiar" y "Nota" |
  | 160 dp o más | Lo anterior, más los últimos 7 días con su cara |

  Desde 200 dp las caras pasan a una cuadrícula. El fondo toma el color pastel del ánimo de hoy.
- Las caras de la notificación y el widget son vectores en `res/drawable/ic_mood_*.xml`, copia de `MoodGlyphs.icon()`: si cambias una, cambia la otra.

## Health Connect y privacidad

Conectar es opcional (Ajustes → Health Connect). Moodly solo **lee**, y solo con la app en primer plano:

| Permiso | Uso |
|---|---|
| `health.READ_SLEEP` | Sueño de la noche anterior a cada día, con una ventana fija de 18:00 a 14:00 |
| `health.READ_STEPS` | Pasos totales del día |

- Al conectar se rellenan los últimos 30 días; después, cada vez que abres la app se releen los últimos 7. Si concedes solo uno de los permisos, se lee solo ese.
- El sueño que escribes a mano **nunca** se sobrescribe. Solo se actualiza el que vino de Health Connect.
- Desconectar revoca los permisos; lo ya importado se queda en tus registros. "Borrar todos los datos" también desconecta.
- Health Connect muestra la explicación de uso (`HealthRationaleActivity`) desde su diálogo de permisos. Si el proveedor necesita actualizarse, Ajustes ofrece abrir Play Store.
- Los datos no salen del teléfono. Los demás permisos de la app son `VIBRATE`, `POST_NOTIFICATIONS` y `RECEIVE_BOOT_COMPLETED` (reprogramar el recordatorio).

## Contexto del día, racha y patrones

- Cada día puede llevar una **nota** (hasta 140 caracteres), **etiquetas incluidas** (Trabajo, Familia, Amigos, Ejercicio, Dormí mal, Estudio, Salud, Descanso) y **etiquetas propias** (hasta 20, de 20 caracteres; se crean desde la hoja del día y se renombran o borran en Ajustes → Etiquetas). Los días con contexto muestran un punto en el calendario.
- El **sueño** se guarda por día (manual o de Health Connect); los **pasos** vienen solo de Health Connect. Con permiso de pasos, Inicio muestra una tarjeta con los pasos de hoy y los últimos 7 días (cada barra lleva el color del ánimo de ese día), y Resumen una versión mini.
- La **racha** cuenta los días consecutivos con registro que terminan hoy; si hoy aún no registras, cuenta desde ayer para no caer a cero con el día abierto.
- **Patrones** (`domain/MoodPatterns.kt`): en los últimos 90 días compara qué tan seguido aparece cada grupo de ánimo (Enojado o Estresado, Feliz o Tranquilo, Somnoliento o Aburrido) los días con y sin cada factor: una etiqueta, **dormir menos de 6 h** o **caminar menos de 5 000 pasos**. Los días sin dato de sueño o pasos no cuentan como "sí dormí" ni "sí caminé". Usa suavizado de Laplace y mínimos de muestra (10 días registrados, al menos 3 por lado) y exige una diferencia de 1.5 veces o más; muestra hasta 3, uno por factor. Son coincidencias, no causas.

## Animaciones

- **Splash nativo** (core-splashscreen) hasta que carga el estado, sin parpadeo.
- **Introducción**: las figuras caen con gravedad y rebote, luego flotan. Se arrastran (regresan con resorte) o se tocan (se aplastan como gelatina). Al tocar "¡Te ayudamos!" caen fuera de la pantalla.
- **Caras con gesto propio**: la cara seleccionada repite un gesto característico tras pausas de 2.5 a 6 segundos.
- **Pestañas** estilo push/pop: la pantalla nueva entra completa y la anterior se desplaza y se atenúa.
- **Atrás predictivo de Android**: desde Resumen, Calendario o Ajustes, el gesto arrastra la pantalla siguiendo el dedo y revela Inicio. Las hojas inferiores también responden.
- **Hojas inferiores** con resorte, arrastre para cerrar y la pantalla de fondo encogiéndose como en iOS.
- **Avisos heads-up**: una tarjeta baja desde la barra de estado, tintada con el color del ánimo, con barra de progreso; se descarta deslizándola hacia arriba o tras 2.4 s.
- Entrada escalonada del inicio, barras que crecen con rebote, contadores animados, celdas del calendario que brotan en diagonal, deslizamiento entre meses con rebote en los límites y dona que crece en sentido horario.
- **Vibración nativa** (`HapticFeedbackConstants`) en toques, confirmaciones y errores; se apaga en Ajustes.
- **Reducir animaciones** (Ajustes) simplifica transiciones y quita las entradas escalonadas y los gestos de las caras.

## Rendimiento

- Las animaciones continuas leen su valor en la fase de dibujo (`graphicsLayer`, `drawBehind`, `offset {}`), así que no recomponen por cuadro.
- Un solo reloj (`Stagger`) alimenta todas las entradas escalonadas de una pantalla.
- Caras, figuras e íconos son paths vectoriales parseados una vez; no hay imágenes rasterizadas.
- Inter Tight va en 4 pesos estáticos recortados a caracteres latinos (~38 KB cada uno).

## Pruebas

`app/src/test` (`./gradlew test`):

| Clase | Cubre |
|---|---|
| `DomainTest` | Quiz de estrés, estadísticas del mes, racha, patrones (sueño, pasos, etiquetas) y fusión de Health Connect en `DayContext` |
| `ReminderTest` | Cálculo de la próxima alarma |
| `WidgetSizeTest` | Umbrales de tamaño del widget |
| `MoodIdleTest` | Gestos de las caras |
| `ShapePhysicsTest` | Física de las figuras del onboarding |

## Licencias

Inter Tight se distribuye bajo SIL Open Font License (ver `FONT_LICENSE_OFL.txt`).
