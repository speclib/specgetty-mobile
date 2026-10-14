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
import java.io.File

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

    val refresh = RefreshControl(repository, repoId, scope)

    /**
     * Read as tabs are opened, so a long design document costs nothing until
     * looked at, and keyed by what the file was when it was read, so a refresh
     * that rewrote it is read again and one that left it alone is not.
     */
    private data class ArtifactKey(val path: String, val length: Long, val modified: Long)

    private val artifacts = mutableMapOf<ArtifactKey, ArtifactContent>()

    /** How many times a file has been read, so a test can see the keying work. */
    var reads: Int = 0
        private set

    init {
        scope.launch {
            repository.states.collect { states ->
                val index = (states[repoId] as? ProjectState.Loaded)?.index
                val found = index?.let { idx ->
                    (idx.activeChanges + idx.archivedChanges)
                        .firstOrNull { it.dir.name == changeDirName }
                }
                _change.value = found
                // A refresh can take the open tab's file away, or add one. The
                // tab follows what the change holds now rather than what it
                // held when it was opened.
                val available = tabs
                _tab.value = _tab.value?.takeIf { it in available } ?: available.firstOrNull()
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

    fun artifact(fileName: String): ArtifactContent {
        val file: File = _change.value?.artifacts?.firstOrNull { it.name == fileName }?.file
            ?: return ArtifactContent.Unreadable("$fileName is not in this change.")
        val key = ArtifactKey(file.path, file.length(), file.lastModified())
        artifacts[key]?.let { return it }

        val content = runCatching { file.readText() }
            .fold(
                onSuccess = { ArtifactContent.Markdown(it) },
                onFailure = { ArtifactContent.Unreadable("$fileName could not be read.") },
            )
        reads++
        // One entry per file, not one per version of it: the text a refresh
        // replaced will never be asked for again.
        artifacts.keys.removeAll { it.path == key.path }
        artifacts[key] = content
        return content
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
