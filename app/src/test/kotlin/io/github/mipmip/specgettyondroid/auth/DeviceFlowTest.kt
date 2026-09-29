package io.github.mipmip.specgettyondroid.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recorded answers, no network. The bodies are the shapes GitHub actually
 * returned during the spike, including the error vocabulary, which is the part
 * that would otherwise only be discovered on a device.
 */
private class FakeTransport(private vararg val answers: String) : AuthTransport {

    val calls = mutableListOf<Pair<String, Map<String, String>>>()
    private var next = 0

    override suspend fun post(url: String, form: Map<String, String>): String {
        calls += url to form
        return answers[minOf(next++, answers.lastIndex)]
    }

    override suspend fun get(url: String, bearer: String): String =
        error("this flow asks nothing by GET")
}

class DeviceFlowTest {

    private val codeBody = """
        {"device_code":"dc-1","user_code":"ABF8-A1AA",
         "verification_uri":"https://github.com/login/device",
         "expires_in":899,"interval":5}
    """.trimIndent()

    @Test
    fun `a device code is read from the response`() = runTest {
        val flow = DeviceFlow(FakeTransport(codeBody), clientId = "cid")
        val code = flow.requestCode().getOrThrow()

        assertEquals("dc-1", code.deviceCode)
        assertEquals("ABF8-A1AA", code.userCode)
        assertEquals("https://github.com/login/device", code.verificationUri)
        assertEquals(5, code.intervalSeconds)
        assertEquals(899, code.expiresInSeconds)
    }

    @Test
    fun `requesting a code sends the client id and no secret`() = runTest {
        val transport = FakeTransport(codeBody)
        DeviceFlow(transport, clientId = "cid").requestCode().getOrThrow()

        val (url, form) = transport.calls.single()
        assertEquals("https://github.com/login/device/code", url)
        assertEquals("cid", form["client_id"])
        assertTrue("no secret may be sent", form.keys.none { it.contains("secret") })
    }

    @Test
    fun `an app without device flow enabled is reported, not thrown`() = runTest {
        val body = """{"error":"device_flow_disabled",
            "error_description":"Device Flow must be explicitly enabled for this App"}"""
        val result = DeviceFlow(FakeTransport(body), clientId = "cid").requestCode()
        assertTrue(result.isFailure)
        assertEquals("device_flow_disabled", (result.exceptionOrNull() as AuthError).code)
    }

    @Test
    fun `waiting reports pending and keeps the interval`() = runTest {
        val flow = DeviceFlow(FakeTransport("""{"error":"authorization_pending"}"""), "cid")
        val outcome = flow.poll("dc-1", interval = 5)
        assertEquals(AuthOutcome.Pending(5), outcome)
    }

    @Test
    fun `a slow down lengthens the interval`() = runTest {
        val flow = DeviceFlow(FakeTransport("""{"error":"slow_down","interval":10}"""), "cid")
        val outcome = flow.poll("dc-1", interval = 5)
        assertEquals(AuthOutcome.Pending(10), outcome)
    }

    @Test
    fun `a slow down without an interval still lengthens it`() = runTest {
        val flow = DeviceFlow(FakeTransport("""{"error":"slow_down"}"""), "cid")
        val outcome = flow.poll("dc-1", interval = 5) as AuthOutcome.Pending
        assertTrue("${outcome.intervalSeconds}", outcome.intervalSeconds > 5)
    }

    @Test
    fun `a slow down never shortens the interval`() = runTest {
        val flow = DeviceFlow(FakeTransport("""{"error":"slow_down","interval":1}"""), "cid")
        val outcome = flow.poll("dc-1", interval = 20) as AuthOutcome.Pending
        assertTrue("${outcome.intervalSeconds}", outcome.intervalSeconds > 20)
    }

    @Test
    fun `approval yields the credential, with its expiry and refresh token`() = runTest {
        val body = """{"access_token":"ghu_abc","token_type":"bearer",
            "expires_in":28800,"refresh_token":"ghr_xyz","scope":""}"""
        val flow = DeviceFlow(FakeTransport(body), "cid")

        val approved = flow.poll("dc-1", 5) as AuthOutcome.Approved
        assertEquals("ghu_abc", approved.credential.token)
        assertEquals(28800, approved.credential.expiresInSeconds)
        assertEquals("ghr_xyz", approved.credential.refreshToken)
    }

