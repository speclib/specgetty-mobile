package io.github.mipmip.specgettyondroid.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryTest {

    @Test
    fun `no sigil is a fuzzy name query`() {
        val q = Query.parse("expzip")
        assertEquals(MatchKind.FUZZY_NAME, q.kind)
        assertEquals("expzip", q.term)
    }

    @Test
    fun `the apostrophe sigil is a literal name query`() {
        val q = Query.parse("'export")
        assertEquals(MatchKind.LITERAL_NAME, q.kind)
        assertEquals("export", q.term)
    }

    @Test
    fun `the colon sigil is a body query`() {
        val q = Query.parse(":inotify")
        assertEquals(MatchKind.BODY, q.kind)
        assertEquals("inotify", q.term)
    }

    @Test
    fun `a sigil with nothing after it is an empty query`() {
        assertTrue(Query.parse("'").isEmpty)
        assertTrue(Query.parse(":").isEmpty)
        assertTrue(Query.parse("").isEmpty)
    }

    @Test
    fun `a sigil in the middle is not a sigil`() {
        val q = Query.parse("a:b")
        assertEquals(MatchKind.FUZZY_NAME, q.kind)
        assertEquals("a:b", q.term)
    }

    @Test
    fun `an all lower case term is case insensitive`() {
        val q = Query.parse("'export")
        assertFalse(q.caseSensitive)
        assertTrue(q.contains("Export-Change"))
        assertTrue(q.contains("EXPORT"))
    }

    @Test
    fun `a term with an upper case character is case sensitive`() {
        val q = Query.parse("'Export")
        assertTrue(q.caseSensitive)
        assertFalse(q.contains("export-change"))
        assertTrue(q.contains("Export-Change"))
    }

    @Test
    fun `the sigil itself does not make a query case sensitive`() {
        assertFalse(Query.parse(":lower").caseSensitive)
    }
}
