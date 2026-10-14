package io.github.mipmip.specgettyondroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.spec.SpecNode
import io.github.mipmip.specgettyondroid.spec.SpecProblem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** What the spec screen has to show. */
sealed interface SpecScreenState {
    data object Loading : SpecScreenState

    data class Outline(val nodes: List<SpecNode>) : SpecScreenState

    /**
     * The file is not a spec. Every reason is carried, not the first: a file
     * that does not fit usually does not fit in more than one way, and naming
     * only the first sends the reader back for the next after each repair.
     */
    data class Report(val problems: List<SpecProblem>) : SpecScreenState

    data class Unreadable(val reason: String) : SpecScreenState
}

class SpecViewModel(
    private val repository: ProjectRepository,
    private val repoId: String,
    val capability: String,
    injectedScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = injectedScope ?: viewModelScope

    private val _state = MutableStateFlow<SpecScreenState>(SpecScreenState.Loading)
    val state: StateFlow<SpecScreenState> = _state.asStateFlow()

    private val _selected = MutableStateFlow<SpecNode?>(null)
    val selected: StateFlow<SpecNode?> = _selected.asStateFlow()

    private val _selectionGone = MutableStateFlow(false)

    /**
     * The open card is for a node the spec no longer holds. Said rather than
     * shown: the text on screen would otherwise be text no longer in the file.
     */
    val selectionGone: StateFlow<Boolean> = _selectionGone.asStateFlow()

    val refresh = RefreshControl(repository, repoId, scope)

    /** The whole file, so a spec that does not fit is still readable as Markdown. */
    private val _rawText = MutableStateFlow("")
    val rawText: StateFlow<String> = _rawText.asStateFlow()

    private val _showingRaw = MutableStateFlow(false)
    val showingRaw: StateFlow<Boolean> = _showingRaw.asStateFlow()

    init {
        scope.launch {
            repository.states.collect { states ->
                val index = (states[repoId] as? ProjectState.Loaded)?.index
                val file = index?.project?.capabilities
                    ?.firstOrNull { it.name == capability }
                    ?.specFile
                _state.value = load(file)
                reconcileSelection()
            }
        }
    }

    /**
     * Finds the open card's node in the outline as it is now. A refresh may
     * have rewritten the spec, in which case the node at the same path is the
     * one to show, or removed the node, in which case there is nothing to show
     * and saying so is the only honest answer.
     */
    private fun reconcileSelection() {
        val current = _selected.value ?: return
        val match = nodes.firstOrNull { it.path == current.path }
        if (match == null) {
            _selectionGone.value = true
        } else {
            _selectionGone.value = false
            _selected.value = match
        }
    }

    private fun load(file: File?): SpecScreenState {
        if (file == null) {
            return SpecScreenState.Unreadable("$capability is not in this project.")
        }
        val text = runCatching { file.readText() }.getOrNull()
            ?: return SpecScreenState.Unreadable("$capability could not be read.")
        _rawText.value = text

        val parsed = repository.cacheOf(repoId).spec(capability, file)
        return if (parsed.isSpec) {
            SpecScreenState.Outline(parsed.tree.nodes)
        } else {
            SpecScreenState.Report(parsed.problems)
        }
    }

    fun select(node: SpecNode) {
        _selected.value = node
        _selectionGone.value = false
    }

    fun clearSelection() {
        _selected.value = null
        _selectionGone.value = false
    }

    /** The outline's own order, or empty when the file is not a spec. */
    private val nodes: List<SpecNode>
        get() = (_state.value as? SpecScreenState.Outline)?.nodes.orEmpty()

    /**
     * Where the card is in the outline, or -1. Found by path rather than by
     * identity, so a re-parse that produced an equal node still locates it.
     */
    private val selectedIndex: Int
        get() = _selected.value?.let { current -> nodes.indexOfFirst { it.path == current.path } }
            ?: -1

    val hasNext: Boolean
        get() = selectedIndex >= 0 && selectedIndex < nodes.lastIndex

    val hasPrevious: Boolean
        get() = selectedIndex > 0

    /** Neither end wraps: returning to the first node would lose a reader's place. */
    fun next() {
        if (hasNext) _selected.value = nodes[selectedIndex + 1]
    }

    fun previous() {
        if (hasPrevious) _selected.value = nodes[selectedIndex - 1]
    }

    fun showRaw(showing: Boolean) {
        _showingRaw.value = showing
    }

    companion object {
        fun factory(repository: ProjectRepository, repoId: String, capability: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SpecViewModel(repository, repoId, capability) as T
            }
    }
}
