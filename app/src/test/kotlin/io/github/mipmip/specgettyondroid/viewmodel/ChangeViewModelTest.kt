package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
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
class ChangeViewModelTest {

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
    ): Pair<ChangeViewModel, String> {
        val remote = LocalRemote(temp.newFolder()).init().apply(build)
        val config = repository.add(served(remote))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes += scope
        return ChangeViewModel(repository, config.id, changeDir, scope) to config.id
    }

    private val delta = "## ADDED Requirements\n\n### Requirement: A\nProse.\n\n" +
        "#### Scenario: s\n- **WHEN** a\n- **THEN** b\n"

    @Test
    fun `the usual change has a tab per artifact plus specs`() = runTest {
        val (vm, _) = open("full") {
            commit("openspec/changes/full/proposal.md", "## Why\n\nBecause.\n", "p")
            commit("openspec/changes/full/design.md", "## Context\n\nThings.\n", "d")
            commit("openspec/changes/full/tasks.md", "- [x] 1.1 a\n- [ ] 1.2 b\n", "t")
            commit("openspec/changes/full/specs/repo-store/spec.md", delta, "s")
        }
        advanceUntilIdle()
        assertEquals(
            listOf("design", "proposal", "Tasks", "Specs"),
            vm.tabs.map { it.label },
        )
    }

    @Test
    fun `a change with one artifact has one artifact tab`() = runTest {
        val (vm, _) = open("small") {
            commit("openspec/changes/small/proposal.md", "## Why\n\nBecause.\n", "p")
        }
        advanceUntilIdle()
        assertEquals(listOf("proposal", "Specs"), vm.tabs.map { it.label })
    }

    @Test
    fun `an artifact nobody expected gets a tab`() = runTest {
        val (vm, _) = open("odd") {
            commit("openspec/changes/odd/proposal.md", "## Why\n\nx\n", "p")
            commit("openspec/changes/odd/notes.md", "Anything at all.\n", "n")
        }
        advanceUntilIdle()
        assertTrue(vm.tabs.map { it.label }.contains("notes"))
    }

    @Test
    fun `there is no tasks tab without a tasks file`() = runTest {
        val (vm, _) = open("notasks") {
            commit("openspec/changes/notasks/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        assertFalse(vm.tabs.contains(ChangeTab.Tasks))
    }

    @Test
    fun `the first tab is selected to begin with`() = runTest {
        val (vm, _) = open("full") {
            commit("openspec/changes/full/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        assertEquals(vm.tabs.first(), vm.tab.value)
    }

    @Test
    fun `a tab can be selected`() = runTest {
        val (vm, _) = open("full") {
            commit("openspec/changes/full/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        vm.selectTab(ChangeTab.Specs)
        assertEquals(ChangeTab.Specs, vm.tab.value)
    }

    @Test
    fun `an artifact is read as Markdown when asked for`() = runTest {
        val (vm, _) = open("full") {
            commit("openspec/changes/full/proposal.md", "## Why\n\nBecause of it.\n", "p")
        }
        advanceUntilIdle()
        val content = vm.artifact("proposal.md")
        assertTrue("$content", content is ArtifactContent.Markdown)
        assertTrue((content as ArtifactContent.Markdown).text.contains("Because of it."))
    }

    @Test
    fun `an artifact that is not in the change reports itself`() = runTest {
        val (vm, _) = open("full") {
            commit("openspec/changes/full/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        val content = vm.artifact("nothing.md")
        assertTrue("$content", content is ArtifactContent.Unreadable)
    }

    @Test
    fun `an artifact that cannot be read reports itself`() = runTest {
        val (vm, id) = open("full") {
            commit("openspec/changes/full/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        // Delete the file behind the model's back, which is what a refresh
        // removing an artifact would look like mid-read.
        store.workingDir(id).resolve("openspec/changes/full/proposal.md").delete()
        assertTrue(vm.artifact("proposal.md") is ArtifactContent.Unreadable)
    }

    @Test
    fun `the tasks tab has the statistics and the items`() = runTest {
        val (vm, _) = open("tasky") {
            commit(
                "openspec/changes/tasky/tasks.md",
                "## 1. Work\n\n- [x] 1.1 done\n- [x] 1.2 also done\n- [ ] 1.3 not yet\n",
                "t",
            )
        }
        advanceUntilIdle()
        assertEquals(TaskStats(2, 3), vm.taskStats)
        assertEquals(3, vm.tasks.items.size)
        assertTrue(vm.tasks.items[0].done)
        assertFalse(vm.tasks.items[2].done)
    }

    @Test
    fun `the tasks carry their headings and continuations`() = runTest {
        val (vm, _) = open("tasky") {
            commit(
                "openspec/changes/tasky/tasks.md",
                "## 1. First\n\n- [ ] 1.1 A task that wraps\n      onto a second line\n\n" +
                    "## 2. Second\n\n- [x] 2.1 Another\n",
                "t",
            )
        }
        advanceUntilIdle()
        val items = vm.tasks.items
        assertEquals("1. First", items[0].heading)
        assertEquals(listOf("      onto a second line"), items[0].continuation)
        assertEquals("2. Second", items[1].heading)
    }

    @Test
    fun `a line that is not a task is not drawn as one`() = runTest {
        val (vm, _) = open("tasky") {
            commit(
                "openspec/changes/tasky/tasks.md",
                "- [ ] 1.1 real\n  - [ ] indented, not its own task\n* [ ] wrong marker\n",
                "t",
            )
        }
        advanceUntilIdle()
        assertEquals(1, vm.tasks.items.size)
        assertEquals(TaskStats(0, 1), vm.taskStats)
    }

    @Test
    fun `a change with no tasks file has no tasks`() = runTest {
        val (vm, _) = open("notasks") {
            commit("openspec/changes/notasks/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        assertEquals(TaskStats.NONE, vm.taskStats)
        assertTrue(vm.tasks.items.isEmpty())
    }

    @Test
    fun `the specs tab lists the capabilities the change touches`() = runTest {
        val (vm, _) = open("two") {
            commit("openspec/changes/two/proposal.md", "## Why\n\nx\n", "p")
            commit("openspec/changes/two/specs/one/spec.md", delta, "d1")
            commit("openspec/changes/two/specs/another/spec.md", delta, "d2")
        }
        advanceUntilIdle()
        assertEquals(listOf("another", "one"), vm.capabilities.map { it.name })
    }

    @Test
    fun `a change touching no capability says so with an empty list`() = runTest {
        val (vm, _) = open("none") {
            commit("openspec/changes/none/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        assertTrue(vm.capabilities.isEmpty())
    }

    @Test
    fun `opening a change parses no delta`() = runTest {
        val (vm, id) = open("two") {
            commit("openspec/changes/two/proposal.md", "## Why\n\nx\n", "p")
            commit("openspec/changes/two/specs/one/spec.md", delta, "d1")
        }
        advanceUntilIdle()
        vm.selectTab(ChangeTab.Specs)
        assertTrue(vm.capabilities.isNotEmpty())
        assertEquals(0, repository.cacheOf(id).parses)
    }

    @Test
    fun `an archived change is found and carries its date`() = runTest {
        val (vm, _) = open("2026-09-21-done") {
            commit(
                "openspec/changes/archive/2026-09-21-done/proposal.md",
                "## Why\n\nIt was done.\n",
                "a",
            )
        }
        advanceUntilIdle()
        assertEquals("done", vm.title)
        assertEquals("2026-09-21", vm.date)
        assertTrue(vm.isArchived)
    }

    @Test
    fun `an active change is not archived and has no date`() = runTest {
        val (vm, _) = open("live") {
            commit("openspec/changes/live/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        assertFalse(vm.isArchived)
        assertNull(vm.date)
    }

    @Test
    fun `a change that is not in the project leaves nothing to show`() = runTest {
        val (vm, _) = open("nowhere") {
            commit("openspec/changes/elsewhere/proposal.md", "## Why\n\nx\n", "p")
        }
        advanceUntilIdle()
        assertNull(vm.change.value)
        assertTrue(vm.tabs.isEmpty())
        assertEquals("nowhere", vm.title)
    }

    @Test
    fun `the tab order does not shift between openings`() = runTest {
        val build: LocalRemote.() -> Unit = {
            commit("openspec/changes/full/proposal.md", "## Why\n\nx\n", "p")
            commit("openspec/changes/full/design.md", "## Context\n\nx\n", "d")
            commit("openspec/changes/full/notes.md", "x\n", "n")
        }
        val (first, _) = open("full", build)
        advanceUntilIdle()
        val (second, _) = open("full", build)
        advanceUntilIdle()
        assertEquals(first.tabs.map { it.label }, second.tabs.map { it.label })
    }
}
