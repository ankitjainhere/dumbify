package com.dumbify.app.ui.home

import com.dumbify.app.data.entities.BypassMode
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class IsWeakeningEditTest {

    private fun rule(
        allowed: Boolean = false,
        bypass: BypassMode = BypassMode.DELAY_AND_PIN,
        delay: Int = 60,
    ) = RuleUiItem(
        packageName  = "com.example.app",
        displayName  = "App",
        appIcon      = null,
        isAllowed    = allowed,
        bypassMode   = bypass,
        delaySeconds = delay,
        grantedUntil = null,
    )

    @Test
    fun `blocked to allowed is weakening`() {
        assertThat(isWeakeningEdit(rule(allowed = false), rule(allowed = true))).isTrue()
    }

    @Test
    fun `allowed to blocked is not weakening`() {
        assertThat(isWeakeningEdit(rule(allowed = true), rule(allowed = false))).isFalse()
    }

    @Test
    fun `no change is not weakening`() {
        val r = rule(allowed = false, bypass = BypassMode.PIN, delay = 30)
        assertThat(isWeakeningEdit(r, r)).isFalse()
    }

    @Test
    fun `decreasing delay is weakening`() {
        assertThat(isWeakeningEdit(rule(delay = 60), rule(delay = 30))).isTrue()
    }

    @Test
    fun `increasing delay is not weakening`() {
        assertThat(isWeakeningEdit(rule(delay = 30), rule(delay = 60))).isFalse()
    }

    @Test
    fun `DELAY_AND_PIN to PIN is weakening`() {
        assertThat(isWeakeningEdit(
            rule(bypass = BypassMode.DELAY_AND_PIN),
            rule(bypass = BypassMode.PIN),
        )).isTrue()
    }

    @Test
    fun `DELAY_AND_PIN to DELAY is weakening`() {
        assertThat(isWeakeningEdit(
            rule(bypass = BypassMode.DELAY_AND_PIN),
            rule(bypass = BypassMode.DELAY),
        )).isTrue()
    }

    @Test
    fun `PIN to DELAY_AND_PIN is not weakening`() {
        assertThat(isWeakeningEdit(
            rule(bypass = BypassMode.PIN),
            rule(bypass = BypassMode.DELAY_AND_PIN),
        )).isFalse()
    }
}
