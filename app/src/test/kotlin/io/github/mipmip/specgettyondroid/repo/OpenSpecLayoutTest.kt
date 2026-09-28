package io.github.mipmip.specgettyondroid.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OpenSpecLayoutTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun workingDir(build: File.() -> Unit = {}): File =
        temp.newFolder().apply(build)

    private fun File.file(path: String, content: String = "x") {
        val f = File(this, path)
        f.parentFile?.mkdirs()
        f.writeText(content)
    }

    private fun File.dir(path: String) {
        File(this, path).mkdirs()
    }

    @Test
    fun `a project at the root is found`() {
        val wd = workingDir { dir("openspec/specs") }
        val result = OpenSpecLayout.projectDir(wd)
        assertEquals(File(wd, "openspec"), result.valueOrNull())
    }

    @Test
    fun `no project at the root says so`() {
        val result = OpenSpecLayout.projectDir(workingDir())
        val error = result.errorOrNull()
        assertTrue("$error", error is RepoError.NoOpenSpecProject)
        assertEquals("no OpenSpec project here", error?.message)
    }

    @Test
    fun `a project nested deeper is not found`() {
        val wd = workingDir { dir("sub/project/openspec/specs") }
        assertTrue(OpenSpecLayout.projectDir(wd).errorOrNull() is RepoError.NoOpenSpecProject)
    }

    @Test
    fun `a file called openspec is not a project`() {
        val wd = workingDir { file("openspec", "not a directory") }
        assertTrue(OpenSpecLayout.projectDir(wd).errorOrNull() is RepoError.NoOpenSpecProject)
    }

    @Test
    fun `an empty project is found and reported empty`() {
        val wd = workingDir { dir("openspec") }
        val project = OpenSpecLayout.projectDir(wd).valueOrNull()!!
        assertTrue(OpenSpecLayout.isEmpty(project))
    }

    @Test
    fun `a project with a spec is not empty`() {
        val wd = workingDir { file("openspec/specs/thing/spec.md") }
        assertFalse(OpenSpecLayout.isEmpty(File(wd, "openspec")))
    }

    @Test
    fun `a project with only an active change is not empty`() {
        val wd = workingDir { file("openspec/changes/doing/proposal.md") }
        assertFalse(OpenSpecLayout.isEmpty(File(wd, "openspec")))
    }

    @Test
    fun `a project with only an archived change is not empty`() {
        val wd = workingDir { file("openspec/changes/archive/2026-01-01-done/proposal.md") }
        assertFalse(OpenSpecLayout.isEmpty(File(wd, "openspec")))
    }

    @Test
    fun `an archive directory alone does not make a project non-empty`() {
        val wd = workingDir { dir("openspec/changes/archive") }
        assertTrue(OpenSpecLayout.isEmpty(File(wd, "openspec")))
    }

    @Test
    fun `the standard directories are derived from the project`() {
        val wd = workingDir { dir("openspec") }
        val project = File(wd, "openspec")
        assertEquals(File(project, "specs"), OpenSpecLayout.specsDir(project))
        assertEquals(File(project, "changes"), OpenSpecLayout.changesDir(project))
        assertEquals(File(project, "changes/archive"), OpenSpecLayout.archiveDir(project))
    }

    @Test
    fun `config yaml is found`() {
        val wd = workingDir { file("openspec/config.yaml", "schema: spec-driven\n") }
        assertEquals("config.yaml", OpenSpecLayout.configFile(File(wd, "openspec"))?.name)
    }

    @Test
    fun `config yml is found`() {
        val wd = workingDir { file("openspec/config.yml", "schema: spec-driven\n") }
        assertEquals("config.yml", OpenSpecLayout.configFile(File(wd, "openspec"))?.name)
    }

    @Test
    fun `yaml wins when both spellings are present`() {
        val wd = workingDir {
            file("openspec/config.yaml")
            file("openspec/config.yml")
        }
        assertEquals("config.yaml", OpenSpecLayout.configFile(File(wd, "openspec"))?.name)
    }

    @Test
    fun `no config at all is absent rather than an error`() {
        val wd = workingDir { dir("openspec") }
        assertNull(OpenSpecLayout.configFile(File(wd, "openspec")))
    }

    @Test
    fun `project markdown is found when present and absent when not`() {
        val withFile = workingDir { file("openspec/project.md", "# Project\n") }
        assertEquals("project.md", OpenSpecLayout.projectFile(File(withFile, "openspec"))?.name)

        val without = workingDir { dir("openspec") }
        assertNull(OpenSpecLayout.projectFile(File(without, "openspec")))
    }
}
