# Moodly (Android nativo)

Réplica nativa en **Kotlin + Jetpack Compose** del diseño de seguimiento de ánimo: introducción con figuras animadas, inicio con registro del día, sueño, estrés y quiz, calendario de ánimo, resumen del mes y ajustes. Toda la interfaz está en español.

## Cómo abrirlo

1. Descomprime el .zip y abre la carpeta `Moodly` en Android Studio.
2. Deja que Gradle sincronice (descarga Gradle 8.14.3 y las dependencias la primera vez).
3. Ejecuta en tu teléfono con **Run ▶**.
4. Para medir fluidez real usa la variante **release** (`Build Variants → release`). Compose en debug es mucho más lento; release activa R8 y está firmada con la llave de debug solo para pruebas locales.

> Este proyecto se escribió sin poder compilarlo (el entorno donde se generó no tiene SDK de Android). Si Android Studio marca algún error en la primera sincronización o compilación, pégalo tal cual y se corrige puntualmente.

## Versiones

| Pieza | Versión |
|---|---|
| Android Gradle Plugin | 8.13.0 |
| Gradle (wrapper) | 8.14.3 |
| Kotlin | 2.2.20 |
| Compose BOM | 2025.10.01 |
| minSdk / targetSdk | 26 / 36 |

Android Studio puede sugerir actualizar AGP o librerías; es seguro aceptar el asistente de actualización.

## Arquitectura

```
com.asahioo.moodly
├── data
│   ├── model        Modelos serializables (Mood, AppData, Settings…)
│   ├── local        DataStore + kotlinx.serialization (un documento JSON, escrituras atómicas)
│   └── repository   Interfaz MoodRepository + implementación (única fuente de verdad)
├── domain           Reglas puras y probadas: quiz de estrés, estadísticas del mes, patrones, fecha actual
├── quicklog         Recordatorio diario, caras en la notificación y widget (RemoteViews)
├── di               AppContainer (inyección manual; reemplazable por Hilt si crece)
└── ui
    ├── theme        Paleta, tipografía Inter Tight, curvas de movimiento
    ├── components   Motor de glifos vectoriales, hoja inferior, aviso tipo isla, tab bar, modificadores
    ├── home / calendar / insights / settings / onboarding / sheets
    └── MoodlyRoot   Navegación por pestañas, hojas y avisos
```

- **MVVM con flujo unidireccional**: cada pantalla tiene su ViewModel con `StateFlow` de estado inmutable; las pantallas son composables sin estado propio de negocio.
- **Sin dependencias de Android en datos/dominio**: los textos viven en `strings.xml` y se mapean en la UI (`MoodUi.kt`, `UiText`).
- **Persistencia local**: DataStore no bloquea el hilo principal; la decodificación JSON corre en `Dispatchers.Default`.
- **Pruebas**: `app/src/test` cubre el quiz, las estadísticas, los patrones y el cálculo de la próxima alarma (`./gradlew test`).

## Registro rápido

La app arranca vacía; el onboarding pide tu nombre y la hora del recordatorio. Registrar tu ánimo toma segundos:

- **Recordatorio diario** a la hora que elijas (Ajustes → Recordatorio). Usa `AlarmManager.setWindow` con 10 minutos de margen (sin permiso de alarmas exactas); en reposo profundo puede tardar más. Se reprograma solo tras reiniciar, cambiar la hora o actualizar la app, y no suena si ya registraste el día.
- **Caras en la notificación**: tocas una y queda guardado sin abrir la app.
- **Widget** de pantalla de inicio (4x1) con las 6 caras; resalta la de hoy.
- Las caras de la notificación y el widget son vectores en `res/drawable/ic_mood_*.xml`, copia de `MoodGlyphs.icon()`: si cambias una, cambia ambas.

## Contexto del día y patrones

- Cada día puede llevar una **nota corta** (140 caracteres), **etiquetas** incluidas (Trabajo, Familia, Ejercicio, Dormí mal…) y **etiquetas propias** (hasta 20; se crean desde la hoja del día y se renombran o borran en Ajustes → Etiquetas).
- Tras registrar el ánimo en Inicio aparece "Agregar nota o etiquetas"; en el Calendario la hoja del día edita todo y se guarda con un solo botón. Los días con contexto muestran un punto.
- El **sueño se guarda por día** (el de hoy, desde la tarjeta de Inicio).
- **Patrones** (Resumen): `domain/MoodPatterns.kt` compara, en los últimos 90 días, qué tan seguido aparece cada grupo de ánimo (Enojado o Estresado, Feliz o Tranquilo, Somnoliento o Aburrido) los días con y sin cada etiqueta o con menos de 6 h de sueño. Usa suavizado de Laplace y mínimos de muestra para no inventar patrones con pocos datos; muestra hasta 3, uno por factor. Son coincidencias, no causas.
- Los valores de `PresetTag` se guardan por nombre: no los renombres, solo agrega al final.

## Animaciones nativas

- **Splash nativo** (core-splashscreen) que se mantiene hasta que carga el estado, sin parpadeo.
- **Introducción**: las figuras caen con gravedad y rebote, luego flotan. Puedes arrastrarlas (regresan con resorte) o tocarlas (se aplastan como gelatina). Al tocar "¡Te ayudamos!" caen fuera de la pantalla.
- **Cambio de pestañas** estilo push/pop: la pantalla nueva entra completa y la anterior se desplaza y atenúa.
- **Atrás predictivo de Android**: desde Resumen, Calendario o Ajustes, el gesto de atrás arrastra la pantalla siguiendo tu dedo y revela Inicio. Las hojas inferiores también responden al gesto.
- **Hojas inferiores** con resorte, arrastre para cerrar y la pantalla de fondo encogiéndose como en iOS.
- **Avisos tipo isla**: una píldora negra que se expande desde arriba.
- Entrada escalonada del inicio, barras que crecen con rebote, contadores animados, celdas del calendario que brotan en diagonal, deslizamiento entre meses con rebote en los límites, dona que crece en sentido horario.
- **Vibración nativa** (`HapticFeedbackConstants`) en toques, confirmaciones y errores; se puede apagar en Ajustes.
- **Reducir animaciones** en Ajustes simplifica transiciones y quita las entradas escalonadas.

## Rendimiento

- Las animaciones continuas leen su valor en la fase de dibujo (`graphicsLayer`, `drawBehind`, `offset {}`), así que no recomponen por cuadro.
- Un solo reloj (`Stagger`) alimenta todas las entradas escalonadas de una pantalla.
- Caras, figuras e íconos se dibujan con paths vectoriales parseados una vez; no hay imágenes rasterizadas.
- La fuente Inter Tight va en 4 pesos estáticos recortados a caracteres latinos (~38 KB cada uno).

## Licencias

Inter Tight se distribuye bajo SIL Open Font License (ver `FONT_LICENSE_OFL.txt`).
