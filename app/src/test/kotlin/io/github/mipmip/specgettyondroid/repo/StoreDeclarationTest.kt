package io.github.mipmip.specgettyondroid.repo

import io.github.mipmip.specgettyondroid.project.ProjectLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * A repository that points at a store is a real project whose content lives on
 * another machine. Reporting it as absent sends a person looking for the wrong
 * thing, which is why specgetty's own spec insists on naming the id and the
 * file that declared it.
 */
class StoreDeclarationTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun projectWith(config: String, name: String = "config.yaml"): File {
        val root = temp.newFolder()
        val openspec = File(root, "openspec").apply { mkdirs() }
        File(openspec, name).writeText(config)
        return root
    }

    @Test
    fun `a declaration is read from config yaml`() {
        val root = projectWith("store: nivis-tunnel\n")
        val found = StoreDeclarations.read(File(root, "openspec"))

        assertTrue("$found", found is StoreDeclaration.Named)
        assertEquals("nivis-tunnel", (found as StoreDeclaration.Named).id)
        assertEquals("openspec/config.yaml", found.declaredIn)
    }

    @Test
    fun `a declaration is read from the yml spelling too`() {
        val root = projectWith("store: nivis\n", name = "config.yml")
        val found = StoreDeclarations.read(File(root, "openspec"))

        assertEquals("nivis", (found as StoreDeclaration.Named).id)
        assertEquals("openspec/config.yml", found.declaredIn)
    }

    @Test
    fun `a configuration with no store key declares nothing`() {
        val root = projectWith("schema: spec-driven\n")
        assertNull(StoreDeclarations.read(File(root, "openspec")))
    }

    @Test
    fun `a store key holding a list is malformed`() {
        val root = projectWith("store:\n  - a\n  - b\n")
        assertTrue(StoreDeclarations.read(File(root, "openspec")) is StoreDeclaration.Malformed)
    }

    @Test
    fun `a store key holding a mapping is malformed`() {
        val root = projectWith("store:\n  id: a\n")
        assertTrue(StoreDeclarations.read(File(root, "openspec")) is StoreDeclaration.Malformed)
    }

    @Test
    fun `loading reports the id and the file rather than an absent project`() {
        val root = projectWith("store: nivis-tunnel\n")
        val loaded = ProjectLoader.load(root)

        val error = (loaded as RepoResult.Failure).error
        assertTrue("$error", error is RepoError.PointsElsewhere)
        assertTrue(error.message, error.message.contains("nivis-tunnel"))
        assertTrue(error.message, error.message.contains("openspec/config.yaml"))
        assertFalse(error.message, error.message.contains("no OpenSpec project here"))
    }

    @Test
    fun `it is not the absent-project failure`() {
        val root = projectWith("store: nivis\n")
        val error = (ProjectLoader.load(root) as RepoResult.Failure).error
        assertFalse("$error", error is RepoError.NoOpenSpecProject)
    }

    /** Content present locally outranks a pointer, which is specgetty's rule. */
    @Test
    fun `a declaration is ignored when the project holds specs of its own`() {
        val root = projectWith("store: nivis\n")
        File(root, "openspec/specs/thing").mkdirs()
        File(root, "openspec/specs/thing/spec.md").writeText("# thing\n")

        val loaded = ProjectLoader.load(root)
        assertTrue("$loaded", loaded is RepoResult.Success)
    }

    @Test
    fun `a declaration is ignored when the project holds changes of its own`() {
        val root = projectWith("store: nivis\n")
        File(root, "openspec/changes/a-change").mkdirs()

        assertTrue(ProjectLoader.load(root) is RepoResult.Success)
    }

    @Test
    fun `an empty project with no declaration still loads as empty`() {
        val root = projectWith("schema: spec-driven\n")
        assertTrue(ProjectLoader.load(root) is RepoResult.Success)
    }

    @Test
    fun `a malformed declaration is reported as malformed`() {
        val root = projectWith("store:\n  - a\n")
        val error = (ProjectLoader.load(root) as RepoResult.Failure).error

        assertTrue("$error", error is RepoError.PointsElsewhere)
        assertTrue(error.message, error.message.contains("not a single id"))
    }

    @Test
    fun `configuration that will not parse declares nothing`() {
        val root = projectWith("store: [unclosed\n")
        assertNull(StoreDeclarations.read(File(root, "openspec")))
    }

    @Test
    fun `a project with no configuration file declares nothing`() {
        val root = temp.newFolder()
        File(root, "openspec").mkdirs()
        assertNull(StoreDeclarations.read(File(root, "openspec")))
    }
}
