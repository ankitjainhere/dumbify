package com.dumbify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbify.app.setup.SetupWizardScreen
import com.dumbify.app.ui.home.HomeScreen
import com.dumbify.app.ui.settings.SettingsScreen
import com.dumbify.app.ui.theme.DumbifyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DumbifyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
                    val currentScreen = viewModel.currentScreen

                    BackHandler(enabled = currentScreen == Screen.SETTINGS) {
                        viewModel.navigateBack()
                    }

                    when (onboardingComplete) {
                        null  -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        false -> SetupWizardScreen()
                        true  -> when (currentScreen) {
                            Screen.HOME     -> HomeScreen(onNavigateToSettings = { viewModel.openSettings() })
                            Screen.SETTINGS -> SettingsScreen(onBack = { viewModel.navigateBack() })
                        }
                    }
                }
            }
        }
    }
}
