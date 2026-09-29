package io.github.mipmip.specgettyondroid.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The repository that prompted this holds four projects, one per top-level
 * directory. The app used to look at the root and nowhere else, so it read as
 * holding nothing at all.
 */
class DiscoveryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun project(root: File, path: String, marker: String = "config.yaml"): File {
        val dir = if (path.isEmpty()) root else File(root, path).apply { mkdirs() }
        val openspec = File(dir, "openspec").apply { mkdirs() }
        File(openspec, marker).writeText("schema: spec-driven\n")
        File(openspec, "specs").mkdirs()
        return dir
    }

    @Test
    fun `a directory qualifies on config yaml`() {
        val root = temp.newFolder("r")
        project(root, "")
        assertTrue(OpenSpecLayout.qualifies(root))
    }

    @Test
    fun `a directory qualifies on the yml spelling`() {
        val root = temp.newFolder("r")
        project(root, "", marker = "config.yml")
        assertTrue(OpenSpecLayout.qualifies(root))
    }

    @Test
    fun `a directory qualifies on a project file`() {
        val root = temp.newFolder("r")
        project(root, "", marker = "project.md")
        assertTrue(OpenSpecLayout.qualifies(root))
    }

    /** Content alone is not enough, or a sweep fills the list with junk. */
    @Test
    fun `content without any of the three does not qualify`() {
        val root = temp.newFolder("r")
        File(root, "openspec/specs/thing").mkdirs()
        File(root, "openspec/changes").mkdirs()
        assertFalse(OpenSpecLayout.qualifies(root))
    }

    @Test
    fun `a directory called openspec holding something else does not qualify`() {
        val root = temp.newFolder("r")
        File(root, "openspec").mkdirs()
        File(root, "openspec/notes.txt").writeText("hello")
        assertFalse(OpenSpecLayout.qualifies(root))
    }

    @Test
    fun `no openspec at all does not qualify`() {
        assertFalse(OpenSpecLayout.qualifies(temp.newFolder("empty")))
    }

    @Test
    fun `four projects at the top level are all found`() {
        val root = temp.newFolder("r")
        listOf("nivis", "nivis-demos", "nivis-tunnel", "registry").forEach { project(root, it) }
        File(root, "untangle").mkdirs()
        File(root, "untangle/README.md").writeText("no project here")

        val found = OpenSpecLayout.discover(root)

        assertEquals(
            listOf("nivis", "nivis-demos", "nivis-tunnel", "registry"),
            found.map { it.path },
        )
    }

    @Test
    fun `a project below the top level is found at its full path`() {
        val root = temp.newFolder("r")
        project(root, "clients/acme")

        assertEquals(listOf("clients/acme"), OpenSpecLayout.discover(root).map { it.path })
    }

    @Test
    fun `a root project comes first`() {
        val root = temp.newFolder("r")
        project(root, "")
        project(root, "nivis")

        assertEquals(listOf("", "nivis"), OpenSpecLayout.discover(root).map { it.path })
    }

    @Test
    fun `a repository with none reports none`() {
        val root = temp.newFolder("r")
        File(root, "src/main").mkdirs()
        assertTrue(OpenSpecLayout.discover(root).isEmpty())
    }

    @Test
    fun `the order does not depend on the filesystem`() {
        val root = temp.newFolder("r")
        listOf("zeta", "alpha", "middle").forEach { project(root, it) }

        assertEquals(
            OpenSpecLayout.discover(root).map { it.path },
            OpenSpecLayout.discover(root).map { it.path },
        )
        assertEquals(listOf("alpha", "middle", "zeta"), OpenSpecLayout.discover(root).map { it.path })
    }

    @Test
    fun `a project inside the git directory is not found`() {
        val root = temp.newFolder("r")
        project(root, ".git/modules/thing")

        assertTrue(OpenSpecLayout.discover(root).isEmpty())
    }

    /** An openspec directory is not walked into looking for another. */
    @Test
    fun `a project inside another project's openspec directory is not found`() {
        val root = temp.newFolder("r")
        project(root, "")
        project(root, "openspec/nested")

        assertEquals(listOf(""), OpenSpecLayout.discover(root).map { it.path })
    }

    @Test
    fun `a discovered project is named by its directory`() {
        val root = temp.newFolder("r")
        project(root, "nivis")
        File(root, "nivis/.openspec-store").mkdirs()
        File(root, "nivis/.openspec-store/store.yaml").writeText("version: 1\nid: something-else\n")

        val found = OpenSpecLayout.discover(root).single()
        assertEquals("nivis", found.name)
        assertEquals("nivis", found.path)
    }

    @Test
    fun `a nested project is named by its own directory, not its parent`() {
        val root = temp.newFolder("r")
        project(root, "clients/acme")
        assertEquals("acme", OpenSpecLayout.discover(root).single().name)
    }

    @Test
    fun `a qualifying project holding nothing is still offered`() {
        val root = temp.newFolder("r")
        val dir = File(root, "empty/openspec").apply { mkdirs() }
        File(dir, "config.yaml").writeText("schema: spec-driven\n")

        assertEquals(listOf("empty"), OpenSpecLayout.discover(root).map { it.path })
    }

    @Test
    fun `locating a project at a path finds it`() {
        val root = temp.newFolder("r")
        project(root, "nivis")

        val found = OpenSpecLayout.projectDir(root, "nivis")
        assertTrue("$found", found is RepoResult.Success)
    }

    @Test
    fun `locating a project at a path that holds none does not fall back to a sibling`() {
        val root = temp.newFolder("r")
        project(root, "nivis")
        File(root, "registry").mkdirs()

        val found = OpenSpecLayout.projectDir(root, "registry")
        assertTrue("$found", found is RepoResult.Failure)
        assertTrue((found as RepoResult.Failure).error.message.contains("registry"))
    }

    @Test
    fun `locating at the empty path is the root, as before`() {
        val root = temp.newFolder("r")
        project(root, "")

        assertTrue(OpenSpecLayout.projectDir(root, "") is RepoResult.Success)
        assertTrue(OpenSpecLayout.projectDir(root) is RepoResult.Success)
    }
}
