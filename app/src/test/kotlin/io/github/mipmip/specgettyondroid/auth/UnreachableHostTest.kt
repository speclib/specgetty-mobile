package io.github.mipmip.specgettyondroid.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/**
 * The failure a Fairphone found that no emulator did: DNS stopped resolving
 * `github.com` mid-poll, and the flow reported it as an answer from GitHub and
 * threw a perfectly good authorization away.
 */
class UnreachableHostTest {

    private class Unresolvable(private val failures: Int = Int.MAX_VALUE) : AuthTransport {

        var asked = 0
            private set

        override suspend fun post(url: String, form: Map<String, String>): String {
            asked++
            if (asked <= failures) {
                throw UnknownHostException(
                    """Unable to resolve host "github.com": No address associated with hostname""",
                )
            }
            return """{"access_token":"ghu_abc","expires_in":28800,"refresh_token":"ghr_xyz"}"""
        }

        override suspend fun get(url: String, bearer: String) = error("not used here")
    }

    @Test
    fun `a host that will not resolve is unreachable, not an answer`() = runTest {
        val outcome = DeviceFlow(Unresolvable(), "cid").poll("dc", 5)

        assertTrue("$outcome", outcome is AuthOutcome.Unreachable)
        assertTrue((outcome as AuthOutcome.Unreachable).reason.contains("github.com"))
    }

    @Test
    fun `a renewal that cannot reach the host is unreachable too`() = runTest {
        val outcome = DeviceFlow(Unresolvable(), "cid").refresh("ghr_xyz")
        assertTrue("$outcome", outcome is AuthOutcome.Unreachable)
    }

    /** Any other IO failure is the same kind of thing, and must not end the flow. */
    @Test
    fun `a dropped connection is unreachable rather than failed`() = runTest {
        val transport = object : AuthTransport {
            override suspend fun post(url: String, form: Map<String, String>): String =
                throw IOException("unexpected end of stream")

            override suspend fun get(url: String, bearer: String) = error("not used here")
        }
        assertTrue(DeviceFlow(transport, "cid").poll("dc", 5) is AuthOutcome.Unreachable)
    }

    /**
     * A malformed body is not a network problem: GitHub answered, and its
     * answer was unusable. That still ends the flow.
     */
    @Test
    fun `a body that is not JSON is a failure, not an unreachable host`() = runTest {
        val transport = object : AuthTransport {
            override suspend fun post(url: String, form: Map<String, String>) = "<html>502</html>"
            override suspend fun get(url: String, bearer: String) = error("not used here")
        }
        assertTrue(DeviceFlow(transport, "cid").poll("dc", 5) is AuthOutcome.Failed)
    }

    @Test
    fun `an unreachable renewal does not ask anyone to authorize again`() = runTest {
        val vault = io.github.mipmip.specgettyondroid.store.FakeVault()
        vault.put(
            "r",
            io.github.mipmip.specgettyondroid.store.Credential.encode(
                io.github.mipmip.specgettyondroid.store.Credential.GitHub(
                    token = "ghu_old",
                    expiresAtMillis = 1_000L,
                    refreshToken = "ghr_old",
                ),
            ),
        )
        val source = TokenSource(vault, DeviceFlow(Unresolvable(), "cid")) { 1_000_000L }

        val result = source.current("r")

        assertEquals(null, result.token)
        assertTrue("${result.problem}", result.problem is TokenProblem.Unreachable)
    }
}
