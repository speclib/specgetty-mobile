package io.github.mipmip.specgettyondroid.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoListTest {

    private fun repo(url: String, label: String = labelFor(url), hasToken: Boolean = false) =
        RepoConfig(repoIdFor(url), url, label, hasToken)

    private val a = repo("https://github.com/speclib/specgetty.git")
    private val b = repo("https://github.com/mipmip/beans-on-droid.git")

    @Test
    fun `the same URL gives the same id`() {
        assertEquals(repoIdFor("https://example.test/a"), repoIdFor("https://example.test/a"))
    }

    @Test
    fun `different URLs give different ids`() {
        assertNotEquals(repoIdFor("https://example.test/a"), repoIdFor("https://example.test/b"))
    }

    @Test
    fun `whitespace and case do not change the id`() {
        assertEquals(
            repoIdFor("https://example.test/a"),
            repoIdFor("  HTTPS://Example.Test/A  "),
        )
    }

    @Test
    fun `an id is safe as a file name`() {
        val urls = listOf(
            "https://github.com/speclib/specgetty.git",
            "http://example.test:8080/a/b/c?d=e#f",
            "https://user:token@host.test/x",
        )
        val forbidden = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', ' ')
        urls.forEach { url ->
            val id = repoIdFor(url)
            assertTrue(id, id.isNotEmpty())
            forbidden.forEach { c -> assertTrue("$id contains $c", c !in id) }
        }
    }

    @Test
    fun `a label defaults to the last URL segment without dot git`() {
        assertEquals("specgetty", labelFor("https://github.com/speclib/specgetty.git"))
        assertEquals("specgetty", labelFor("https://github.com/speclib/specgetty"))
    }

    @Test
    fun `a trailing slash does not become part of the label`() {
        assertEquals("specgetty", labelFor("https://github.com/speclib/specgetty/"))
    }

    @Test
    fun `adding puts a repository in the list and makes the first one active`() {
        val list = RepoList().add(a)
        assertEquals(listOf(a), list.repos)
        assertEquals(a.id, list.activeId)
        assertEquals(a, list.active)
    }

    @Test
    fun `a second repository does not steal the active slot`() {
        val list = RepoList().add(a).add(b)
        assertEquals(2, list.repos.size)
        assertEquals(a.id, list.activeId)
    }

    @Test
    fun `adding the same URL updates rather than duplicates`() {
        val renamed = a.copy(label = "renamed", hasToken = true)
        val list = RepoList().add(a).add(renamed)
        assertEquals(1, list.repos.size)
        assertEquals("renamed", list.repos.single().label)
        assertTrue(list.repos.single().hasToken)
    }

    @Test
    fun `removing the active repository promotes another`() {
        val list = RepoList().add(a).add(b).remove(a.id)
        assertEquals(listOf(b), list.repos)
        assertEquals(b.id, list.activeId)
    }

    @Test
    fun `removing a repository that is not active leaves the active one alone`() {
        val list = RepoList().add(a).add(b).remove(b.id)
        assertEquals(a.id, list.activeId)
    }

    @Test
    fun `removing the last repository leaves nothing active`() {
        val list = RepoList().add(a).remove(a.id)
        assertTrue(list.repos.isEmpty())
        assertNull(list.activeId)
        assertNull(list.active)
    }

    @Test
    fun `removing an unknown id changes nothing`() {
        val list = RepoList().add(a)
        assertEquals(list, list.remove("nope"))
    }

    @Test
    fun `activating switches the active repository`() {
        val list = RepoList().add(a).add(b).activate(b.id)
        assertEquals(b.id, list.activeId)
    }

    @Test
    fun `activating an unknown id changes nothing`() {
        val list = RepoList().add(a).add(b)
        assertEquals(list, list.activate("nope"))
    }

    @Test
    fun `the token flag can be set on one repository alone`() {
        val list = RepoList().add(a).add(b).withToken(b.id, true)
        assertTrue(list.repos.first { it.id == b.id }.hasToken)
        assertTrue(!list.repos.first { it.id == a.id }.hasToken)
    }
}
