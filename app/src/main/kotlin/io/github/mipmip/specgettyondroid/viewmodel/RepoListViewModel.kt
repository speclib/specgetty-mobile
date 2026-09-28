package io.github.mipmip.specgettyondroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mipmip.specgettyondroid.capture.CaptureResult
import io.github.mipmip.specgettyondroid.capture.UrlCapture
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.index.ProjectCounts
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.store.RepoConfig
import io.github.mipmip.specgettyondroid.store.RepoUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row of the list: a repository and whatever is known about its project. */
data class RepoRow(
    val config: RepoConfig,
    val state: ProjectState,
    val isActive: Boolean,
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
            is ProjectState.Failed -> when (val e = state.error) {
                is RepoError.Authentication -> "Authentication failed. Check the access token."
                is RepoError.Network -> "The repository could not be reached."
                is RepoError.NoOpenSpecProject -> "No OpenSpec project here"
                is RepoError.Unknown -> e.message
            }

            is ProjectState.Loaded -> null
        }
}

data class AddRepoForm(
    val url: String = "",
    val label: String = "",
    val token: String = "",
    val error: String? = null,
    val busy: Boolean = false,
) {
    val isBlank: Boolean get() = url.isBlank() && label.isBlank() && token.isBlank()
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

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** A repository whose removal has been asked for but not confirmed. */
    private val _pendingRemoval = MutableStateFlow<String?>(null)
    val pendingRemoval: StateFlow<String?> = _pendingRemoval.asStateFlow()

    init {
        scope.launch {
            combine(repository.repos, repository.states) { list, states ->
                list.repos.map { config ->
                    RepoRow(
                        config = config,
                        state = states[config.id] ?: ProjectState.Absent,
                        isActive = config.id == list.activeId,
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
            repository.add(
                url = current.url.trim(),
                label = current.label.trim(),
                token = current.token.ifBlank { null },
            )
            _form.value = AddRepoForm()
            _formOpen.value = false
        }
    }

    fun refreshAll() {
        if (_refreshing.value) return
        _refreshing.value = true
        scope.launch {
            try {
                _rows.value.forEach { repository.refresh(it.config.id) }
            } finally {
                _refreshing.value = false
            }
        }
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
