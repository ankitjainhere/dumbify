package com.dumbify.app.policy

import com.dumbify.app.data.FakeSecurePrefs
import com.dumbify.app.policy.PinManager.Scope
import com.dumbify.app.policy.PinManager.VerifyResult
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PinManagerTest {

    private lateinit var prefs: FakeSecurePrefs
    private lateinit var clock: FakeClock
    private lateinit var pinManager: PinManager

    @BeforeEach
    fun setup() {
        prefs = FakeSecurePrefs()
        clock = FakeClock(1_000_000L)
        pinManager = PinManager(prefs, clock)
    }

    @Test
    fun `setPin stores hash and salt for removal scope`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        assertThat(prefs.getString("removal_pin_hash")).isNotNull()
        assertThat(prefs.getBytes("removal_pin_salt")).isNotNull()
    }

    @Test
    fun `setPin stores hash and salt for bypass scope`() {
        pinManager.setPin(Scope.BYPASS, "5678")
        assertThat(prefs.getString("bypass_pin_hash")).isNotNull()
        assertThat(prefs.getBytes("bypass_pin_salt")).isNotNull()
    }

    @Test
    fun `hasPin returns false when unset`() {
        assertThat(pinManager.hasPin(Scope.REMOVAL)).isFalse()
        assertThat(pinManager.hasPin(Scope.BYPASS)).isFalse()
    }

    @Test
    fun `hasPin returns true after setPin`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        assertThat(pinManager.hasPin(Scope.REMOVAL)).isTrue()
    }

    @Test
    fun `verify returns SUCCESS for correct PIN`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        val result = pinManager.verify(Scope.REMOVAL, "1234")
        assertThat(result).isEqualTo(VerifyResult.SUCCESS)
    }

    @Test
    fun `verify returns WRONG for incorrect PIN`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        val result = pinManager.verify(Scope.REMOVAL, "9999")
        assertThat(result).isEqualTo(VerifyResult.WRONG)
    }

    @Test
    fun `verify returns NOT_SET when no PIN configured`() {
        val result = pinManager.verify(Scope.REMOVAL, "1234")
        assertThat(result).isEqualTo(VerifyResult.NOT_SET)
    }

    @Test
    fun `three consecutive wrong attempts trigger cooldown`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.REMOVAL, "0000")
        val third = pinManager.verify(Scope.REMOVAL, "0000")
        assertThat(third).isEqualTo(VerifyResult.WRONG)
        val fourth = pinManager.verify(Scope.REMOVAL, "1234") // even correct PIN refused while cooling down
        assertThat(fourth).isEqualTo(VerifyResult.COOLDOWN)
    }

    @Test
    fun `cooldown expires after 5 minutes`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        repeat(3) { pinManager.verify(Scope.REMOVAL, "0000") }
        clock.advance(5 * 60 * 1000L + 1)
        val result = pinManager.verify(Scope.REMOVAL, "1234")
        assertThat(result).isEqualTo(VerifyResult.SUCCESS)
    }

    @Test
    fun `successful verify resets fail counter`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.REMOVAL, "1234") // success
        // Now two more wrong attempts should NOT trigger cooldown
        pinManager.verify(Scope.REMOVAL, "0000")
        val result = pinManager.verify(Scope.REMOVAL, "0000")
        assertThat(result).isEqualTo(VerifyResult.WRONG)
    }

    @Test
    fun `fail counter is shared across scopes`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.setPin(Scope.BYPASS, "5678")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.BYPASS, "0000")
        pinManager.verify(Scope.REMOVAL, "0000")
        // 3 total fails — cooldown active for either scope
        assertThat(pinManager.verify(Scope.BYPASS, "5678")).isEqualTo(VerifyResult.COOLDOWN)
    }

    @Test
    fun `clearPin removes hash and salt`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.clearPin(Scope.REMOVAL)
        assertThat(pinManager.hasPin(Scope.REMOVAL)).isFalse()
        assertThat(prefs.getString("removal_pin_hash")).isNull()
        assertThat(prefs.getBytes("removal_pin_salt")).isNull()
    }

    @Test
    fun `different PINs produce different hashes`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        val hash1 = prefs.getString("removal_pin_hash")!!
        pinManager.clearPin(Scope.REMOVAL)
        pinManager.setPin(Scope.REMOVAL, "1234")
        val hash2 = prefs.getString("removal_pin_hash")!!
        // Different salts → different hashes even for same PIN
        assertThat(hash1).isNotEqualTo(hash2)
    }
}
