package io.github.mipmip.specgettyondroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.data.ProjectState
import io.github.mipmip.specgettyondroid.project.Capability
import io.github.mipmip.specgettyondroid.project.ChangeInfo
import io.github.mipmip.specgettyondroid.tasks.TaskList
import io.github.mipmip.specgettyondroid.tasks.TaskParser
import io.github.mipmip.specgettyondroid.tasks.TaskStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One tab of the change screen. */
sealed interface ChangeTab {
    val label: String

    data class Artifact(val fileName: String) : ChangeTab {
        override val label: String get() = fileName.removeSuffix(".md")
    }

    data object Tasks : ChangeTab {
        override val label: String get() = "Tasks"
    }

    data object Specs : ChangeTab {
        override val label: String get() = "Specs"
    }
}

/** What an artifact tab has to show. */
sealed interface ArtifactContent {
    data class Markdown(val text: String) : ArtifactContent

    data class Unreadable(val reason: String) : ArtifactContent
}

class ChangeViewModel(
    private val repository: ProjectRepository,
    private val repoId: String,
    private val changeDirName: String,
    injectedScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = injectedScope ?: viewModelScope

    private val _change = MutableStateFlow<ChangeInfo?>(null)
    val change: StateFlow<ChangeInfo?> = _change.asStateFlow()

    private val _tab = MutableStateFlow<ChangeTab?>(null)
    val tab: StateFlow<ChangeTab?> = _tab.asStateFlow()

    /** Read as tabs are opened, so a long design document costs nothing until looked at. */
    private val artifacts = mutableMapOf<String, ArtifactContent>()

    init {
        scope.launch {
            repository.states.collect { states ->
                val index = (states[repoId] as? ProjectState.Loaded)?.index
                val found = index?.let { idx ->
                    (idx.activeChanges + idx.archivedChanges)
                        .firstOrNull { it.dir.name == changeDirName }
                }
                _change.value = found
                if (_tab.value == null) _tab.value = tabs.firstOrNull()
            }
        }
    }

    /**
     * One tab per markdown file actually present, then Tasks when there is a
     * tasks file, then Specs. A change is not required to hold any particular
     * artifact, so the tabs come from the listing rather than from a list of
     * expected names.
     */
    val tabs: List<ChangeTab>
        get() {
            val info = _change.value ?: return emptyList()
            val artifactTabs = info.artifacts
                .filterNot { it.name == TASKS_FILE }
                .map { ChangeTab.Artifact(it.name) }
            val tasksTab = if (info.tasksFile != null) listOf(ChangeTab.Tasks) else emptyList()
            return artifactTabs + tasksTab + ChangeTab.Specs
        }

    fun selectTab(tab: ChangeTab) {
        _tab.value = tab
    }

    fun artifact(fileName: String): ArtifactContent = artifacts.getOrPut(fileName) {
        val file = _change.value?.artifacts?.firstOrNull { it.name == fileName }?.file
            ?: return@getOrPut ArtifactContent.Unreadable("$fileName is not in this change.")
        runCatching { file.readText() }
            .fold(
                onSuccess = { ArtifactContent.Markdown(it) },
                onFailure = { ArtifactContent.Unreadable("$fileName could not be read.") },
            )
    }

    val tasks: TaskList
        get() = _change.value?.tasksFile
            ?.let { runCatching { TaskParser.parse(it.readText()) }.getOrNull() }
            ?: TaskList(emptyList())

    val taskStats: TaskStats get() = _change.value?.tasks ?: TaskStats.NONE

    val capabilities: List<Capability> get() = _change.value?.capabilities.orEmpty()

    val title: String get() = _change.value?.name ?: changeDirName

    val date: String? get() = _change.value?.date

    val isArchived: Boolean get() = _change.value?.archived == true

    companion object {
        private const val TASKS_FILE = "tasks.md"

        fun factory(repository: ProjectRepository, repoId: String, changeDirName: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChangeViewModel(repository, repoId, changeDirName) as T
            }
    }
}
