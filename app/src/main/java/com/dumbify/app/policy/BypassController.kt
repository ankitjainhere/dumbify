package com.dumbify.app.policy

import android.util.Log
import com.dumbify.app.admin.PolicyEnforcer
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Event
import com.dumbify.app.data.entities.EventType
import com.dumbify.app.di.ApplicationScope
import com.dumbify.app.util.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class BypassState {
    object Idle : BypassState()
    data class CountingDown(
        val pkg: String,
        val secondsRemaining: Int,
        val totalSeconds: Int,
        val durationMinutes: Int,
    ) : BypassState()
    data class AwaitingPin(val pkg: String, val durationMinutes: Int) : BypassState()
    data class Granted(val pkg: String, val until: Long) : BypassState()
    data class Refused(val pkg: String, val reason: RefuseReason) : BypassState()
    data class PinError(val pkg: String, val durationMinutes: Int, val isCooldown: Boolean) : BypassState()
}

enum class RefuseReason { NO_RULE, GRANT_FAILED }

@Singleton
class BypassController @Inject constructor(
    private val ruleStore: RuleStore,
    private val pinManager: PinManager,
    private val policyEnforcer: PolicyEnforcer,
    private val grantScheduler: GrantScheduler,
    private val eventDao: EventDao,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<BypassState>(BypassState.Idle)
    val state: StateFlow<BypassState> get() = _state

    @Volatile private var activeJob: Job? = null

    fun requestUnblock(pkg: String, durationMinutes: Int) {
        activeJob?.cancel()
        activeJob = scope.launch {
            val rule = ruleStore.byPkg(pkg)
            if (rule == null) {
                _state.value = BypassState.Refused(pkg, RefuseReason.NO_RULE)
                return@launch
            }

            when (rule.bypassMode) {
                BypassMode.DELAY -> {
                    runCountdown(pkg, rule.delaySeconds, durationMinutes)
                    grant(pkg, durationMinutes)
                }
                BypassMode.PIN -> {
                    _state.value = BypassState.AwaitingPin(pkg, durationMinutes)
                }
                BypassMode.DELAY_AND_PIN -> {
                    runCountdown(pkg, rule.delaySeconds, durationMinutes)
                    _state.value = BypassState.AwaitingPin(pkg, durationMinutes)
                }
            }
        }
    }

    fun submitPin(pin: String) {
        val current = _state.value
        if (current !is BypassState.AwaitingPin) return

        val result = pinManager.verify(PinManager.Scope.BYPASS, pin)
        when (result) {
            PinManager.VerifyResult.SUCCESS -> {
                activeJob?.cancel()
                activeJob = scope.launch {
                    grant(current.pkg, current.durationMinutes)
                }
            }
            PinManager.VerifyResult.WRONG -> {
                _state.value = BypassState.PinError(current.pkg, current.durationMinutes, isCooldown = false)
            }
            PinManager.VerifyResult.COOLDOWN -> {
                _state.value = BypassState.PinError(current.pkg, current.durationMinutes, isCooldown = true)
            }
            PinManager.VerifyResult.NOT_SET -> {
                // No pin configured; grant directly
                activeJob?.cancel()
                activeJob = scope.launch {
                    grant(current.pkg, current.durationMinutes)
                }
            }
        }
    }

    fun cancelRequest() {
        activeJob?.cancel()
        activeJob = null
        _state.value = BypassState.Idle
    }

    private suspend fun runCountdown(pkg: String, delaySeconds: Int, durationMinutes: Int) {
        for (remaining in delaySeconds downTo 1) {
            _state.value = BypassState.CountingDown(pkg, remaining, delaySeconds, durationMinutes)
            delay(1_000L)
        }
    }

    private suspend fun grant(pkg: String, durationMinutes: Int) {
        try {
            val until = clock.nowMillis() + durationMinutes * 60_000L
            ruleStore.setGrantedUntil(pkg, until)
            policyEnforcer.setPackagesSuspended(listOf(pkg), false)
            grantScheduler.scheduleResuspend(pkg, until)
            eventDao.insert(
                Event(
                    timestamp = clock.nowMillis(),
                    type = EventType.UNBLOCK_GRANTED,
                    packageName = pkg,
                    detail = "duration=${durationMinutes}m",
                )
            )
            _state.value = BypassState.Granted(pkg, until)
        } catch (e: Exception) {
            Log.e("BypassController", "grant() failed for $pkg", e)
            _state.value = BypassState.Refused(pkg, RefuseReason.GRANT_FAILED)
        }
    }
}
