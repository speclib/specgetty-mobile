package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.GitHttpServer
import io.github.mipmip.specgettyondroid.repo.LocalRemote
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.spec.NodeKind
import io.github.mipmip.specgettyondroid.spec.PartKind
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
class SpecViewModelTest {

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

    private suspend fun TestScope.open(
        capability: String,
        build: LocalRemote.() -> Unit,
    ): Pair<SpecViewModel, String> {
        val remote = LocalRemote(temp.newFolder()).init().apply(build)
        val config = repository.add(served(remote))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        return SpecViewModel(repository, config.id, capability, scope) to config.id
    }

    private val goodSpec = """
        # thing Specification

        ## Purpose
        What thing is for, at enough length to be a purpose.

        ## Requirements

        ### Requirement: The first thing
        Its prose, which the card shows.

        #### Scenario: One clause
        - **WHEN** something happens
        - **THEN** something else does

        #### Scenario: Written as prose

        A paragraph carrying no keyword at all, which is still content.

        ### Requirement: The second thing
        More prose.

        #### Scenario: All four
        - **GIVEN** a
        - **WHEN** b
        - **THEN** c
        - **AND** d
    """.trimIndent() + "\n"

    @Test
    fun `a good spec yields an outline in file order`() = runTest {
        val (vm, _) = open("thing") {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is SpecScreenState.Outline)
        assertEquals(
            listOf(
                NodeKind.PURPOSE to "Purpose",
                NodeKind.REQUIREMENT to "The first thing",
                NodeKind.SCENARIO to "One clause",
                NodeKind.SCENARIO to "Written as prose",
                NodeKind.REQUIREMENT to "The second thing",
                NodeKind.SCENARIO to "All four",
            ),
            (state as SpecScreenState.Outline).nodes.map { it.kind to it.title },
        )
    }

    @Test
    fun `the purpose card shows the purpose text`() = runTest {
        val (vm, _) = open("thing") {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        advanceUntilIdle()
        val purpose = (vm.state.value as SpecScreenState.Outline).nodes.first()
        vm.select(purpose)
        assertTrue(vm.selected.value!!.body.contains("What thing is for"))
    }

    @Test
    fun `a requirement card shows its prose and not its scenarios`() = runTest {
        val (vm, _) = open("thing") {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        advanceUntilIdle()
        val requirement = (vm.state.value as SpecScreenState.Outline).nodes
            .first { it.kind == NodeKind.REQUIREMENT }
        assertEquals("Its prose, which the card shows.", requirement.body)
        assertFalse(requirement.body.contains("Scenario"))
        assertTrue(requirement.parts.isEmpty())
    }

    @Test
    fun `a scenario card carries its clauses with their keywords`() = runTest {
        val (vm, _) = open("thing") {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        advanceUntilIdle()
        val scenario = (vm.state.value as SpecScreenState.Outline).nodes
            .first { it.title == "All four" }
        assertEquals(
            listOf("GIVEN", "WHEN", "THEN", "AND"),
            scenario.parts.map { it.keyword },
        )
        assertTrue(scenario.parts.all { it.kind == PartKind.CLAUSE })
    }

    @Test
    fun `a scenario written as prose is carried as prose`() = runTest {
        val (vm, _) = open("thing") {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        advanceUntilIdle()
        val scenario = (vm.state.value as SpecScreenState.Outline).nodes
            .first { it.title == "Written as prose" }
        assertEquals(1, scenario.parts.size)
        assertEquals(PartKind.PROSE, scenario.parts.single().kind)
        assertTrue(scenario.parts.single().text.contains("no keyword at all"))
    }

    @Test
    fun `selecting and clearing a node`() = runTest {
        val (vm, _) = open("thing") {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        advanceUntilIdle()
        val node = (vm.state.value as SpecScreenState.Outline).nodes.first()
        vm.select(node)
        assertEquals(node, vm.selected.value)
        vm.clearSelection()
        assertNull(vm.selected.value)
    }

    @Test
    fun `a file that does not fit reports every reason with its line`() = runTest {
        val (vm, _) = open("broken") {
            commit(
                "openspec/specs/broken/spec.md",
                "# broken Specification\n\n## ADDED Requirements\n\n" +
                    "### Requirement: A\nProse.\n\n#### Scenario: s\n- **WHEN** a\n- **THEN** b\n",
                "s",
            )
        }
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is SpecScreenState.Report)
        val problems = (state as SpecScreenState.Report).problems
        assertTrue("more than one reason", problems.size >= 2)
        assertTrue(problems.any { it.text.contains("delta header") && it.line > 0 })
        assertTrue(problems.any { it.text.contains("`## Purpose`") })
    }

    @Test
    fun `a file that does not fit is still readable as Markdown`() = runTest {
        val text = "# broken Specification\n\n## ADDED Requirements\n\n" +
            "### Requirement: A\nProse nobody should lose.\n"
        val (vm, _) = open("broken") {
            commit("openspec/specs/broken/spec.md", text, "s")
        }
        advanceUntilIdle()

        assertTrue(vm.state.value is SpecScreenState.Report)
        assertEquals(text, vm.rawText.value)

        vm.showRaw(true)
        assertTrue(vm.showingRaw.value)
        vm.showRaw(false)
        assertFalse(vm.showingRaw.value)
    }

    @Test
    fun `a capability that is not in the project says so`() = runTest {
        val (vm, _) = open("nothing-like-it") {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue("$state", state is SpecScreenState.Unreadable)
    }

    @Test
    fun `a spec is parsed once and not again on reopening`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/specs/thing/spec.md", goodSpec, "s")
        }
        val config = repository.add(served(remote))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope

        SpecViewModel(repository, config.id, "thing", scope)
        advanceUntilIdle()
        val after = repository.cacheOf(config.id).parses
        assertEquals(1, after)

        SpecViewModel(repository, config.id, "thing", scope)
        advanceUntilIdle()
        assertEquals("reopening reused the cache", after, repository.cacheOf(config.id).parses)
    }

    @Test
    fun `a real corpus spec opens as an outline`() = runTest {
        val corpusSpec = io.github.mipmip.specgettyondroid.spec.Corpus
            .mainSpecs()
            .first { it.parentFile.name == "spec-detail-view" }
            .readText()

        val (vm, _) = open("spec-detail-view") {
            commit("openspec/specs/spec-detail-view/spec.md", corpusSpec, "s")
        }
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("$state", state is SpecScreenState.Outline)
        val nodes = (state as SpecScreenState.Outline).nodes
        assertTrue("only ${nodes.size} nodes", nodes.size > 40)
        assertTrue(nodes.none { it.kind == NodeKind.SCENARIO && it.parts.isEmpty() })
    }
}
