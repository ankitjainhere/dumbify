package com.dumbify.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbify.app.admin.PolicyEnforcer
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.data.entities.Event
import com.dumbify.app.policy.PinManager
import com.dumbify.app.policy.PinManager.VerifyResult
import com.dumbify.app.ui.common.PinPromptState
import com.dumbify.app.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val config: Config? = null,
    val auditEvents: List<Event> = emptyList(),
    val auditExpanded: Boolean = false,
    val pinPrompt: PinPromptState? = null,
)

/** Controls the two-step PIN-change flow without leaking lambdas into UiState. */
private enum class PinMode { VERIFY, SET_NEW }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configDao: ConfigDao,
    private val eventDao: EventDao,
    private val pinManager: PinManager,
    private val policyEnforcer: PolicyEnforcer,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private var pendingAction: (() -> Unit)? = null
    private var pinMode      = PinMode.VERIFY
    private var pinSetScope  : PinManager.Scope? = null

    init {
        viewModelScope.launch { configDao.observe().collect { c -> _uiState.update { it.copy(config = c) } } }
        viewModelScope.launch {
            _uiState.update { it.copy(auditEvents = eventDao.recent(50)) }
        }
    }

    fun toggleAuditLog() = _uiState.update { it.copy(auditExpanded = !it.auditExpanded) }

    fun dismissPinDialog() {
        pendingAction = null
        pinMode       = PinMode.VERIFY
        pinSetScope   = null
        _uiState.update { it.copy(pinPrompt = null) }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    fun changeMode(newMode: BlockMode) {
        gateWithRemovalPin("Enter Removal PIN", "Required to change block mode.") {
            viewModelScope.launch {
                val cfg = _uiState.value.config ?: return@launch
                if (cfg.mode != newMode) configDao.upsert(cfg.copy(mode = newMode))
            }
        }
    }

    fun startChangePin(scope: PinManager.Scope) {
        val (verifyTitle, verifySubtitle) = when (scope) {
            PinManager.Scope.REMOVAL -> "Verify Removal PIN" to "Enter your current Removal PIN."
            PinManager.Scope.BYPASS  -> "Verify Bypass PIN"  to "Enter your current Bypass PIN."
        }
        gateWithPin(scope, verifyTitle, verifySubtitle) {
            // Old PIN verified — now collect new PIN
            val (newTitle, _) = when (scope) {
                PinManager.Scope.REMOVAL -> "New Removal PIN" to ""
                PinManager.Scope.BYPASS  -> "New Bypass PIN"  to ""
            }
            pinMode     = PinMode.SET_NEW
            pinSetScope = scope
            _uiState.update { it.copy(pinPrompt = PinPromptState(newTitle, "Enter your new PIN.")) }
        }
    }

    fun startRemoval() {
        gateWithRemovalPin("Enter Removal PIN", "Required to remove Dumbify.") {
            policyEnforcer.clearAllRestrictionsForRemoval()
            policyEnforcer.clearDeviceOwner()
            runCatching {
                val intent = Intent(Intent.ACTION_DELETE,
                    Uri.parse("package:${context.packageName}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    // ── PIN submission ────────────────────────────────────────────────────────

    fun submitPin(pin: String) {
        when (pinMode) {
            PinMode.VERIFY  -> verifyAndRun(pin)
            PinMode.SET_NEW -> setNewPin(pin)
        }
    }

    private fun verifyAndRun(pin: String) {
        val prompt = _uiState.value.pinPrompt ?: return
        val scope  = pinSetScope ?: PinManager.Scope.REMOVAL
        viewModelScope.launch {
            when (pinManager.verify(scope, pin)) {
                VerifyResult.SUCCESS, VerifyResult.NOT_SET -> {
                    pendingAction?.invoke()
                    pendingAction = null
                    if (pinMode == PinMode.VERIFY) {     // may have been flipped to SET_NEW by pendingAction
                        _uiState.update { it.copy(pinPrompt = null) }
                        pinSetScope = null
                    }
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

    private fun setNewPin(pin: String) {
        val scope = pinSetScope ?: return
        viewModelScope.launch {
            pinManager.setPin(scope, pin)
            pinMode     = PinMode.VERIFY
            pinSetScope = null
            _uiState.update { it.copy(pinPrompt = null) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gateWithRemovalPin(title: String, subtitle: String, action: () -> Unit) =
        gateWithPin(PinManager.Scope.REMOVAL, title, subtitle, action)

    private fun gateWithPin(scope: PinManager.Scope, title: String, subtitle: String, action: () -> Unit) {
        pendingAction = action
        pinMode       = PinMode.VERIFY
        pinSetScope   = scope
        _uiState.update { it.copy(pinPrompt = PinPromptState(title, subtitle)) }
    }
}
