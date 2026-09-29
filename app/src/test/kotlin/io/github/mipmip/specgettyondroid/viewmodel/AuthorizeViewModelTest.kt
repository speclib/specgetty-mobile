package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.auth.AuthTransport
import io.github.mipmip.specgettyondroid.auth.DeviceFlow
import io.github.mipmip.specgettyondroid.auth.Installations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One recorded answer per call, in order, for both verbs. */
private class Script(
    private val posts: List<String>,
    private val gets: Map<String, String> = emptyMap(),
) : AuthTransport {

    var postCount = 0
        private set

    override suspend fun post(url: String, form: Map<String, String>): String =
        posts[minOf(postCount++, posts.lastIndex)]

    // Longest key first: the installations URL is a prefix of the repositories
    // URL, so a first-match lookup answers the wrong one.
    override suspend fun get(url: String, bearer: String): String =
        gets.entries.filter { url.startsWith(it.key) }.maxByOrNull { it.key.length }?.value
            ?: """{"total_count":0,"installations":[]}"""
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthorizeViewModelTest {

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    private val codeBody = """{"device_code":"dc","user_code":"ABCD-1234",
        "verification_uri":"https://github.com/login/device","expires_in":900,"interval":5}"""

    private val approvalBody =
        """{"access_token":"ghu_abc","expires_in":28800,"refresh_token":"ghr_xyz"}"""

    private val root = "https://api.github.com/user/installations"

    private fun TestScope.model(
        posts: List<String>,
        gets: Map<String, String> = emptyMap(),
        url: String? = "https://github.com/mipmip/test.git",
    ): AuthorizeViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val transport = Script(posts, gets)
        return AuthorizeViewModel(
            deviceFlow = DeviceFlow(transport, "cid"),
            installations = Installations(transport),
            repositoryUrl = url,
            injectedScope = scope,
            now = { 1_700_000_000_000L },
        )
    }

    @Test
    fun `it begins by showing the code to type`() = runTest {
        val vm = model(listOf(codeBody, """{"error":"authorization_pending"}"""))
        vm.begin()
        advanceTimeBy(1_000)

        val state = vm.state.value
        assertTrue("$state", state is AuthState.Waiting)
        assertEquals("ABCD-1234", (state as AuthState.Waiting).code.userCode)
        assertEquals("https://github.com/login/device", state.code.verificationUri)
    }

    @Test
    fun `approval with the repository covered is a success`() = runTest {
        val vm = model(
            posts = listOf(codeBody, approvalBody),
            gets = mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"selected"}]}""",
                "$root/7/repositories" to
                    """{"repositories":[{"full_name":"mipmip/test"}]}""",
            ),
        )
        vm.begin()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is AuthState.Approved)
        assertEquals("ghu_abc", (state as AuthState.Approved).credential.token)
        assertEquals("ghr_xyz", state.credential.refreshToken)
        assertEquals(1_700_000_000_000L + 28_800_000, state.credential.expiresAtMillis)
    }

    /** The trap the spike fell into twice: approved, and reaching nothing. */
    @Test
    fun `approval that reaches nothing is not reported as success`() = runTest {
        val vm = model(
            posts = listOf(codeBody, approvalBody),
            gets = mapOf(root to """{"total_count":0,"installations":[]}"""),
        )
        vm.begin()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is AuthState.NeedsInstalling)
        assertTrue((state as AuthState.NeedsInstalling).reach.reachesNothing)
    }

    @Test
    fun `approval that does not cover this repository is not reported as success`() = runTest {
        val vm = model(
            posts = listOf(codeBody, approvalBody),
            gets = mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"selected"}]}""",
                "$root/7/repositories" to
                    """{"repositories":[{"full_name":"someone/else"}]}""",
            ),
        )
        vm.begin()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is AuthState.NeedsInstalling)
        assertEquals(listOf("someone/else"), (state as AuthState.NeedsInstalling).reach.repositories)
    }

    @Test
    fun `an installation on everything covers the repository`() = runTest {
        val vm = model(
            posts = listOf(codeBody, approvalBody),
            gets = mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"all"}]}""",
            ),
        )
        vm.begin()
        advanceUntilIdle()
        assertTrue("${vm.state.value}", vm.state.value is AuthState.Approved)
    }

    @Test
    fun `waiting continues while GitHub says pending`() = runTest {
        val vm = model(
            posts = listOf(
                codeBody,
                """{"error":"authorization_pending"}""",
                """{"error":"authorization_pending"}""",
                approvalBody,
            ),
            gets = mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"all"}]}""",
            ),
        )
        vm.begin()
        advanceUntilIdle()
        assertTrue("${vm.state.value}", vm.state.value is AuthState.Approved)
    }

    @Test
    fun `each refusal is explained in its own words`() = runTest {
        fun messageFor(error: String): String {
            val vm = model(listOf(codeBody, """{"error":"$error"}"""))
            vm.begin()
            advanceUntilIdle()
            return (vm.state.value as AuthState.Failed).message
        }

        assertTrue(messageFor("expired_token").contains("expired"))
        assertTrue(messageFor("access_denied").contains("declined"))
        assertTrue(messageFor("device_flow_disabled").contains("access token"))
    }

    @Test
    fun `an app that cannot authorize says so at the first step`() = runTest {
        val vm = model(listOf("""{"error":"device_flow_disabled"}"""))
        vm.begin()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is AuthState.Failed)
        assertTrue((state as AuthState.Failed).message.contains("access token"))
    }

    /**
     * The hang that found this: a code nobody approves must stop being polled.
     * GitHub gives an expiry and the loop honours it, rather than asking
     * forever for an answer that can no longer come.
     */
    @Test
    fun `a code nobody approves stops being polled when it expires`() = runTest {
        val vm = model(listOf(codeBody, """{"error":"authorization_pending"}"""))
        vm.begin()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is AuthState.Failed)
        assertTrue((state as AuthState.Failed).message.contains("expired"))
    }

    @Test
    fun `abandoning stops the waiting and stores nothing`() = runTest {
        val vm = model(listOf(codeBody, """{"error":"authorization_pending"}"""))
        vm.begin()
        advanceTimeBy(1000)
        vm.cancel()
        advanceUntilIdle()

        assertEquals(AuthState.Idle, vm.state.value)
    }

    @Test
    fun `beginning twice does not start a second flow`() = runTest {
        val vm = model(listOf(codeBody, """{"error":"authorization_pending"}"""))
        vm.begin()
        advanceTimeBy(100)
        vm.begin()
        advanceTimeBy(1_000)
        assertTrue("${vm.state.value}", vm.state.value is AuthState.Waiting)
    }

    /**
     * What the Fairphone found. A poll hit a DNS failure while the browser was
     * in front, and the flow reported it as an answer from GitHub and gave up
     * on a code that was still perfectly good.
     */
    @Test
    fun `a network blip mid-poll does not throw the authorization away`() = runTest {
        val flaky = object : AuthTransport {
            private var n = 0
            override suspend fun post(url: String, form: Map<String, String>): String {
                n++
                return when (n) {
                    1 -> codeBody
                    2, 3 -> throw java.net.UnknownHostException(
                        """Unable to resolve host "github.com": No address associated""",
                    )

                    else -> approvalBody
                }
            }

            override suspend fun get(url: String, bearer: String) =
                """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"all"}]}"""
        }
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = AuthorizeViewModel(
            DeviceFlow(flaky, "cid"),
            Installations(flaky),
            "https://github.com/mipmip/test.git",
            scope,
        ) { 0L }

        vm.begin()
        advanceUntilIdle()

        assertTrue("${vm.state.value}", vm.state.value is AuthState.Approved)
    }

    @Test
    fun `while the host is unreachable the code stays on screen`() = runTest {
        val neverResolves = object : AuthTransport {
            private var n = 0
            override suspend fun post(url: String, form: Map<String, String>): String {
                if (n++ == 0) return codeBody
                throw java.net.UnknownHostException("Unable to resolve host")
            }

            override suspend fun get(url: String, bearer: String) = error("never reached")
        }
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = AuthorizeViewModel(
            DeviceFlow(neverResolves, "cid"),
            Installations(neverResolves),
            null,
            scope,
        ) { 0L }

        vm.begin()
        advanceTimeBy(6_000)

        val state = vm.state.value
        assertTrue("$state", state is AuthState.Waiting)
        assertEquals("ABCD-1234", (state as AuthState.Waiting).code.userCode)
        assertTrue(state.reconnecting)
    }

    /** It does give up eventually, rather than asking a dead network forever. */
    @Test
    fun `a host that never comes back does end the waiting`() = runTest {
        val neverResolves = object : AuthTransport {
            private var n = 0
            override suspend fun post(url: String, form: Map<String, String>): String {
                if (n++ == 0) return codeBody
                throw java.net.UnknownHostException("Unable to resolve host")
            }

            override suspend fun get(url: String, bearer: String) = error("never reached")
        }
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = AuthorizeViewModel(
            DeviceFlow(neverResolves, "cid"),
            Installations(neverResolves),
            null,
            scope,
        ) { 0L }

        vm.begin()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is AuthState.Failed)
        assertTrue((state as AuthState.Failed).message.contains("Could not reach GitHub"))
    }

    @Test
    fun `a listing that fails is treated as reaching nothing rather than assumed fine`() = runTest {
        val transportThatFailsTheListing = object : AuthTransport {
            private val posts = listOf(codeBody, approvalBody)
            private var n = 0
            override suspend fun post(url: String, form: Map<String, String>) =
                posts[minOf(n++, posts.lastIndex)]

            override suspend fun get(url: String, bearer: String): String =
                throw java.io.IOException("the listing went away")
        }
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = AuthorizeViewModel(
            DeviceFlow(transportThatFailsTheListing, "cid"),
            Installations(transportThatFailsTheListing),
            "https://github.com/mipmip/test.git",
            scope,
        ) { 0L }

        vm.begin()
        advanceUntilIdle()
        assertTrue("${vm.state.value}", vm.state.value is AuthState.NeedsInstalling)
    }
}
