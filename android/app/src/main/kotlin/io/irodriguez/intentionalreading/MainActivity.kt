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
        val repository = (application as IntentionalReadingApplication).container.datasetRepository
        val factory = AppViewModel.Factory(repository)
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = factory)
            IntentionalReadingApp(viewModel = appViewModel)
        }
    }
}
