package io.github.mipmip.specgettyondroid.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bodies are the shapes the spike saw, including the one that cost two
 * round trips to understand: a valid credential with `total_count: 0`, which
 * reaches nothing at all.
 */
private class GetTransport(private val byUrl: Map<String, String>) : AuthTransport {

    val asked = mutableListOf<String>()

    override suspend fun post(url: String, form: Map<String, String>) =
        error("this reads by GET only")

    override suspend fun get(url: String, bearer: String): String {
        asked += url
        return byUrl[url]
            ?: byUrl.entries.filter { url.startsWith(it.key) }.maxByOrNull { it.key.length }?.value
            ?: error("nothing recorded for $url")
    }
}

class InstallationsTest {

    private val root = "https://api.github.com/user/installations"

    @Test
    fun `an authorization that reaches nothing is recognised`() = runTest {
        val transport = GetTransport(mapOf(root to """{"total_count":0,"installations":[]}"""))
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()

        assertTrue(reach.reachesNothing)
        assertTrue(reach.repositories.isEmpty())
        assertFalse(reach.coversEverything)
    }

    @Test
    fun `an installation on selected repositories lists them`() = runTest {
        val transport = GetTransport(
            mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":42,"app_slug":"specgetty","repository_selection":"selected"}]}""",
                "$root/42/repositories" to """{"total_count":2,"repositories":[
                    {"full_name":"mipmip/test","private":true},
                    {"full_name":"speclib/specgetty","private":false}]}""",
            ),
        )
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()

        assertEquals(listOf("mipmip/test", "speclib/specgetty"), reach.repositories)
        assertFalse(reach.coversEverything)
        assertFalse(reach.reachesNothing)
    }

    @Test
    fun `an installation on everything says so`() = runTest {
        val transport = GetTransport(
            mapOf(
                root to """{"total_count":1,"installations":[
                    {"id":42,"app_slug":"specgetty","account":{"login":"mipmip"},"repository_selection":"all"}]}""",
                "$root/42/repositories" to """{"total_count":0,"repositories":[]}""",
            ),
        )
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()

        assertTrue(reach.coversEverything)
        assertFalse(reach.reachesNothing)
    }

    @Test
    fun `a selected installation covers only what was selected`() {
        val reach = Reach(listOf("mipmip/test"))

        assertTrue(reach.covers("https://github.com/mipmip/test.git"))
        assertTrue(reach.covers("https://github.com/mipmip/test"))
        assertFalse(reach.covers("https://github.com/mipmip/secondbrain.git"))
        assertFalse(reach.covers("https://github.com/someone/test.git"))
    }

    /**
     * Not any repository: every repository of the account it was installed on.
     * Reading it as "anywhere" is what let an installation on one account claim
     * an organization's repository and then fail at the clone.
     */
    @Test
    fun `an installation on everything covers that account and no other`() {
        val reach = Reach(emptyList(), wholeAccounts = listOf("anyone"))
        assertTrue(reach.covers("https://github.com/anyone/anything.git"))
        assertFalse(reach.covers("https://github.com/someone-else/anything.git"))
    }

    @Test
    fun `an authorization reaching nothing covers nothing`() {
        val reach = Reach(emptyList())
        assertFalse(reach.covers("https://github.com/mipmip/test.git"))
    }

    @Test
    fun `the owner and name are taken from the URL whatever its shape`() {
        listOf(
            "https://github.com/mipmip/test.git",
            "https://github.com/mipmip/test",
            "https://github.com/mipmip/test/",
            "http://github.com/mipmip/test.git",
            "https://github.com/mipmip/test/tree/main",
        ).forEach { url ->
            assertEquals(url, "mipmip/test", Reach.ownerAndName(url))
        }
    }

    @Test
    fun `a URL with no owner and name yields nothing`() {
        assertEquals(null, Reach.ownerAndName("https://github.com"))
        assertEquals(null, Reach.ownerAndName("https://github.com/mipmip"))
    }

    @Test
    fun `matching a repository ignores case, as GitHub does`() {
        val reach = Reach(listOf("MipMip/Test"))
        assertTrue(reach.covers("https://github.com/mipmip/test.git"))
    }

    @Test
    fun `a listing that fails still reports what the installation selection was`() = runTest {
        val transport = object : AuthTransport {
            override suspend fun post(url: String, form: Map<String, String>) = error("no")
            override suspend fun get(url: String, bearer: String): String =
                if (url.endsWith("/repositories")) {
                    throw java.io.IOException("the listing went away")
                } else {
                    """{"total_count":1,"installations":[
                        {"id":42,"app_slug":"s","account":{"login":"mipmip"},"repository_selection":"all"}]}"""
                }
        }
        val reach = Installations(transport).reachOf("ghu_abc").getOrThrow()
        assertTrue(reach.coversEverything)
    }

    @Test
    fun `a transport that fails is a failure, not an exception thrown at the caller`() = runTest {
        val transport = object : AuthTransport {
            override suspend fun post(url: String, form: Map<String, String>) = error("no")
            override suspend fun get(url: String, bearer: String): String =
                throw java.io.IOException("the network went away")
        }
        assertTrue(Installations(transport).reachOf("ghu_abc").isFailure)
    }
}
