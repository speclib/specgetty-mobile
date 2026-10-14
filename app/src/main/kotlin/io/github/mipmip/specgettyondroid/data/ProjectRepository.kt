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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /**
     * A refresh runs here rather than in the scope of whichever screen asked
     * for it. A reader who asks for a fetch and then taps back has not
     * withdrawn the request, and this scope outlives every screen.
     */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher),
) {

    private val _states = MutableStateFlow<Map<String, ProjectState>>(emptyMap())
    val states: Flow<Map<String, ProjectState>> = _states.asStateFlow()

    private val caches = mutableMapOf<String, SpecCache>()

    /**
     * A refresh belongs to the working copy it fetches, not to the entry that
     * asked. Several entries share one, so one fetch serves all of them and a
     * second ask while one is in flight has nothing to start.
     */
    private val jobs = mutableMapOf<String, Job>()
    private val jobLock = Mutex()

    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())

    /** The working copies being fetched right now, by clone id. */
    val refreshing: StateFlow<Set<String>> = _refreshing.asStateFlow()

    private val _outcomes = MutableStateFlow<Map<String, RepoError>>(emptyMap())

    /**
     * Why the last refresh of a working copy failed, held rather than emitted.
     * A refresh outlives the screen that asked for it, so the screen in front
     * when it lands may not be the one that asked, and an event would be lost
     * between the two.
     */
    val outcomes: StateFlow<Map<String, RepoError>> = _outcomes.asStateFlow()

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

    /**
     * Ensures a refresh is running for this entry's working copy and waits for
     * it. Waiting is the caller's, the work is not: a caller that goes away
     * stops waiting and the fetch runs on to its end.
     */
    suspend fun refresh(id: String) {
        val config = catalog.current().repos.firstOrNull { it.id == id } ?: return
        jobFor(config).join()
    }

    private suspend fun jobFor(config: RepoConfig): Job = jobLock.withLock {
        val running = jobs[config.cloneId]
        if (running != null && running.isActive) return@withLock running

        _outcomes.update { it - config.cloneId }
        _refreshing.update { it + config.cloneId }
        scope.launch {
            try {
                runRefresh(config)
            } finally {
                _refreshing.update { it - config.cloneId }
            }
        }.also { jobs[config.cloneId] = it }
    }

    private suspend fun runRefresh(config: RepoConfig) {
        val clone = config.cloneId
        val entries = entriesOn(clone)

        // Loading means there is nothing to show. An entry already holding an
        // index keeps it, so that a screen is never asked to unshow a project
        // it is showing for the length of a fetch.
        entries.forEach { entry ->
            if (stateOf(entry.id) !is ProjectState.Loaded) set(entry.id, ProjectState.Loading)
        }

        val held = tokens?.current(clone)
        val token: String? = if (held == null) {
            catalog.tokenFor(config.id)
        } else {
            when (val problem = held.problem) {
                is TokenProblem.NeedsAuthorizing -> {
                    failed(
                        clone,
                        entries,
                        RepoError.Authentication(
                            "Authorize this repository with GitHub again: ${problem.reason}.",
                        ),
                    )
                    return
                }

                is TokenProblem.Unreachable -> {
                    failed(
                        clone,
                        entries,
                        RepoError.Network(
                            "GitHub could not be reached to renew the credential: " +
                                "${problem.reason}.",
                        ),
                    )
                    return
                }

                null -> held.token
            }
        }

        val refreshed = withContext(dispatcher) {
            if (store.isCloned(clone)) {
                store.refresh(clone, token)
            } else {
                store.clone(clone, config.url, token)
            }
        }
        when (refreshed) {
            is RepoResult.Failure -> failed(clone, entries, refreshed.error)
            is RepoResult.Success -> {
                // The working copy may have been rewritten, so nothing parsed
                // from the old content may be kept. Every entry on this clone
                // is reading that same copy, so every one of them reloads.
                entries.forEach { entry ->
                    caches.remove(entry.id)
                    loadInto(entry.id)
                }
            }
        }
    }

    /**
     * Fetching and resetting leaves the working copy untouched when the fetch
     * is what failed, so an entry that holds an index keeps it and the reason
     * is reported separately. An entry with nothing to show says why.
     */
    private fun failed(clone: String, entries: List<RepoConfig>, error: RepoError) {
        _outcomes.update { it + (clone to error) }
        entries.forEach { entry ->
            if (stateOf(entry.id) !is ProjectState.Loaded) {
                set(entry.id, ProjectState.Failed(error))
            }
        }
    }

    /** Whether this entry's working copy is being fetched. */
    fun refreshingOf(id: String): Flow<Boolean> =
        combine(catalog.repos, _refreshing) { list, clones ->
            val clone = list.repos.firstOrNull { it.id == id }?.cloneId
            clone != null && clone in clones
        }

    /** Why the last refresh of this entry's working copy failed, or null. */
    fun outcomeOf(id: String): Flow<RepoError?> =
        combine(catalog.repos, _outcomes) { list, errors ->
            list.repos.firstOrNull { it.id == id }?.cloneId?.let { errors[it] }
        }

    suspend fun clearOutcome(id: String) {
        val clone = catalog.current().repos.firstOrNull { it.id == id }?.cloneId ?: return
        clearOutcomeOf(clone)
    }

    fun clearOutcomeOf(cloneId: String) {
        _outcomes.update { it - cloneId }
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
