package io.github.mipmip.specgettyondroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mipmip.specgettyondroid.auth.GitHubHost
import io.github.mipmip.specgettyondroid.capture.CaptureResult
import io.github.mipmip.specgettyondroid.capture.UrlCapture
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.index.ProjectCounts
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoResult
import io.github.mipmip.specgettyondroid.repo.explanation
import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.store.RepoConfig
import io.github.mipmip.specgettyondroid.store.RepoUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row of the list: a repository and whatever is known about its project. */
data class RepoRow(
    val config: RepoConfig,
    val state: ProjectState,
    val isActive: Boolean,
    /** Its working copy is being fetched, whichever screen asked for it. */
    val isRefreshing: Boolean = false,
) {
    val counts: ProjectCounts? get() = (state as? ProjectState.Loaded)?.index?.counts

    val isLoading: Boolean get() = state is ProjectState.Loading

    val canOpen: Boolean get() = state is ProjectState.Loaded

    /** The row's own message, or null when there is nothing to explain. */
    val message: String?
        get() = when (state) {
            is ProjectState.Absent -> "Not loaded yet"
            is ProjectState.Loading -> "Loading"
            is ProjectState.NoProject -> "No OpenSpec project here"
            is ProjectState.Failed -> state.error.explanation
            is ProjectState.Loaded -> null
        }
}

data class AddRepoForm(
    val url: String = "",
    val label: String = "",
    val token: String = "",
    val error: String? = null,
    val busy: Boolean = false,
    /**
     * Held after authorizing and before confirming. Never bound to a field: the
     * screen may say one is held, and may not show it.
     */
    val authorized: Credential.GitHub? = null,
) {
    val isBlank: Boolean get() = url.isBlank() && label.isBlank() && token.isBlank()

    /** Whether offering to authorize would lead anywhere for this URL. */
    val canAuthorize: Boolean get() = GitHubHost.recognises(url)

    val hasAuthorized: Boolean get() = authorized != null
}

/**
 * A repository holding more than one project, waiting for the person to say
 * which of them to add. Nothing is in the list while this stands.
 */
data class ProjectChoice(
    val url: String,
    val label: String,
    val token: String?,
    val credential: Credential.GitHub?,
    /** Paths within the repository, in the order the survey found them. */
    val projects: List<String>,
    val selected: Set<String> = emptySet(),
) {
    fun nameOf(path: String): String = path.substringAfterLast('/').ifEmpty { "The repository root" }

    val canConfirm: Boolean get() = selected.isNotEmpty()
}

