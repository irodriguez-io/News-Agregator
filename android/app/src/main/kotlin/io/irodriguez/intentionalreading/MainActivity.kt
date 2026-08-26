package io.irodriguez.intentionalreading

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.irodriguez.intentionalreading.ui.AppViewModel
import io.irodriguez.intentionalreading.ui.IntentionalReadingApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as IntentionalReadingApplication).container
        val factory = AppViewModel.Factory(
            datasetRepository = container.datasetRepository,
            localStateRepository = container.localStateRepository,
            applyNightMode = container.applyNightMode,
        )
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = factory)
            IntentionalReadingApp(viewModel = appViewModel)
        }
    }
}
