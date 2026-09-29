package io.github.mipmip.specgettyondroid.store

import kotlinx.serialization.Serializable

@Serializable
data class RepoConfig(
    val id: String,
    val url: String,
    val label: String,
    val hasToken: Boolean = false,
    /**
     * Where the project sits inside the repository, empty for the root. Absent
     * from a list written before entries carried one, which decodes to the
     * empty string and so to the behaviour that list already had.
     */
    val path: String = "",
) {
    /** The working copy and the credential, shared by every entry on this URL. */
    val cloneId: String get() = repoIdFor(url)
}

@Serializable
data class RepoList(
    val repos: List<RepoConfig> = emptyList(),
    val activeId: String? = null,
) {
    val active: RepoConfig? get() = repos.firstOrNull { it.id == activeId }

    /**
     * One entry per URL and path together. Two projects in one repository are
     * two entries; the same project added twice is an update to the one entry.
     */
    fun add(repo: RepoConfig): RepoList {
        val existing = repos.firstOrNull {
            it.url == repo.url && normalisePath(it.path) == normalisePath(repo.path)
        }
        if (existing != null) {
            return copy(
                repos = repos.map {
                    if (it.id == existing.id) {
                        it.copy(label = repo.label, hasToken = repo.hasToken)
                    } else {
                        it
                    }
                },
                activeId = activeId ?: existing.id,
            )
        }
        return copy(repos = repos + repo, activeId = activeId ?: repo.id)
    }

    /** Whether any entry other than [id] still needs that URL's working copy. */
    fun othersShare(id: String): Boolean {
        val going = repos.firstOrNull { it.id == id } ?: return false
        return repos.any { it.id != id && it.url == going.url }
    }

    fun remove(id: String): RepoList {
        val remaining = repos.filterNot { it.id == id }
        val nextActive = when {
            activeId != id -> activeId
            else -> remaining.firstOrNull()?.id
        }
        return RepoList(remaining, nextActive)
    }

    fun activate(id: String): RepoList =
        if (repos.any { it.id == id }) copy(activeId = id) else this

    fun withToken(id: String, hasToken: Boolean): RepoList =
        copy(repos = repos.map { if (it.id == id) it.copy(hasToken = hasToken) else it })
}

/**
 * The id names the directory a clone lands in, so it has to be stable across
 * launches and safe as a file name. Base 36 of a hash of the URL gives both.
 *
 * Derived from the URL alone. Several list entries on one repository share this
 * one working copy and this one credential.
 */
fun repoIdFor(url: String): String = hashToId(url.trim().lowercase())

/**
 * The id of one list entry, which is a repository together with a path inside
 * it. The empty path gives exactly [repoIdFor], so a list written before
 * entries carried a path keeps every id it had, and with it the working copy
 * already on the device.
 *
 * NUL separates the two because it is the one byte a path cannot hold, so
 * `a` with `b/c` cannot collide with `a/b` with `c`.
 */
fun entryIdFor(url: String, path: String): String {
    val cleaned = normalisePath(path)
    if (cleaned.isEmpty()) return repoIdFor(url)
    return hashToId(url.trim().lowercase() + '\u0000' + cleaned)
}

/** Trimmed, with the separators a path may pick up at either end removed. */
fun normalisePath(path: String): String = path.trim().trim('/')

private fun hashToId(value: String): String =
    value.fold(0L) { acc, c -> acc * 31 + c.code }
        .toULong()
        .toString(36)
        .padStart(8, '0')

fun labelFor(url: String): String = url
    .trim()
    .removeSuffix("/")
    .removeSuffix(".git")
    .substringAfterLast('/')
    .ifBlank { url }
