package com.asahioo.moodly.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asahioo.moodly.R
import com.asahioo.moodly.data.health.HealthConnect
import com.asahioo.moodly.data.health.HealthStatus
import com.asahioo.moodly.data.model.AppData
import com.asahioo.moodly.data.model.Mood
import com.asahioo.moodly.ui.calendar.CalendarScreen
import com.asahioo.moodly.ui.calendar.CalendarViewModel
import com.asahioo.moodly.ui.components.IslandToast
import com.asahioo.moodly.ui.components.IslandToastState
import com.asahioo.moodly.ui.components.LocalHaptics
import com.asahioo.moodly.ui.components.LocalToast
import com.asahioo.moodly.ui.components.SheetHost
import com.asahioo.moodly.ui.components.TabBar
import com.asahioo.moodly.ui.components.ToastData
import com.asahioo.moodly.ui.components.ToastIcon
import com.asahioo.moodly.ui.components.rememberHaptics
import com.asahioo.moodly.ui.components.rememberNotificationPermission
import com.asahioo.moodly.ui.components.rememberStagger
import com.asahioo.moodly.ui.home.HomeEvent
import com.asahioo.moodly.ui.home.HomeScreen
import com.asahioo.moodly.ui.home.HomeViewModel
import com.asahioo.moodly.ui.insights.InsightsScreen
import com.asahioo.moodly.ui.insights.InsightsViewModel
import com.asahioo.moodly.ui.onboarding.OnboardingScreen
import com.asahioo.moodly.ui.settings.SettingsScreen
import com.asahioo.moodly.ui.sheets.DaySheet
import com.asahioo.moodly.ui.sheets.MonthPickerSheet
import com.asahioo.moodly.ui.sheets.ReminderSheet
import com.asahioo.moodly.ui.sheets.ResetSheet
import com.asahioo.moodly.ui.sheets.SleepSheet
import com.asahioo.moodly.ui.sheets.StressSheet
import com.asahioo.moodly.ui.sheets.TagsSheet
import com.asahioo.moodly.ui.theme.LocalReduceMotion
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Raíz de la app: pestañas con transiciones y "atrás predictivo", onboarding superpuesto,
 * hojas inferiores y avisos tipo isla.
 */
