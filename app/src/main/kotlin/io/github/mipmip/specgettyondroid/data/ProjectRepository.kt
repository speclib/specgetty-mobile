package io.github.mipmip.specgettyondroid.data

import io.github.mipmip.specgettyondroid.index.ProjectIndex
import io.github.mipmip.specgettyondroid.project.ProjectLoader
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoResult
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.RepoCatalog
import io.github.mipmip.specgettyondroid.store.RepoConfig
import io.github.mipmip.specgettyondroid.store.RepoList
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
) {

    private val _states = MutableStateFlow<Map<String, ProjectState>>(emptyMap())
    val states: Flow<Map<String, ProjectState>> = _states.asStateFlow()

    private val caches = mutableMapOf<String, SpecCache>()

    val repos: Flow<RepoList> get() = catalog.repos

    suspend fun current(): RepoList = catalog.current()

    fun stateOf(id: String): ProjectState = _states.value[id] ?: ProjectState.Absent

    fun cacheOf(id: String): SpecCache = caches.getOrPut(id) { SpecCache() }

    /** Registers the repository, clones it, then loads its project. */
    suspend fun add(url: String, label: String = "", token: String? = null): RepoConfig {
        val config = catalog.add(url, label, token)
        set(config.id, ProjectState.Loading)

        val cloned = withContext(dispatcher) { store.clone(config.id, config.url, token) }
        when (cloned) {
            is RepoResult.Failure -> set(config.id, ProjectState.Failed(cloned.error))
            is RepoResult.Success -> loadInto(config.id)
        }
        return config
    }

    suspend fun refresh(id: String) {
        val config = catalog.current().repos.firstOrNull { it.id == id } ?: return
        set(id, ProjectState.Loading)

        val token = catalog.tokenFor(id)
        val refreshed = withContext(dispatcher) {
            if (store.isCloned(id)) store.refresh(id, token) else store.clone(id, config.url, token)
        }
        when (refreshed) {
            is RepoResult.Failure -> set(id, ProjectState.Failed(refreshed.error))
            is RepoResult.Success -> {
                // The working copy may have been rewritten, so nothing parsed
                // from the old content may be kept.
                caches.remove(id)
                loadInto(id)
            }
        }
    }

    suspend fun remove(id: String) {
        store.delete(id)
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
        if (catalog.current().repos.none { it.id == id }) return
        if (!store.isCloned(id)) {
            refresh(id)
            return
        }
        set(id, ProjectState.Loading)
        loadInto(id)
    }

    private suspend fun loadInto(id: String) {
        val loaded = withContext(dispatcher) { ProjectLoader.load(store.workingDir(id)) }
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
