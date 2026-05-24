package com.dumbify.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbify.app.data.dao.ConfigDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class Screen { HOME, SETTINGS }

@HiltViewModel
class MainViewModel @Inject constructor(
    configDao: ConfigDao,
) : ViewModel() {

    // null = loading; false = not onboarded; true = onboarded
    val onboardingComplete: StateFlow<Boolean?> = configDao.observe()
        .map { config -> config?.onboardingComplete ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var currentScreen by mutableStateOf(Screen.HOME)
        private set

    fun openSettings() { currentScreen = Screen.SETTINGS }
    fun navigateBack() { currentScreen = Screen.HOME }
}
