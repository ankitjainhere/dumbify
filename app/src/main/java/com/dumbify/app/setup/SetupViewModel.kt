package com.dumbify.app.setup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbify.app.data.SecurePrefs
import com.dumbify.app.data.SecurePrefsKeys
import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.data.entities.UserRole
import com.dumbify.app.policy.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val label: String,
    val isAlwaysAllowed: Boolean,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val configDao: ConfigDao,
    private val appRuleDao: AppRuleDao,
    private val pinManager: PinManager,
    private val securePrefs: SecurePrefs,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        const val STEP_WELCOME = 0
        const val STEP_MODE = 1
        const val STEP_PINS = 2
        const val STEP_MESSAGE = 3
        const val STEP_APP_PICKER = 4
        const val STEP_BYPASS = 5
        const val STEP_LAUNCHER = 6
        const val STEP_DONE = 7
    }

    var step by mutableStateOf(securePrefs.getInt(SecurePrefsKeys.WIZARD_STEP, 0))
        private set

    var role by mutableStateOf(UserRole.SELF)
    var mode by mutableStateOf(BlockMode.ALLOWLIST)
    var removalPin by mutableStateOf("")
    var bypassPin by mutableStateOf("")
    var customMessage by mutableStateOf("")
    var selectedPkgs by mutableStateOf(emptySet<String>())
        private set
    var alwaysAllowedPkgs by mutableStateOf(emptySet<String>())
        private set
    var installedApps by mutableStateOf(emptyList<AppInfo>())
        private set
    var appsLoading by mutableStateOf(false)
        private set
    var defaultBypassMode by mutableStateOf(BypassMode.DELAY)
    var defaultDelaySeconds by mutableStateOf(30)
    var perAppBypassOverrides by mutableStateOf(emptyMap<String, BypassMode>())
        private set
    var launcherEnabled by mutableStateOf(false)
    var saving by mutableStateOf(false)
        private set

    init {
        if (step == STEP_APP_PICKER || step == STEP_BYPASS) loadApps()
    }

    fun nextStep() {
        val next = (step + 1).coerceAtMost(STEP_DONE)
        step = next
        securePrefs.putInt(SecurePrefsKeys.WIZARD_STEP, next)
        if (next == STEP_APP_PICKER && installedApps.isEmpty()) loadApps()
    }

    fun prevStep() {
        val prev = (step - 1).coerceAtLeast(STEP_WELCOME)
        step = prev
        securePrefs.putInt(SecurePrefsKeys.WIZARD_STEP, prev)
    }

    fun loadApps() {
        if (appsLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            appsLoading = true
            val pm = context.packageManager
            val alwaysAllowed = resolveAlwaysAllowedPkgs(pm)
            val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launcherPkgs = pm.queryIntentActivities(mainIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()
            val relevantPkgs = launcherPkgs + alwaysAllowed
            val apps = pm.getInstalledPackages(0)
                .filter { it.packageName in relevantPkgs }
                .map { pkg ->
                    AppInfo(
                        packageName = pkg.packageName,
                        label = pm.getApplicationLabel(pkg.applicationInfo).toString(),
                        isAlwaysAllowed = pkg.packageName in alwaysAllowed,
                    )
                }
                .sortedBy { it.label.lowercase() }
            alwaysAllowedPkgs = alwaysAllowed
            installedApps = apps
            val appPkgs = apps.map { it.packageName }.toSet()
            selectedPkgs = selectedPkgs.intersect(appPkgs) + alwaysAllowed
            appsLoading = false
        }
    }

    private fun resolveAlwaysAllowedPkgs(pm: PackageManager): Set<String> {
        val result = mutableSetOf(context.packageName)
        fun resolve(intent: Intent): String? =
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        resolve(Intent(Intent.ACTION_DIAL))?.let { result.add(it) }
        resolve(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))?.let { result.add(it) }
        resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE))?.let { result.add(it) }
        resolve(Intent(Settings.ACTION_SETTINGS))?.let { result.add(it) }
        return result
    }

    fun toggleApp(pkg: String) {
        if (pkg in alwaysAllowedPkgs) return
        selectedPkgs = if (pkg in selectedPkgs) selectedPkgs - pkg else selectedPkgs + pkg
    }

    fun applyDefaultBypassToAll() {
        perAppBypassOverrides = emptyMap()
    }

    fun setPerAppBypass(pkg: String, bypassMode: BypassMode) {
        perAppBypassOverrides = perAppBypassOverrides + (pkg to bypassMode)
    }

    fun finishWizard() {
        viewModelScope.launch {
            saving = true
            withContext(Dispatchers.Default) {
                if (removalPin.isNotBlank()) pinManager.setPin(PinManager.Scope.REMOVAL, removalPin)
                if (bypassPin.isNotBlank()) pinManager.setPin(PinManager.Scope.BYPASS, bypassPin)
            }
            val apps = installedApps
            for (app in apps) {
                val isAllowed = app.packageName in selectedPkgs
                val bypass = perAppBypassOverrides[app.packageName] ?: defaultBypassMode
                appRuleDao.upsert(
                    AppRule(
                        packageName = app.packageName,
                        isAllowed = isAllowed,
                        bypassMode = bypass,
                        delaySeconds = if (bypass == BypassMode.PIN) 0 else defaultDelaySeconds,
                        grantedUntil = null,
                    )
                )
            }
            securePrefs.remove(SecurePrefsKeys.WIZARD_STEP)
            // Write config last — triggers the onboardingComplete Flow which routes away from wizard
            configDao.upsert(
                Config(
                    mode = mode,
                    userRole = role,
                    customMessage = customMessage,
                    launcherEnabled = launcherEnabled,
                    onboardingComplete = true,
                )
            )
        }
    }
}
