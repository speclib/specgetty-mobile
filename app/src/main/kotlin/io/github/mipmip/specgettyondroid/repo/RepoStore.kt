package io.github.mipmip.specgettyondroid.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.net.UnknownHostException

class RepoStore(
    private val rootDir: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    fun workingDir(id: String): File = File(rootDir, id)

    fun isCloned(id: String): Boolean = File(workingDir(id), Constants.DOT_GIT).exists()

    suspend fun clone(id: String, url: String, token: String? = null): RepoResult<File> =
        withContext(dispatcher) {
            val target = workingDir(id)
            target.deleteRecursively()
            try {
                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(target)
                    .setDepth(1)
                    .setCloneAllBranches(false)
                    .apply { credentials(token)?.let { setCredentialsProvider(it) } }
                    .call()
                    .use { }
                RepoResult.Success(target)
            } catch (e: Exception) {
                target.deleteRecursively()
                RepoResult.Failure(classify(e, token))
            }
        }

    suspend fun refresh(id: String, token: String? = null): RepoResult<File> =
        withContext(dispatcher) {
            val target = workingDir(id)
            if (!isCloned(id)) {
                return@withContext RepoResult.Failure(
                    RepoError.Unknown("repository $id has not been cloned"),
                )
            }
            try {
                Git.open(target).use { git ->
                    val branch = git.repository.branch
                    git.fetch()
                        .setRemote(Constants.DEFAULT_REMOTE_NAME)
                        .setDepth(1)
                        .setRemoveDeletedRefs(true)
                        .apply { credentials(token)?.let { setCredentialsProvider(it) } }
                        .call()

                    val remoteRef = remoteTip(git.repository, branch)
                        ?: return@withContext RepoResult.Failure(
                            RepoError.Unknown("remote branch for $branch not found after fetch"),
                        )

                    git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef(remoteRef)
                        .call()
                }
                RepoResult.Success(target)
            } catch (e: Exception) {
                RepoResult.Failure(classify(e, token))
            }
        }

    fun delete(id: String): Boolean {
        val target = workingDir(id)
        if (!target.exists()) return false
        return target.deleteRecursively()
    }

    fun projectDir(id: String, path: String = ""): RepoResult<File> =
        OpenSpecLayout.projectDir(workingDir(id), path)

    /** Every project the working copy holds, for choosing between them. */
    fun discover(id: String): List<DiscoveredProject> =
        OpenSpecLayout.discover(workingDir(id))

    private fun remoteTip(repository: Repository, branch: String): String? {
        val candidates = listOf(
            "${Constants.R_REMOTES}${Constants.DEFAULT_REMOTE_NAME}/$branch",
            "${Constants.R_REMOTES}${Constants.DEFAULT_REMOTE_NAME}/${Constants.HEAD}",
        )
        return candidates.firstOrNull { repository.resolve(it) != null }
    }

    private fun credentials(token: String?): UsernamePasswordCredentialsProvider? =
        token?.takeIf { it.isNotBlank() }
            ?.let { UsernamePasswordCredentialsProvider(it, "") }

    /** Internal so the wording a real host sends can be pinned by a test. */
    internal fun classify(e: Exception, token: String? = null): RepoError {
        val text = buildString {
            var cause: Throwable? = e
            while (cause != null) {
                append(cause.message ?: cause::class.java.simpleName)
                append(' ')
                cause = cause.cause?.takeIf { it !== cause }
            }
        }.lowercase()

        return when {
            // Before the authentication markers, which include 403: this is a
            // 403 too, and the credential is not what is wrong.
            NO_ACCESS_MARKERS.any { it in text } -> RepoError.NoAccessToRepository(
                "the credential does not reach this repository",
            )

            AUTH_MARKERS.any { it in text } -> RepoError.Authentication(authMessage(text))

            // GitHub answers 404 rather than 403 for a private repository a
            // credential cannot see, so as not to reveal that it exists. With a
            // credential in hand the host was plainly reached, and what is
            // missing is access rather than a connection.
            hasMissingRemote(e) && !token.isNullOrBlank() ->
                RepoError.NoAccessToRepository(
                    "the credential does not reach this repository",
                )

            hasUnknownHost(e) || NETWORK_MARKERS.any { it in text } ->
                RepoError.Network(e.message ?: "the repository could not be reached")

            e is TransportException -> RepoError.Network(
                e.message ?: "the repository could not be reached",
            )

            else -> RepoError.Unknown(e.message ?: e::class.java.simpleName)
        }
    }

    private fun authMessage(text: String): String = when {
        "not authorized" in text || "401" in text ->
            "authentication failed: the token was rejected or is missing"

        else -> "authentication failed"
    }

    /**
     * Matched on the type rather than the wording. The recorded message carries
     * the URL, and an ephemeral port such as `127.0.0.1:40412` contains the
     * digits a status-code match would have tripped over.
     */
    private fun hasMissingRemote(e: Throwable): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is NoRemoteRepositoryException) return true
            cause = cause.cause?.takeIf { it !== cause }
        }
        return false
    }

    private fun hasUnknownHost(e: Throwable): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is UnknownHostException) return true
            cause = cause.cause?.takeIf { it !== cause }
        }
        return false
    }

    private companion object {
        /**
         * What GitHub says when a token is valid but carries no grant on the
         * repository. The wording is the host's: it mentions write access even
         * for a read, because that is the message it sends when an app token
         * has no applicable permission. Recorded from a real refusal rather
         * than guessed at.
         */
        val NO_ACCESS_MARKERS = listOf(
            "write access to repository not granted",
            "resource not accessible by integration",
        )
        val AUTH_MARKERS = listOf(
            "not authorized",
            "authentication is required",
            "authentication failed",
            "401",
            "403",
            "invalid credentials",
        )
        val NETWORK_MARKERS = listOf(
            "unknownhost",
            "unknown host",
            "connection refused",
            "network is unreachable",
            "timed out",
            "no route to host",
            "cannot open git-upload-pack",
        )

    }
}
