package io.github.mipmip.specgettyondroid.project

import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoResult
import io.github.mipmip.specgettyondroid.repo.errorOrNull
import io.github.mipmip.specgettyondroid.repo.valueOrNull
import io.github.mipmip.specgettyondroid.tasks.TaskStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectLoaderTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun project() = ProjectBuilder(temp.newFolder())

    @Test
    fun `a repository with a project loads it`() {
        val b = project().spec("change-search")
        val result = ProjectLoader.load(b.openspec.parentFile)
        assertEquals(1, result.valueOrNull()?.specCount)
    }

    @Test
    fun `a repository without a project says so`() {
        val result = ProjectLoader.load(temp.newFolder())
        assertTrue("$result", result.errorOrNull() is RepoError.NoOpenSpecProject)
    }

    @Test
    fun `a project nested deeper is not loaded`() {
        val root = temp.newFolder()
        File(root, "sub/openspec/specs/a").mkdirs()
        assertTrue(ProjectLoader.load(root) is RepoResult.Failure)
    }

    @Test
    fun `capabilities are the spec directories, in name order`() {
        val info = project().spec("overview-tab").spec("change-search").load()
        assertEquals(listOf("change-search", "overview-tab"), info.capabilities.map { it.name })
        assertTrue(info.capabilities.all { it.specFile.isFile })
    }

    @Test
    fun `a directory with no spec file is not a capability`() {
        val info = project().spec("real").emptySpecDir("empty").load()
        assertEquals(listOf("real"), info.capabilities.map { it.name })
    }

    @Test
    fun `a project with no specs directory has no capabilities`() {
        val info = project().emptyProject().load()
        assertTrue(info.capabilities.isEmpty())
    }

    @Test
    fun `active and archived changes are told apart`() {
        val info = project()
            .change("add-a-thing")
            .change("2026-09-21-did-a-thing", archived = true)
            .load()
        assertEquals(listOf("add-a-thing"), info.activeChanges.map { it.name })
        assertEquals(listOf("did-a-thing"), info.archivedChanges.map { it.name })
        assertFalse(info.activeChanges.single().archived)
        assertTrue(info.archivedChanges.single().archived)
    }

    @Test
    fun `the archive directory is not an active change`() {
        val info = project().change("2026-09-21-x", archived = true).load()
        assertTrue(info.activeChanges.none { it.name == "archive" })
        assertTrue(info.activeChanges.isEmpty())
    }

    @Test
    fun `a project with no changes directory has no changes`() {
        val info = project().emptyProject().load()
        assertTrue(info.activeChanges.isEmpty())
        assertTrue(info.archivedChanges.isEmpty())
    }

    @Test
    fun `an archived change carries the date from its name`() {
        val info = project().change("2026-09-21-open-a-change", archived = true).load()
        val change = info.archivedChanges.single()
        assertEquals("2026-09-21", change.date)
        assertEquals("open-a-change", change.name)
    }

    @Test
    fun `an undated archived directory is still listed`() {
        val info = project().change("no-date-here", archived = true).load()
        val change = info.archivedChanges.single()
        assertNull(change.date)
        assertEquals("no-date-here", change.name)
    }

    @Test
    fun `an impossible date is not read as one`() {
        assertEquals(null to "2026-13-45-nonsense", ProjectLoader.splitDate("2026-13-45-nonsense"))
        assertEquals(null to "2026-02-30-nope", ProjectLoader.splitDate("2026-02-30-nope"))
        assertEquals("2024-02-29" to "leap", ProjectLoader.splitDate("2024-02-29-leap"))
        assertEquals(null to "2026-02-29-not-leap", ProjectLoader.splitDate("2026-02-29-not-leap"))
    }

    @Test
    fun `archived changes are newest first`() {
        val info = project()
            .change("2026-01-01-oldest", archived = true)
            .change("2026-09-21-newest", archived = true)
            .change("2026-05-05-middle", archived = true)
            .load()
        assertEquals(
            listOf("newest", "middle", "oldest"),
            info.archivedChanges.map { it.name },
        )
    }

    @Test
    fun `a change lists the markdown files it has`() {
        val info = project().change(
            "a-change",
            artifacts = mapOf(
                "proposal.md" to "## Why\n\nx\n",
                "design.md" to "## Context\n\nx\n",
                "tasks.md" to "- [ ] 1.1 x\n",
                "notes.md" to "Anything.\n",
            ),
        ).load()
        assertEquals(
            listOf("design.md", "notes.md", "proposal.md", "tasks.md"),
            info.activeChanges.single().artifacts.map { it.name },
        )
    }

    @Test
    fun `a change with only a proposal still loads`() {
        val info = project().change("a", artifacts = mapOf("proposal.md" to "## Why\n\nx\n")).load()
        assertEquals(listOf("proposal.md"), info.activeChanges.single().artifacts.map { it.name })
    }

    @Test
    fun `the yaml file and the specs directory are not artifacts`() {
        val info = project().change("a", capabilities = listOf("x")).load()
        val names = info.activeChanges.single().artifacts.map { it.name }
        assertFalse(names.contains(".openspec.yaml"))
        assertFalse(names.contains("specs"))
    }

    @Test
    fun `a change lists the capabilities it touches`() {
        val info = project().change("a", capabilities = listOf("one", "two")).load()
        assertEquals(listOf("one", "two"), info.activeChanges.single().capabilities.map { it.name })
    }

    @Test
    fun `a change touching no capability still loads`() {
        val info = project().change("a", capabilities = emptyList()).load()
        assertTrue(info.activeChanges.single().capabilities.isEmpty())
    }

    @Test
    fun `a change names its workflow schema`() {
        val info = project()
            .change("a", schema = "spec-driven")
            .change("b", schema = "tinychange")
            .load()
        assertEquals(
            mapOf("a" to "spec-driven", "b" to "tinychange"),
            info.activeChanges.associate { it.name to it.schema },
        )
    }

    @Test
    fun `a change with no yaml has no schema`() {
        val info = project().change("a", schema = null).load()
        assertNull(info.activeChanges.single().schema)
    }

    @Test
    fun `unreadable yaml leaves the schema unnamed and does not stop the load`() {
        val b = project().change("a", schema = "spec-driven")
        b.write("openspec/changes/a/.openspec.yaml", "schema: [unclosed\n")
        val info = b.load()
        assertNull(info.activeChanges.single().schema)
        assertEquals(1, info.activeCount)
    }

    @Test
    fun `yaml asking for an arbitrary type is refused`() {
        val b = project().change("a", schema = "spec-driven")
        b.write(
            "openspec/changes/a/.openspec.yaml",
            "!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL " +
                "[\"http://localhost/\"]]]]\n",
        )
        assertNull(b.load().activeChanges.single().schema)
    }

    @Test
    fun `a change carries its task counts`() {
        val info = project().change(
            "a",
            artifacts = mapOf(
                "proposal.md" to "## Why\n\nx\n",
                "tasks.md" to ProjectBuilder.tasks(done = 3, total = 5),
            ),
        ).load()
        assertEquals(TaskStats(3, 5), info.activeChanges.single().tasks)
    }

    @Test
    fun `a change with no tasks file counts zero of zero`() {
        val info = project().change("a").load()
        assertEquals(TaskStats.NONE, info.activeChanges.single().tasks)
    }

    @Test
    fun `open tasks are summed across active changes only`() {
        val info = project()
            .change(
                "a",
                artifacts = mapOf("tasks.md" to ProjectBuilder.tasks(3, 5)),
            )
            .change(
                "b",
                artifacts = mapOf("tasks.md" to ProjectBuilder.tasks(2, 4)),
            )
            .change(
                "2026-01-01-done",
                archived = true,
                artifacts = mapOf("tasks.md" to ProjectBuilder.tasks(9, 9)),
            )
            .load()
        assertEquals(TaskStats(5, 9), info.tasks)
    }

    @Test
    fun `either configuration spelling is found and read`() {
        assertEquals("spec-driven", project().config("config.yaml").load().schema)
        assertEquals("spec-driven", project().config("config.yml").load().schema)
    }

    @Test
    fun `a project description is located when present`() {
        val info = project().projectMarkdown().load()
        assertEquals("project.md", info.projectFile?.name)
    }

    @Test
    fun `a project with neither loads with both absent`() {
        val info = project().emptyProject().load()
        assertNull(info.configFile)
        assertNull(info.projectFile)
        assertNull(info.schema)
    }

    @Test
    fun `an empty project loads, and is not the same as an absent one`() {
        val empty = project().emptyProject()
        val info = empty.load()
        assertTrue(info.isEmpty)
        assertTrue(ProjectLoader.load(empty.openspec.parentFile) is RepoResult.Success)
        assertTrue(ProjectLoader.load(temp.newFolder()) is RepoResult.Failure)
    }

    @Test
    fun `a project with content is not empty`() {
        assertFalse(project().spec("a").load().isEmpty)
        assertFalse(project().change("a").load().isEmpty)
    }

    @Test
    fun `the schemas in use are listed once each, in order`() {
        val info = project()
            .change("a", schema = "tinychange")
            .change("b", schema = "spec-driven")
            .change("c", schema = "spec-driven")
            .load()
        assertEquals(listOf("spec-driven", "tinychange"), info.schemasInUse)
    }
}
