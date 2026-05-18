package com.dumbify.app.policy

import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.data.entities.UserRole
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RuleStoreTest {

    private val ownPackage = "com.dumbify.app"

    private fun config(mode: BlockMode) = Config(
        id = 0,
        mode = mode,
        userRole = UserRole.SELF,
        customMessage = "",
        launcherEnabled = false,
        onboardingComplete = true,
    )

    private fun rule(pkg: String, allowed: Boolean) = AppRule(
        packageName = pkg,
        isAllowed = allowed,
        bypassMode = BypassMode.DELAY,
        delaySeconds = 30,
        grantedUntil = null,
    )

    private fun makeStore(config: Config, rules: Map<String, AppRule>): RuleStore {
        val configDao = mockk<com.dumbify.app.data.dao.ConfigDao>()
        coEvery { configDao.get() } returns config
        val appRuleDao = mockk<com.dumbify.app.data.dao.AppRuleDao>()
        coEvery { appRuleDao.byPkg(any()) } answers { rules[firstArg<String>()] }
        return RuleStore(configDao, appRuleDao, ownPackage)
    }

    @Test
    fun `allowlist mode blocks unlisted package`() = runTest {
        val store = makeStore(config(BlockMode.ALLOWLIST), emptyMap())
        assertThat(store.isBlocked("com.instagram.android")).isTrue()
    }

    @Test
    fun `allowlist mode allows package marked allowed`() = runTest {
        val rules = mapOf("com.maps" to rule("com.maps", allowed = true))
        val store = makeStore(config(BlockMode.ALLOWLIST), rules)
        assertThat(store.isBlocked("com.maps")).isFalse()
    }

    @Test
    fun `allowlist mode blocks package marked not allowed`() = runTest {
        val rules = mapOf("com.tiktok" to rule("com.tiktok", allowed = false))
        val store = makeStore(config(BlockMode.ALLOWLIST), rules)
        assertThat(store.isBlocked("com.tiktok")).isTrue()
    }

    @Test
    fun `denylist mode allows unlisted package`() = runTest {
        val store = makeStore(config(BlockMode.DENYLIST), emptyMap())
        assertThat(store.isBlocked("com.random")).isFalse()
    }

    @Test
    fun `denylist mode blocks package marked not allowed`() = runTest {
        val rules = mapOf("com.tiktok" to rule("com.tiktok", allowed = false))
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.tiktok")).isTrue()
    }

    @Test
    fun `denylist mode allows package marked allowed`() = runTest {
        val rules = mapOf("com.maps" to rule("com.maps", allowed = true))
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.maps")).isFalse()
    }

    @Test
    fun `dumbify itself is never blocked in allowlist mode`() = runTest {
        val store = makeStore(config(BlockMode.ALLOWLIST), emptyMap())
        assertThat(store.isBlocked(ownPackage)).isFalse()
    }

    @Test
    fun `dumbify itself is never blocked in denylist mode even if rule says blocked`() = runTest {
        val rules = mapOf(ownPackage to rule(ownPackage, allowed = false))
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked(ownPackage)).isFalse()
    }

    @Test
    fun `system essentials are never blocked`() = runTest {
        val store = makeStore(config(BlockMode.ALLOWLIST), emptyMap())
        assertThat(store.isBlocked("com.android.dialer")).isFalse()
        assertThat(store.isBlocked("com.android.mms")).isFalse()
        assertThat(store.isBlocked("com.android.settings")).isFalse()
        assertThat(store.isBlocked("com.android.camera2")).isFalse()
    }

    @Test
    fun `package with active grant is not blocked`() = runTest {
        // grantedUntil in the future — treated as currently allowed regardless of mode
        val futureRule = rule("com.tiktok", allowed = false).copy(grantedUntil = Long.MAX_VALUE)
        val rules = mapOf("com.tiktok" to futureRule)
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.tiktok", now = 0L)).isFalse()
    }

    @Test
    fun `package with expired grant is blocked again`() = runTest {
        val expiredRule = rule("com.tiktok", allowed = false).copy(grantedUntil = 100L)
        val rules = mapOf("com.tiktok" to expiredRule)
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.tiktok", now = 200L)).isTrue()
    }
}
