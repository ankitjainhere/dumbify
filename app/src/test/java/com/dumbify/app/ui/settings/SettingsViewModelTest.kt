package com.dumbify.app.ui.settings

import android.content.Context
import com.dumbify.app.admin.PolicyEnforcer
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.data.entities.UserRole
import com.dumbify.app.policy.PinManager
import com.dumbify.app.policy.PinManager.VerifyResult
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope      = TestScope(testDispatcher)

    private lateinit var configDao: ConfigDao
    private lateinit var eventDao: EventDao
    private lateinit var pinManager: PinManager
    private lateinit var policyEnforcer: PolicyEnforcer
    private lateinit var context: Context

    private val sampleConfig = Config(
        mode               = BlockMode.ALLOWLIST,
        userRole           = UserRole.SELF,
        customMessage      = "",
        launcherEnabled    = false,
        onboardingComplete = true,
    )

    @BeforeEach
    fun setup() {
        // Must set before constructing ViewModel so viewModelScope uses testDispatcher
        Dispatchers.setMain(testDispatcher)

        configDao      = mockk()
        eventDao       = mockk()
        pinManager     = mockk()
        policyEnforcer = mockk()
        context        = mockk()

        coEvery { eventDao.recent(any()) } returns emptyList()
        every { configDao.observe() } returns flowOf(sampleConfig)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun makeVm() = SettingsViewModel(
        configDao      = configDao,
        eventDao       = eventDao,
        pinManager     = pinManager,
        policyEnforcer = policyEnforcer,
        context        = context,
    )

    @Test
    fun `changeMode does not update config before PIN verified`() = testScope.runTest {
        every { pinManager.verify(PinManager.Scope.REMOVAL, any()) } returns VerifyResult.WRONG
        coEvery { configDao.upsert(any()) } just runs
        val vm = makeVm()

        vm.changeMode(BlockMode.DENYLIST)
        // PIN prompt should be shown, but config not yet changed
        assertThat(vm.uiState.value.pinPrompt).isNotNull()
        coVerify(exactly = 0) { configDao.upsert(any()) }
    }

    @Test
    fun `changeMode updates config after correct PIN`() = testScope.runTest {
        every { pinManager.verify(PinManager.Scope.REMOVAL, "1234") } returns VerifyResult.SUCCESS
        coEvery { configDao.upsert(any()) } just runs
        val vm = makeVm()

        vm.changeMode(BlockMode.DENYLIST)
        assertThat(vm.uiState.value.pinPrompt).isNotNull()

        vm.submitPin("1234")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { configDao.upsert(match { it.mode == BlockMode.DENYLIST }) }
        assertThat(vm.uiState.value.pinPrompt).isNull()
    }

    @Test
    fun `startRemoval calls clearAllRestrictions and clearDeviceOwner after correct PIN`() = testScope.runTest {
        every { pinManager.verify(PinManager.Scope.REMOVAL, "5678") } returns VerifyResult.SUCCESS
        every { policyEnforcer.clearAllRestrictionsForRemoval() } just runs
        every { policyEnforcer.clearDeviceOwner() } just runs
        every { context.packageName } returns "com.dumbify.app"
        every { context.startActivity(any()) } just runs
        val vm = makeVm()

        vm.startRemoval()
        vm.submitPin("5678")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { policyEnforcer.clearAllRestrictionsForRemoval() }
        coVerify { policyEnforcer.clearDeviceOwner() }
    }

    @Test
    fun `submitPin decrements attemptsRemaining on wrong PIN`() = testScope.runTest {
        every { pinManager.verify(PinManager.Scope.REMOVAL, any()) } returns VerifyResult.WRONG
        val vm = makeVm()

        vm.changeMode(BlockMode.DENYLIST)
        vm.submitPin("wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.pinPrompt?.attemptsRemaining).isEqualTo(2)
    }
}
