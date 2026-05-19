package com.dumbify.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbify.app.data.dao.ConfigDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    configDao: ConfigDao,
) : ViewModel() {

    // null = still loading; false = not onboarded; true = onboarded
    val onboardingComplete: StateFlow<Boolean?> = configDao.observe()
        .map { config -> config?.onboardingComplete ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
