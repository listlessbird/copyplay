package com.copyplay.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Server and playback behavior",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Copyparty host",
                    style = MaterialTheme.typography.titleMedium,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.width(16.dp))
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
            Switch(
                checked = state.autoplayNext,
                onCheckedChange = viewModel::setAutoplayNext,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                Text(
                    text = "Format support still depends on device codecs unless a future build bundles native FFmpeg.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val DecoderExtensionMode.label: String
    get() = when (this) {
        DecoderExtensionMode.PlatformOnly -> "Platform only"
        DecoderExtensionMode.UseAfterPlatform -> "Platform first, extensions after"
        DecoderExtensionMode.PreferExtensions -> "Extensions preferred"
    }
