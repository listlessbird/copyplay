package com.copyplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.copyplay.ui.CopyplayApp
import com.copyplay.ui.CopyplayViewModelFactory
import com.copyplay.ui.theme.CopyplayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CopyplayApplication).container

        setContent {
            CopyplayRoot(container)
        }
    }
}

@Composable
private fun CopyplayRoot(container: CopyplayContainer) {
    CopyplayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            CopyplayApp(
                viewModelFactory = CopyplayViewModelFactory(container),
                appViewModel = viewModel(factory = CopyplayViewModelFactory(container)),
            )
        }
    }
}
