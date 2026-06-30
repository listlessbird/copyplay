package com.copyplay.ui.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copyplay.domain.server.ServerConfig

@Composable
fun BrowserPlaceholderScreen(
    configuredServer: ServerConfig?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Browser",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = configuredServer?.baseUrl.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Folder browsing starts in the next issue slice.",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
