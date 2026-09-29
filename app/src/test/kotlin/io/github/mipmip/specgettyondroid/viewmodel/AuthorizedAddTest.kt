package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.GitHttpServer
import io.github.mipmip.specgettyondroid.repo.LocalRemote
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.store.FakeVault
import io.github.mipmip.specgettyondroid.store.InMemoryPreferences
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The add form's half of authorizing: offering it, holding it, and using it. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthorizedAddTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var vault: FakeVault
    private lateinit var repository: ProjectRepository
    private val servers = mutableListOf<GitHttpServer>()
    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        servers.forEach { it.stop() }
    }

    @Before
    fun setUp() {
        vault = FakeVault()
        repository = ProjectRepository(
            RepoStore(temp.newFolder("repos"), Dispatchers.Unconfined),
            RepoRegistry(InMemoryPreferences(), vault),
            Dispatchers.Unconfined,
        )
    }

    private fun served(remote: LocalRemote, token: String? = null): String {
        val server = GitHttpServer(remote.dir, token).start()
        servers += server
        return server.url
    }

    private fun TestScope.model(): RepoListViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        return RepoListViewModel(repository, scope)
    }

    private fun credential(token: String = "ghu_abc") =
        Credential.GitHub(token, expiresAtMillis = 9_999_999_999_999L, refreshToken = "ghr_xyz")

    @Test
    fun `a github url is one the app can authorize for`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm("https://github.com/mipmip/test.git")
        assertTrue(vm.form.value.canAuthorize)
    }

    @Test
    fun `a gitlab url is not`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm("https://gitlab.com/mipmip/test.git")
        assertFalse(vm.form.value.canAuthorize)
    }

    @Test
    fun `an unrecognised host is not`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm("https://git.example.test/mipmip/test.git")
        assertFalse(vm.form.value.canAuthorize)
    }

    /**
     * A GitHub App is registered on one host. An Enterprise installation is a
     * different app with a different client id, so offering the button there
     * would promise what this app cannot do.
     */
    @Test
    fun `a github enterprise host is not`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm("https://github.example.test/mipmip/test.git")
        assertFalse(vm.form.value.canAuthorize)
    }

    @Test
    fun `a github url without an owner and name is not`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm("https://github.com/mipmip")
        assertFalse(vm.form.value.canAuthorize)
    }

    @Test
    fun `an empty form offers nothing`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        assertFalse(vm.form.value.canAuthorize)
    }

    @Test
    fun `the credential is held but not in any field a screen binds`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm("https://github.com/mipmip/test.git")
            vm.onAuthorized(credential())

            val form = vm.form.value
            assertTrue(form.hasAuthorized)
            assertEquals("", form.token)
            assertFalse(form.url.contains("ghu_"))
            assertFalse(form.label.contains("ghu_"))
        }

    @Test
    fun `authorizing adds nothing until it is confirmed`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm("https://github.com/mipmip/test.git")
        vm.onAuthorized(credential())
        advanceUntilIdle()

        assertTrue(repository.current().repos.isEmpty())
        assertTrue(vault.tokens.isEmpty())
    }

    @Test
    fun `forgetting an authorization puts the typed field back`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm("https://github.com/mipmip/test.git")
            vm.onAuthorized(credential())
            vm.discardAuthorization()

            assertFalse(vm.form.value.hasAuthorized)
        }

    @Test
    fun `confirming stores the expiry and the renewal material, not a bare token`() =
        runTest(StandardTestDispatcher()) {
            val remote = LocalRemote(temp.newFolder("remote")).init()
            val vm = model()
            vm.openForm(served(remote, token = "ghu_abc"))
            vm.onAuthorized(credential())
            vm.submit()
            advanceUntilIdle()

            val stored = vault.tokens.values.single()
            val decoded = Credential.decode(stored)
            assertTrue("$decoded", decoded is Credential.GitHub)
            assertEquals("ghu_abc", decoded.token)
            assertEquals("ghr_xyz", (decoded as Credential.GitHub).refreshToken)
            assertNotNull(decoded.expiresAtMillis)
        }

    /** What git is sent is the bare token, whatever shape the vault holds. */
    @Test
    fun `an authorized add clones with the token the host expects`() =
        runTest(StandardTestDispatcher()) {
            val remote = LocalRemote(temp.newFolder("remote")).init()
            val vm = model()
            vm.openForm(served(remote, token = "ghu_abc"))
            vm.onAuthorized(credential())
            vm.submit()
            advanceUntilIdle()

            val row = vm.rows.value.single()
            assertTrue("${row.state}", row.canOpen)
        }

    @Test
    fun `the typed token path is untouched by any of this`() =
        runTest(StandardTestDispatcher()) {
            val remote = LocalRemote(temp.newFolder("remote")).init()
            val vm = model()
            vm.openForm(served(remote, token = "ghp_secret"))
            vm.onTokenChanged("ghp_secret")
            vm.submit()
            advanceUntilIdle()

            assertEquals("ghp_secret", vault.tokens.values.single())
            assertTrue(vm.rows.value.single().canOpen)
        }

    @Test
    fun `adding clears the held authorization`() = runTest(StandardTestDispatcher()) {
        val remote = LocalRemote(temp.newFolder("remote")).init()
        val vm = model()
        vm.openForm(served(remote, token = "ghu_abc"))
        vm.onAuthorized(credential())
        vm.submit()
        advanceUntilIdle()

        assertNull(vm.form.value.authorized)
    }

    /** Removing takes the renewal material with it, not only the token. */
    @Test
    fun `removing an authorized repository leaves nothing in the vault`() =
        runTest(StandardTestDispatcher()) {
            val remote = LocalRemote(temp.newFolder("remote")).init()
            val vm = model()
            vm.openForm(served(remote, token = "ghu_abc"))
            vm.onAuthorized(credential())
            vm.submit()
            advanceUntilIdle()

            vm.askToRemove(vm.rows.value.single().config.id)
            vm.confirmRemoval()
            advanceUntilIdle()

            assertTrue("${vault.tokens}", vault.tokens.isEmpty())
        }
}
