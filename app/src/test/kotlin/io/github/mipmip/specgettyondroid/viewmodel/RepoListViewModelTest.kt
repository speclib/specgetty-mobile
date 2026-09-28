package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.repo.GitHttpServer
import io.github.mipmip.specgettyondroid.repo.LocalRemote
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.FakeVault
import io.github.mipmip.specgettyondroid.store.InMemoryPreferences
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import io.github.mipmip.specgettyondroid.tasks.TaskStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RepoListViewModelTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var store: RepoStore
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
        store = RepoStore(temp.newFolder("repos"), Dispatchers.Unconfined)
        vault = FakeVault()
        repository = ProjectRepository(
            store,
            RepoRegistry(InMemoryPreferences(), vault),
            Dispatchers.Unconfined,
        )
    }

    /**
     * A repository served over HTTP rather than `file://`. The app validates a
     * URL before cloning it and refuses anything that is not http or https, so
     * a `file://` fixture would never reach the code under test.
     */
    private fun served(remote: LocalRemote, token: String? = null): String {
        val server = GitHttpServer(remote.dir, token).start()
        servers += server
        return server.url
    }

    private fun remote(build: LocalRemote.() -> Unit = {}) =
        LocalRemote(temp.newFolder()).init().apply(build)

    private fun servedRemote(build: LocalRemote.() -> Unit = {}): Pair<LocalRemote, String> {
        val r = remote(build)
        return r to served(r)
    }

    /**
     * A scope of the test's own rather than the test scope itself. The view
     * model collects the repository flow for as long as it lives, and a collect
     * that never completes would make `runTest` wait for it forever. It shares
     * the test's scheduler, so virtual time still applies, and it is cancelled
     * in teardown.
     */
    private fun TestScope.model(): RepoListViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        return RepoListViewModel(repository, scope)
    }

    @Test
    fun `an empty list starts empty`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        advanceUntilIdle()
        assertTrue(vm.rows.value.isEmpty())
    }

    @Test
    fun `adding a repository puts a loaded row in the list`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote()))
        vm.submit()
        advanceUntilIdle()

        val row = vm.rows.value.single()
        assertTrue("${row.state}", row.state is ProjectState.Loaded)
        assertTrue(row.canOpen)
        assertNull(row.message)
        assertEquals(1, row.counts?.specs)
    }

    @Test
    fun `the row carries the statistics`() = runTest(StandardTestDispatcher()) {
        val r = remote {
            commit("openspec/changes/first-change/tasks.md", "- [x] 1.1 a\n- [ ] 1.2 b\n", "tasks")
            commit("openspec/specs/second/spec.md", LocalRemote.spec("second"), "second spec")
            commit("openspec/changes/archive/2026-01-01-old/proposal.md", "## Why\n\nx\n", "old")
        }
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(r))
        vm.submit()
        advanceUntilIdle()

        val counts = vm.rows.value.single().counts!!
        assertEquals(2, counts.specs)
        assertEquals(1, counts.active)
        assertEquals(1, counts.archived)
        assertEquals(TaskStats(1, 2), counts.tasks)
    }

    @Test
    fun `a project with no tasks reports an empty task figure`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm()
            vm.onUrlChanged(
                served(
                    remote {
                        removeAndCommit("openspec/changes/first-change/tasks.md", "no tasks")
                    },
                ),
            )
            vm.submit()
            advanceUntilIdle()
            assertTrue(vm.rows.value.single().counts!!.tasks.isEmpty)
        }

    @Test
    fun `an invalid URL is refused before anything is attempted`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm()
            vm.onUrlChanged("git@github.com:speclib/specgetty.git")
            vm.submit()
            advanceUntilIdle()

            assertTrue(vm.rows.value.isEmpty())
            assertTrue("${vm.form.value.error}", vm.form.value.error!!.contains("SSH"))
            assertTrue("the form stays open so it can be corrected", vm.formOpen.value)
        }

    @Test
    fun `an empty URL is refused`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.form.value.error)
        assertTrue(vm.rows.value.isEmpty())
    }

    @Test
    fun `typing clears the previous error`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.form.value.error)

        vm.onUrlChanged("https://example.test/a.git")
        assertNull(vm.form.value.error)
    }

    @Test
    fun `nothing is added before the form is submitted`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote()))
        advanceUntilIdle()
        assertTrue(vm.rows.value.isEmpty())
    }

    @Test
    fun `the form can be opened already filled in`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm("https://github.com/speclib/specgetty")
        assertEquals("https://github.com/speclib/specgetty", vm.form.value.url)
        assertTrue(vm.formOpen.value)
        assertTrue(vm.rows.value.isEmpty())
    }

    @Test
    fun `a captured URL fills the form, normalised, and adds nothing`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            val found = vm.capture("look at https://github.com/speclib/specgetty/issues/12 please")
            advanceUntilIdle()

            assertTrue(found)
            assertEquals("https://github.com/speclib/specgetty", vm.form.value.url)
            assertTrue(vm.formOpen.value)
            assertTrue(vm.rows.value.isEmpty())
        }

    @Test
    fun `captured text with no URL says so and fills nothing`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            assertFalse(vm.capture("nothing useful here"))
            assertEquals("", vm.form.value.url)
            assertNotNull(vm.form.value.error)
        }

    @Test
    fun `a captured URL never fills the token field`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.capture("https://user:ghp_secret@github.com/a/b")
        assertEquals("", vm.form.value.token)
        assertFalse(vm.form.value.url.contains("ghp_secret"))
    }

    @Test
    fun `the form is cleared after a successful add`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote(), token = "ghp_secret"))
        vm.onLabelChanged("My specs")
        vm.onTokenChanged("ghp_secret")
        vm.submit()
        advanceUntilIdle()

        assertTrue(vm.form.value.isBlank)
        assertFalse(vm.formOpen.value)
    }

    @Test
    fun `a token is used and stored but never shown back`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote(), token = "ghp_secret"))
        vm.onTokenChanged("ghp_secret")
        vm.submit()
        advanceUntilIdle()

        assertEquals("ghp_secret", vault.tokens.values.single())
        assertTrue(vm.rows.value.single().config.hasToken)

        vm.openForm()
        assertEquals("", vm.form.value.token)
    }

    @Test
    fun `a typed label is used`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote()))
        vm.onLabelChanged("My specs")
        vm.submit()
        advanceUntilIdle()
        assertEquals("My specs", vm.rows.value.single().config.label)
    }

    @Test
    fun `an unreachable host leaves a row explaining itself`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm()
            vm.onUrlChanged("https://no-such-host.invalid/a/b.git")
            vm.submit()
            advanceUntilIdle()

            val row = vm.rows.value.single()
            assertFalse(row.canOpen)
            assertEquals("The repository could not be reached.", row.message)
        }

    @Test
    fun `a refused token says the authentication failed`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote(), token = "the-right-token"))
        vm.onTokenChanged("the-wrong-token")
        vm.submit()
        advanceUntilIdle()

        val row = vm.rows.value.single()
        assertFalse(row.canOpen)
        assertEquals("Authentication failed. Check the access token.", row.message)
    }

    @Test
    fun `the right token gets through`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote(), token = "the-right-token"))
        vm.onTokenChanged("the-right-token")
        vm.submit()
        advanceUntilIdle()

        val row = vm.rows.value.single()
        assertTrue("${row.state}", row.canOpen)
        assertNull(row.message)
    }

    @Test
    fun `no token at all against a private repository fails authentication`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm()
            vm.onUrlChanged(served(remote(), token = "the-right-token"))
            vm.submit()
            advanceUntilIdle()

            assertEquals(
                "Authentication failed. Check the access token.",
                vm.rows.value.single().message,
            )
        }

    @Test
    fun `a repository with no openspec directory says so`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(LocalRemote(temp.newFolder()).initBare()))
        vm.submit()
        advanceUntilIdle()

        assertEquals("No OpenSpec project here", vm.rows.value.single().message)
    }

    @Test
    fun `an empty project is not an error`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(LocalRemote(temp.newFolder()).initEmptyProject()))
        vm.submit()
        advanceUntilIdle()

        val row = vm.rows.value.single()
        assertNull(row.message)
        assertEquals(0, row.counts?.specs)
        assertTrue(row.canOpen)
    }

    @Test
    fun `a failed repository can be retried without being removed`() =
        runTest(StandardTestDispatcher()) {
            val r = remote()
            val vm = model()
            vm.openForm()
            vm.onUrlChanged("https://no-such-host.invalid/a/b.git")
            vm.submit()
            advanceUntilIdle()
            val id = vm.rows.value.single().config.id
            assertNotNull(vm.rows.value.single().message)

            // The retry path is the same one a pull-to-refresh uses.
            vm.refresh(id)
            advanceUntilIdle()
            assertEquals("still one row, still failed", 1, vm.rows.value.size)
            assertNotNull(vm.rows.value.single().message)
            assertTrue(r.commitCount() > 0)
        }

    @Test
    fun `refreshing picks up a new spec`() = runTest(StandardTestDispatcher()) {
        val r = remote()
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(r))
        vm.submit()
        advanceUntilIdle()
        assertEquals(1, vm.rows.value.single().counts?.specs)

        r.commit("openspec/specs/later/spec.md", LocalRemote.spec("later"), "later")
        vm.refreshAll()
        advanceUntilIdle()

        assertEquals(2, vm.rows.value.single().counts?.specs)
        assertFalse(vm.refreshing.value)
    }

    @Test
    fun `removal is confirmed before it happens`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm()
        vm.onUrlChanged(served(remote()))
        vm.submit()
        advanceUntilIdle()
        val id = vm.rows.value.single().config.id

        vm.askToRemove(id)
        assertEquals(id, vm.pendingRemoval.value)
        advanceUntilIdle()
        assertEquals("nothing removed until confirmed", 1, vm.rows.value.size)

        vm.cancelRemoval()
        advanceUntilIdle()
        assertNull(vm.pendingRemoval.value)
        assertEquals(1, vm.rows.value.size)

        vm.askToRemove(id)
        vm.confirmRemoval()
        advanceUntilIdle()
        assertTrue(vm.rows.value.isEmpty())
        assertFalse(store.isCloned(id))
    }

    @Test
    fun `the first repository is active and switching changes that`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm()
            vm.onUrlChanged(served(remote()))
            vm.submit()
            advanceUntilIdle()
            vm.openForm()
            vm.onUrlChanged(served(remote()))
            vm.submit()
            advanceUntilIdle()

            assertTrue(vm.rows.value[0].isActive)
            assertFalse(vm.rows.value[1].isActive)

            vm.activate(vm.rows.value[1].config.id)
            advanceUntilIdle()
            assertTrue(vm.rows.value[1].isActive)
            assertFalse(vm.rows.value[0].isActive)
        }
}
