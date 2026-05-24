package com.dumbify.app.ui.home

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbify.app.admin.PolicyEnforcer
import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.policy.PinManager
import com.dumbify.app.policy.PinManager.VerifyResult
import com.dumbify.app.policy.RuleStore
import com.dumbify.app.ui.common.PinPromptState
import com.dumbify.app.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RuleUiItem(
    val packageName: String,
    val displayName: String,
    val appIcon: Drawable?,
    val isAllowed: Boolean,
    val bypassMode: BypassMode,
    val delaySeconds: Int,
    val grantedUntil: Long?,
)

data class HomeUiState(
    val rules: List<RuleUiItem> = emptyList(),
    val config: Config? = null,
    val isDeviceOwner: Boolean = false,
    val loading: Boolean = true,
    val editingRule: RuleUiItem? = null,
    val pinPrompt: PinPromptState? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRuleDao: AppRuleDao,
    private val configDao: ConfigDao,
    private val ruleStore: RuleStore,
    private val pinManager: PinManager,
    private val policyEnforcer: PolicyEnforcer,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var pendingPinAction: (() -> Unit)? = null
    private var appLabels: Map<String, String> = emptyMap()
    private var appIcons: Map<String, Drawable> = emptyMap()

    init {
        viewModelScope.launch(Dispatchers.IO) { loadAppMetadata() }
        viewModelScope.launch {
            combine(appRuleDao.observeAll(), configDao.observe()) { rules, config ->
                val now = clock.nowMillis()
                val items = rules.map { rule ->
                    RuleUiItem(
                        packageName  = rule.packageName,
                        displayName  = appLabels[rule.packageName] ?: rule.packageName,
                        appIcon      = appIcons[rule.packageName],
                        isAllowed    = rule.isAllowed,
                        bypassMode   = rule.bypassMode,
                        delaySeconds = rule.delaySeconds,
                        grantedUntil = rule.grantedUntil?.takeIf { it > now },
                    )
                }.sortedWith(compareBy({ !it.isAllowed }, { it.displayName.lowercase() }))
                Triple(items, config, policyEnforcer.isDeviceOwner())
            }.collect { (items, config, isOwner) ->
                _uiState.update {
                    it.copy(rules = items, config = config, isDeviceOwner = isOwner, loading = false)
                }
            }
        }
    }

    private fun loadAppMetadata() {
        val pm = context.packageManager
        val labels = mutableMapOf<String, String>()
        val icons  = mutableMapOf<String, Drawable>()
        try {
            pm.getInstalledPackages(0).forEach { pkg ->
                labels[pkg.packageName] = pm.getApplicationLabel(pkg.applicationInfo).toString()
                runCatching { icons[pkg.packageName] = pm.getApplicationIcon(pkg.packageName) }
            }
        } catch (_: Exception) {}
        appLabels = labels
        appIcons  = icons
    }

    fun openEditSheet(rule: RuleUiItem) = _uiState.update { it.copy(editingRule = rule) }
    fun dismissSheet()                   = _uiState.update { it.copy(editingRule = null) }

    fun dismissPinDialog() {
        pendingPinAction = null
        _uiState.update { it.copy(pinPrompt = null) }
    }

    fun saveEdit(old: RuleUiItem, new: RuleUiItem) {
        if (isWeakeningEdit(old, new)) {
            showPinPrompt("Enter Removal PIN", "Required to reduce app protection.") {
                applyEdit(new)
            }
        } else {
            applyEdit(new)
        }
    }

    fun onDirectToggle(rule: RuleUiItem) {
        if (!rule.isAllowed) {
            // blocked → allowed = weakening: gate with PIN
            showPinPrompt("Enter Removal PIN", "Required to allow this app.") {
                applyEdit(rule.copy(isAllowed = true))
            }
        } else {
            applyEdit(rule.copy(isAllowed = false))
        }
    }

    fun submitPin(pin: String) {
        val prompt = _uiState.value.pinPrompt ?: return
        viewModelScope.launch(Dispatchers.Default) {
            when (pinManager.verify(PinManager.Scope.REMOVAL, pin)) {
                VerifyResult.SUCCESS, VerifyResult.NOT_SET -> {
                    pendingPinAction?.invoke()
                    pendingPinAction = null
                    _uiState.update { it.copy(pinPrompt = null) }
                }
                VerifyResult.WRONG -> {
                    val remaining = (prompt.attemptsRemaining - 1).coerceAtLeast(0)
                    _uiState.update { it.copy(pinPrompt = prompt.copy(attemptsRemaining = remaining)) }
                }
                VerifyResult.COOLDOWN -> {
                    val mins = pinManager.cooldownRemainingMinutes()
                    _uiState.update { it.copy(pinPrompt = prompt.copy(lockedForMinutes = mins)) }
                }
            }
        }
    }

    private fun showPinPrompt(title: String, subtitle: String, action: () -> Unit) {
        pendingPinAction = action
        _uiState.update { it.copy(pinPrompt = PinPromptState(title, subtitle)) }
    }

    private fun applyEdit(item: RuleUiItem) {
        viewModelScope.launch {
            val existing = _uiState.value.rules.find { it.packageName == item.packageName }
            ruleStore.upsert(
                AppRule(
                    packageName  = item.packageName,
                    isAllowed    = item.isAllowed,
                    bypassMode   = item.bypassMode,
                    delaySeconds = item.delaySeconds,
                    grantedUntil = existing?.grantedUntil,
                )
            )
            _uiState.update { it.copy(editingRule = null) }
        }
    }
}

/** Package-internal so unit tests can access it directly. */
internal fun isWeakeningEdit(old: RuleUiItem, new: RuleUiItem): Boolean {
    if (!old.isAllowed && new.isAllowed) return true
    if (new.delaySeconds < old.delaySeconds) return true
    val strength = mapOf(BypassMode.DELAY to 0, BypassMode.PIN to 1, BypassMode.DELAY_AND_PIN to 2)
    return (strength[new.bypassMode] ?: 0) < (strength[old.bypassMode] ?: 0)
}
