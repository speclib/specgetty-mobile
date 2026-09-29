package io.github.mipmip.specgettyondroid.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class Recorded(private val byUrl: Map<String, String>) : AuthTransport {

    override suspend fun post(url: String, form: Map<String, String>) = error("reads only")

    override suspend fun get(url: String, bearer: String): String =
        byUrl.entries.filter { url.startsWith(it.key) }.maxByOrNull { it.key.length }?.value
            ?: error("nothing recorded for $url")
}

/**
 * The failure a Fairphone found: authorizing succeeded, the screen said the app
 * could read everything, and the clone then failed. GitHub's
 * `repository_selection: all` is scoped to the account the app was installed
 * on, and reading it as "everything anywhere" let an installation on a personal
 * account claim an organization's repository.
 */
class AccountScopedReachTest {

    private val root = "https://api.github.com/user/installations"

    @Test
    fun `all repositories on one account does not cover another account`() {
        val reach = Reach(repositories = emptyList(), wholeAccounts = listOf("mipmip"))

        assertTrue(reach.covers("https://github.com/mipmip/anything.git"))
        assertFalse(reach.covers("https://github.com/speclib/specgetty-mobile.git"))
    }

    @Test
    fun `the account is matched the way GitHub matches it, ignoring case`() {
        val reach = Reach(emptyList(), wholeAccounts = listOf("MipMip"))
        assertTrue(reach.covers("https://github.com/mipmip/anything.git"))
    }

    @Test
    fun `a selected repository is still covered alongside a whole account`() {
        val reach = Reach(
            repositories = listOf("speclib/specgetty-mobile"),
            wholeAccounts = listOf("mipmip"),
        )
        assertTrue(reach.covers("https://github.com/speclib/specgetty-mobile.git"))
        assertTrue(reach.covers("https://github.com/mipmip/whatever.git"))
        assertFalse(reach.covers("https://github.com/speclib/something-else.git"))
    }

    @Test
    fun `an installation on a whole account reads back scoped to that account`() = runTest {
        val transport = Recorded(
            mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":7,"account":{"login":"mipmip"},"repository_selection":"all"}]}""",
                "$root/7/repositories" to """{"repositories":[]}""",
            ),
        )
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()

        assertEquals(listOf("mipmip"), reach.wholeAccounts)
        assertTrue(reach.coversEverything)
        assertFalse(reach.reachesNothing)
        assertFalse(reach.covers("https://github.com/speclib/specgetty-mobile.git"))
    }

    /**
     * One person commonly has the app on their own account and on an
     * organization. Reading only the first hid half of what was granted.
     */
    @Test
    fun `every installation is read, not just the first`() = runTest {
        val transport = Recorded(
            mapOf(
                root to """{"total_count":2,"installations":[
                    {"id":7,"account":{"login":"mipmip"},"repository_selection":"all"},
                    {"id":9,"account":{"login":"speclib"},"repository_selection":"selected"}]}""",
                "$root/7/repositories" to """{"repositories":[]}""",
                "$root/9/repositories" to
                    """{"repositories":[{"full_name":"speclib/specgetty-mobile"}]}""",
            ),
        )
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()

        assertEquals(listOf("mipmip"), reach.wholeAccounts)
        assertEquals(listOf("speclib/specgetty-mobile"), reach.repositories)
        assertTrue(reach.covers("https://github.com/mipmip/anything.git"))
        assertTrue(reach.covers("https://github.com/speclib/specgetty-mobile.git"))
        assertFalse(reach.covers("https://github.com/speclib/other.git"))
    }

    @Test
    fun `an installation with no account named covers nothing wholesale`() = runTest {
        val transport = Recorded(
            mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":7,"repository_selection":"all"}]}""",
                "$root/7/repositories" to """{"repositories":[]}""",
            ),
        )
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()

        assertTrue(reach.wholeAccounts.isEmpty())
        assertFalse(reach.covers("https://github.com/anyone/anything.git"))
    }

    @Test
    fun `no installations at all still reaches nothing`() = runTest {
        val transport = Recorded(mapOf(root to """{"total_count":0,"installations":[]}"""))
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()

        assertTrue(reach.reachesNothing)
        assertFalse(reach.coversEverything)
    }

    @Test
    fun `the same repository listed by two installations is named once`() = runTest {
        val transport = Recorded(
            mapOf(
                root to """{"total_count":2,"installations":[
                    {"id":7,"account":{"login":"a"},"repository_selection":"selected"},
                    {"id":9,"account":{"login":"b"},"repository_selection":"selected"}]}""",
                "$root/7/repositories" to """{"repositories":[{"full_name":"x/y"}]}""",
                "$root/9/repositories" to """{"repositories":[{"full_name":"x/y"}]}""",
            ),
        )
        assertEquals(listOf("x/y"), Installations(transport).reachOf("ghu_abc").getOrThrow().repositories)
    }
}
