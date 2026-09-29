package io.github.mipmip.specgettyondroid.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One hash used to do four jobs. It now does two: the clone is named by the URL
 * and the list entry by the URL together with a path inside the repository.
 */
class EntryIdTest {

    private val url = "https://github.com/mipmip/test.git"

    @Test
    fun `the same url and path give the same entry id`() {
        assertEquals(entryIdFor(url, "nivis"), entryIdFor(url, "nivis"))
    }

    @Test
    fun `one url with two paths gives two entry ids`() {
        assertNotEquals(entryIdFor(url, "nivis"), entryIdFor(url, "registry"))
    }

    @Test
    fun `two urls give two entry ids`() {
        assertNotEquals(
            entryIdFor(url, "nivis"),
            entryIdFor("https://github.com/mipmip/other.git", "nivis"),
        )
    }

    @Test
    fun `the clone id ignores the path`() {
        assertEquals(repoIdFor(url), repoIdFor(url))
        assertEquals(
            repoIdFor(url),
            repoIdFor(url),
        )
    }

    /** What makes the migration nothing at all. */
    @Test
    fun `the empty path gives the id the url alone gives`() {
        assertEquals(repoIdFor(url), entryIdFor(url, ""))
        assertEquals(repoIdFor(url), entryIdFor(url, "   "))
        assertEquals(repoIdFor(url), entryIdFor(url, "/"))
    }

    /**
     * A separator a path can hold would let `a` with `b/c` and `a/b` with `c`
     * hash to one id. NUL cannot appear in a path, so they cannot.
     */
    @Test
    fun `a path boundary cannot be moved to produce a collision`() {
        assertNotEquals(
            entryIdFor("https://example.test/a", "b/c"),
            entryIdFor("https://example.test/a/b", "c"),
        )
    }

    @Test
    fun `surrounding whitespace and case do not change either id`() {
        assertEquals(repoIdFor(url), repoIdFor("  ${url.uppercase()}  "))
        assertEquals(entryIdFor(url, "nivis"), entryIdFor("  ${url.uppercase()}  ", " nivis "))
    }

    @Test
    fun `a trailing separator on the path does not change the id`() {
        assertEquals(entryIdFor(url, "nivis"), entryIdFor(url, "nivis/"))
        assertEquals(entryIdFor(url, "a/b"), entryIdFor(url, "/a/b/"))
    }

    @Test
    fun `both ids are safe as a file name`() {
        val ids = listOf(
            repoIdFor("https://example.test/a b/c?d=e#f"),
            entryIdFor("https://example.test/a b/c?d=e#f", "deep/nested/path"),
            entryIdFor("", ""),
        )
        ids.forEach { id ->
            assertTrue(id, id.isNotEmpty())
            assertTrue(id, id.none { it in "/\\:*?\"<>| " })
        }
    }

    @Test
    fun `a nested path differs from its own parent`() {
        assertNotEquals(entryIdFor(url, "a"), entryIdFor(url, "a/b"))
    }

    @Test
    fun `normalising a path trims and strips separators`() {
        assertEquals("nivis", normalisePath("  /nivis/  "))
        assertEquals("", normalisePath("/"))
        assertEquals("a/b", normalisePath("a/b"))
    }
}