    @Test
    fun `an app with expiry switched off yields a credential with neither`() = runTest {
        val body = """{"access_token":"ghu_abc","token_type":"bearer","scope":""}"""
        val approved = DeviceFlow(FakeTransport(body), "cid").poll("dc-1", 5) as AuthOutcome.Approved

        assertNull(approved.credential.expiresInSeconds)
        assertNull(approved.credential.refreshToken)
    }

    @Test
    fun `each ending is distinct`() = runTest {
        suspend fun outcomeFor(error: String) =
            DeviceFlow(FakeTransport("""{"error":"$error"}"""), "cid").poll("dc-1", 5)

        assertEquals(AuthOutcome.Expired, outcomeFor("expired_token"))
        assertEquals(AuthOutcome.Declined, outcomeFor("access_denied"))
        assertEquals(AuthOutcome.Unavailable, outcomeFor("device_flow_disabled"))
        assertEquals(AuthOutcome.Unavailable, outcomeFor("unauthorized_client"))
        assertTrue(outcomeFor("something_new") is AuthOutcome.Failed)
    }

    @Test
    fun `a transport that fails is an outcome, not an exception`() = runTest {
        val broken = object : AuthTransport {
            override suspend fun post(url: String, form: Map<String, String>) =
                throw java.io.IOException("the network went away")

            override suspend fun get(url: String, bearer: String) =
                throw java.io.IOException("the network went away")
        }
        val outcome = DeviceFlow(broken, "cid").poll("dc-1", 5)
        // Unreachable rather than Failed: a lost network is not GitHub's
        // answer, and the caller must be able to keep waiting through it.
        assertTrue("$outcome", outcome is AuthOutcome.Unreachable)
    }

    @Test
    fun `a body that is not json is an outcome, not an exception`() = runTest {
        val outcome = DeviceFlow(FakeTransport("<html>502</html>"), "cid").poll("dc-1", 5)
        assertTrue("$outcome", outcome is AuthOutcome.Failed)
    }

    @Test
    fun `polling three times then succeeding`() = runTest {
        val transport = FakeTransport(
            """{"error":"authorization_pending"}""",
            """{"error":"authorization_pending"}""",
            """{"error":"authorization_pending"}""",
            """{"access_token":"ghu_abc","expires_in":28800,"refresh_token":"ghr_xyz"}""",
        )
        val flow = DeviceFlow(transport, "cid")

        var interval = 5
        var outcome = flow.poll("dc-1", interval)
        var rounds = 1
        while (outcome is AuthOutcome.Pending) {
            interval = outcome.intervalSeconds
            outcome = flow.poll("dc-1", interval)
            rounds++
        }
        assertEquals(4, rounds)
        assertTrue("$outcome", outcome is AuthOutcome.Approved)
    }

    @Test
    fun `refreshing sends no secret and yields a fresh credential`() = runTest {
        val body = """{"access_token":"ghu_new","expires_in":28800,"refresh_token":"ghr_new"}"""
        val transport = FakeTransport(body)
        val approved = DeviceFlow(transport, "cid").refresh("ghr_old") as AuthOutcome.Approved

        assertEquals("ghu_new", approved.credential.token)
        assertEquals("ghr_new", approved.credential.refreshToken)

        val (_, form) = transport.calls.single()
        assertEquals("refresh_token", form["grant_type"])
        assertEquals("ghr_old", form["refresh_token"])
        assertTrue("no secret may be sent", form.keys.none { it.contains("secret") })
    }

    @Test
    fun `a refusal to refresh is reported`() = runTest {
        val body = """{"error":"bad_refresh_token"}"""
        val outcome = DeviceFlow(FakeTransport(body), "cid").refresh("ghr_old")
        assertTrue("$outcome", outcome is AuthOutcome.Failed)
    }

    @Test
    fun `the published client id is a public identifier and carries no secret`() {
        assertTrue(GITHUB_CLIENT_ID.startsWith("Iv"))
        assertTrue(GITHUB_CLIENT_ID.length in 16..40)
    }
}
