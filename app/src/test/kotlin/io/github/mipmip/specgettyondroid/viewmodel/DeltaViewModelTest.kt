package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.GitHttpServer
import io.github.mipmip.specgettyondroid.repo.LocalRemote
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.spec.DiffMark
import io.github.mipmip.specgettyondroid.spec.NodeKind
import io.github.mipmip.specgettyondroid.spec.Op
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

@OptIn(ExperimentalCoroutinesApi::class)
class DeltaViewModelTest {

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
        changeDir: String,
        build: LocalRemote.() -> Unit,
    ): DeltaViewModel {
        val remote = LocalRemote(temp.newFolder()).init().apply(build)
        val config = repository.add(served(remote))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        return DeltaViewModel(repository, config.id, changeDir, scope)
    }

    private val mainSpec = """
        # thing Specification

        ## Purpose
        What thing is for, at length.

        ## Requirements

        ### Requirement: A settled thing
        The system SHALL do the old thing.

        #### Scenario: It does
        - **WHEN** asked
        - **THEN** the old outcome
    """.trimIndent() + "\n"

    private val modifyingDelta = """
        ## MODIFIED Requirements

        ### Requirement: A settled thing
        The system SHALL do the new thing.

        #### Scenario: It does
        - **WHEN** asked
        - **THEN** the new outcome
    """.trimIndent() + "\n"

    private val addingDelta = """
        ## ADDED Requirements

        ### Requirement: Something brand new
        The system SHALL do it.

        #### Scenario: It does
        - **WHEN** asked
        - **THEN** done
    """.trimIndent() + "\n"

    @Test
    fun `a change touching two capabilities gets a section each, in name order`() = runTest {
        val vm = open("two") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit("openspec/changes/two/specs/zebra/spec.md", addingDelta, "d1")
            commit("openspec/changes/two/specs/alpha/spec.md", addingDelta, "d2")
        }
        advanceUntilIdle()
        assertEquals(listOf("alpha", "zebra"), vm.sections.value.map { it.capability })
    }

    @Test
    fun `a change with no deltas has no sections`() = runTest {
        val vm = open("none") {
            commit("openspec/changes/none/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        assertTrue(vm.sections.value.isEmpty())
    }

    @Test
    fun `requirements carry their operation`() = runTest {
        val vm = open("mix") {
            commit(
                "openspec/changes/mix/specs/thing/spec.md",
                addingDelta + "\n" + modifyingDelta,
                "d",
            )
        }
        advanceUntilIdle()
        val requirements = vm.sections.value.single().nodes
            .filter { it.kind == NodeKind.REQUIREMENT }
        assertEquals(
            mapOf("Something brand new" to Op.ADDED, "A settled thing" to Op.MODIFIED),
            requirements.associate { it.title to it.op },
        )
    }

    @Test
    fun `an unrecognised operation is shown as written`() = runTest {
        val vm = open("odd") {
            commit(
                "openspec/changes/odd/specs/thing/spec.md",
                addingDelta.replace("## ADDED Requirements", "## DEPRECATED Requirements"),
                "d",
            )
        }
        advanceUntilIdle()
        assertEquals(
            "DEPRECATED",
            vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT }.op,
        )
    }

    @Test
    fun `a delta that does not fit the grammar reports its reasons`() = runTest {
        val vm = open("broken") {
            commit(
                "openspec/changes/broken/specs/thing/spec.md",
                "### Requirement: Orphan\nProse.\n\n#### Scenario: s\n- **WHEN** a\n- **THEN** b\n",
                "d",
            )
        }
        advanceUntilIdle()
        val section = vm.sections.value.single()
        assertFalse(section.isSpec)
        assertTrue(section.problems.any { it.text.contains("under no delta header") })
        assertTrue("the file is kept rather than dropped", vm.sections.value.isNotEmpty())
    }

    @Test
    fun `selecting a node shows it and opens on the difference`() = runTest {
        val vm = open("mod") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit("openspec/changes/mod/specs/thing/spec.md", modifyingDelta, "d")
        }
        advanceUntilIdle()
        val node = vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT }
        vm.select(node)
        assertEquals(node, vm.selected.value)
        assertEquals(ComparisonView.DIFFERENCE, vm.view.value)
    }

    @Test
    fun `a modified requirement with an original is compared`() = runTest {
        val vm = open("mod") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit("openspec/changes/mod/specs/thing/spec.md", modifyingDelta, "d")
        }
        advanceUntilIdle()
        vm.select(vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT })

        val comparison = vm.comparison
        assertNotNull(comparison)
        assertTrue(comparison!!.original.contains("the old thing"))
        assertTrue(comparison.proposed.contains("the new thing"))
        assertFalse(comparison.isIdentical)

        assertTrue(
            comparison.diff.any {
                it.mark == DiffMark.ORIGINAL_ONLY && it.text.contains("old outcome")
            },
        )
        assertTrue(
            comparison.diff.any {
                it.mark == DiffMark.PROPOSED_ONLY && it.text.contains("new outcome")
            },
        )
    }

    @Test
    fun `the three views can each be shown`() = runTest {
        val vm = open("mod") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit("openspec/changes/mod/specs/thing/spec.md", modifyingDelta, "d")
        }
        advanceUntilIdle()
        vm.select(vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT })

        ComparisonView.entries.forEach {
            vm.showView(it)
            assertEquals(it, vm.view.value)
        }
    }

    @Test
    fun `an added requirement offers no comparison`() = runTest {
        val vm = open("add") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit("openspec/changes/add/specs/thing/spec.md", addingDelta, "d")
        }
        advanceUntilIdle()
        vm.select(vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT })
        assertNull(vm.comparison)
    }

    @Test
    fun `a modified requirement whose original is absent offers no comparison`() = runTest {
        val vm = open("mod") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit(
                "openspec/changes/mod/specs/thing/spec.md",
                modifyingDelta.replace("A settled thing", "A name the spec does not have"),
                "d",
            )
        }
        advanceUntilIdle()
        vm.select(vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT })
        assertNull(vm.comparison)
    }

    @Test
    fun `a capability the main spec does not hold offers no comparison`() = runTest {
        val vm = open("new") {
            commit("openspec/changes/new/specs/brand-new-capability/spec.md", modifyingDelta, "d")
        }
        advanceUntilIdle()
        vm.select(vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT })
        assertNull(vm.comparison)
    }

    @Test
    fun `a scenario node offers no comparison`() = runTest {
        val vm = open("mod") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit("openspec/changes/mod/specs/thing/spec.md", modifyingDelta, "d")
        }
        advanceUntilIdle()
        vm.select(vm.sections.value.single().nodes.first { it.kind == NodeKind.SCENARIO })
        assertNull(vm.comparison)
    }

    @Test
    fun `an archived change offers no comparison`() = runTest {
        val vm = open("2026-09-21-mod") {
            commit("openspec/specs/thing/spec.md", mainSpec, "m")
            commit(
                "openspec/changes/archive/2026-09-21-mod/specs/thing/spec.md",
                modifyingDelta,
                "d",
            )
        }
        advanceUntilIdle()
        assertTrue(vm.isArchived)
        vm.select(vm.sections.value.single().nodes.first { it.kind == NodeKind.REQUIREMENT })
        assertNull("an applied change would diff against itself", vm.comparison)
    }

    @Test
    fun `an archived change still shows its outline`() = runTest {
        val vm = open("2026-09-21-mod") {
            commit(
                "openspec/changes/archive/2026-09-21-mod/specs/thing/spec.md",
                modifyingDelta,
                "d",
            )
        }
        advanceUntilIdle()
        assertEquals("mod", vm.title)
        assertTrue(vm.sections.value.single().nodes.isNotEmpty())
    }

    @Test
    fun `clearing the selection returns to the outline`() = runTest {
        val vm = open("add") {
            commit("openspec/changes/add/specs/thing/spec.md", addingDelta, "d")
        }
        advanceUntilIdle()
        vm.select(vm.sections.value.single().nodes.first())
        assertNotNull(vm.selected.value)
        vm.clearSelection()
        assertNull(vm.selected.value)
    }

    @Test
    fun `the deltas are parsed through the cache, so twice costs once`() = runTest {
        val remote = LocalRemote(temp.newFolder()).init().apply {
            commit("openspec/changes/add/specs/thing/spec.md", addingDelta, "d")
        }
        val config = repository.add(served(remote))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope

        DeltaViewModel(repository, config.id, "add", scope)
        advanceUntilIdle()
        val after = repository.cacheOf(config.id).parses

        DeltaViewModel(repository, config.id, "add", scope)
        advanceUntilIdle()
        assertEquals("the second view model reused the cache", after, repository.cacheOf(config.id).parses)
    }

    @Test
    fun `a purpose in a delta is a node of the outline`() = runTest {
        val vm = open("new") {
            commit(
                "openspec/changes/new/specs/brand-new/spec.md",
                "## Purpose\nWhat the new capability is for.\n\n" + addingDelta,
                "d",
            )
        }
        advanceUntilIdle()
        val purpose = vm.sections.value.single().nodes.first()
        assertEquals(NodeKind.PURPOSE, purpose.kind)
        assertTrue(purpose.body.contains("What the new capability is for."))
    }
}
