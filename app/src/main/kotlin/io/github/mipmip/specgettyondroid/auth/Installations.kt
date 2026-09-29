package io.github.mipmip.specgettyondroid.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** What a credential actually reaches, which is not what authorizing granted. */
data class Reach(
    /** `owner/name` for each repository, in the order GitHub listed them. */
    val repositories: List<String>,
    /**
     * Accounts every one of whose repositories is covered.
     *
     * GitHub calls this `repository_selection: all`, and "all" is scoped to the
     * account the app was installed on. Reading it as "everything anywhere" is
     * what let an installation on a personal account claim to cover an
     * organization's repository, which then failed at the clone.
     */
    val wholeAccounts: List<String> = emptyList(),
) {
    val reachesNothing: Boolean get() = repositories.isEmpty() && wholeAccounts.isEmpty()

    /** Whether any installation covers a whole account rather than a selection. */
    val coversEverything: Boolean get() = wholeAccounts.isNotEmpty()

    /**
     * Whether a clone URL is covered. Compared on `owner/name` taken from the
     * URL, so the `.git` suffix and the host do not matter.
     */
    fun covers(cloneUrl: String): Boolean {
        val path = ownerAndName(cloneUrl) ?: return false
        val owner = path.substringBefore('/')
        if (wholeAccounts.any { it.equals(owner, ignoreCase = true) }) return true
        return repositories.any { it.equals(path, ignoreCase = true) }
    }

    companion object {
        fun ownerAndName(cloneUrl: String): String? {
            val afterHost = cloneUrl.substringAfter("://", "")
                .substringAfter('/', "")
                .substringBefore('?')
                .substringBefore('#')
                .removeSuffix("/")
                .removeSuffix(".git")
            val parts = afterHost.split('/').filter { it.isNotEmpty() }
            return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else null
        }
    }
}

/**
 * Authorizing and installing are two things. The device flow yields a valid
 * credential that reaches nothing until the app is also installed on some
 * repositories, and saying nothing about that is the worst possible answer:
 * the person believes they are done and the clone fails later for a reason
 * that looks like a broken token.
 */
class Installations(private val transport: AuthTransport) {

    suspend fun reachOf(token: String): Result<Reach> = runCatching {
        val body = transport.get(INSTALLATIONS_URL, token)
        val root = Json.parseToJsonElement(body).jsonObject
        val installations = root["installations"]?.jsonArray.orEmpty()

        val repositories = mutableListOf<String>()
        val wholeAccounts = mutableListOf<String>()

        // Every installation, not just the first. One person commonly has the
        // app on their own account and on an organization, and reading one of
        // them hides half of what the credential reaches.
        installations.forEach { element ->
            val installation = element.jsonObject
            val account = installation["account"]?.jsonObject
                ?.get("login")?.jsonPrimitive?.content

            if (installation["repository_selection"]?.jsonPrimitive?.content == "all") {
                account?.let { wholeAccounts += it }
            }

            val id = installation["id"]?.jsonPrimitive?.content ?: return@forEach
            repositories += runCatching {
                val reposBody = transport.get("$INSTALLATIONS_URL/$id/repositories", token)
                Json.parseToJsonElement(reposBody).jsonObject["repositories"]?.jsonArray.orEmpty()
                    .mapNotNull { it.jsonObject["full_name"]?.jsonPrimitive?.content }
            }.getOrDefault(emptyList())
        }

        Reach(repositories.distinct(), wholeAccounts.distinct())
    }

    private companion object {
        const val INSTALLATIONS_URL = "https://api.github.com/user/installations"
    }
}
