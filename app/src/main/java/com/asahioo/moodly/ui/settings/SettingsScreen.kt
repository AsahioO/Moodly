package com.asahioo.moodly.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asahioo.moodly.R
import com.asahioo.moodly.data.health.HealthStatus
import com.asahioo.moodly.data.model.Settings
import com.asahioo.moodly.ui.components.AppIcons
import com.asahioo.moodly.ui.components.Avatar
import com.asahioo.moodly.ui.components.Glyph
import com.asahioo.moodly.ui.components.MoodSwitch
import com.asahioo.moodly.ui.components.NavHeader
import com.asahioo.moodly.ui.components.PanelShape
import com.asahioo.moodly.ui.components.SectionTitle
import com.asahioo.moodly.ui.components.bounceClick
import com.asahioo.moodly.ui.components.rememberStagger
import com.asahioo.moodly.ui.components.staggered
import com.asahioo.moodly.ui.labelRes
import com.asahioo.moodly.ui.monthName
import com.asahioo.moodly.ui.timeOfDay
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.Motion
import com.asahioo.moodly.ui.theme.Palette
import java.time.LocalDate

@Composable
fun SettingsScreen(
    userName: String,
    trackingSince: LocalDate?,
    settings: Settings,
    onBack: () -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onPickReminderTime: () -> Unit,
    onPickTheme: () -> Unit,
    health: HealthStatus,
    onHealthChange: (Boolean) -> Unit,
    onInstallHealth: () -> Unit,
    customTagCount: Int,
    onManageTags: () -> Unit,
    onReplayIntro: () -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetData: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val pop = rememberStagger(Unit, totalMs = 1000)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.Night),
    ) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewport)
                    .clip(PanelShape)
                    .background(Palette.Paper)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 22.dp),
            ) {
                NavHeader(
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_sub),
                    onBack = onBack,
                )
                Row(
                    Modifier
                        .padding(top = 22.dp)
                        .staggered(pop, 40, 600, 20.dp, 0.96f, Motion.Navigation)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .bounceClick(onClick = onPickAvatar, pressedScale = 0.98f)
                        .background(Palette.Lime)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(settings.avatarFile, Modifier.size(60.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        if (userName.isNotBlank()) {
                            Text(userName, style = MoodType.Base.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold))
                        }
                        if (trackingSince != null) {
                            Text(
                                stringResource(R.string.checking_since, monthName(trackingSince.monthValue), trackingSince.year),
                                style = MoodType.Base.copy(fontSize = 12.sp, color = Palette.Ink.copy(alpha = 0.6f)),
                            )
                        }
                    }
                }

                SectionTitle(stringResource(R.string.preferences), Modifier.staggered(pop, 120, 500, 12.dp))
                Group(Modifier.staggered(pop, 160, 600, 20.dp, easing = Motion.Navigation)) {
                    ToggleRow(
                        title = stringResource(R.string.haptics),
                        subtitle = stringResource(R.string.haptics_sub),
                        checked = settings.haptics,
                        onChange = onHapticsChange,
                    )
                    Divider()
                    ToggleRow(
                        title = stringResource(R.string.reduce_motion),
                        subtitle = stringResource(R.string.reduce_motion_sub),
                        checked = settings.reduceMotion,
                        onChange = onReduceMotionChange,
                    )
                    Divider()
                    ToggleRow(
                        title = stringResource(R.string.app_lock),
                        subtitle = stringResource(R.string.app_lock_sub),
                        checked = settings.appLock,
                        onChange = onAppLockChange,
                    )
                    Divider()
                    ActionRow(
                        title = stringResource(R.string.theme_setting),
                        color = Palette.Ink,
                        onClick = onPickTheme,
                        value = stringResource(settings.theme.labelRes),
                    )
                }

                SectionTitle(stringResource(R.string.reminder_section), Modifier.staggered(pop, 200, 500, 12.dp))
                Group(Modifier.staggered(pop, 240, 600, 20.dp, easing = Motion.Navigation)) {
                    ToggleRow(
                        title = stringResource(R.string.reminder_daily),
                        subtitle = stringResource(R.string.reminder_daily_sub),
                        checked = settings.reminderEnabled,
                        onChange = onReminderChange,
                    )
                    Divider()
                    ActionRow(
                        title = stringResource(R.string.reminder_time),
                        color = Palette.Ink,
                        onClick = onPickReminderTime,
                        value = timeOfDay(settings.reminderMinutes),
                    )
                }

                SectionTitle(stringResource(R.string.health_section), Modifier.staggered(pop, 280, 500, 12.dp))
                Group(Modifier.staggered(pop, 320, 600, 20.dp, easing = Motion.Navigation)) {
                    when (health) {
                        HealthStatus.Unavailable -> Text(
                            stringResource(R.string.health_unavailable),
                            style = MoodType.Caption,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        )
                        HealthStatus.NeedsUpdate -> ActionRow(stringResource(R.string.health_update), Palette.Ink, onInstallHealth)
                        HealthStatus.Disconnected, is HealthStatus.Connected -> ToggleRow(
                            title = stringResource(R.string.health_connect),
                            subtitle = stringResource(
                                when {
                                    health !is HealthStatus.Connected -> R.string.health_sub_off
                                    health.sleep && health.steps -> R.string.health_sub_both
                                    health.sleep -> R.string.health_sub_sleep
                                    else -> R.string.health_sub_steps
                                }
                            ),
                            checked = health is HealthStatus.Connected,
                            onChange = onHealthChange,
                        )
                    }
                }

                SectionTitle(stringResource(R.string.app_section), Modifier.staggered(pop, 320, 500, 12.dp))
                Group(Modifier.staggered(pop, 360, 600, 20.dp, easing = Motion.Navigation)) {
                    ActionRow(
                        title = stringResource(R.string.tags_setting),
                        color = Palette.Ink,
                        onClick = onManageTags,
                        value = customTagCount.takeIf { it > 0 }?.toString(),
                    )
                    Divider()
                    ActionRow(stringResource(R.string.replay_intro), Palette.Ink, onReplayIntro)
                    Divider()
                    ActionRow(stringResource(R.string.backup_export), Palette.Ink, onExportBackup)
                    Divider()
                    ActionRow(stringResource(R.string.backup_import), Palette.Ink, onImportBackup)
                    Divider()
                    ActionRow(stringResource(R.string.reset_data), Palette.Danger, onResetData)
                }

                Text(
                    stringResource(R.string.footer_local),
                    style = MoodType.Base.copy(fontSize = 11.5.sp, color = Palette.Grey),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun Group(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Mist),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(start = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Palette.Divider),
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MoodType.Base.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium))
            Text(subtitle, style = MoodType.Caption)
        }
        MoodSwitch(checked = checked, onCheckedChange = onChange, contentDescription = title)
    }
}

@Composable
private fun ActionRow(title: String, color: Color, onClick: () -> Unit, value: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick, pressedScale = 0.98f)
            .heightIn(min = 58.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MoodType.Base.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = color),
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(value, style = MoodType.Base.copy(fontSize = 14.5.sp, color = Palette.Grey2))
            Spacer(Modifier.width(6.dp))
        }
        Glyph(AppIcons.ChevronRight, Modifier.size(18.dp), tint = Color(0xFFB4B4B9))
    }
}