@Composable
fun MoodlyRoot(appViewModel: AppViewModel) {
    val app by appViewModel.uiState.collectAsStateWithLifecycle()
    if (!app.isReady) {
        // El splash nativo sigue visible mientras tanto.
        Box(Modifier.fillMaxSize().background(Palette.Paper))
        return
    }

    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val calendarVm: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
    val insightsVm: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory)

    val home by homeVm.uiState.collectAsStateWithLifecycle()
    val calendar by calendarVm.uiState.collectAsStateWithLifecycle()
    val monthOptions by calendarVm.monthOptions.collectAsStateWithLifecycle()
    val insights by insightsVm.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val toast = remember { IslandToastState(scope) }
    val haptics = rememberHaptics(app.settings.haptics)
    val requestNotifications = rememberNotificationPermission()
    val healthStatus by appViewModel.healthStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestHealth = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        appViewModel.onHealthPermissionsResult()
        toast.show(
            ToastData(
                ToastIcon.Check,
                UiText.res(R.string.health_connect),
                UiText.res(if (granted.isEmpty()) R.string.toast_health_denied else R.string.toast_health_connected),
            )
        )
    }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }
    val sheetOffset = remember { Animatable(1f) }
    var entranceKey by rememberSaveable { mutableIntStateOf(0) }
    val homeStagger = rememberStagger(entranceKey)
    val homeScroll = rememberScrollState()

    val showOnboarding = !app.onboarded

    // Íconos de la barra de navegación del sistema: oscuros sobre blanco, claros sobre negro.
    val view = LocalView.current
    val darkNavIcons = showOnboarding || sheet != null
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = darkNavIcons
        }
    }

    LaunchedEffect(showOnboarding) {
        if (showOnboarding) {
            sheet = null
            tab = Tab.Home
        }
    }

    LaunchedEffect(homeVm) {
        homeVm.events.collect { event ->
            when (event) {
                is HomeEvent.QuizCompleted -> toast.show(
                    ToastData(
                        ToastIcon.Check,
                        UiText.res(R.string.toast_quiz_done),
                        UiText.res(R.string.toast_stress_level, UiText.res(event.level.labelRes)),
                    )
                )
            }
        }
    }

    fun openTab(target: Tab) {
        tab = target
    }

    fun dateLabel(date: LocalDate): UiText = UiText.res(
        R.string.day_month,
        date.dayOfMonth,
        UiText.ArrayItem(R.array.months_short, date.monthValue - 1),
    )

    /** Activa el recordatorio (opcionalmente a otra hora) solo si la app puede notificar. */
    fun enableReminder(minutes: Int = app.settings.reminderMinutes) {
        requestNotifications { granted ->
            if (granted) {
                appViewModel.setReminderTime(minutes)
                haptics.confirm()
                toast.show(
                    ToastData(
                        ToastIcon.Check,
                        UiText.res(R.string.toast_reminder_set),
                        UiText.res(R.string.toast_reminder_at, formatTimeOfDay(minutes, is24Hour)),
                    )
                )
            } else {
                haptics.reject()
                toast.show(
                    ToastData(
                        ToastIcon.Alert,
                        UiText.res(R.string.toast_notifications_off),
                        UiText.res(R.string.toast_notifications_off_sub),
                    )
                )
            }
        }
    }

    fun pickTodayMood(mood: Mood) {
        val already = home.todayMood == mood
        homeVm.logTodayMood(mood)
        haptics.confirm()
        toast.show(
            ToastData(
                ToastIcon.OfMood(mood),
                UiText.res(if (already) R.string.toast_already_logged else R.string.toast_mood_logged),
                UiText.res(R.string.toast_date_mood, UiText.res(mood.labelRes), dateLabel(home.today)),
            )
        )
    }

    CompositionLocalProvider(
        LocalReduceMotion provides app.settings.reduceMotion,
        LocalToast provides toast,
        LocalHaptics provides haptics,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            // Contenido principal: se encoge y redondea mientras hay una hoja abierta.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = (1f - sheetOffset.value).coerceIn(0f, 1f)
                        val s = 1f - 0.06f * p
                        scaleX = s
                        scaleY = s
                        translationY = 6.dp.toPx() * p
                        transformOrigin = TransformOrigin(0.5f, 0.3f)
                        shape = RoundedCornerShape((30f * p).dp)
                        clip = p > 0f
                    }
                    .background(Palette.Night),
            ) {
                MainTabs(
                    tab = tab,
                    onTabChange = ::openTab,
                    backEnabled = sheet == null && !showOnboarding,
                ) { current ->
                    when (current) {
                        Tab.Home -> HomeScreen(
                            state = home,
                            stagger = homeStagger,
                            onOpenSettings = { openTab(Tab.Settings) },
                            onPickMood = ::pickTodayMood,
                            onAnswer = homeVm::answerQuiz,
                            onRestartQuiz = homeVm::restartQuiz,
                            onOpenSleep = { sheet = SheetRequest.Sleep },
                            onOpenStress = { sheet = SheetRequest.Stress },
                            onOpenContext = { sheet = SheetRequest.Day(home.today) },
                            scrollState = homeScroll,
                        )
                        Tab.Insights -> InsightsScreen(
                            state = insights,
                            onBack = { openTab(Tab.Home) },
                            onOpenSleep = { sheet = SheetRequest.Sleep },
                            onOpenStress = { sheet = SheetRequest.Stress },
                        )
                        Tab.Calendar -> CalendarScreen(
                            state = calendar,
                            onBack = { openTab(Tab.Home) },
                            onPickMonth = { sheet = SheetRequest.MonthPicker },
                            onDayClick = { cell ->
                                if (cell.isFuture) {
                                    haptics.reject()
                                    toast.show(
                                        ToastData(
                                            ToastIcon.Alert,
                                            UiText.res(R.string.toast_not_yet),
                                            UiText.res(R.string.toast_future),
                                        )
                                    )
                                } else {
                                    sheet = SheetRequest.Day(cell.date)
                                }
                            },
                            onStep = calendarVm::step,
                        )
                        Tab.Settings -> SettingsScreen(
                            userName = app.userName,
                            trackingSince = app.trackingSince,
                            settings = app.settings,
                            onBack = { openTab(Tab.Home) },
                            onHapticsChange = { enabled ->
                                appViewModel.setHaptics(enabled)
                                toast.show(
                                    ToastData(
                                        ToastIcon.Check,
                                        UiText.res(R.string.haptics),
                                        UiText.res(if (enabled) R.string.toast_on else R.string.toast_off),
                                    )
                                )
                            },
                            onReduceMotionChange = { enabled ->
                                appViewModel.setReduceMotion(enabled)
                                toast.show(
                                    ToastData(
                                        ToastIcon.Check,
                                        UiText.res(R.string.reduce_motion),
                                        UiText.res(if (enabled) R.string.toast_on else R.string.toast_off),
                                    )
                                )
                            },
                            onReminderChange = { enabled ->
                                if (enabled) {
                                    enableReminder()
                                } else {
                                    appViewModel.setReminderEnabled(false)
                                    toast.show(
                                        ToastData(
                                            ToastIcon.Check,
                                            UiText.res(R.string.reminder_daily),
                                            UiText.res(R.string.toast_off),
                                        )
                                    )
                                }
                            },
                            onPickReminderTime = { sheet = SheetRequest.Reminder },
                            health = healthStatus,
                            onHealthChange = { enabled ->
                                if (enabled) {
                                    requestHealth.launch(HealthConnect.PERMISSIONS)
                                } else {
                                    appViewModel.disconnectHealth()
                                    toast.show(
                                        ToastData(
                                            ToastIcon.Check,
                                            UiText.res(R.string.health_connect),
                                            UiText.res(R.string.toast_health_disconnected),
                                        )
                                    )
                                }
                            },
                            onInstallHealth = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(HealthConnect.PLAY_STORE_URI))
                                        .setPackage("com.android.vending")
                                )
                            },
                            customTagCount = app.customTags.size,
                            onManageTags = { sheet = SheetRequest.Tags },
                            onReplayIntro = appViewModel::replayOnboarding,
                            onResetData = { sheet = SheetRequest.Reset },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showOnboarding,
                enter = fadeIn(tween(420)) + scaleIn(tween(420, easing = Motion.EaseOut), initialScale = 1.04f),
                exit = fadeOut(tween(380)),
            ) {
                OnboardingScreen(
                    initialName = app.userName,
                    onFinished = { name, reminderMinutes ->
                        appViewModel.completeOnboarding(name, reminderMinutes)
                        entranceKey++
                    },
                )
            }

            SheetHost(
                request = sheet,
                offset = sheetOffset,
                onDismiss = { sheet = null },
            ) { request ->
                when (request) {
                    is SheetRequest.Day -> {
                        val key = request.date.toString()
                        DaySheet(
                            date = request.date,
                            isToday = request.date == home.today,
                            current = app.moods[key],
                            context = app.days[key],
                            customTags = app.customTags,
                            onSave = { mood, note, tags, customTags ->
                                calendarVm.saveDay(request.date, mood, note, tags, customTags)
                                sheet = null
                                toast.show(
                                    ToastData(
                                        ToastIcon.OfMood(mood),
                                        UiText.res(R.string.toast_mood_saved),
                                        UiText.res(R.string.toast_date_mood, UiText.res(mood.labelRes), dateLabel(request.date)),
                                    )
                                )
                            },
                            onClear = {
                                calendarVm.setMood(request.date, null)
                                sheet = null
                                toast.show(ToastData(ToastIcon.Check, UiText.res(R.string.toast_entry_cleared), dateLabel(request.date)))
                            },
                            onCreateTag = appViewModel::addTag,
                        )
                    }
                    SheetRequest.Sleep -> SleepSheet(
                        initialMinutes = home.sleepMinutes ?: AppData.DEFAULT_SLEEP_MINUTES,
                        fromHealth = home.todayContext?.sleepFromHealth == true,
                        onSave = { minutes ->
                            homeVm.setSleep(minutes)
                            haptics.confirm()
                            sheet = null
                            toast.show(
                                ToastData(
                                    ToastIcon.Sleep,
                                    UiText.res(R.string.toast_sleep_saved),
                                    UiText.res(R.string.toast_sleep_sub, minutes / 60, minutes % 60),
                                )
                            )
                        },
                    )
                    SheetRequest.Stress -> StressSheet(
                        level = home.stress,
                        quizIndex = home.quizIndex,
                        quizTotal = home.quizTotal,
                        onQuiz = {
                            if (home.quizDone) homeVm.restartQuiz()
                            sheet = null
                            scope.launch {
                                if (tab != Tab.Home) {
                                    openTab(Tab.Home)
                                    delay(Motion.NAV_MS.toLong())
                                }
                                homeScroll.animateScrollTo(homeScroll.maxValue)
                            }
                        },
                    )
                    SheetRequest.MonthPicker -> MonthPickerSheet(
                        options = monthOptions,
                        current = calendar.month,
                        onSelect = { month ->
                            calendarVm.show(month)
                            sheet = null
                        },
                    )
                    SheetRequest.Reminder -> ReminderSheet(
                        initialMinutes = app.settings.reminderMinutes,
                        onSave = { minutes ->
                            sheet = null
                            enableReminder(minutes)
                        },
                    )
                    SheetRequest.Tags -> TagsSheet(
                        customTags = app.customTags,
                        usage = { id -> app.days.values.count { id in it.customTags } },
                        onAdd = appViewModel::addTag,
                        onRename = appViewModel::renameTag,
                        onDelete = appViewModel::deleteTag,
                    )
                    SheetRequest.Reset -> ResetSheet(
                        onConfirm = {
                            appViewModel.resetAll()
                            haptics.confirm()
                            sheet = null
                            toast.show(
                                ToastData(
                                    ToastIcon.Check,
                                    UiText.res(R.string.toast_reset),
                                    UiText.res(R.string.toast_reset_sub),
                                )
                            )
                        },
                        onCancel = { sheet = null },
                    )
                }
            }

            IslandToast(toast)
        }
    }
}

