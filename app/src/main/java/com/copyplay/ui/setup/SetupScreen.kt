package com.copyplay.ui.setup

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.copyplay.domain.server.SavedServerHost

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onConnected: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Copyplay",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Connect one copyparty server and browse video files directly.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Server",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "HTTP and HTTPS are both supported for private Tailnet use.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.baseUrl,
                    onValueChange = viewModel::updateBaseUrl,
                    enabled = !state.isValidating,
                    singleLine = true,
                    label = { Text("Copyparty URL") },
                    placeholder = { Text("http://copybox.tailnet.local:3923") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    supportingText = state.errorMessage?.let { message ->
                        { Text(message, color = MaterialTheme.colorScheme.error) }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.isValidating) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Button(
                        enabled = !state.isValidating,
                        onClick = { viewModel.connect(onConnected) },
                    ) {
                        Text(if (state.isValidating) "Checking" else "Connect")
                    }
                }
            }
        }
        if (state.savedServers.isNotEmpty()) {
            PreviousServers(
                savedServers = state.savedServers,
                enabled = !state.isValidating,
                onSelect = viewModel::selectSavedServer,
            )
        }
    }
}

@Composable
private fun PreviousServers(
    savedServers: List<SavedServerHost>,
    enabled: Boolean,
    onSelect: (SavedServerHost) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Previous servers",
            style = MaterialTheme.typography.titleMedium,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(savedServers, key = { it.baseUrl }) { savedServer ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    onClick = { onSelect(savedServer) },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = savedServer.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = savedServer.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
