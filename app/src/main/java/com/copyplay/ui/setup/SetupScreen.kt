package com.copyplay.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onConnected: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Copyplay",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Connect one copyparty server to start browsing videos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
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
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isValidating) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
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
