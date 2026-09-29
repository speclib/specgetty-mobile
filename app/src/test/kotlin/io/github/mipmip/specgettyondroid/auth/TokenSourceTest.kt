package io.github.mipmip.specgettyondroid.auth

import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.store.FakeVault
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class ScriptedTransport(private val body: String) : AuthTransport {
    var calls = 0
        private set

    override suspend fun post(url: String, form: Map<String, String>): String {
        calls++
        return body
    }

    override suspend fun get(url: String, bearer: String): String =
        error("renewal asks nothing by GET")
}

class TokenSourceTest {

    private val now = 1_700_000_000_000L

    private fun source(
        vault: FakeVault,
        body: String = """{"error":"nothing_asked"}""",
        clock: () -> Long = { now },
    ) = TokenSource(vault, DeviceFlow(ScriptedTransport(body), "cid"), clock) to
        ScriptedTransport(body)

    @Test
    fun `no credential yields nothing and no problem`() = runTest {
        val vault = FakeVault()
        val (tokens, _) = source(vault)
        val result = tokens.current("repo")
        assertNull(result.token)
        assertNull(result.problem)
    }

    @Test
    fun `a typed token is handed over as it is`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.Typed("ghp_typed")))
        val (tokens, _) = source(vault)

        assertEquals("ghp_typed", tokens.current("repo").token)
    }

    @Test
    fun `a credential stored before this change is handed over as it is`() = runTest {
        val vault = FakeVault()
        vault.put("repo", "ghp_written_before")
        val (tokens, _) = source(vault)

        assertEquals("ghp_written_before", tokens.current("repo").token)
    }

    @Test
    fun `a credential well before its expiry is used without renewing`() = runTest {
        val vault = FakeVault()
        vault.put(
            "repo",
            Credential.encode(Credential.GitHub("ghu_abc", now + 8 * 3600_000L, "ghr")),
        )
        val transport = ScriptedTransport("""{"error":"should_not_be_called"}""")
        val tokens = TokenSource(vault, DeviceFlow(transport, "cid")) { now }

        assertEquals("ghu_abc", tokens.current("repo").token)
        assertEquals("nothing was renewed", 0, transport.calls)
    }

    @Test
    fun `a credential near expiry is renewed first`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.GitHub("ghu_old", now + 60_000, "ghr_old")))
        val transport = ScriptedTransport(
            """{"access_token":"ghu_new","expires_in":28800,"refresh_token":"ghr_new"}""",
        )
        val tokens = TokenSource(vault, DeviceFlow(transport, "cid")) { now }

        assertEquals("ghu_new", tokens.current("repo").token)
        assertEquals(1, transport.calls)
    }

    @Test
    fun `the renewed credential is stored, so it is not renewed twice`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.GitHub("ghu_old", now + 60_000, "ghr_old")))
        val transport = ScriptedTransport(
            """{"access_token":"ghu_new","expires_in":28800,"refresh_token":"ghr_new"}""",
        )
        val tokens = TokenSource(vault, DeviceFlow(transport, "cid")) { now }

        tokens.current("repo")
        val stored = Credential.decode(vault.get("repo")!!) as Credential.GitHub
        assertEquals("ghu_new", stored.token)
        assertEquals("ghr_new", stored.refreshToken)
        assertEquals(now + 28_800_000, stored.expiresAtMillis)

        tokens.current("repo")
        assertEquals("the second read renewed nothing", 1, transport.calls)
    }

    @Test
    fun `a renewal that returns no new refresh token keeps the old one`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.GitHub("ghu_old", now + 60_000, "ghr_old")))
        val transport = ScriptedTransport("""{"access_token":"ghu_new","expires_in":28800}""")
        val tokens = TokenSource(vault, DeviceFlow(transport, "cid")) { now }

        tokens.current("repo")
        val stored = Credential.decode(vault.get("repo")!!) as Credential.GitHub
        assertEquals("ghr_old", stored.refreshToken)
    }

    @Test
    fun `a refused renewal says the repository must be authorized again`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.GitHub("ghu_old", now + 60_000, "ghr_old")))
        val transport = ScriptedTransport("""{"error":"bad_refresh_token"}""")
        val tokens = TokenSource(vault, DeviceFlow(transport, "cid")) { now }

        val result = tokens.current("repo")
        assertNull(result.token)
        assertTrue("${result.problem}", result.problem is TokenProblem.NeedsAuthorizing)
    }

    @Test
    fun `an expired credential with nothing to renew with says so`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.GitHub("ghu_old", now - 1, null)))
        val (tokens, _) = source(vault)

        val result = tokens.current("repo")
        assertNull(result.token)
        assertTrue("${result.problem}", result.problem is TokenProblem.NeedsAuthorizing)
    }

    @Test
    fun `a credential that never expires is never renewed`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.GitHub("ghu_forever", null, "ghr")))
        val transport = ScriptedTransport("""{"error":"should_not_be_called"}""")
        val tokens = TokenSource(vault, DeviceFlow(transport, "cid")) { now }

        assertEquals("ghu_forever", tokens.current("repo").token)
        assertEquals(0, transport.calls)
    }

    @Test
    fun `an approval is stamped against the clock`() = runTest {
        val vault = FakeVault()
        val (tokens, _) = source(vault)
        val stamped = tokens.fromApproval(GitHubCredential("ghu_abc", 28800, "ghr"))

        assertEquals(now + 28_800_000, stamped.expiresAtMillis)
        assertEquals("ghr", stamped.refreshToken)
    }

    @Test
    fun `an approval with no expiry is stamped with none`() = runTest {
        val vault = FakeVault()
        val (tokens, _) = source(vault)
        assertNull(tokens.fromApproval(GitHubCredential("ghu_abc", null, null)).expiresAtMillis)
    }

    @Test
    fun `storing a credential puts it in the vault encoded`() = runTest {
        val vault = FakeVault()
        val (tokens, _) = source(vault)
        tokens.store("repo", Credential.GitHub("ghu_abc", now, "ghr"))

        val stored = Credential.decode(vault.get("repo")!!)
        assertTrue("$stored", stored is Credential.GitHub)
        assertEquals("ghu_abc", stored.token)
    }

    @Test
    fun `time passing turns a fresh credential into one that needs renewing`() = runTest {
        val vault = FakeVault()
        vault.put("repo", Credential.encode(Credential.GitHub("ghu_old", now + 3600_000, "ghr")))
        val transport = ScriptedTransport(
            """{"access_token":"ghu_new","expires_in":28800,"refresh_token":"ghr_new"}""",
        )
        var clock = now
        val tokens = TokenSource(vault, DeviceFlow(transport, "cid")) { clock }

        assertEquals("ghu_old", tokens.current("repo").token)
        assertEquals(0, transport.calls)

        clock = now + 3600_000 - 60_000
        assertEquals("ghu_new", tokens.current("repo").token)
        assertEquals(1, transport.calls)
    }
}
