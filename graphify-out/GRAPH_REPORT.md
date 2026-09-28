# Graph Report - Moodly  (2026-09-28)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 473 nodes · 1250 edges · 25 communities (21 shown, 4 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 51 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- InsightsScreen.kt
- Sheets.kt
- Glyph
- CalendarViewModel.kt
- CustomTag
- MoodFaces.kt
- AppViewModel
- PresetTag
- OnboardingScreen.kt
- DefaultMoodRepository
- Magnitude
- Mood
- DayContext
- MoodRepository
- HomeScreen.kt
- DomainTest.kt
- AppData
- Tab
- SheetRequest
- Haptics
- gradlew
- graphify.js

## God Nodes (most connected - your core abstractions)
1. `Mood` - 46 edges
2. `MoodlyRoot()` - 25 edges
3. `MoodRepository` - 22 edges
4. `DayContext` - 21 edges
5. `AppData` - 20 edges
6. `StressLevel` - 19 edges
7. `Glyph()` - 19 edges
8. `Palette` - 18 edges
9. `AppViewModel` - 18 edges
10. `PresetTag` - 18 edges

## Surprising Connections (you probably didn't know these)
- `SwipeableMonth()` --calls--> `with()`  [INFERRED]
  app/src/main/java/com/asahioo/moodly/ui/calendar/CalendarScreen.kt → app/src/main/java/com/asahioo/moodly/data/repository/MoodRepository.kt
- `CardHeader()` --calls--> `Glyph()`  [INFERRED]
  app/src/main/java/com/asahioo/moodly/ui/components/Common.kt → app/src/main/java/com/asahioo/moodly/ui/components/Glyph.kt
- `CircleIconButton()` --calls--> `Glyph()`  [INFERRED]
  app/src/main/java/com/asahioo/moodly/ui/components/Common.kt → app/src/main/java/com/asahioo/moodly/ui/components/Glyph.kt
- `ToastIconView()` --calls--> `MoodIcon()`  [INFERRED]
  app/src/main/java/com/asahioo/moodly/ui/components/IslandToast.kt → app/src/main/java/com/asahioo/moodly/ui/components/MoodGlyphs.kt
- `HomeScreen()` --calls--> `with()`  [INFERRED]
  app/src/main/java/com/asahioo/moodly/ui/home/HomeScreen.kt → app/src/main/java/com/asahioo/moodly/data/repository/MoodRepository.kt

## Import Cycles
- None detected.

## Communities (25 total, 4 thin omitted)

### Community 0 - "InsightsScreen.kt"
Cohesion: 0.08
Nodes (62): Alignment, Pattern, PatternsResult, MonthSummary, CalendarScreen(), DayCellView(), Modifier, MonthGrid() (+54 more)

### Community 1 - "Sheets.kt"
Cohesion: 0.08
Nodes (55): Animatable, AnimationVector1D, PrimaryButton(), Alert, Check, IslandToast(), IslandToastState, Modifier (+47 more)

### Community 2 - "Glyph"
Cohesion: 0.19
Nodes (23): AppIcons, drawGlyph(), drawItem(), GCircle, GFill, GGroup, GLook, Glyph() (+15 more)

### Community 3 - "CalendarViewModel.kt"
Cohesion: 0.08
Nodes (25): AppContainer, DateProvider, SystemDateProvider, YearMonth, MoodStats, MoodlyApplication, Context, MoodWidget (+17 more)

### Community 4 - "CustomTag"
Cohesion: 0.18
Nodes (6): CustomTag, MoodGroup, GOOD, LOW, TENSE, normalizeTagLabel()

### Community 5 - "MoodFaces.kt"
Cohesion: 0.15
Nodes (15): Context, PendingIntent, logMoodIntent(), moodFacesViews(), QuickLogReceiver, viewsWithFaces(), Context, PendingIntent (+7 more)

### Community 6 - "AppViewModel"
Cohesion: 0.14
Nodes (9): Settings, MainActivity, AppUiState, AppViewModel, StateFlow, ViewModel, MoodlyTheme(), Bundle (+1 more)

### Community 7 - "PresetTag"
Cohesion: 0.20
Nodes (9): PresetTag, EXERCISE, FAMILY, FRIENDS, HEALTH, POOR_SLEEP, REST, STUDY (+1 more)

### Community 8 - "OnboardingScreen.kt"
Cohesion: 0.11
Nodes (20): Intro(), androidx, Modifier, Offset, localProgress(), PhysicsShape(), rememberGravity(), SensorEventListener (+12 more)

### Community 9 - "DefaultMoodRepository"
Cohesion: 0.17
Nodes (4): QuizProgress, DefaultMoodRepository, Flow, with()

### Community 10 - "Magnitude"
Cohesion: 0.18
Nodes (12): Custom, Day, Factor, Magnitude, DOUBLE, HALF, LESS, MORE (+4 more)

### Community 11 - "Mood"
Cohesion: 0.22
Nodes (7): Mood, ANGRY, BORED, CALM, HAPPY, SLEEPY, STRESSED

### Community 14 - "HomeScreen.kt"
Cohesion: 0.10
Nodes (29): StressLevel, HIGH, LOW, MEDIUM, SeededRandom, StressQuiz, CardHeader(), EmptyValue() (+21 more)

### Community 15 - "DomainTest.kt"
Cohesion: 0.22
Nodes (3): MoodStatsTest, StressQuizTest, TagsTest

### Community 16 - "AppData"
Cohesion: 0.46
Nodes (3): Flow, MoodLocalDataSource, AppData

### Community 17 - "Tab"
Cohesion: 0.32
Nodes (7): Modifier, TabBar(), Tab, Calendar, Home, Insights, Settings

### Community 18 - "SheetRequest"
Cohesion: 0.25
Nodes (8): Day, MonthPicker, Reminder, Reset, SheetRequest, Sleep, Stress, Tags

### Community 21 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **46 isolated node(s):** `Alert`, `Check`, `OfMood`, `Sleep`, `ArrayItem` (+41 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Mood` connect `Mood` to `InsightsScreen.kt`, `Sheets.kt`, `Glyph`, `CalendarViewModel.kt`, `CustomTag`, `MoodFaces.kt`, `AppViewModel`, `PresetTag`, `DefaultMoodRepository`, `DayContext`, `MoodRepository`, `HomeScreen.kt`, `DomainTest.kt`?**
  _High betweenness centrality (0.233) - this node is a cross-community bridge._
- **Why does `AppData` connect `AppData` to `Sheets.kt`, `CustomTag`, `OnboardingScreen.kt`, `DefaultMoodRepository`, `Magnitude`, `DayContext`, `MoodRepository`, `HomeScreen.kt`, `DomainTest.kt`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Why does `MoodlyRoot()` connect `Sheets.kt` to `InsightsScreen.kt`, `Haptics`, `AppViewModel`, `HomeScreen.kt`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **What connects `Alert`, `Check`, `OfMood` to the rest of the system?**
  _46 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InsightsScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.0772635814889336 - nodes in this community are weakly interconnected._
- **Should `Sheets.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.07643600180913614 - nodes in this community are weakly interconnected._
- **Should `CalendarViewModel.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08084163898117387 - nodes in this community are weakly interconnected._