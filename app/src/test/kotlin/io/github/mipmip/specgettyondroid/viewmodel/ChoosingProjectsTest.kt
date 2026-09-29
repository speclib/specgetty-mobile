package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.repo.GitHttpServer
import io.github.mipmip.specgettyondroid.repo.LocalRemote
import io.github.mipmip.specgettyondroid.repo.RepoStore
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
import java.io.File

/**
 * `nivis-openspec-stores` holds four projects, one per top-level directory.
 * Adding it used to report that it held none.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChoosingProjectsTest {

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
     * A remote holding a project in each of the named directories and nowhere
     * else. Built on [LocalRemote.initBare], because [LocalRemote.init] puts a
     * project at the root and would add a fifth nobody asked for.
     */
    private fun remoteHolding(vararg paths: String): String {
        val remote = LocalRemote(temp.newFolder()).initBare()
        paths.forEach { path ->
            val prefix = if (path.isEmpty()) "" else "$path/"
            remote.commit(
                "${prefix}openspec/config.yaml",
                "schema: spec-driven\n",
                "add $path",
            )
            val name = path.ifEmpty { "root" }
            remote.commit(
                "${prefix}openspec/specs/thing-$name/spec.md",
                "# thing\n\n## Requirements\n",
                "spec for $path",
            )
        }
        val server = GitHttpServer(remote.dir).start()
        servers += server
        return server.url
    }

    private fun TestScope.model(): RepoListViewModel {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        return RepoListViewModel(repository, scope)
    }

    @Test
    fun `a repository holding one project is added with nothing to choose`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm(remoteHolding(""))
            vm.submit()
            advanceUntilIdle()

            assertNull(vm.choice.value)
            assertEquals(1, vm.rows.value.size)
            assertEquals("", vm.rows.value.single().config.path)
        }

    @Test
    fun `a repository holding four projects offers them and adds nothing yet`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            vm.openForm(remoteHolding("nivis", "nivis-demos", "nivis-tunnel", "registry"))
            vm.submit()
            advanceUntilIdle()

            val choice = vm.choice.value
            assertNotNull(choice)
            assertEquals(
                listOf("nivis", "nivis-demos", "nivis-tunnel", "registry"),
                choice!!.projects,
            )
            assertTrue(vm.rows.value.isEmpty())
        }

    @Test
    fun `choosing two of four adds two rows`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm(remoteHolding("nivis", "nivis-demos", "nivis-tunnel", "registry"))
        vm.submit()
        advanceUntilIdle()

        vm.toggleChoice("nivis")
        vm.toggleChoice("registry")
        vm.confirmChoice()
        advanceUntilIdle()

        assertEquals(listOf("nivis", "registry"), vm.rows.value.map { it.config.path })
        assertNull(vm.choice.value)
    }

    @Test
    fun `both chosen rows load their own project`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm(remoteHolding("nivis", "registry"))
        vm.submit()
        advanceUntilIdle()
        vm.toggleChoice("nivis")
        vm.toggleChoice("registry")
        vm.confirmChoice()
        advanceUntilIdle()

        vm.rows.value.forEach { row ->
            assertTrue("${row.config.path}: ${row.state}", row.canOpen)
        }
        val names = vm.rows.value.map { row ->
            (row.state as ProjectState.Loaded).index.project.capabilities.map { it.name }
        }
        assertEquals(listOf(listOf("thing-nivis"), listOf("thing-registry")), names)
    }

    @Test
    fun `abandoning the choice adds nothing and keeps the working copy`() =
        runTest(StandardTestDispatcher()) {
            val vm = model()
            val url = remoteHolding("nivis", "registry")
            vm.openForm(url)
            vm.submit()
            advanceUntilIdle()

            vm.cancelChoice()
            advanceUntilIdle()

            assertTrue(vm.rows.value.isEmpty())
            assertNull(vm.choice.value)
            assertTrue(
                store.isCloned(io.github.mipmip.specgettyondroid.store.repoIdFor(url)),
            )
        }

    @Test
    fun `confirming nothing chosen does nothing`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm(remoteHolding("nivis", "registry"))
        vm.submit()
        advanceUntilIdle()

        vm.confirmChoice()
        advanceUntilIdle()

        assertTrue(vm.rows.value.isEmpty())
        assertNotNull(vm.choice.value)
    }

    @Test
    fun `toggling a project on and off again deselects it`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm(remoteHolding("nivis", "registry"))
        vm.submit()
        advanceUntilIdle()

        vm.toggleChoice("nivis")
        assertTrue(vm.choice.value!!.canConfirm)
        vm.toggleChoice("nivis")
        assertFalse(vm.choice.value!!.canConfirm)
    }

    @Test
    fun `two rows on one repository share one working copy`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm(remoteHolding("nivis", "registry"))
        vm.submit()
        advanceUntilIdle()
        vm.toggleChoice("nivis")
        vm.toggleChoice("registry")
        vm.confirmChoice()
        advanceUntilIdle()

        val cloneIds = vm.rows.value.map { it.config.cloneId }.distinct()
        assertEquals(1, cloneIds.size)
        assertEquals(1, File(temp.root, "repos").listFiles().orEmpty().size)
    }

    @Test
    fun `removing one of two rows keeps the other loading`() = runTest(StandardTestDispatcher()) {
        val vm = model()
        vm.openForm(remoteHolding("nivis", "registry"))
        vm.submit()
        advanceUntilIdle()
        vm.toggleChoice("nivis")
        vm.toggleChoice("registry")
        vm.confirmChoice()
        advanceUntilIdle()

        vm.askToRemove(vm.rows.value.first().config.id)
        vm.confirmRemoval()
        advanceUntilIdle()

        val remaining = vm.rows.value.single()
        assertEquals("registry", remaining.config.path)
        assertTrue("${remaining.state}", remaining.canOpen)
    }

    @Test
    fun `a repository holding no project reports that, as before`() =
        runTest(StandardTestDispatcher()) {
            val remote = LocalRemote(temp.newFolder()).initBare()
            val server = GitHttpServer(remote.dir).start()
            servers += server

            val vm = model()
            vm.openForm(server.url)
            vm.submit()
            advanceUntilIdle()

            assertNull(vm.choice.value)
            assertEquals("No OpenSpec project here", vm.rows.value.single().message)
        }
}
