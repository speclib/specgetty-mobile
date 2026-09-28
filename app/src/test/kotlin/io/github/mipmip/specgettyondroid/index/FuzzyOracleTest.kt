package io.github.mipmip.specgettyondroid.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The port is pinned against the library it was ported from. specgetty ranks
 * with `github.com/sahilm/fuzzy`, so a difference here is a difference the user
 * would see between the two tools looking at the same project.
 *
 * `expected.txt` was produced by running that library over `cases.tsv`.
 */
class FuzzyOracleTest {

    private fun resource(name: String): List<String> =
        requireNotNull(javaClass.getResourceAsStream("/fuzzy/$name")) { "$name is missing" }
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }

    private val cases: List<Pair<String, List<String>>> by lazy {
        resource("cases.tsv").map { line ->
            val parts = line.split('\t')
            parts.first() to parts.drop(1)
        }
    }

    private val expected: List<String> by lazy { resource("expected.txt") }

    private fun render(pattern: String, candidates: List<String>): String {
        val ranked = Fuzzy.find(pattern, candidates)
            .joinToString(" ") { "${candidates[it.index]}=${it.score}" }
        return "$pattern -> $ranked"
    }

    @Test
    fun `the oracle is present and covers both real names and edge cases`() {
        assertEquals("a case per expectation", cases.size, expected.size)
        assertTrue("only ${cases.size} cases", cases.size >= 20)
        assertTrue(cases.any { it.second.size >= 50 })
    }

    @Test
    fun `every case scores and ranks exactly as the library does`() {
        val mismatches = cases.mapIndexedNotNull { i, (pattern, candidates) ->
            val got = render(pattern, candidates)
            if (got == expected[i]) null else "want: ${expected[i]}\ngot:  $got"
        }
        assertTrue(
            "${mismatches.size} of ${cases.size} cases differ:\n" +
                mismatches.take(3).joinToString("\n\n"),
            mismatches.isEmpty(),
        )
    }
}
