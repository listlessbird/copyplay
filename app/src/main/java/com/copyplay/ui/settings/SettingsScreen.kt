package com.copyplay.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.copyplay.domain.playback.DecoderExtensionMode
import com.copyplay.domain.playback.PlaybackCompatibilityPolicy

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    val savedMessage = state.savedMessage
    val compatibilitySettings = PlaybackCompatibilityPolicy.defaultSettings()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.baseUrl,
            onValueChange = viewModel::updateBaseUrl,
            enabled = !state.isSaving,
            singleLine = true,
            label = { Text("Copyparty URL") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            supportingText = {
                when {
                    errorMessage != null -> Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                    )

                    savedMessage != null -> Text(
                        text = savedMessage,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Autoplay next",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Continue to the next video in the current folder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = state.autoplayNext,
                onCheckedChange = viewModel::setAutoplayNext,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Playback compatibility",
                style = MaterialTheme.typography.titleMedium,
            )
            SettingsFact(
                label = "Decoder priority",
                value = compatibilitySettings.decoderExtensionMode.label,
            )
            SettingsFact(
                label = "Decoder fallback",
                value = if (compatibilitySettings.enableDecoderFallback) "On" else "Off",
            )
            SettingsFact(
                label = "Native FFmpeg",
                value = if (compatibilitySettings.includesNativeFfmpegExtension) "Bundled" else "Not bundled",
            )
            SettingsFact(
                label = "Subtitle style",
                value = "Android caption settings",
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator()
                }
                Button(
                    enabled = !state.isSaving,
                    onClick = viewModel::save,
                ) {
                    Text(if (state.isSaving) "Checking" else "Save")
                }
            }
        }
    }
}

@Composable
private fun SettingsFact(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private val DecoderExtensionMode.label: String
    get() = when (this) {
        DecoderExtensionMode.PlatformOnly -> "Platform only"
        DecoderExtensionMode.UseAfterPlatform -> "Platform first, extensions after"
        DecoderExtensionMode.PreferExtensions -> "Extensions preferred"
    }
