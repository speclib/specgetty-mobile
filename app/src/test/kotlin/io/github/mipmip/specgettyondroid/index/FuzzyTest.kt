package io.github.mipmip.specgettyondroid.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyTest {

    @Test
    fun `the example from the search spec matches`() {
        assertTrue(Fuzzy.matches("expzip", "export-change-as-zip"))
    }

    @Test
    fun `characters out of order do not match`() {
        assertFalse(Fuzzy.matches("zipexp", "export-change-as-zip"))
    }

    @Test
    fun `a character not present does not match`() {
        assertFalse(Fuzzy.matches("expq", "export-change-as-zip"))
    }

    @Test
    fun `matching folds case`() {
        assertTrue(Fuzzy.matches("EXP", "export"))
        assertTrue(Fuzzy.matches("exp", "EXPORT"))
    }

    @Test
    fun `an empty pattern finds nothing to rank`() {
        assertTrue(Fuzzy.find("", listOf("a", "b")).isEmpty())
    }

    @Test
    fun `a match at the start beats one in the middle`() {
        val ranked = Fuzzy.find("ab", listOf("xxxxab", "abxxxx"))
        assertEquals("abxxxx", listOf("xxxxab", "abxxxx")[ranked.first().index])
    }

    /**
     * Confirmed against the library: a separator bonus is 20 and an adjacency
     * bonus starts at 5, so a match after a separator outranks an adjacent one.
     * The oracle test pins the whole table; this names the surprising case.
     */
    @Test
    fun `a separator bonus outweighs a single adjacency`() {
        val candidates = listOf("a-x-y-b", "ab-x-y")
        val ranked = Fuzzy.find("ab", candidates)
        assertEquals("a-x-y-b", candidates[ranked.first().index])
        assertEquals(25, ranked[0].score)
        assertEquals(11, ranked[1].score)
    }

    @Test
    fun `adjacent characters beat scattered ones with no separator between`() {
        val candidates = listOf("axyb", "abxy")
        val ranked = Fuzzy.find("ab", candidates)
        assertEquals("abxy", candidates[ranked.first().index])
    }

    @Test
    fun `a match after a separator scores above one mid-word`() {
        val candidates = listOf("xxb", "x-b")
        val ranked = Fuzzy.find("b", candidates)
        assertEquals("x-b", candidates[ranked.first().index])
    }

    @Test
    fun `every candidate the pattern is a subsequence of is returned`() {
        val candidates = listOf("export-change-as-zip", "expand-zip", "nothing")
        val ranked = Fuzzy.find("expzip", candidates)
        assertEquals(2, ranked.size)
        assertTrue(ranked.none { candidates[it.index] == "nothing" })
    }

    @Test
    fun `results are strongest first`() {
        val candidates = listOf("xxxxxxxxxxab", "ab")
        val ranked = Fuzzy.find("ab", candidates)
        assertTrue(ranked[0].score > ranked[1].score)
        assertEquals("ab", candidates[ranked[0].index])
    }

    @Test
    fun `the matched indexes are where the characters were found`() {
        val match = Fuzzy.scoreOf("ez", "export-zip")
        assertNotNull(match)
        assertEquals(2, match!!.matchedIndexes.size)
        assertEquals('e', "export-zip"[match.matchedIndexes[0]])
        assertEquals('z', "export-zip"[match.matchedIndexes[1]])
    }

    @Test
    fun `a pattern that does not match scores nothing`() {
        assertNull(Fuzzy.scoreOf("q", "export"))
    }

    @Test
    fun `exact case agrees only when the characters agree`() {
        val match = Fuzzy.scoreOf("Ex", "Export")!!
        assertTrue(Fuzzy.exactCase(match, "Export", "Ex"))

        val folded = Fuzzy.scoreOf("Ex", "export")!!
        assertFalse(Fuzzy.exactCase(folded, "export", "Ex"))
    }

    @Test
    fun `a longer candidate is penalised against a shorter one`() {
        val candidates = listOf("ab-and-a-great-deal-more-text", "ab")
        val ranked = Fuzzy.find("ab", candidates)
        assertEquals("ab", candidates[ranked.first().index])
    }

    @Test
    fun `camel case is scored as a word boundary`() {
        val candidates = listOf("xxxxb", "xxxxB")
        val ranked = Fuzzy.find("b", candidates)
        assertEquals("xxxxB", candidates[ranked.first().index])
    }

    @Test
    fun `an empty candidate matches only an empty pattern`() {
        assertTrue(Fuzzy.matches("", ""))
        assertFalse(Fuzzy.matches("a", ""))
    }

    @Test
    fun `the whole candidate matching itself is the strongest case`() {
        val candidates = listOf("export", "ee-xx-pp-oo-rr-tt")
        val ranked = Fuzzy.find("export", candidates)
        assertEquals("export", candidates[ranked.first().index])
    }
}
