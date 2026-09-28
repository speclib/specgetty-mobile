package io.github.mipmip.specgettyondroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.project.ChangeInfo
import io.github.mipmip.specgettyondroid.project.ProjectInfo
import io.github.mipmip.specgettyondroid.spec.Comparison
import io.github.mipmip.specgettyondroid.spec.NodeKind
import io.github.mipmip.specgettyondroid.spec.Op
import io.github.mipmip.specgettyondroid.spec.RequirementSource
import io.github.mipmip.specgettyondroid.spec.SpecNode
import io.github.mipmip.specgettyondroid.spec.SpecProblem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One delta file of the change: its nodes, or the reasons it could not be read. */
data class DeltaSection(
    val capability: String,
    val nodes: List<SpecNode>,
    val problems: List<SpecProblem>,
) {
    val isSpec: Boolean get() = problems.isEmpty()
}

/** Which of a comparison's three views is showing. */
enum class ComparisonView { DIFFERENCE, ORIGINAL, PROPOSED }

class DeltaViewModel(
    private val repository: ProjectRepository,
    private val repoId: String,
    private val changeDirName: String,
    injectedScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = injectedScope ?: viewModelScope

    private val _sections = MutableStateFlow<List<DeltaSection>>(emptyList())
    val sections: StateFlow<List<DeltaSection>> = _sections.asStateFlow()

    private val _selected = MutableStateFlow<SpecNode?>(null)
    val selected: StateFlow<SpecNode?> = _selected.asStateFlow()

    private val _view = MutableStateFlow(ComparisonView.DIFFERENCE)
    val view: StateFlow<ComparisonView> = _view.asStateFlow()

    private var change: ChangeInfo? = null
    private var project: ProjectInfo? = null

    init {
        scope.launch {
            repository.states.collect { states ->
                val index = (states[repoId] as? ProjectState.Loaded)?.index
                project = index?.project
                change = index?.let { idx ->
                    (idx.activeChanges + idx.archivedChanges)
                        .firstOrNull { it.dir.name == changeDirName }
                }
                _sections.value = read()
            }
        }
    }

    val title: String get() = change?.name ?: changeDirName

    val isArchived: Boolean get() = change?.archived == true

    private fun read(): List<DeltaSection> {
        val info = change ?: return emptyList()
        val cache = repository.cacheOf(repoId)
        return info.capabilities.map { capability ->
            val parsed = cache.delta(capability.name, capability.specFile)
            DeltaSection(capability.name, parsed.tree.nodes, parsed.problems)
        }
    }

    fun select(node: SpecNode) {
        _selected.value = node
        // A comparison always opens on the difference, which is what the reader
        // came for; the two sides are one tap away.
        _view.value = ComparisonView.DIFFERENCE
    }

    fun clearSelection() {
        _selected.value = null
    }

    fun showView(view: ComparisonView) {
        _view.value = view
    }

    /**
     * The comparison for the selected node, or null when there is nothing to
     * compare. An archived change never has one: it has already been applied,
     * so the main spec already holds the proposed text and a difference would be
     * empty, implying the change did nothing.
     */
    val comparison: Comparison?
        get() {
            if (isArchived) return null
            val node = _selected.value ?: return null
            if (node.kind != NodeKind.REQUIREMENT) return null
            if (node.op != Op.MODIFIED && node.op != Op.RENAMED) return null

            val mainSpec = project?.capabilities
                ?.firstOrNull { it.name == node.capability }
                ?.specFile
                ?: return null

            val mainText = runCatching { mainSpec.readText() }.getOrNull() ?: return null
            val original = RequirementSource.extract(mainText, node.title) ?: return null

            val deltaFile = change?.capabilities
                ?.firstOrNull { it.name == node.capability }
                ?.specFile
                ?: return null
            val deltaText = runCatching { deltaFile.readText() }.getOrNull() ?: return null
            val proposed = RequirementSource.extract(deltaText, node.title) ?: return null

            return Comparison(node.capability, node.title, original, proposed)
        }

    companion object {
        fun factory(repository: ProjectRepository, repoId: String, changeDirName: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DeltaViewModel(repository, repoId, changeDirName) as T
            }
    }
}
