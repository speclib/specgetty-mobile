package io.github.mipmip.specgettyondroid.data

import io.github.mipmip.specgettyondroid.auth.TokenProblem
import io.github.mipmip.specgettyondroid.auth.TokenSource
import io.github.mipmip.specgettyondroid.index.ProjectIndex
import io.github.mipmip.specgettyondroid.project.ProjectLoader
import io.github.mipmip.specgettyondroid.repo.DiscoveredProject
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoResult
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.store.RepoCatalog
import io.github.mipmip.specgettyondroid.store.RepoConfig
import io.github.mipmip.specgettyondroid.store.RepoList
import io.github.mipmip.specgettyondroid.store.repoIdFor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * What the app is showing, and its transitions. Owns the clone, the load and
 * the index; the screens read the state it publishes and never reach past it.
 */
class ProjectRepository(
    private val store: RepoStore,
    private val catalog: RepoCatalog,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val tokens: TokenSource? = null,
) {

    private val _states = MutableStateFlow<Map<String, ProjectState>>(emptyMap())
    val states: Flow<Map<String, ProjectState>> = _states.asStateFlow()

    private val caches = mutableMapOf<String, SpecCache>()

    val repos: Flow<RepoList> get() = catalog.repos

    suspend fun current(): RepoList = catalog.current()

    fun stateOf(id: String): ProjectState = _states.value[id] ?: ProjectState.Absent

    fun cacheOf(id: String): SpecCache = caches.getOrPut(id) { SpecCache() }

    /**
     * Clones the repository if it is not already on disk, and reports every
     * project it holds. Nothing is registered: what a repository contains is
     * only knowable once it is here, and the choice that follows may add none
     * of them.
     */
    suspend fun survey(url: String, token: String? = null): RepoResult<List<DiscoveredProject>> {
        val cloneId = repoIdFor(url.trim())
        if (!store.isCloned(cloneId)) {
            val cloned = withContext(dispatcher) { store.clone(cloneId, url.trim(), token) }
            if (cloned is RepoResult.Failure) return cloned
        }
        return RepoResult.Success(withContext(dispatcher) { store.discover(cloneId) })
    }

    /** Registers the repository, clones it, then loads its project. */
    suspend fun add(
        url: String,
        label: String = "",
        token: String? = null,
        path: String = "",
    ): RepoConfig {
        val config = catalog.add(url, label, token, path)
        return cloneAndLoad(config, token)
    }

    /**
     * The same as [add], for a credential obtained by authorizing rather than
     * typed. The vault holds the expiry and the renewal material; the clone is
     * given the bare token, which is all git understands.
     */
    suspend fun addAuthorized(
        url: String,
        label: String = "",
        credential: Credential.GitHub,
        path: String = "",
    ): RepoConfig {
        val config = catalog.add(url, label, Credential.encode(credential), path)
        return cloneAndLoad(config, credential.token)
    }

    /**
     * Reuses a working copy already on disk. Several entries share one, and the
     * second of them has nothing to fetch.
     */
    private suspend fun cloneAndLoad(config: RepoConfig, token: String?): RepoConfig {
        set(config.id, ProjectState.Loading)

        if (store.isCloned(config.cloneId)) {
            loadInto(config.id)
            return config
        }

        val cloned = withContext(dispatcher) {
            store.clone(config.cloneId, config.url, token)
        }
        when (cloned) {
            is RepoResult.Failure -> set(config.id, ProjectState.Failed(cloned.error))
            is RepoResult.Success -> loadInto(config.id)
        }
        return config
    }

    suspend fun refresh(id: String) {
        val config = catalog.current().repos.firstOrNull { it.id == id } ?: return
        set(id, ProjectState.Loading)

        val token = when (val held = tokens?.current(config.cloneId)) {
            null -> catalog.tokenFor(id)
            else -> when (val problem = held.problem) {
                is TokenProblem.NeedsAuthorizing -> {
                    set(
                        id,
                        ProjectState.Failed(
                            RepoError.Authentication(
                                "Authorize this repository with GitHub again: ${problem.reason}.",
                            ),
                        ),
                    )
                    return
                }

                is TokenProblem.Unreachable -> {
                    set(
                        id,
                        ProjectState.Failed(
                            RepoError.Network(
                                "GitHub could not be reached to renew the credential: " +
                                    "${problem.reason}.",
                            ),
                        ),
                    )
                    return
                }

                null -> held.token
            }
        }
        val clone = config.cloneId
        val refreshed = withContext(dispatcher) {
            if (store.isCloned(clone)) {
                store.refresh(clone, token)
            } else {
                store.clone(clone, config.url, token)
            }
        }
        when (refreshed) {
            is RepoResult.Failure -> set(id, ProjectState.Failed(refreshed.error))
            is RepoResult.Success -> {
                // The working copy may have been rewritten, so nothing parsed
                // from the old content may be kept. Every entry on this clone
                // is reading that same copy, so every one of them reloads.
                entriesOn(clone).forEach { entry ->
                    caches.remove(entry.id)
                    if (entry.id != id) set(entry.id, ProjectState.Loading)
                    loadInto(entry.id)
                }
            }
        }
    }

    private suspend fun entriesOn(cloneId: String): List<RepoConfig> =
        catalog.current().repos.filter { it.cloneId == cloneId }

    /**
     * The working copy outlives the entry that made it, until the last entry
     * using it goes. Deleting it sooner would break the siblings still reading
     * from it.
     */
    suspend fun remove(id: String) {
        val list = catalog.current()
        val going = list.repos.firstOrNull { it.id == id }
        if (going != null && !list.othersShare(id)) store.delete(going.cloneId)
        catalog.remove(id)
        caches.remove(id)
        _states.update { it - id }
    }

    suspend fun activate(id: String) {
        catalog.activate(id)
        if (stateOf(id) is ProjectState.Absent) load(id)
    }

    /** Loads from the working copy already on disk, cloning if there is none. */
    suspend fun load(id: String) {
        val config = catalog.current().repos.firstOrNull { it.id == id } ?: return
        if (!store.isCloned(config.cloneId)) {
            refresh(id)
            return
        }
        set(id, ProjectState.Loading)
        loadInto(id)
    }

    private suspend fun loadInto(id: String) {
        val config = catalog.current().repos.firstOrNull { it.id == id } ?: return
        val loaded = withContext(dispatcher) {
            ProjectLoader.load(store.workingDir(config.cloneId), config.path)
        }
        set(
            id,
            when (loaded) {
                is RepoResult.Success -> ProjectState.Loaded(ProjectIndex(loaded.value))
                is RepoResult.Failure -> when (loaded.error) {
                    is RepoError.NoOpenSpecProject -> ProjectState.NoProject
                    else -> ProjectState.Failed(loaded.error)
                }
            },
        )
    }

    private fun set(id: String, state: ProjectState) {
        _states.update { it + (id to state) }
    }
}
