package io.github.mipmip.specgettyondroid.auth

/**
 * Which URLs this app can authorize for.
 *
 * Only github.com. A GitHub App is registered on one host, so an Enterprise
 * installation is a different app with a different client id, and every other
 * forge has its own flow entirely. Offering the button anywhere else would
 * promise something the app cannot deliver.
 */
object GitHubHost {

    fun recognises(url: String): Boolean {
        val trimmed = url.trim()
        val host = runCatching { java.net.URI(trimmed).host }.getOrNull()?.lowercase()
            ?: return false
        if (host != "github.com" && host != "www.github.com") return false
        return Reach.ownerAndName(trimmed) != null
    }
}
