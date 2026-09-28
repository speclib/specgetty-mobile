package io.github.mipmip.specgettyondroid.store

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepoRegistryTest {

    private lateinit var store: InMemoryPreferences
    private lateinit var vault: FakeVault
    private lateinit var registry: RepoRegistry

    @Before
    fun setUp() {
        store = InMemoryPreferences()
        vault = FakeVault()
        registry = RepoRegistry(store, vault)
    }

    @Test
    fun `an empty store yields an empty list`() = runTest {
        assertTrue(registry.current().repos.isEmpty())
        assertNull(registry.current().activeId)
    }

    @Test
    fun `an added repository is readable back`() = runTest {
        val added = registry.add("https://github.com/speclib/specgetty.git", "", null)
        val list = registry.current()
        assertEquals(listOf(added), list.repos)
        assertEquals(added.id, list.activeId)
        assertEquals("specgetty", added.label)
    }

    @Test
    fun `a typed label wins over the default`() = runTest {
        val added = registry.add("https://github.com/speclib/specgetty.git", "My specs", null)
        assertEquals("My specs", added.label)
    }

    @Test
    fun `a token is put in the vault and only flagged in the list`() = runTest {
        val added = registry.add("https://example.test/private.git", "", "ghp_secret")
        assertEquals("ghp_secret", vault.tokens[added.id])
        assertTrue(registry.current().repos.single().hasToken)

        val raw = store.data.first()[stringPreferencesKey("repos")]!!
        assertFalse("the list must not carry the token", raw.contains("ghp_secret"))
    }

    @Test
    fun `a blank token stores nothing`() = runTest {
        val added = registry.add("https://example.test/a.git", "", "   ")
        assertTrue(vault.tokens.isEmpty())
        assertEquals(0, vault.puts)
        assertFalse(registry.current().repos.single().hasToken)
        assertNull(registry.tokenFor(added.id))
    }

    @Test
    fun `a token comes back through the registry`() = runTest {
        val added = registry.add("https://example.test/a.git", "", "ghp_secret")
        assertEquals("ghp_secret", registry.tokenFor(added.id))
    }

    @Test
    fun `removing a repository removes its token`() = runTest {
        val added = registry.add("https://example.test/a.git", "", "ghp_secret")
        registry.remove(added.id)
        assertTrue(registry.current().repos.isEmpty())
        assertNull(registry.tokenFor(added.id))
    }

    @Test
    fun `adding the same URL twice keeps one entry`() = runTest {
        registry.add("https://example.test/a.git", "first", null)
        registry.add("https://example.test/a.git", "second", null)
        assertEquals(1, registry.current().repos.size)
        assertEquals("second", registry.current().repos.single().label)
    }

    @Test
    fun `activating switches the active repository`() = runTest {
        registry.add("https://example.test/a.git", "", null)
        val b = registry.add("https://example.test/b.git", "", null)
        registry.activate(b.id)
        assertEquals(b.id, registry.current().activeId)
    }

    @Test
    fun `the list persists across a new registry over the same store`() = runTest {
        registry.add("https://example.test/a.git", "", null)
        val reopened = RepoRegistry(store, vault)
        assertEquals(1, reopened.current().repos.size)
    }

    @Test
    fun `undecodable stored data yields an empty list rather than a failure`() = runTest {
        store.edit { it[stringPreferencesKey("repos")] = "{ not json at all" }
        assertTrue(registry.current().repos.isEmpty())
    }

    @Test
    fun `a field this version does not know is ignored`() = runTest {
        store.edit {
            it[stringPreferencesKey("repos")] = """
                {"repos":[{"id":"x","url":"https://e.test/a","label":"a","futureField":9}],
                 "activeId":"x"}
            """.trimIndent()
        }
        assertEquals("a", registry.current().repos.single().label)
    }

    @Test
    fun `the flow emits the current list`() = runTest {
        registry.add("https://example.test/a.git", "", null)
        assertEquals(1, registry.repos.first().repos.size)
    }
}
