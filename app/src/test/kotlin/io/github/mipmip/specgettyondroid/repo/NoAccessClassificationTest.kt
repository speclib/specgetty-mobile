package io.github.mipmip.specgettyondroid.repo

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.net.UnknownHostException

/**
 * A credential the host accepts, on a repository it will not serve, is not an
 * authentication failure. GitHub says so with a 403 whose text mentions write
 * access even for a read, and `403` is an authentication marker too, so the
 * order of the checks is what this pins.
 *
 * Every message here is one a real refusal produced during the spike against a
 * live GitHub App, not one invented to fit the code.
 */
class NoAccessClassificationTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val store by lazy { RepoStore(temp.newFolder(), Dispatchers.Unconfined) }

    private fun classify(message: String): RepoError = store.classify(IOException(message))

    @Test
    fun `the refusal the spike recorded is a missing grant, not a bad credential`() {
        val error = classify(
            "Write access to repository not granted. " +
                "The requested URL returned error: 403",
        )
        assertTrue("$error", error is RepoError.NoAccessToRepository)
    }

    @Test
    fun `that message would otherwise have been read as an authentication failure`() {
        // It carries 403, which is an authentication marker. Before this change
        // the app told the reader to check a token that was perfectly fine.
        val text = "Write access to repository not granted. error: 403"
        assertTrue("the trap this guards", text.contains("403"))
        assertTrue(classify(text) !is RepoError.Authentication)
    }

    @Test
    fun `an integration refused a resource is also a missing grant`() {
        val error = classify("Resource not accessible by integration")
        assertTrue("$error", error is RepoError.NoAccessToRepository)
    }

    @Test
    fun `a genuinely rejected credential is still an authentication failure`() {
        listOf(
            "not authorized",
            "Authentication is required",
            "authentication failed",
            "invalid credentials",
            "The requested URL returned error: 401",
        ).forEach { message ->
            val error = classify(message)
            assertTrue("$message -> $error", error is RepoError.Authentication)
        }
    }

    @Test
    fun `a plain 403 with no other clue is still an authentication failure`() {
        // Nothing says the credential is fine, so the conservative reading wins.
        assertTrue(classify("The requested URL returned error: 403") is RepoError.Authentication)
    }

    @Test
    fun `an unreachable host is a network failure`() {
        assertTrue(store.classify(UnknownHostException("no-such-host.invalid")) is RepoError.Network)
        assertTrue(classify("Connection refused") is RepoError.Network)
    }

    @Test
    fun `something unrecognised is unknown rather than guessed at`() {
        assertTrue(classify("the disk caught fire") is RepoError.Unknown)
    }

    @Test
    fun `the message points at the authorization rather than the credential`() {
        val error = classify("Write access to repository not granted 403")
        assertTrue(error.message.contains("repository"))
        assertTrue(
            "it must not tell the person their credential is wrong",
            !error.message.lowercase().contains("authentication"),
        )
    }
}
