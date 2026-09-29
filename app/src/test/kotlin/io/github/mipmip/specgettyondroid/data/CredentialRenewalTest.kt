package io.github.mipmip.specgettyondroid.data

import io.github.mipmip.specgettyondroid.auth.AuthTransport
import io.github.mipmip.specgettyondroid.auth.DeviceFlow
import io.github.mipmip.specgettyondroid.auth.TokenSource
import io.github.mipmip.specgettyondroid.repo.GitHttpServer
import io.github.mipmip.specgettyondroid.repo.LocalRemote
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.store.FakeVault
import io.github.mipmip.specgettyondroid.store.InMemoryPreferences
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * A credential that expires while the app still holds the repository. Renewal
 * happens before the read rather than after a failure, because a refused read
 * returns 403 and so does a repository the installation does not cover.
 */
class CredentialRenewalTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val servers = mutableListOf<GitHttpServer>()

    @After
    fun tearDown() = servers.forEach { it.stop() }

    private fun served(remote: LocalRemote, token: String?): String {
        val server = GitHttpServer(remote.dir, token).start()
        servers += server
        return server.url
    }

    /** Answers the renewal, and records that it was asked. */
    private class Renewer(private val newToken: String, private val refused: Boolean = false) :
        AuthTransport {

        var asked = 0
            private set

        override suspend fun post(url: String, form: Map<String, String>): String {
            asked++
            return if (refused) {
                """{"error":"bad_refresh_token"}"""
            } else {
                """{"access_token":"$newToken","expires_in":28800,"refresh_token":"ghr_next"}"""
            }
        }

        override suspend fun get(url: String, bearer: String) = error("not used here")
    }

    private var clock = 1_000_000_000_000L

    private fun build(renewer: AuthTransport): Triple<ProjectRepository, FakeVault, RepoRegistry> {
        val vault = FakeVault()
        val registry = RepoRegistry(InMemoryPreferences(), vault)
        val repository = ProjectRepository(
            RepoStore(temp.newFolder("repos"), Dispatchers.Unconfined),
            registry,
            Dispatchers.Unconfined,
            TokenSource(vault, DeviceFlow(renewer, "cid")) { clock },
        )
        return Triple(repository, vault, registry)
    }

    /**
     * The host is served by a second server that accepts only the renewed
     * token, so a refresh that kept the old one would fail rather than pass
     * quietly.
     */
    @Test
    fun `an expired credential renews itself and the read goes on uninterrupted`() = runTest {
        val remote = LocalRemote(temp.newFolder("remote")).init()
        val renewer = Renewer(newToken = "ghu_second")
        val (repository, vault, _) = build(renewer)

        val url = served(remote, token = "ghu_second")
        val config = repository.addAuthorized(
            url,
            credential = Credential.GitHub(
                token = "ghu_second",
                expiresAtMillis = clock + 28_800_000,
                refreshToken = "ghr_first",
            ),
        )
        assertTrue("${repository.stateOf(config.id)}", repository.stateOf(config.id) is ProjectState.Loaded)

        // Eight hours and a minute later.
        clock += 28_860_000

        repository.refresh(config.id)

        assertEquals(1, renewer.asked)
        assertTrue(
            "${repository.stateOf(config.id)}",
            repository.stateOf(config.id) is ProjectState.Loaded,
        )

        val stored = Credential.decode(vault.tokens.getValue(config.id))
        assertEquals("ghu_second", stored.token)
        assertEquals("ghr_next", (stored as Credential.GitHub).refreshToken)
        assertEquals(clock + 28_800_000, stored.expiresAtMillis)
    }

    @Test
    fun `a credential still well inside its life is not renewed`() = runTest {
        val remote = LocalRemote(temp.newFolder("remote")).init()
        val renewer = Renewer(newToken = "ghu_second")
        val (repository, _, _) = build(renewer)

        val url = served(remote, token = "ghu_first")
        val config = repository.addAuthorized(
            url,
            credential = Credential.GitHub(
                token = "ghu_first",
                expiresAtMillis = clock + 28_800_000,
                refreshToken = "ghr_first",
            ),
        )

        repository.refresh(config.id)

        assertEquals(0, renewer.asked)
        assertTrue(repository.stateOf(config.id) is ProjectState.Loaded)
    }

    /** A refused renewal says to authorize again, not that the token is wrong. */
    @Test
    fun `a refused renewal asks for authorization rather than blaming the token`() = runTest {
        val remote = LocalRemote(temp.newFolder("remote")).init()
        val (repository, _, _) = build(Renewer("unused", refused = true))

        val url = served(remote, token = "ghu_first")
        val config = repository.addAuthorized(
            url,
            credential = Credential.GitHub(
                token = "ghu_first",
                expiresAtMillis = clock + 28_800_000,
                refreshToken = "ghr_first",
            ),
        )

        clock += 28_860_000
        repository.refresh(config.id)

        val state = repository.stateOf(config.id)
        assertTrue("$state", state is ProjectState.Failed)
        val error = (state as ProjectState.Failed).error
        assertTrue("$error", error is RepoError.Authentication)
        assertTrue(error.message, error.message.contains("Authorize"))
    }

    /** A typed token never expires as far as the app knows, so it is never renewed. */
    @Test
    fun `a typed token is passed through untouched`() = runTest {
        val remote = LocalRemote(temp.newFolder("remote")).init()
        val renewer = Renewer(newToken = "ghu_second")
        val (repository, _, _) = build(renewer)

        val url = served(remote, token = "ghp_typed")
        val config = repository.add(url, token = "ghp_typed")

        clock += 10_000_000_000L
        repository.refresh(config.id)

        assertEquals(0, renewer.asked)
        assertTrue(repository.stateOf(config.id) is ProjectState.Loaded)
    }
}
