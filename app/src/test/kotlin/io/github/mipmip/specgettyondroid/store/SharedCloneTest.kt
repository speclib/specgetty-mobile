package io.github.mipmip.specgettyondroid.store

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Several entries on one repository. The trap is that removing one of them used
 * to delete the working copy and the credential the others still need, which
 * looks like a working feature until there are two.
 */
class SharedCloneTest {

    private val url = "https://example.test/stores.git"

    private fun registry(vault: TokenVault = FakeVault()) =
        RepoRegistry(InMemoryPreferences(), vault)

    @Test
    fun `two paths from one url are two entries`() = runTest {
        val r = registry()
        r.add(url, "", null, "nivis")
        r.add(url, "", null, "registry")

        assertEquals(2, r.current().repos.size)
    }

    @Test
    fun `the same path twice is one entry`() = runTest {
        val r = registry()
        r.add(url, "first", null, "nivis")
        r.add(url, "second", null, "nivis")

        val repos = r.current().repos
        assertEquals(1, repos.size)
        assertEquals("second", repos.single().label)
    }

    @Test
    fun `two entries on one url share one clone id`() = runTest {
        val r = registry()
        val a = r.add(url, "", null, "nivis")
        val b = r.add(url, "", null, "registry")

        assertEquals(a.cloneId, b.cloneId)
        assertTrue(a.id != b.id)
    }

    @Test
    fun `a second entry on a repository with a token stores no second copy`() = runTest {
        val vault = FakeVault()
        val r = registry(vault)
        r.add(url, "", "ghp_secret", "nivis")
        r.add(url, "", null, "registry")

        assertEquals(1, vault.tokens.size)
        assertEquals("ghp_secret", vault.tokens.values.single())
    }

    @Test
    fun `a second entry inherits the credential the first stored`() = runTest {
        val vault = FakeVault()
        val r = registry(vault)
        r.add(url, "", "ghp_secret", "nivis")
        val second = r.add(url, "", null, "registry")

        assertTrue(second.hasToken)
        assertEquals("ghp_secret", r.tokenFor(second.id))
    }

    @Test
    fun `removing one of two keeps the credential`() = runTest {
        val vault = FakeVault()
        val r = registry(vault)
        val a = r.add(url, "", "ghp_secret", "nivis")
        val b = r.add(url, "", null, "registry")

        r.remove(a.id)

        assertEquals(1, r.current().repos.size)
        assertEquals("ghp_secret", r.tokenFor(b.id))
    }

    @Test
    fun `removing the last entry removes the credential`() = runTest {
        val vault = FakeVault()
        val r = registry(vault)
        val a = r.add(url, "", "ghp_secret", "nivis")
        val b = r.add(url, "", null, "registry")

        r.remove(a.id)
        r.remove(b.id)

        assertTrue("${vault.tokens}", vault.tokens.isEmpty())
    }

    @Test
    fun `removing the only entry on a url removes its credential`() = runTest {
        val vault = FakeVault()
        val r = registry(vault)
        val only = r.add(url, "", "ghp_secret", "nivis")

        r.remove(only.id)

        assertTrue(vault.tokens.isEmpty())
    }

    @Test
    fun `an entry on another url is not counted as sharing`() = runTest {
        val vault = FakeVault()
        val r = registry(vault)
        val a = r.add(url, "", "ghp_a", "")
        r.add("https://example.test/other.git", "", "ghp_b", "")

        r.remove(a.id)

        assertEquals(1, vault.tokens.size)
        assertEquals("ghp_b", vault.tokens.values.single())
    }

    @Test
    fun `a label defaults to the directory for a project in a subdirectory`() = runTest {
        val r = registry()
        assertEquals("nivis", r.add(url, "", null, "nivis").label)
    }

    @Test
    fun `a label defaults to the repository name at the root`() = runTest {
        val r = registry()
        assertEquals("stores", r.add(url, "", null, "").label)
    }

    /** What makes the migration nothing at all. */
    @Test
    fun `a list written before paths existed decodes with the empty path`() {
        val stored = """{"repos":[{"id":"abc","url":"$url","label":"stores","hasToken":true}],
            "activeId":"abc"}"""
        val list = Json { ignoreUnknownKeys = true }.decodeFromString<RepoList>(stored)

        val entry = list.repos.single()
        assertEquals("", entry.path)
        assertEquals("abc", entry.id)
    }

    @Test
    fun `an entry stored before paths keeps the clone it already has`() {
        val entry = RepoConfig(id = repoIdFor(url), url = url, label = "stores")

        assertEquals(entry.id, entry.cloneId)
        assertEquals(entry.id, entryIdFor(url, entry.path))
    }

    @Test
    fun `othersShare is false for an id the list does not hold`() {
        assertFalse(RepoList().othersShare("nope"))
    }

    @Test
    fun `asking for the token of an unknown id yields nothing`() = runTest {
        assertNull(registry().tokenFor("nope"))
    }

    @Test
    fun `the first entry becomes active and a later one does not steal focus`() = runTest {
        val r = registry()
        val a = r.add(url, "", null, "nivis")
        r.add(url, "", null, "registry")

        assertEquals(a.id, r.current().activeId)
        assertNotNull(r.current().active)
    }
}
