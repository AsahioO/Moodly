package com.asahioo.moodly.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asahioo.moodly.R
import com.asahioo.moodly.ui.components.PrimaryButton
import com.asahioo.moodly.ui.theme.MoodType
import com.asahioo.moodly.ui.theme.MoodlyTheme
import com.asahioo.moodly.ui.theme.Palette

/**
 * Explica qué datos de salud lee Moodly y para qué. Health Connect la abre desde su diálogo de
 * permisos ("política de privacidad") y desde sus ajustes; sin ella no muestra el diálogo.
 */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoodlyTheme {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Palette.Paper)
                        .systemBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    Text(stringResource(R.string.health_rationale_title), style = MoodType.SheetTitle)
                    Text(
                        stringResource(R.string.health_rationale_body),
                        style = MoodType.Body,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                    )
                    PrimaryButton(stringResource(R.string.health_rationale_ok), onClick = ::finish)
                }
            }
        }
    }
}
