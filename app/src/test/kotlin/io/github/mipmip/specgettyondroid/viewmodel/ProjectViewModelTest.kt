package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.SpecCache
import io.github.mipmip.specgettyondroid.index.MatchKind
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModelTest {

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

    private fun remote(build: LocalRemote.() -> Unit = {}) =
        LocalRemote(temp.newFolder()).init().apply(build)

    private suspend fun TestScope.open(remote: LocalRemote): ProjectViewModel {
        val config = repository.add(served(remote))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        return ProjectViewModel(repository, config.id, scope)
    }

    /** A project with enough in it that grouping and ordering are visible. */
    private fun populated() = remote {
        commit("openspec/specs/change-search/spec.md", LocalRemote.spec("change-search"), "s1")
        commit("openspec/specs/overview-tab/spec.md", LocalRemote.spec("overview-tab"), "s2")
        commit("openspec/changes/first-change/tasks.md", "- [x] 1.1 a\n- [ ] 1.2 b\n", "tasks")
        commit(
            "openspec/changes/first-change/specs/change-search/spec.md",
            "## ADDED Requirements\n\n### Requirement: A\nProse about inotify.\n\n" +
                "#### Scenario: s\n- **WHEN** a\n- **THEN** b\n",
            "delta",
        )
        commit("openspec/changes/second-change/proposal.md", "## Why\n\nBecause.\n", "second")
        commit(
            "openspec/changes/archive/2026-01-01-oldest/proposal.md",
            "## Why\n\nThe oldest one.\n",
            "old",
        )
        commit(
            "openspec/changes/archive/2026-09-21-newest/proposal.md",
            "## Why\n\nThe newest one.\n",
            "new",
        )
    }

    @Test
    fun `the tabs start on overview`() = runTest {
        val vm = open(remote())
        advanceUntilIdle()
        assertEquals(ProjectTab.OVERVIEW, vm.tab.value)
    }

    @Test
    fun `a tab can be selected`() = runTest {
        val vm = open(remote())
        vm.selectTab(ProjectTab.SPECS)
        assertEquals(ProjectTab.SPECS, vm.tab.value)
    }

    @Test
    fun `the counts come from the index`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        val counts = vm.index!!.counts
        assertEquals(3, counts.specs)
        assertEquals("first-change comes from the fixture, second-change from here", 2, counts.active)
        assertEquals(2, counts.archived)
        assertEquals(1, counts.tasks.done)
        assertEquals(2, counts.tasks.total)
    }

    @Test
    fun `a loaded project has nothing to explain`() = runTest {
        val vm = open(remote())
        advanceUntilIdle()
        assertNull(vm.message)
    }

    @Test
    fun `a repository with no project says so instead of showing an empty one`() = runTest {
        val config = repository.add(served(LocalRemote(temp.newFolder()).initBare()))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = ProjectViewModel(repository, config.id, scope)
        advanceUntilIdle()

        assertEquals("No OpenSpec project here", vm.message)
        assertNull(vm.index)
    }

    @Test
    fun `an empty project is loaded and empty, not an error`() = runTest {
        val config = repository.add(served(LocalRemote(temp.newFolder()).initEmptyProject()))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = ProjectViewModel(repository, config.id, scope)
        advanceUntilIdle()

        assertNull(vm.message)
        assertTrue(vm.index!!.project.isEmpty)
    }

    @Test
    fun `the changes tab lists everything when the query is empty`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        val changes = vm.changes.value
        assertEquals(2, changes.active.size)
        assertEquals(2, changes.archived.size)
        assertFalse(changes.isFiltered)
    }

    @Test
    fun `active changes are in name order and archived newest first`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        assertEquals(
            listOf("first-change", "second-change"),
            vm.changes.value.active.map { it.change.name },
        )
        assertEquals(
            listOf("newest", "oldest"),
            vm.changes.value.archived.map { it.change.name },
        )
    }

    @Test
    fun `a fuzzy query narrows the list`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        vm.onQueryChanged("frstch")
        assertEquals(listOf("first-change"), vm.changes.value.active.map { it.change.name })
    }

    @Test
    fun `a body query finds text and names the files`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        vm.onQueryChanged(":inotify")

        val match = vm.changes.value.active.single()
        assertEquals("first-change", match.change.name)
        assertEquals(listOf("change-search/spec.md"), match.matchedFiles)
    }

    @Test
    fun `a change matching on its name reports no files`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        vm.onQueryChanged(":second")
        assertTrue(vm.changes.value.active.single().matchedFiles.isEmpty())
    }

    @Test
    fun `a query that matches nothing leaves an empty filtered list`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        vm.onQueryChanged("zzzzz")
        assertTrue(vm.changes.value.isEmpty)
        assertTrue(vm.changes.value.isFiltered)
        assertEquals("zzzzz", vm.changes.value.query)
    }

    @Test
    fun `clearing the query restores the list`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        vm.onQueryChanged("zzzzz")
        assertTrue(vm.changes.value.isEmpty)

        vm.clearQuery()
        assertEquals(2, vm.changes.value.active.size)
        assertFalse(vm.changes.value.isFiltered)
    }

    @Test
    fun `the matcher control rewrites the query rather than the grammar`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()

        vm.onQueryChanged("first")
        assertEquals(MatchKind.FUZZY_NAME, vm.matcher)

        vm.selectMatcher(MatchKind.BODY)
        assertEquals(":first", vm.changes.value.query)
        assertEquals(MatchKind.BODY, vm.matcher)

        vm.selectMatcher(MatchKind.LITERAL_NAME)
        assertEquals("'first", vm.changes.value.query)
        assertEquals(MatchKind.LITERAL_NAME, vm.matcher)

        vm.selectMatcher(MatchKind.FUZZY_NAME)
        assertEquals("first", vm.changes.value.query)
    }

    @Test
    fun `switching the matcher on an empty query leaves it empty`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        vm.selectMatcher(MatchKind.BODY)
        assertEquals(":", vm.changes.value.query)
        assertFalse(vm.changes.value.isFiltered)
        assertEquals(2, vm.changes.value.active.size)
    }

    @Test
    fun `the specs tab lists the capabilities in name order`() = runTest {
        val vm = open(populated())
        advanceUntilIdle()
        assertEquals(
            listOf("change-search", "overview-tab", "repo-store"),
            vm.index!!.capabilityNames,
        )
    }

    @Test
    fun `listing the specs parses none of them`() = runTest {
        val config = repository.add(served(populated()))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = ProjectViewModel(repository, config.id, scope)
        advanceUntilIdle()

        vm.selectTab(ProjectTab.SPECS)
        assertTrue(vm.index!!.capabilityNames.isNotEmpty())
        assertEquals(0, repository.cacheOf(config.id).parses)
    }

    @Test
    fun `the properties tab prefers project markdown`() = runTest {
        val vm = open(
            remote {
                commit("openspec/project.md", "# The project\n\nAbout it.\n", "project.md")
            },
        )
        advanceUntilIdle()

        val properties = vm.properties
        assertTrue("$properties", properties is Properties.Description)
        assertTrue((properties as Properties.Description).markdown.contains("About it."))
    }

    @Test
    fun `the properties tab falls back to the configuration`() = runTest {
        val vm = open(remote())
        advanceUntilIdle()

        val properties = vm.properties
        assertTrue("$properties", properties is Properties.Configuration)
        assertEquals("config.yaml", (properties as Properties.Configuration).fileName)
        assertTrue(properties.yaml.contains("schema: spec-driven"))
    }

    @Test
    fun `a project with neither says so`() = runTest {
        val config = repository.add(served(LocalRemote(temp.newFolder()).initEmptyProject()))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        val vm = ProjectViewModel(repository, config.id, scope)
        advanceUntilIdle()

        assertEquals(Properties.None, vm.properties)
    }

    @Test
    fun `the schemas in use are listed once each`() = runTest {
        val vm = open(
            remote {
                commit("openspec/changes/a/.openspec.yaml", "schema: tinychange\n", "a")
                commit("openspec/changes/b/.openspec.yaml", "schema: spec-driven\n", "b")
                commit("openspec/changes/c/.openspec.yaml", "schema: spec-driven\n", "c")
            },
        )
        advanceUntilIdle()
        assertEquals(listOf("spec-driven", "tinychange"), vm.schemasInUse)
    }

    @Test
    fun `a change with no schema contributes no row`() = runTest {
        val vm = open(remote())
        advanceUntilIdle()
        assertTrue(vm.schemasInUse.isEmpty())
    }

    @Test
    fun `the cache is the repository's, so a spec opened twice parses once`() = runTest {
        val config = repository.add(served(populated()))
        advanceUntilIdle()

        val cache: SpecCache = repository.cacheOf(config.id)
        val capability = repository.stateOf(config.id).let {
            (it as io.github.mipmip.specgettyondroid.data.ProjectState.Loaded).index
        }.project.capabilities.first()

        cache.spec(capability.name, capability.specFile)
        cache.spec(capability.name, capability.specFile)
        assertEquals(1, cache.parses)
    }
}
