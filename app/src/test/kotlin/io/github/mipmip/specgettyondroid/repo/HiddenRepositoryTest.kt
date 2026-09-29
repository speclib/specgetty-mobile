package io.github.mipmip.specgettyondroid.repo

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * GitHub answers 404 rather than 403 for a private repository a credential
 * cannot see, so as not to reveal that it exists. That used to be classified as
 * a failure to reach the host, which sent a person off to check a connection
 * that was fine.
 *
 * Driven by a server that really answers 404, so what is pinned is JGit's own
 * behaviour rather than a guess at its wording. Its recorded chain is
 * `InvalidRemoteException: Invalid remote: origin` caused by
 * `NoRemoteRepositoryException: <url>/info/refs?service=git-upload-pack not
 * found: Not Found`.
 */
class HiddenRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val servers = mutableListOf<GitHttpServer>()

    @After
    fun tearDown() = servers.forEach { it.stop() }

    private fun hiddenRemote(): String {
        val remote = LocalRemote(temp.newFolder("remote")).init()
        val server = GitHttpServer(remote.dir, hidden = true).start()
        servers += server
        return server.url
    }

    private fun store() = RepoStore(temp.newFolder("repos"))

    @Test
    fun `a repository the credential cannot see is no access, not a dead network`() = runTest {
        val result = store().clone("r", hiddenRemote(), "ghu_abc")

        val error = (result as RepoResult.Failure).error
        assertTrue("$error", error is RepoError.NoAccessToRepository)
    }

    /** Without a credential the URL really may just be wrong. */
    @Test
    fun `the same answer with no credential is not blamed on access`() = runTest {
        val result = store().clone("r", hiddenRemote(), null)

        val error = (result as RepoResult.Failure).error
        assertTrue("$error", error !is RepoError.NoAccessToRepository)
    }

    @Test
    fun `a blank token counts as no credential`() = runTest {
        val result = store().clone("r", hiddenRemote(), "   ")

        val error = (result as RepoResult.Failure).error
        assertTrue("$error", error !is RepoError.NoAccessToRepository)
    }

    /**
     * An ephemeral port can contain the digits `404`, which is why the check is
     * on the exception type and not on the recorded message.
     */
    @Test
    fun `a host that cannot be resolved is still a network failure with a credential`() = runTest {
        val result = store().clone("r", "https://no-such-host.invalid/a/b.git", "ghu_abc")

        val error = (result as RepoResult.Failure).error
        assertTrue("$error", error is RepoError.Network)
    }

    /** A rejected credential stays an authentication failure. */
    @Test
    fun `a rejected credential is not mistaken for a hidden repository`() = runTest {
        val remote = LocalRemote(temp.newFolder("remote2")).init()
        val server = GitHttpServer(remote.dir, requiredToken = "right").start()
        servers += server

        val result = store().clone("r", server.url, "wrong")

        val error = (result as RepoResult.Failure).error
        assertTrue("$error", error is RepoError.Authentication)
    }
}
