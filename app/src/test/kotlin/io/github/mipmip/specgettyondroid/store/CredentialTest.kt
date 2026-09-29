package io.github.mipmip.specgettyondroid.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `a typed token has no expiry and no renewal material`() {
        val typed = Credential.Typed("ghp_typed")
        assertEquals("ghp_typed", typed.token)
    }

    @Test
    fun `a typed token round trips as the bare string`() {
        val encoded = Credential.encode(Credential.Typed("ghp_typed"))
        assertEquals("ghp_typed", encoded)
        assertEquals(Credential.Typed("ghp_typed"), Credential.decode(encoded))
    }

    @Test
    fun `a github credential round trips with its expiry and refresh token`() {
        val original = Credential.GitHub("ghu_abc", now + 28_800_000, "ghr_xyz")
        val decoded = Credential.decode(Credential.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `a github credential with no expiry round trips too`() {
        val original = Credential.GitHub("ghu_abc", null, null)
        assertEquals(original, Credential.decode(Credential.encode(original)))
    }

    /** A value written before this change is a bare token, not JSON. */
    @Test
    fun `a credential stored before this change reads as a typed token`() {
        assertEquals(Credential.Typed("ghp_old"), Credential.decode("ghp_old"))
        assertEquals("ghp_old", Credential.decode("ghp_old").token)
    }

    @Test
    fun `stored material that is not decodable falls back to a typed token`() {
        val decoded = Credential.decode("{ not json at all")
        assertTrue("$decoded", decoded is Credential.Typed)
        assertEquals("{ not json at all", decoded.token)
    }

    @Test
    fun `a credential well before its expiry is not near it`() {
        val c = Credential.GitHub("ghu_abc", now + 8 * 60 * 60 * 1000L, "ghr")
        assertFalse(c.isNearExpiry(now))
    }

    @Test
    fun `a credential within the renewal window is near expiry`() {
        val c = Credential.GitHub("ghu_abc", now + 60_000, "ghr")
        assertTrue(c.isNearExpiry(now))
    }

    @Test
    fun `a credential already past its expiry is near it`() {
        val c = Credential.GitHub("ghu_abc", now - 1, "ghr")
        assertTrue(c.isNearExpiry(now))
    }

    @Test
    fun `a credential with no expiry is never near one`() {
        val c = Credential.GitHub("ghu_abc", null, "ghr")
        assertFalse(c.isNearExpiry(now))
        assertFalse(c.isNearExpiry(Long.MAX_VALUE))
    }

    @Test
    fun `renewal needs renewal material`() {
        assertTrue(Credential.GitHub("t", now, "ghr").canRenew)
        assertFalse(Credential.GitHub("t", now, null).canRenew)
        assertFalse(Credential.GitHub("t", now, "  ").canRenew)
    }

    @Test
    fun `the encoded form of a github credential does not reveal the token in the clear as a bare string`() {
        // It is JSON, so the vault stores structure rather than a lone secret;
        // encryption is the vault's job and is tested there.
        val encoded = Credential.encode(Credential.GitHub("ghu_abc", now, "ghr_xyz"))
        assertTrue(encoded.startsWith("{"))
        assertTrue(encoded.contains("ghu_abc"))
        assertTrue(encoded.contains("ghr_xyz"))
    }

    @Test
    fun `an empty stored value is a typed token rather than nothing`() {
        assertEquals("", Credential.decode("").token)
    }

    @Test
    fun `the expiry survives being written and read`() {
        val expiry = now + 28_800_000
        val decoded = Credential.decode(
            Credential.encode(Credential.GitHub("ghu_abc", expiry, "ghr")),
        ) as Credential.GitHub
        assertEquals(expiry, decoded.expiresAtMillis)
        assertNull(Credential.decode(Credential.encode(Credential.Typed("x")))
            .let { it as? Credential.GitHub }?.expiresAtMillis)
    }
}