class RepoListViewModel(
    private val repository: ProjectRepository,
    injectedScope: CoroutineScope? = null,
) : ViewModel() {

    /** Injectable so the whole view model runs on a test dispatcher off device. */
    private val scope: CoroutineScope = injectedScope ?: viewModelScope

    private val _rows = MutableStateFlow<List<RepoRow>>(emptyList())
    val rows: StateFlow<List<RepoRow>> = _rows.asStateFlow()

    private val _form = MutableStateFlow(AddRepoForm())
    val form: StateFlow<AddRepoForm> = _form.asStateFlow()

    private val _formOpen = MutableStateFlow(false)
    val formOpen: StateFlow<Boolean> = _formOpen.asStateFlow()

    /**
     * Held across a pull of the whole list, which is several fetches. The
     * repository's own set empties between them, and the indicator should not
     * blink in the gaps.
     */
    private val _refreshingAll = MutableStateFlow(false)

    val refreshing: StateFlow<Boolean> =
        combine(_refreshingAll, repository.refreshing) { all, clones ->
            all || clones.isNotEmpty()
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /** Why a refresh failed, for the list to tell. Cleared once told. */
    val failure: StateFlow<RefreshFailure?> =
        repository.outcomes.map { errors ->
            errors.entries.firstOrNull()?.let { RefreshFailure(it.key, it.value) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    /** A repository holding several projects, waiting to be chosen from. */
    private val _choice = MutableStateFlow<ProjectChoice?>(null)
    val choice: StateFlow<ProjectChoice?> = _choice.asStateFlow()

    /** A repository whose removal has been asked for but not confirmed. */
    private val _pendingRemoval = MutableStateFlow<String?>(null)
    val pendingRemoval: StateFlow<String?> = _pendingRemoval.asStateFlow()

    init {
        scope.launch {
            combine(
                repository.repos,
                repository.states,
                repository.refreshing,
            ) { list, states, clones ->
                list.repos.map { config ->
                    RepoRow(
                        config = config,
                        state = states[config.id] ?: ProjectState.Absent,
                        isActive = config.id == list.activeId,
                        isRefreshing = config.cloneId in clones,
                    )
                }
            }.collect { rows -> _rows.value = rows }
        }
    }

    fun openForm(prefilledUrl: String? = null) {
        _form.value = AddRepoForm(url = prefilledUrl.orEmpty())
        _formOpen.value = true
    }

    fun closeForm() {
        _formOpen.value = false
    }

    fun onUrlChanged(url: String) = _form.update { it.copy(url = url, error = null) }

    fun onLabelChanged(label: String) = _form.update { it.copy(label = label) }

    fun onTokenChanged(token: String) = _form.update { it.copy(token = token) }

    /**
     * Carries an authorization back into the form. Adds nothing: the person
     * still confirms, so abandoning here leaves the catalog untouched.
     */
    fun onAuthorized(credential: Credential.GitHub) =
        _form.update { it.copy(authorized = credential, token = "", error = null) }

    fun discardAuthorization() = _form.update { it.copy(authorized = null) }

    /** A URL captured from a scan, a share or the clipboard. Fills the form only. */
    fun capture(text: String): Boolean = when (val result = UrlCapture.capture(text)) {
        is CaptureResult.Found -> {
            _form.value = AddRepoForm(url = result.url)
            _formOpen.value = true
            true
        }

        CaptureResult.NoUrl -> {
            _form.value = AddRepoForm(error = "No repository URL in that.")
            _formOpen.value = true
            false
        }
    }

    fun submit() {
        val current = _form.value
        if (current.busy) return

        val problem = RepoUrl.validate(current.url)
        if (problem != null) {
            _form.update { it.copy(error = problem) }
            return
        }

        _form.update { it.copy(busy = true, error = null) }
        scope.launch {
            val url = current.url.trim()
            val token = current.authorized?.token ?: current.token.ifBlank { null }

            // What a repository holds is only knowable once it is here, so the
            // survey clones first and registers nothing.
            when (val surveyed = repository.survey(url, token)) {
                is RepoResult.Failure -> {
                    // Register it anyway, so the row carries the failure the
                    // way it always has rather than the form swallowing it.
                    addOne(current, url, "")
                    return@launch
                }

                is RepoResult.Success -> {
                    val paths = surveyed.value.map { it.path }
                    when {
                        paths.size > 1 -> {
                            _choice.value = ProjectChoice(
                                url = url,
                                label = current.label.trim(),
                                token = current.token.ifBlank { null },
                                credential = current.authorized,
                                projects = paths,
                            )
                            _form.update { it.copy(busy = false) }
                            _formOpen.value = false
                        }

                        else -> addOne(current, url, paths.firstOrNull().orEmpty())
                    }
                }
            }
        }
    }

    private suspend fun addOne(form: AddRepoForm, url: String, path: String) {
        val authorized = form.authorized
        if (authorized != null) {
            repository.addAuthorized(url, form.label.trim(), authorized, path)
        } else {
            repository.add(url, form.label.trim(), form.token.ifBlank { null }, path)
        }
        _form.value = AddRepoForm()
        _formOpen.value = false
    }

    fun toggleChoice(path: String) = _choice.update { choice ->
        choice?.copy(
            selected = if (path in choice.selected) {
                choice.selected - path
            } else {
                choice.selected + path
            },
        )
    }

    /** Abandoning adds nothing. The working copy stays, since it would only
     * have to be fetched again. */
    fun cancelChoice() {
        _choice.value = null
        _form.value = AddRepoForm()
    }

    fun confirmChoice() {
        val choice = _choice.value ?: return
        if (!choice.canConfirm) return
        _choice.value = null
        scope.launch {
            choice.projects.filter { it in choice.selected }.forEach { path ->
                val label = choice.label.ifBlank { "" }
                if (choice.credential != null) {
                    repository.addAuthorized(choice.url, label, choice.credential, path)
                } else {
                    repository.add(choice.url, label, choice.token, path)
                }
            }
            _form.value = AddRepoForm()
        }
    }

    fun refreshAll() {
        if (_refreshingAll.value) return
        _refreshingAll.value = true
        scope.launch {
            try {
                // One fetch per repository, not per row. Refreshing an entry
                // reloads every entry reading the same working copy.
                _rows.value
                    .distinctBy { it.config.cloneId }
                    .forEach { repository.refresh(it.config.id) }
            } finally {
                _refreshingAll.value = false
            }
        }
    }

    /** Told once, so returning to the list does not repeat it. */
    fun failureShown(cloneId: String) {
        repository.clearOutcomeOf(cloneId)
    }

    fun refresh(id: String) {
        scope.launch { repository.refresh(id) }
    }

    fun askToRemove(id: String) {
        _pendingRemoval.value = id
    }

    fun cancelRemoval() {
        _pendingRemoval.value = null
    }

    fun confirmRemoval() {
        val id = _pendingRemoval.value ?: return
        _pendingRemoval.value = null
        scope.launch { repository.remove(id) }
    }

    fun activate(id: String) {
        scope.launch { repository.activate(id) }
    }

    companion object {
        fun factory(repository: ProjectRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RepoListViewModel(repository) as T
        }
    }
}
