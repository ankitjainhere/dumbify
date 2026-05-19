package com.dumbify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbify.app.setup.SetupWizardScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (onboardingComplete) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        false -> SetupWizardScreen()
                        true -> HomeScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Text(
        text = "Dumbify — home (M6)",
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
        style = MaterialTheme.typography.headlineSmall,
    )
}
