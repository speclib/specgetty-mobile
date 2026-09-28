package io.github.mipmip.specgettyondroid.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoUrlTest {

    @Test
    fun `an empty URL asks for one`() {
        assertEquals("Enter the repository's clone URL.", RepoUrl.validate(""))
        assertEquals("Enter the repository's clone URL.", RepoUrl.validate("   "))
    }

    @Test
    fun `an scp style SSH URL is refused`() {
        val message = RepoUrl.validate("git@github.com:speclib/specgetty.git")
        assertTrue("$message", message!!.contains("SSH"))
    }

    @Test
    fun `an ssh scheme URL is refused`() {
        val message = RepoUrl.validate("ssh://git@github.com/speclib/specgetty.git")
        assertTrue("$message", message!!.contains("SSH"))
    }

    @Test
    fun `something that is not a web address is refused`() {
        assertEquals("The URL must start with https://.", RepoUrl.validate("github.com/a/b"))
        assertEquals("The URL must start with https://.", RepoUrl.validate("ftp://host/a"))
    }

    @Test
    fun `a scheme with no host is refused`() {
        assertEquals("That URL has no host.", RepoUrl.validate("https://"))
    }

    @Test
    fun `an https URL with a host passes`() {
        assertNull(RepoUrl.validate("https://github.com/speclib/specgetty.git"))
    }

    @Test
    fun `surrounding whitespace does not fail a good URL`() {
        assertNull(RepoUrl.validate("  https://github.com/speclib/specgetty  "))
    }

    @Test
    fun `plain http is allowed, for a local server`() {
        assertNull(RepoUrl.validate("http://localhost:8080/a.git"))
    }
}
