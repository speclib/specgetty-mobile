package io.github.mipmip.specgettyondroid.repo

import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RepoStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var store: RepoStore
    private lateinit var remote: LocalRemote

    @Before
    fun setUp() {
        store = RepoStore(temp.newFolder("repos"))
        remote = LocalRemote(temp.newFolder("remote")).init()
    }

    private fun clone(id: String = "one", url: String = remote.url, token: String? = null) =
        runBlocking { store.clone(id, url, token) }

    @Test
    fun `a clone produces a working copy`() {
        val dir = clone().valueOrNull()!!
        assertTrue(dir.isDirectory)
        assertTrue(File(dir, "openspec/config.yaml").isFile)
        assertTrue(store.isCloned("one"))
    }

    @Test
    fun `a clone fetches only the latest commit`() {
        remote.commit("second.txt", "two", "second")
        remote.commit("third.txt", "three", "third")
        assertEquals(3, remote.commitCount())

        clone()
        Git.open(store.workingDir("one")).use { assertEquals(1, it.log().call().count()) }
    }

    @Test
    fun `two repositories do not collide`() {
        val other = LocalRemote(temp.newFolder("remote-two")).init()
        other.commit("openspec/specs/other/spec.md", LocalRemote.spec("other"), "other spec")

        clone("one")
        clone("two", other.url)

        assertTrue(File(store.workingDir("two"), "openspec/specs/other/spec.md").isFile)
        assertFalse(File(store.workingDir("one"), "openspec/specs/other/spec.md").isFile)
    }

    @Test
    fun `cloning over an existing directory replaces it`() {
        clone()
        File(store.workingDir("one"), "stale.txt").writeText("left over")
        clone()
        assertFalse(File(store.workingDir("one"), "stale.txt").exists())
    }

    @Test
    fun `a refresh picks up a new commit`() {
        clone()
        remote.commit("openspec/specs/later/spec.md", LocalRemote.spec("later"), "later")

        val result = runBlocking { store.refresh("one") }
        assertTrue("$result", result is RepoResult.Success)
        assertTrue(File(store.workingDir("one"), "openspec/specs/later/spec.md").isFile)
    }

    @Test
    fun `a refresh removes a file deleted upstream`() {
        clone()
        assertTrue(File(store.workingDir("one"), "openspec/config.yaml").isFile)
        remote.removeAndCommit("openspec/config.yaml", "drop the config")

        runBlocking { store.refresh("one") }
        assertFalse(File(store.workingDir("one"), "openspec/config.yaml").exists())
    }

    @Test
    fun `a refresh discards a local modification`() {
        clone()
        val local = File(store.workingDir("one"), "openspec/config.yaml")
        local.writeText("scribbled over\n")

        runBlocking { store.refresh("one") }
        assertEquals("schema: spec-driven\n", local.readText())
    }

    @Test
    fun `a refresh of something never cloned fails`() {
        val result = runBlocking { store.refresh("never") }
        assertTrue("$result", result is RepoResult.Failure)
        assertTrue(result.errorOrNull() is RepoError.Unknown)
    }

    @Test
    fun `deleting a cloned repository removes it`() {
        clone()
        assertTrue(store.delete("one"))
        assertFalse(store.workingDir("one").exists())
        assertFalse(store.isCloned("one"))
    }

    @Test
    fun `deleting something that is not there reports nothing removed`() {
        assertFalse(store.delete("never"))
    }

    @Test
    fun `a clone of a host that does not resolve is a network failure`() {
        val result = clone("bad", "https://no-such-host.invalid/x/y.git")
        assertTrue("$result", result is RepoResult.Failure)
        assertTrue("${result.errorOrNull()}", result.errorOrNull() is RepoError.Network)
    }

    @Test
    fun `a failed clone leaves no working directory behind`() {
        clone("bad", "https://no-such-host.invalid/x/y.git")
        assertFalse(store.workingDir("bad").exists())
    }

    @Test
    fun `a URL that is not a repository fails rather than throwing`() {
        val notARepo = temp.newFolder("not-a-repo")
        val result = clone("nope", notARepo.toURI().toString())
        assertTrue("$result", result is RepoResult.Failure)
        assertNull(result.valueOrNull())
    }

    @Test
    fun `a blank token is the same as no token`() {
        val result = clone("blank", remote.url, "   ")
        assertTrue("$result", result is RepoResult.Success)
    }

    @Test
    fun `the project directory of a cloned repository is found`() {
        clone()
        assertTrue(store.projectDir("one") is RepoResult.Success)
    }

    @Test
    fun `a repository with no openspec directory says so`() {
        val plain = LocalRemote(temp.newFolder("plain")).initBare()
        clone("plain", plain.url)
        val result = store.projectDir("plain")
        assertTrue("$result", result.errorOrNull() is RepoError.NoOpenSpecProject)
    }
}
