package com.copyplay.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.copyplay.domain.server.SavedServerHost
import com.copyplay.domain.server.TailscaleStatus

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onConnected: () -> Unit,
    onOpenTailscale: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.connectedBaseUrl) {
        if (state.connectedBaseUrl != null) onConnected()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Find your videos", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Copyplay looks for your private Copyparty server over Tailscale.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "discovery") {
            DiscoveryCard(
                state = state,
                onRetry = viewModel::retryDiscovery,
                onOpenTailscale = onOpenTailscale,
                onShowManualEntry = viewModel::showManualEntry,
            )
        }

        if (state.showManualEntry) {
            item(key = "manual") {
                ManualConnectionCard(
                    state = state,
                    onValueChange = viewModel::updateBaseUrl,
                    onConnect = viewModel::connect,
                )
            }
        }

        if (state.savedServers.isNotEmpty()) {
            item(key = "previous-heading") {
                Text("Connected before", style = MaterialTheme.typography.titleMedium)
            }
            items(state.savedServers, key = { it.baseUrl }) { savedServer ->
                PreviousServerRow(
                    savedServer = savedServer,
                    enabled = !state.isValidating,
                    onSelect = viewModel::selectSavedServer,
                )
            }
        }
    }
}

@Composable
private fun DiscoveryCard(
    state: SetupUiState,
    onRetry: () -> Unit,
    onOpenTailscale: () -> Unit,
    onShowManualEntry: () -> Unit,
) {
    val presentation = discoveryPresentation(state)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = presentation.iconBackground,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (presentation.showProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = presentation.iconTint,
                            )
                        } else {
                            Icon(
                                imageVector = presentation.icon,
                                contentDescription = null,
                                tint = presentation.iconTint,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(presentation.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = presentation.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                state.tailscaleStatus is TailscaleStatus.NotInstalled -> {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenTailscale) {
                        Text("Get Tailscale")
                    }
                }

                state.tailscaleStatus is TailscaleStatus.Disconnected -> {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenTailscale) {
                        Text("Open Tailscale")
                    }
                }

                state.discoveryState == SetupDiscoveryState.NotFound -> {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Search again")
                    }
                }
            }

            if (!state.showManualEntry &&
                state.discoveryState !in listOf(
                    SetupDiscoveryState.Searching,
                    SetupDiscoveryState.Connecting,
                    SetupDiscoveryState.Connected,
                )
            ) {
                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = onShowManualEntry,
                ) {
                    Text("Enter server address")
                }
            }
        }
    }
}

@Composable
private fun ManualConnectionCard(
    state: SetupUiState,
    onValueChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Connect manually", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Use this when your server has a custom name, port, or path.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.baseUrl,
                onValueChange = onValueChange,
                enabled = !state.isValidating,
                singleLine = true,
                label = { Text("Copyparty address") },
                placeholder = { Text("http://media-box:3923") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                isError = state.errorMessage != null,
                supportingText = state.errorMessage?.let { message -> { Text(message) } },
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isValidating && state.baseUrl.isNotBlank(),
                onClick = onConnect,
            ) {
                if (state.isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Checking server")
                } else {
                    Text("Connect to server")
                }
            }
        }
    }
}

@Composable
private fun PreviousServerRow(
    savedServer: SavedServerHost,
    enabled: Boolean,
    onSelect: (SavedServerHost) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onSelect(savedServer) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = savedServer.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
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
            Text(
                text = "Connect",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun discoveryPresentation(state: SetupUiState): DiscoveryPresentation = when {
    state.tailscaleStatus == null -> DiscoveryPresentation(
        title = "Checking your network",
        body = "This should only take a moment.",
        icon = Icons.Rounded.Search,
        showProgress = true,
    )

    state.tailscaleStatus is TailscaleStatus.NotInstalled -> DiscoveryPresentation(
        title = "Tailscale isn't installed",
        body = "Install Tailscale to reach your private server without exposing it to the internet.",
        icon = Icons.Rounded.CloudOff,
    )

    state.tailscaleStatus is TailscaleStatus.Disconnected -> DiscoveryPresentation(
        title = "Tailscale is disconnected",
        body = "Connect to your tailnet, then return here. Copyplay will resume automatically.",
        icon = Icons.Rounded.CloudOff,
    )

    state.discoveryState == SetupDiscoveryState.Searching -> DiscoveryPresentation(
        title = "Looking for Copyparty",
        body = "Searching for the copyparty service on your tailnet.",
        icon = Icons.Rounded.Search,
        showProgress = true,
    )

    state.discoveryState == SetupDiscoveryState.NotFound -> DiscoveryPresentation(
        title = "No Copyparty service found",
        body = "Advertise svc:copyparty from your server, or connect with its address.",
        icon = Icons.Rounded.Search,
    )

    state.discoveryState == SetupDiscoveryState.Connecting -> DiscoveryPresentation(
        title = state.discoveredServerLabel ?: "Copyparty found",
        body = "Checking the server before opening your library.",
        icon = Icons.Rounded.CheckCircle,
        showProgress = true,
    )

    else -> DiscoveryPresentation(
        title = state.discoveredServerLabel ?: "Copyparty connected",
        body = "Opening your library.",
        icon = Icons.Rounded.CheckCircle,
    )
}

private data class DiscoveryPresentation(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val showProgress: Boolean = false,
    val iconBackground: Color = Color(0xFF20352F),
    val iconTint: Color = Color(0xFF4FE0B4),
)
