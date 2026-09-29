package io.github.mipmip.specgettyondroid

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.github.mipmip.specgettyondroid.auth.AuthTransport
import io.github.mipmip.specgettyondroid.auth.DeviceFlow
import io.github.mipmip.specgettyondroid.auth.Installations
import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.ui.screen.AuthorizeScreen
import io.github.mipmip.specgettyondroid.ui.theme.SpecgettyTheme
import io.github.mipmip.specgettyondroid.viewmodel.AuthorizeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val CODE_BODY = """{"device_code":"dc","user_code":"WXYZ-7788",
    "verification_uri":"https://github.com/login/device","expires_in":900,"interval":1}"""

private const val APPROVAL_BODY =
    """{"access_token":"ghu_abc","expires_in":28800,"refresh_token":"ghr_xyz"}"""

private const val INSTALLATIONS = "https://api.github.com/user/installations"

/**
 * The real screen against a fake transport. Nothing here reaches github.com:
 * the browser round trip is the one thing only a phone can prove, and that is
 * task 9.1's job rather than this file's.
 */
private class Script(
    private val posts: List<String>,
    private val gets: Map<String, String> = emptyMap(),
) : AuthTransport {

    private var n = 0

    override suspend fun post(url: String, form: Map<String, String>): String =
        posts[minOf(n++, posts.lastIndex)]

    override suspend fun get(url: String, bearer: String): String =
        gets.entries.filter { url.startsWith(it.key) }.maxByOrNull { it.key.length }?.value
            ?: """{"total_count":0,"installations":[]}"""
}

class AuthorizeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var approved: Credential.GitHub? = null
    private var dismissed = false

    private fun show(
        posts: List<String>,
        gets: Map<String, String> = emptyMap(),
        url: String? = "https://github.com/mipmip/test.git",
    ) {
        val transport = Script(posts, gets)
        compose.setContent {
            val model = androidx.compose.runtime.remember {
                AuthorizeViewModel(
                    deviceFlow = DeviceFlow(transport, "cid"),
                    installations = Installations(transport),
                    repositoryUrl = url,
                )
            }
            SpecgettyTheme {
                AuthorizeScreen(
                    viewModel = model,
                    onAuthorized = { approved = it },
                    onDismiss = { dismissed = true },
                )
            }
        }
    }

    private val pending = """{"error":"authorization_pending"}"""

    @Test
    fun theCodeTheAddressAndTheWaitingAreAllOnScreen() {
        show(listOf(CODE_BODY, pending))

        compose.awaitText("WXYZ-7788")
        compose.onNodeWithText("WXYZ-7788").assertIsDisplayed()
        compose.onNodeWithText("https://github.com/login/device").assertIsDisplayed()
        compose.onNodeWithText("Waiting for you to approve").assertIsDisplayed()
    }

    @Test
    fun copyingPutsTheCodeOnTheClipboard() {
        show(listOf(CODE_BODY, pending))
        compose.awaitText("WXYZ-7788")

        compose.firstWithDescription("Copy the code").performClick()
        compose.waitForIdle()

        var clipped: String? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipped = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        }
        assertEquals("WXYZ-7788", clipped)
    }

    /**
     * Leaving for the browser tears the screen down. The polling lives in the
     * view model, so coming back must find the same flow rather than a new code.
     */
    @Test
    fun theFlowSurvivesLeavingTheScreenAndComingBack() {
        val transport = Script(
            posts = listOf(CODE_BODY, pending, pending, APPROVAL_BODY),
            gets = mapOf(
                INSTALLATIONS to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"all"}]}""",
            ),
        )
        var onScreen by mutableStateOf(true)

        compose.setContent {
            val model = androidx.compose.runtime.remember {
                AuthorizeViewModel(
                    deviceFlow = DeviceFlow(transport, "cid"),
                    installations = Installations(transport),
                    repositoryUrl = "https://github.com/mipmip/test.git",
                )
            }
            SpecgettyTheme {
                if (onScreen) {
                    AuthorizeScreen(
                        viewModel = model,
                        onAuthorized = { approved = it },
                        onDismiss = {},
                    )
                }
            }
        }

        compose.awaitText("WXYZ-7788")
        compose.runOnUiThread { onScreen = false }
        compose.waitForIdle()
        compose.runOnUiThread { onScreen = true }

        compose.awaitText("Authorized")
        assertEquals("ghu_abc", approved?.token)
    }

    /**
     * The Fairphone's failure, on screen: the network goes while the browser is
     * in front, and the code has to stay put rather than the flow collapsing.
     */
    @Test
    fun aLostConnectionKeepsTheCodeOnScreen() {
        val flaky = object : AuthTransport {
            private var n = 0
            override suspend fun post(url: String, form: Map<String, String>): String {
                if (n++ == 0) return CODE_BODY
                throw java.net.UnknownHostException("Unable to resolve host \"github.com\"")
            }

            override suspend fun get(url: String, bearer: String) = error("never reached")
        }
        compose.setContent {
            val model = androidx.compose.runtime.remember {
                AuthorizeViewModel(
                    deviceFlow = DeviceFlow(flaky, "cid"),
                    installations = Installations(flaky),
                    repositoryUrl = null,
                )
            }
            SpecgettyTheme {
                AuthorizeScreen(model, onAuthorized = { approved = it }, onDismiss = {})
            }
        }

        compose.awaitText("The code is still good", substring = true)
        compose.onNodeWithText("WXYZ-7788").assertIsDisplayed()
    }

    @Test
    fun anExpiredCodeSaysSoAndOffersAFreshOne() {
        show(listOf(CODE_BODY, """{"error":"expired_token"}"""))

        compose.awaitText("expired", substring = true)
        compose.onNodeWithText("Could not authorize").assertIsDisplayed()
        compose.firstWithDescription("Start again").assertIsDisplayed()
    }

    @Test
    fun aDeclinedAuthorizationSaysItWasDeclined() {
        show(listOf(CODE_BODY, """{"error":"access_denied"}"""))
        compose.awaitText("declined", substring = true)
    }

    @Test
    fun anAppThatCannotAuthorizePointsAtTypingAToken() {
        show(listOf(CODE_BODY, """{"error":"device_flow_disabled"}"""))
        compose.awaitText("access token", substring = true)
    }

    @Test
    fun startingAgainAsksForAFreshCode() {
        show(
            listOf(
                CODE_BODY,
                """{"error":"expired_token"}""",
                """{"device_code":"dc2","user_code":"AAAA-1111",
                   "verification_uri":"https://github.com/login/device",
                   "expires_in":900,"interval":1}""",
                pending,
            ),
        )

        compose.awaitText("Could not authorize")
        compose.firstWithDescription("Start again").performClick()
        compose.awaitText("AAAA-1111")
    }

    /** Backing out must leave nothing behind: the caller is never told of a credential. */
    @Test
    fun abandoningStoresNothing() {
        show(listOf(CODE_BODY, pending))
        compose.awaitText("WXYZ-7788")

        compose.firstWithDescription("Back").performClick()
        compose.waitForIdle()

        assertTrue(dismissed)
        assertEquals(null, approved)
    }

    /** The trap the spike fell into: approved, and reaching nothing at all. */
    @Test
    fun anAuthorizationThatReachesNothingDoesNotClaimSuccess() {
        show(
            posts = listOf(CODE_BODY, APPROVAL_BODY),
            gets = mapOf(INSTALLATIONS to """{"total_count":0,"installations":[]}"""),
        )

        compose.awaitText("Almost there")
        compose.onNodeWithText("Choose repositories").assertIsDisplayed()
        assertEquals(null, approved)
    }

    @Test
    fun anAuthorizationMissingThisRepositorySaysWhatItDoesReach() {
        show(
            posts = listOf(CODE_BODY, APPROVAL_BODY),
            gets = mapOf(
                INSTALLATIONS to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"selected"}]}""",
                "$INSTALLATIONS/7/repositories" to
                    """{"repositories":[{"full_name":"someone/else"}]}""",
            ),
        )

        compose.awaitText("someone/else", substring = true)
        assertEquals(null, approved)
    }

    @Test
    fun successNamesEveryRepositoryItWasGranted() {
        show(
            posts = listOf(CODE_BODY, APPROVAL_BODY),
            gets = mapOf(
                INSTALLATIONS to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"selected"}]}""",
                "$INSTALLATIONS/7/repositories" to
                    """{"repositories":[{"full_name":"mipmip/test"},
                        {"full_name":"speclib/specgetty"}]}""",
            ),
        )

        compose.awaitText("Authorized")
        compose.onNodeWithText("mipmip/test", substring = true).assertIsDisplayed()
        compose.onNodeWithText("speclib/specgetty", substring = true).assertIsDisplayed()
        assertEquals("ghu_abc", approved?.token)
    }

    @Test
    fun anInstallationOnEverythingSaysThatRatherThanListingNothing() {
        show(
            posts = listOf(CODE_BODY, APPROVAL_BODY),
            gets = mapOf(
                INSTALLATIONS to """{"total_count":1,"installations":[
                    {"id":7,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"all"}]}""",
            ),
        )

        compose.awaitText("every repository", substring = true)
    }
}
