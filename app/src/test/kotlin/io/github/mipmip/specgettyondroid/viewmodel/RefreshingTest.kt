package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.data.indexOrNull
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
 * The gesture as the five screens see it: a fetch they can ask for, a reason
 * when it fails, and what a refresh does to what is already open.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RefreshingTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var store: RepoStore
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
        repository = ProjectRepository(
            store,
            RepoRegistry(InMemoryPreferences(), FakeVault()),
            Dispatchers.Unconfined,
        )
    }

    private fun served(remote: LocalRemote): String {
        val server = GitHttpServer(remote.dir, null).start()
        servers += server
        return server.url
    }

    private fun TestScope.scope(): CoroutineScope =
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).also { scopes += it }

    private val delta = "## ADDED Requirements\n\n### Requirement: A\nProse.\n\n" +
        "#### Scenario: s\n- **WHEN** a\n- **THEN** b\n"

    @Test
    fun `pulling on the project view fetches and recomputes the counts`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(served(remote))
        val vm = ProjectViewModel(repository, config.id, scope())
        advanceUntilIdle()
        assertEquals(1, vm.index?.counts?.specs)

        remote.commit("openspec/specs/later/spec.md", LocalRemote.spec("later"), "later")
        vm.refresh.refresh()
        advanceUntilIdle()

        assertEquals(2, vm.index?.counts?.specs)
    }

    @Test
    fun `the project view keeps its project and its tab through a failed refresh`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(remote.url)
        val vm = ProjectViewModel(repository, config.id, scope())
        advanceUntilIdle()
        vm.selectTab(ProjectTab.SPECS)
        val before = vm.index?.counts

        File(remote.url.removePrefix("file:")).deleteRecursively()
        vm.refresh.refresh()
        advanceUntilIdle()

        assertEquals(before, vm.index?.counts)
        assertEquals(ProjectTab.SPECS, vm.tab.value)
        assertNull(vm.message)
        assertNotNull(vm.refresh.failure.value)
    }

    @Test
    fun `a failure already told is not told again`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(remote.url)
        val vm = ProjectViewModel(repository, config.id, scope())
        advanceUntilIdle()

        File(remote.url.removePrefix("file:")).deleteRecursively()
        vm.refresh.refresh()
        advanceUntilIdle()
        assertNotNull(vm.refresh.failure.value)

        vm.refresh.failureShown()
        advanceUntilIdle()
        assertNull(vm.refresh.failure.value)
    }

    @Test
    fun `a project with nothing to show still says why after a failed refresh`() = runTest {
        val config = repository.add("https://no-such-host.invalid/a/b.git")
        val vm = ProjectViewModel(repository, config.id, scope())
        advanceUntilIdle()

        vm.refresh.refresh()
        advanceUntilIdle()

        assertTrue("${vm.state.value}", vm.state.value is ProjectState.Failed)
        assertNotNull(vm.message)
    }

    @Test
    fun `an artifact the refresh rewrote is read again`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/changes/one/proposal.md", "## Why\n\nFirst.\n", "p")
        }
        val config = repository.add(served(remote))
        val vm = ChangeViewModel(repository, config.id, "one", scope())
        advanceUntilIdle()

        val first = vm.artifact("proposal.md")
        assertEquals(ArtifactContent.Markdown("## Why\n\nFirst.\n"), first)
        assertEquals(1, vm.reads)

        remote.commit("openspec/changes/one/proposal.md", "## Why\n\nSecond.\n", "p2")
        vm.refresh.refresh()
        advanceUntilIdle()

        assertEquals(ArtifactContent.Markdown("## Why\n\nSecond.\n"), vm.artifact("proposal.md"))
        assertEquals(2, vm.reads)
    }

    @Test
    fun `an artifact the refresh left alone is not read again`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/changes/one/proposal.md", "## Why\n\nFirst.\n", "p")
            commit("openspec/changes/one/design.md", "## Context\n\nThings.\n", "d")
        }
        val config = repository.add(served(remote))
        val vm = ChangeViewModel(repository, config.id, "one", scope())
        advanceUntilIdle()

        vm.artifact("proposal.md")
        assertEquals(1, vm.reads)

        remote.commit("openspec/changes/one/design.md", "## Context\n\nOther.\n", "d2")
        vm.refresh.refresh()
        advanceUntilIdle()

        vm.artifact("proposal.md")
        assertEquals(1, vm.reads)
    }

    @Test
    fun `a tab the refresh took away gives way to another`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/changes/one/design.md", "## Context\n\nThings.\n", "d")
            commit("openspec/changes/one/proposal.md", "## Why\n\nFirst.\n", "p")
        }
        val config = repository.add(served(remote))
        val vm = ChangeViewModel(repository, config.id, "one", scope())
        advanceUntilIdle()
        vm.selectTab(ChangeTab.Artifact("design.md"))

        remote.removeAndCommit("openspec/changes/one/design.md", "gone")
        vm.refresh.refresh()
        advanceUntilIdle()

        assertFalse(vm.tabs.contains(ChangeTab.Artifact("design.md")))
        assertTrue("${vm.tab.value}", vm.tab.value in vm.tabs)
    }

    @Test
    fun `a tab the refresh added is there`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/changes/one/proposal.md", "## Why\n\nFirst.\n", "p")
        }
        val config = repository.add(served(remote))
        val vm = ChangeViewModel(repository, config.id, "one", scope())
        advanceUntilIdle()
        assertFalse(vm.tabs.contains(ChangeTab.Artifact("design.md")))

        remote.commit("openspec/changes/one/design.md", "## Context\n\nThings.\n", "d")
        vm.refresh.refresh()
        advanceUntilIdle()

        assertTrue(vm.tabs.contains(ChangeTab.Artifact("design.md")))
    }

    @Test
    fun `a change archived by a refresh is no longer in the project`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/changes/one/proposal.md", "## Why\n\nFirst.\n", "p")
        }
        val config = repository.add(served(remote))
        val vm = ChangeViewModel(repository, config.id, "one", scope())
        advanceUntilIdle()
        assertNotNull(vm.change.value)

        remote.removeAndCommit("openspec/changes/one/proposal.md", "archived away")
        vm.refresh.refresh()
        advanceUntilIdle()

        assertNull(vm.change.value)
    }

    @Test
    fun `a card survives a refresh that rewrote the spec around it`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(served(remote))
        val vm = SpecViewModel(repository, config.id, "repo-store", scope())
        advanceUntilIdle()

        val node = (vm.state.value as SpecScreenState.Outline).nodes.first { it.title == "Something holds" }
        vm.select(node)

        remote.commit(
            "openspec/specs/repo-store/spec.md",
            LocalRemote.spec("repo-store").replace("The system SHALL do the thing.", "The system SHALL do it twice."),
            "reworded",
        )
        vm.refresh.refresh()
        advanceUntilIdle()

        assertFalse(vm.selectionGone.value)
        assertEquals(node.path, vm.selected.value?.path)
        assertTrue("${vm.selected.value?.body}", vm.selected.value?.body.orEmpty().contains("twice"))
    }

    @Test
    fun `a card whose node the refresh removed says so`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(served(remote))
        val vm = SpecViewModel(repository, config.id, "repo-store", scope())
        advanceUntilIdle()

        vm.select((vm.state.value as SpecScreenState.Outline).nodes.first { it.title == "Something holds" })

        remote.commit(
            "openspec/specs/repo-store/spec.md",
            "# repo-store Specification\n\n## Purpose\nThe purpose of repo-store.\n\n## Requirements\n\n" +
                "### Requirement: Another thing\n\nThe system SHALL do another thing.\n\n" +
                "#### Scenario: It does\n\n- **WHEN** asked\n- **THEN** it does\n",
            "replaced",
        )
        vm.refresh.refresh()
        advanceUntilIdle()

        assertTrue(vm.selectionGone.value)
    }

    @Test
    fun `choosing another node after one went away clears the message`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(served(remote))
        val vm = SpecViewModel(repository, config.id, "repo-store", scope())
        advanceUntilIdle()

        vm.select((vm.state.value as SpecScreenState.Outline).nodes.first { it.title == "Something holds" })
        remote.commit(
            "openspec/specs/repo-store/spec.md",
            "# repo-store Specification\n\n## Purpose\nThe purpose of repo-store.\n\n## Requirements\n\n" +
                "### Requirement: Another thing\n\nThe system SHALL do another thing.\n\n" +
                "#### Scenario: It does\n\n- **WHEN** asked\n- **THEN** it does\n",
            "replaced",
        )
        vm.refresh.refresh()
        advanceUntilIdle()
        assertTrue(vm.selectionGone.value)

        vm.select((vm.state.value as SpecScreenState.Outline).nodes.first())
        assertFalse(vm.selectionGone.value)
    }

    @Test
    fun `a capability the refresh removed is not in this project`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(served(remote))
        val vm = SpecViewModel(repository, config.id, "repo-store", scope())
        advanceUntilIdle()
        assertTrue(vm.state.value is SpecScreenState.Outline)

        remote.removeAndCommit("openspec/specs/repo-store/spec.md", "gone")
        vm.refresh.refresh()
        advanceUntilIdle()

        assertTrue("${vm.state.value}", vm.state.value is SpecScreenState.Unreadable)
    }

    @Test
    fun `a comparison whose requirement the refresh removed says so`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/changes/one/proposal.md", "## Why\n\nx\n", "p")
            commit("openspec/changes/one/specs/repo-store/spec.md", delta, "s")
        }
        val config = repository.add(served(remote))
        val vm = DeltaViewModel(repository, config.id, "one", scope())
        advanceUntilIdle()

        val node = vm.sections.value.flatMap { it.nodes }.first()
        vm.select(node)
        assertFalse(vm.selectionGone.value)

        remote.removeAndCommit("openspec/changes/one/specs/repo-store/spec.md", "no deltas")
        vm.refresh.refresh()
        advanceUntilIdle()

        assertTrue(vm.selectionGone.value)
    }

    @Test
    fun `the list keeps a loaded row through a failed refresh and says why`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        val config = repository.add(remote.url)
        val vm = RepoListViewModel(repository, scope())
        advanceUntilIdle()
        val before = vm.rows.value.single().counts
        assertNotNull(before)

        File(remote.url.removePrefix("file:")).deleteRecursively()
        vm.refreshAll()
        advanceUntilIdle()

        assertEquals(before, vm.rows.value.single().counts)
        assertNull(vm.rows.value.single().message)
        val failure = vm.failure.value
        assertNotNull(failure)
        assertEquals(config.cloneId, failure?.cloneId)

        vm.failureShown(failure!!.cloneId)
        advanceUntilIdle()
        assertNull(vm.failure.value)
    }

    @Test
    fun `a row that never loaded still says what went wrong`() = runTest {
        repository.add("https://no-such-host.invalid/a/b.git")
        val vm = RepoListViewModel(repository, scope())
        advanceUntilIdle()

        vm.refreshAll()
        advanceUntilIdle()

        assertNotNull(vm.rows.value.single().message)
        assertTrue(vm.rows.value.single().state is ProjectState.Failed)
    }

    @Test
    fun `nothing is refreshing once the list has finished`() = runTest {
        repository.add(served(LocalRemote(temp.newFolder()).init()))
        val vm = RepoListViewModel(repository, scope())
        advanceUntilIdle()

        vm.refreshAll()
        advanceUntilIdle()

        assertFalse(vm.refreshing.value)
        assertFalse(vm.rows.value.single().isRefreshing)
    }

    @Test
    fun `two rows on one working copy both reload from one refresh`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init()
        remote.commit("second/openspec/config.yaml", "schema: spec-driven\n", "a second project")
        val url = served(remote)
        val first = repository.add(url, path = "")
        val second = repository.add(url, path = "second")
        val vm = RepoListViewModel(repository, scope())
        advanceUntilIdle()
        assertEquals(first.cloneId, second.cloneId)

        remote.commit("openspec/specs/later/spec.md", LocalRemote.spec("later"), "later")
        vm.refresh(first.id)
        advanceUntilIdle()

        assertEquals(2, repository.stateOf(first.id).indexOrNull?.counts?.specs)
        assertTrue(repository.stateOf(second.id) is ProjectState.Loaded)
    }
}