/**
 * Contenedor de pestañas sobre una transición "buscable": permite que el gesto de atrás
 * predictivo de Android arrastre la pantalla actual y revele Inicio siguiendo el dedo.
 */
@Composable
private fun MainTabs(
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    backEnabled: Boolean,
    content: @Composable (Tab) -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val seekState = remember { SeekableTransitionState(tab) }
    val transition = rememberTransition(seekState, label = "tabs")
    val scope = rememberCoroutineScope()

    LaunchedEffect(tab) {
        if (seekState.currentState != tab || seekState.targetState != tab) seekState.animateTo(tab)
    }

    PredictiveBackHandler(enabled = backEnabled && tab != Tab.Home) { events ->
        try {
            events.collect { event -> seekState.seekTo(event.progress, targetState = Tab.Home) }
            seekState.animateTo(Tab.Home)
            onTabChange(Tab.Home)
        } catch (e: CancellationException) {
            scope.launch { seekState.animateTo(seekState.currentState) }
            throw e
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            transition.AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { tabTransition(reduce) },
            ) { current ->
                Box(Modifier.fillMaxSize()) { content(current) }
            }
        }
        TabBar(current = tab, onSelect = onTabChange)
    }
}

/** Push/pop estilo iOS: la nueva pantalla entra completa y la anterior se desplaza un 28% y se atenúa. */
private fun AnimatedContentTransitionScope<Tab>.tabTransition(reduce: Boolean): ContentTransform {
    if (reduce) return fadeIn(tween(150)) togetherWith fadeOut(tween(150))
    val forward = targetState.ordinal > initialState.ordinal
    val slide = tween<androidx.compose.ui.unit.IntOffset>(Motion.NAV_MS, easing = Motion.Navigation)
    val fade = tween<Float>(Motion.NAV_MS, easing = Motion.Navigation)
    return if (forward) {
        (slideInHorizontally(slide) { it } togetherWith
            (slideOutHorizontally(slide) { -it * 28 / 100 } + fadeOut(fade, targetAlpha = 0.45f)))
            .apply { targetContentZIndex = 1f }
    } else {
        ((slideInHorizontally(slide) { -it * 28 / 100 } + fadeIn(fade, initialAlpha = 0.45f)) togetherWith
            slideOutHorizontally(slide) { it })
            .apply { targetContentZIndex = -1f }
    }
}

