package io.github.mipmip.specgettyondroid.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlCaptureTest {

    private fun found(text: String): String =
        (UrlCapture.capture(text) as CaptureResult.Found).url

    @Test
    fun `an address is picked out of surrounding text`() {
        assertEquals(
            "https://github.com/speclib/specgetty",
            found("Look at https://github.com/speclib/specgetty nice one"),
        )
    }

    @Test
    fun `text that is only an address`() {
        assertEquals(
            "https://github.com/speclib/specgetty",
            found("https://github.com/speclib/specgetty"),
        )
    }

    @Test
    fun `trailing punctuation is not part of the address`() {
        listOf(".", ",", ")", "]", "}", ";", ":", "!", "?", "\"", "'").forEach { mark ->
            assertEquals(
                "punctuation $mark",
                "https://github.com/speclib/specgetty",
                found("see https://github.com/speclib/specgetty$mark"),
            )
        }
    }

    @Test
    fun `the first of several addresses is taken`() {
        assertEquals(
            "https://example.test/a",
            found("https://example.test/a and https://example.test/b"),
        )
    }

    @Test
    fun `text with no address reports none`() {
        assertEquals(CaptureResult.NoUrl, UrlCapture.capture("nothing to see"))
        assertEquals(CaptureResult.NoUrl, UrlCapture.capture(""))
        assertNull(UrlCapture.extract("just words"))
    }

    @Test
    fun `plain http is an address too`() {
        assertEquals("http://localhost:8080/a", found("http://localhost:8080/a"))
    }

    @Test
    fun `the measured forge URLs normalise to something that clones`() {
        val table = listOf(
            "https://github.com/speclib/specgetty.git" to
                "https://github.com/speclib/specgetty.git",
            "https://github.com/speclib/specgetty" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty?tab=readme-ov-file" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty#readme" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty/issues" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty/issues/12" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty/tree/main/src" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty/blob/main/README.md" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty/pull/3" to
                "https://github.com/speclib/specgetty",
            "https://github.com/speclib/specgetty/releases/tag/v1" to
                "https://github.com/speclib/specgetty",
            "https://gitlab.com/group/subgroup/proj/-/tree/main" to
                "https://gitlab.com/group/subgroup/proj",
            "https://gitlab.com/group/proj/-/merge_requests/7" to
                "https://gitlab.com/group/proj",
            "https://codeberg.org/owner/repo/src/branch/main" to
                "https://codeberg.org/owner/repo",
            "https://github.com/speclib/specgetty/" to
                "https://github.com/speclib/specgetty",
            "https://git.example.test/team/repo" to
                "https://git.example.test/team/repo",
        )
        table.forEach { (input, expected) ->
            assertEquals(input, expected, UrlCapture.normalise(input))
        }
    }

    @Test
    fun `a repository named after a view segment survives`() {
        assertEquals(
            "https://github.com/someone/issues",
            UrlCapture.normalise("https://github.com/someone/issues"),
        )
        assertEquals(
            "https://github.com/someone/tree",
            UrlCapture.normalise("https://github.com/someone/tree"),
        )
    }

    @Test
    fun `a repository named after a view segment is still truncated past itself`() {
        assertEquals(
            "https://github.com/someone/issues",
            UrlCapture.normalise("https://github.com/someone/issues/issues/4"),
        )
    }

    @Test
    fun `no dot git is added to a URL that lacks one`() {
        assertTrue(!UrlCapture.normalise("https://github.com/a/b").endsWith(".git"))
    }

    @Test
    fun `a bare host is returned unchanged`() {
        assertEquals("https://github.com", UrlCapture.normalise("https://github.com"))
        assertEquals("https://github.com", UrlCapture.normalise("https://github.com/"))
    }

    @Test
    fun `something with no scheme separator is left alone`() {
        assertEquals("github.com/a/b", UrlCapture.normalise("github.com/a/b"))
    }

    @Test
    fun `credentials in the authority are stripped`() {
        val result = UrlCapture.normalise("https://user:ghp_secret@github.com/a/b")
        assertEquals("https://github.com/a/b", result)
        assertTrue(!result.contains("ghp_secret"))
        assertTrue(!result.contains("user"))
    }

    @Test
    fun `a token in the query string goes with the query string`() {
        val result = found("https://github.com/a/b?access_token=ghp_secret")
        assertEquals("https://github.com/a/b", result)
        assertTrue(!result.contains("ghp_secret"))
    }

    @Test
    fun `credentials do not survive a capture from surrounding text`() {
        val result = found("clone https://user:ghp_secret@gitlab.com/g/p/-/tree/main please")
        assertEquals("https://gitlab.com/g/p", result)
        assertTrue(!result.contains("ghp_secret"))
    }

    @Test
    fun `capture normalises as well as extracts`() {
        assertEquals(
            "https://github.com/speclib/specgetty",
            found("have a look at https://github.com/speclib/specgetty/issues/12, thanks"),
        )
    }
}
