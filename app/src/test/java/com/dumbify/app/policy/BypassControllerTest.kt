package com.dumbify.app.policy

import com.dumbify.app.admin.PolicyEnforcer
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Event
import com.dumbify.app.data.entities.EventType
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BypassControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var ruleStore: RuleStore
    private lateinit var pinManager: PinManager
    private lateinit var policyEnforcer: PolicyEnforcer
    private lateinit var grantScheduler: GrantScheduler
    private lateinit var eventDao: EventDao
    private lateinit var clock: FakeClock

    @BeforeEach
    fun setup() {
        ruleStore = mockk()
        pinManager = mockk()
        policyEnforcer = mockk()
        grantScheduler = mockk(relaxed = true)
        eventDao = mockk()
        clock = FakeClock(1_000_000L)

        coEvery { ruleStore.setGrantedUntil(any(), any()) } returns Unit
        coEvery { eventDao.insert(any()) } returns 1L
    }

    /** Create controller using the given scope (should be backgroundScope inside runTest). */
    private fun makeController(scope: CoroutineScope) = BypassController(
        ruleStore = ruleStore,
        pinManager = pinManager,
        policyEnforcer = policyEnforcer,
        grantScheduler = grantScheduler,
        eventDao = eventDao,
        clock = clock,
        scope = scope,
    )

    // ─── helper factories ─────────────────────────────────────────────────────

    private fun delayRule(pkg: String, delaySeconds: Int = 5) = AppRule(
        packageName = pkg,
        isAllowed = false,
        bypassMode = BypassMode.DELAY,
        delaySeconds = delaySeconds,
        grantedUntil = null,
    )

    private fun pinRule(pkg: String) = AppRule(
        packageName = pkg,
        isAllowed = false,
        bypassMode = BypassMode.PIN,
        delaySeconds = 0,
        grantedUntil = null,
    )

    private fun delayAndPinRule(pkg: String, delaySeconds: Int = 5) = AppRule(
        packageName = pkg,
        isAllowed = false,
        bypassMode = BypassMode.DELAY_AND_PIN,
        delaySeconds = delaySeconds,
        grantedUntil = null,
    )

    // ─── tests ────────────────────────────────────────────────────────────────

    @Test
    fun `requestUnblock with no rule (ruleStore_byPkg returns null) transitions to Refused(NO_RULE)`() =
        testScope.runTest {
            val controller = makeController(backgroundScope)
            val pkg = "com.example.blocked"
            coEvery { ruleStore.byPkg(pkg) } returns null

            controller.requestUnblock(pkg, durationMinutes = 30)
            advanceTimeBy(100)

            assertThat(controller.state.value).isInstanceOf(BypassState.Refused::class.java)
            val refused = controller.state.value as BypassState.Refused
            assertThat(refused.pkg).isEqualTo(pkg)
            assertThat(refused.reason).isEqualTo(RefuseReason.NO_RULE)
        }

    @Test
    fun `DELAY mode - after countdown completes state is Granted`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        val delaySeconds = 5
        coEvery { ruleStore.byPkg(pkg) } returns delayRule(pkg, delaySeconds)
        every { policyEnforcer.setPackagesSuspended(any(), any()) } returns emptyList()

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(delaySeconds * 1_000L + 100)

        assertThat(controller.state.value).isInstanceOf(BypassState.Granted::class.java)
        val granted = controller.state.value as BypassState.Granted
        assertThat(granted.pkg).isEqualTo(pkg)
    }

    @Test
    fun `DELAY mode - CountingDown secondsRemaining decrements correctly`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        val delaySeconds = 5
        coEvery { ruleStore.byPkg(pkg) } returns delayRule(pkg, delaySeconds)
        every { policyEnforcer.setPackagesSuspended(any(), any()) } returns emptyList()

        controller.requestUnblock(pkg, durationMinutes = 30)

        // After 1ms: first CountingDown with remaining == delaySeconds
        advanceTimeBy(1)
        val stateAt0 = controller.state.value
        assertThat(stateAt0).isInstanceOf(BypassState.CountingDown::class.java)
        assertThat((stateAt0 as BypassState.CountingDown).secondsRemaining).isEqualTo(delaySeconds)

        // After 1 second: secondsRemaining == 4
        advanceTimeBy(1_000)
        val stateAt1 = controller.state.value
        assertThat(stateAt1).isInstanceOf(BypassState.CountingDown::class.java)
        assertThat((stateAt1 as BypassState.CountingDown).secondsRemaining).isEqualTo(4)

        // After 2 seconds: secondsRemaining == 3
        advanceTimeBy(1_000)
        val stateAt2 = controller.state.value
        assertThat(stateAt2).isInstanceOf(BypassState.CountingDown::class.java)
        assertThat((stateAt2 as BypassState.CountingDown).secondsRemaining).isEqualTo(3)
    }

    @Test
    fun `PIN mode - after requestUnblock state is AwaitingPin`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        coEvery { ruleStore.byPkg(pkg) } returns pinRule(pkg)

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(100)

        assertThat(controller.state.value).isInstanceOf(BypassState.AwaitingPin::class.java)
        val awaiting = controller.state.value as BypassState.AwaitingPin
        assertThat(awaiting.pkg).isEqualTo(pkg)
        assertThat(awaiting.durationMinutes).isEqualTo(30)
    }

    @Test
    fun `PIN mode - correct PIN (PinManager_verify returns SUCCESS) transitions to Granted`() =
        testScope.runTest {
            val controller = makeController(backgroundScope)
            val pkg = "com.example.app"
            coEvery { ruleStore.byPkg(pkg) } returns pinRule(pkg)
            every { pinManager.verify(PinManager.Scope.BYPASS, any()) } returns PinManager.VerifyResult.SUCCESS
            every { policyEnforcer.setPackagesSuspended(any(), any()) } returns emptyList()

            controller.requestUnblock(pkg, durationMinutes = 30)
            advanceTimeBy(100)

            controller.submitPin("1234")
            advanceTimeBy(100)

            assertThat(controller.state.value).isInstanceOf(BypassState.Granted::class.java)
            val granted = controller.state.value as BypassState.Granted
            assertThat(granted.pkg).isEqualTo(pkg)
        }

    @Test
    fun `PIN mode - wrong PIN (PinManager_verify returns WRONG) transitions to PinError(isCooldown=false)`() =
        testScope.runTest {
            val controller = makeController(backgroundScope)
            val pkg = "com.example.app"
            coEvery { ruleStore.byPkg(pkg) } returns pinRule(pkg)
            every { pinManager.verify(PinManager.Scope.BYPASS, any()) } returns PinManager.VerifyResult.WRONG

            controller.requestUnblock(pkg, durationMinutes = 30)
            advanceTimeBy(100)

            controller.submitPin("9999")
            advanceTimeBy(100)

            assertThat(controller.state.value).isInstanceOf(BypassState.PinError::class.java)
            val pinError = controller.state.value as BypassState.PinError
            assertThat(pinError.pkg).isEqualTo(pkg)
            assertThat(pinError.isCooldown).isFalse()
        }

    @Test
    fun `PIN mode - cooldown (PinManager_verify returns COOLDOWN) transitions to PinError(isCooldown=true)`() =
        testScope.runTest {
            val controller = makeController(backgroundScope)
            val pkg = "com.example.app"
            coEvery { ruleStore.byPkg(pkg) } returns pinRule(pkg)
            every { pinManager.verify(PinManager.Scope.BYPASS, any()) } returns PinManager.VerifyResult.COOLDOWN

            controller.requestUnblock(pkg, durationMinutes = 30)
            advanceTimeBy(100)

            controller.submitPin("0000")
            advanceTimeBy(100)

            assertThat(controller.state.value).isInstanceOf(BypassState.PinError::class.java)
            val pinError = controller.state.value as BypassState.PinError
            assertThat(pinError.pkg).isEqualTo(pkg)
            assertThat(pinError.isCooldown).isTrue()
        }

    @Test
    fun `DELAY_AND_PIN - after countdown completes state becomes AwaitingPin`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        val delaySeconds = 5
        coEvery { ruleStore.byPkg(pkg) } returns delayAndPinRule(pkg, delaySeconds)

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(delaySeconds * 1_000L + 100)

        assertThat(controller.state.value).isInstanceOf(BypassState.AwaitingPin::class.java)
        val awaiting = controller.state.value as BypassState.AwaitingPin
        assertThat(awaiting.pkg).isEqualTo(pkg)
    }

    @Test
    fun `cancelRequest while in CountingDown transitions to Idle`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        coEvery { ruleStore.byPkg(pkg) } returns delayRule(pkg, delaySeconds = 10)

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(2_000) // mid-countdown

        assertThat(controller.state.value).isInstanceOf(BypassState.CountingDown::class.java)

        controller.cancelRequest()
        advanceTimeBy(100)

        assertThat(controller.state.value).isInstanceOf(BypassState.Idle::class.java)
    }

    @Test
    fun `grant calls PolicyEnforcer_setPackagesSuspended(pkg, false)`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        coEvery { ruleStore.byPkg(pkg) } returns delayRule(pkg, delaySeconds = 0)
        every { policyEnforcer.setPackagesSuspended(any(), any()) } returns emptyList()

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(100)

        coVerify { policyEnforcer.setPackagesSuspended(listOf(pkg), false) }
    }

    @Test
    fun `grant calls GrantScheduler_scheduleResuspend(pkg, any)`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        coEvery { ruleStore.byPkg(pkg) } returns delayRule(pkg, delaySeconds = 0)
        every { policyEnforcer.setPackagesSuspended(any(), any()) } returns emptyList()

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(100)

        coVerify { grantScheduler.scheduleResuspend(eq(pkg), any()) }
    }

    @Test
    fun `grant calls ruleStore_setGrantedUntil(pkg, any)`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        coEvery { ruleStore.byPkg(pkg) } returns delayRule(pkg, delaySeconds = 0)
        every { policyEnforcer.setPackagesSuspended(any(), any()) } returns emptyList()

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(100)

        coVerify { ruleStore.setGrantedUntil(eq(pkg), any()) }
    }

    @Test
    fun `grant inserts UNBLOCK_GRANTED event in eventDao`() = testScope.runTest {
        val controller = makeController(backgroundScope)
        val pkg = "com.example.app"
        coEvery { ruleStore.byPkg(pkg) } returns delayRule(pkg, delaySeconds = 0)
        every { policyEnforcer.setPackagesSuspended(any(), any()) } returns emptyList()

        controller.requestUnblock(pkg, durationMinutes = 30)
        advanceTimeBy(100)

        val eventSlot = slot<Event>()
        coVerify { eventDao.insert(capture(eventSlot)) }
        assertThat(eventSlot.captured.type).isEqualTo(EventType.UNBLOCK_GRANTED)
        assertThat(eventSlot.captured.packageName).isEqualTo(pkg)
    }
}
