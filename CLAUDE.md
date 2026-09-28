# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Project

Moodly is a native Android mood-tracking app (Kotlin + Jetpack Compose), UI entirely in Spanish. Single Gradle module (`app`), namespace `com.asahioo.moodly`. AGP 8.13.0, Gradle 8.14.3, Kotlin 2.2.20, Compose BOM 2025.10.01, minSdk 26 / targetSdk 36 / compileSdk 36, JVM target 17.

This project was written without a compilable environment (no Android SDK available where it was generated) — if a build/sync error shows up, paste it as-is for a targeted fix rather than assuming the described architecture is wrong.

## Commands

- Run unit tests: `./gradlew test` (covers the stress quiz, month stats, patterns, and next-alarm calculation — `app/src/test`)
- Run a single test class: `./gradlew test --tests "com.asahioo.moodly.domain.DomainTest"`
- Build debug APK: `./gradlew assembleDebug`
- Build release APK: `./gradlew assembleRelease` (release is signed with the debug key for local perf testing only — not suitable for Play Store)
- There is no instrumented/UI test suite; only JVM unit tests exist.
- Compose runs much slower in debug; use the **release** build variant to judge real animation smoothness.

## Architecture

```
com.asahioo.moodly
├── data
│   ├── model        Serializable models (Mood, AppData, Settings, DayContext, PresetTag, CustomTag...)
│   ├── local        DataStore + kotlinx.serialization (single JSON document, atomic writes)
│   └── repository   MoodRepository interface + DefaultMoodRepository impl (single source of truth)
├── domain           Pure, tested rules: stress quiz, month stats, mood patterns, current date
├── quicklog         Daily reminder, notification mood faces, home-screen widget (RemoteViews)
├── di               AppContainer — manual DI, no framework
└── ui
    ├── theme        Palette, Inter Tight typography, motion curves
    ├── components   Vector glyph engine, bottom sheet host, island toast, tab bar, modifiers
    ├── home / calendar / insights / settings / onboarding / sheets
    └── MoodlyRoot   Tab navigation, sheet routing, toast/notice presentation
```

- **MVVM, unidirectional flow**: each screen has a ViewModel exposing immutable `StateFlow` state; composables hold no business state of their own.
- **No Android dependency in data/domain**: user-facing strings live in `strings.xml` and are mapped to domain values in the UI layer (`MoodUi.kt`, `UiText`).
- **Persistence**: DataStore off the main thread; JSON decoding runs on `Dispatchers.Default`. `AppData` is the single persisted document.
- **`PresetTag` values are persisted by name** — never rename an existing entry, only append new ones at the end, or saved user data will silently break.
- **Notification faces and the widget are separate vector drawables** (`res/drawable/ic_mood_*.xml`) duplicating `MoodGlyphs.icon()` — if you change one mood glyph, update both the Compose glyph and its drawable copy.
- **Continuous animations read state in the draw phase** (`graphicsLayer`, `drawBehind`, `offset {}`) to avoid per-frame recomposition; a single `Stagger` clock drives all staggered entrances on a screen. Faces/shapes/icons are parsed vector paths, not raster images.
