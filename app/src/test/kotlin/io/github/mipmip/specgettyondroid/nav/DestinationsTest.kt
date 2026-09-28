package io.github.mipmip.specgettyondroid.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationsTest {

    @Test
    fun `a plain change name round trips`() {
        val route = Destinations.change("repo-1", "add-repo-by-scan")
        assertEquals("change/repo-1/add-repo-by-scan", route)
    }

    @Test
    fun `an archived change name keeps its date`() {
        val route = Destinations.change("r", "2026-09-28-add-repo-by-scan")
        assertEquals("change/r/2026-09-28-add-repo-by-scan", route)
    }

    @Test
    fun `a capability with a slash does not invent a route segment`() {
        val route = Destinations.spec("r", "repo/url-capture")
        assertEquals("spec/r/repo%2Furl-capture", route)
        assertEquals(2, route.count { it == '/' })
    }

    @Test
    fun `a name with a space survives the round trip`() {
        val original = "a name with spaces"
        assertEquals(original, Destinations.decode(Destinations.encode(original)))
    }

    @Test
    fun `a name with characters a URL would eat survives the round trip`() {
        val original = "weird/name?with#bits&more=1 and a plus+"
        assertEquals(original, Destinations.decode(Destinations.encode(original)))
    }

    @Test
    fun `every pattern names the arguments its builder fills`() {
        assertTrue(Destinations.PROJECT_PATTERN.contains("{${Destinations.PROJECT_ARG_REPO}}"))
        assertTrue(Destinations.CHANGE_PATTERN.contains("{${Destinations.CHANGE_ARG_NAME}}"))
        assertTrue(Destinations.SPEC_PATTERN.contains("{${Destinations.SPEC_ARG_CAPABILITY}}"))
        assertTrue(Destinations.DELTA_PATTERN.contains("{${Destinations.DELTA_ARG_NAME}}"))
    }

    @Test
    fun `a built route matches the shape of its pattern`() {
        fun segments(s: String) = s.split('/').size
        assertEquals(segments(Destinations.PROJECT_PATTERN), segments(Destinations.project("r")))
        assertEquals(segments(Destinations.CHANGE_PATTERN), segments(Destinations.change("r", "c")))
        assertEquals(segments(Destinations.SPEC_PATTERN), segments(Destinations.spec("r", "c")))
        assertEquals(segments(Destinations.DELTA_PATTERN), segments(Destinations.delta("r", "c")))
    }

    @Test
    fun `the repo list is a bare route with no arguments`() {
        assertFalse(Destinations.REPO_LIST.contains("{"))
    }
}
