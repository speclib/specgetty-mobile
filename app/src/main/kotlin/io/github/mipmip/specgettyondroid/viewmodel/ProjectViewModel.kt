package io.github.mipmip.specgettyondroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.index.ChangeMatch
import io.github.mipmip.specgettyondroid.index.MatchKind
import io.github.mipmip.specgettyondroid.index.ProjectIndex
import io.github.mipmip.specgettyondroid.index.Query
import io.github.mipmip.specgettyondroid.index.SearchResult
import io.github.mipmip.specgettyondroid.repo.RepoError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProjectTab(val label: String) {
    OVERVIEW("Overview"),
    CHANGES("Changes"),
    SPECS("Specs"),
    PROPERTIES("Properties"),
}

/** What the Properties tab has to show, decided once rather than in the layout. */
sealed interface Properties {
    data class Description(val markdown: String) : Properties

    data class Configuration(val yaml: String, val fileName: String) : Properties

    data object None : Properties
}

data class ChangesTabState(
    val query: String = "",
    val active: List<ChangeMatch> = emptyList(),
    val archived: List<ChangeMatch> = emptyList(),
) {
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()

    val isFiltered: Boolean get() = !Query.parse(query).isEmpty
}

class ProjectViewModel(
    private val repository: ProjectRepository,
    private val repoId: String,
    injectedScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = injectedScope ?: viewModelScope

    private val _tab = MutableStateFlow(ProjectTab.OVERVIEW)
    val tab: StateFlow<ProjectTab> = _tab.asStateFlow()

    private val _changes = MutableStateFlow(ChangesTabState())
    val changes: StateFlow<ChangesTabState> = _changes.asStateFlow()

    private val _state = MutableStateFlow<ProjectState>(ProjectState.Absent)
    val state: StateFlow<ProjectState> = _state.asStateFlow()

    val index: ProjectIndex? get() = (_state.value as? ProjectState.Loaded)?.index

    /** The reason there is nothing to show, or null when there is. */
    val message: String?
        get() = when (val s = _state.value) {
            is ProjectState.Loaded -> null
            is ProjectState.Loading -> "Loading"
            is ProjectState.Absent -> "Not loaded yet"
            is ProjectState.NoProject -> "No OpenSpec project here"
            is ProjectState.Failed -> when (val e = s.error) {
                is RepoError.Authentication -> "Authentication failed. Check the access token."
                is RepoError.Network -> "The repository could not be reached."
                is RepoError.NoOpenSpecProject -> "No OpenSpec project here"
                is RepoError.NoAccessToRepository ->
                    "The authorization does not cover this repository. " +
                        "Choose it on GitHub, or use an access token."

                is RepoError.PointsElsewhere -> e.message

                is RepoError.Unknown -> e.message
            }
        }

    init {
        scope.launch {
            repository.states.collect { states ->
                _state.value = states[repoId] ?: ProjectState.Absent
                refreshChanges()
            }
        }
    }

    fun selectTab(tab: ProjectTab) {
        _tab.value = tab
    }

    fun onQueryChanged(query: String) {
        _changes.value = _changes.value.copy(query = query)
        refreshChanges()
    }

    /**
     * Rewrites the query to carry the chosen matcher's sigil. The grammar is the
     * contract with specgetty; only the way of typing it differs on a phone.
     */
    fun selectMatcher(kind: MatchKind) {
        val bare = _changes.value.query.removePrefix("'").removePrefix(":")
        onQueryChanged(
            when (kind) {
                MatchKind.FUZZY_NAME -> bare
                MatchKind.LITERAL_NAME -> "'$bare"
                MatchKind.BODY -> ":$bare"
            },
        )
    }

    val matcher: MatchKind get() = Query.parse(_changes.value.query).kind

    fun clearQuery() = onQueryChanged("")

    private fun refreshChanges() {
        val idx = index
        if (idx == null) {
            _changes.value = _changes.value.copy(active = emptyList(), archived = emptyList())
            return
        }
        val result: SearchResult = idx.search(_changes.value.query)
        _changes.value = _changes.value.copy(active = result.active, archived = result.archived)
    }

    val properties: Properties
        get() {
            val info = index?.project ?: return Properties.None
            info.projectFile?.let { file ->
                runCatching { file.readText() }.getOrNull()?.let {
                    return Properties.Description(it)
                }
            }
            info.configFile?.let { file ->
                runCatching { file.readText() }.getOrNull()?.let {
                    return Properties.Configuration(it, file.name)
                }
            }
            return Properties.None
        }

    val schemasInUse: List<String> get() = index?.project?.schemasInUse.orEmpty()

    companion object {
        fun factory(repository: ProjectRepository, repoId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProjectViewModel(repository, repoId) as T
            }
    }
}
