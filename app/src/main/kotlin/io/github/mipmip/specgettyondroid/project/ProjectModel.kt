package io.github.mipmip.specgettyondroid.project

import io.github.mipmip.specgettyondroid.tasks.TaskStats
import java.io.File

data class Capability(
    val name: String,
    val specFile: File,
)

data class Artifact(
    val name: String,
    val file: File,
)

data class ChangeInfo(
    val name: String,
    val dir: File,
    val archived: Boolean,
    /** `YYYY-MM-DD` from an archived change's directory name, else null. */
    val date: String? = null,
    val artifacts: List<Artifact> = emptyList(),
    /** Capabilities this change touches, named by its own `specs/` directories. */
    val capabilities: List<Capability> = emptyList(),
    val schema: String? = null,
    val tasks: TaskStats = TaskStats.NONE,
) {
    val tasksFile: File? get() = artifacts.firstOrNull { it.name == "tasks.md" }?.file
}

data class ProjectInfo(
    val dir: File,
    val capabilities: List<Capability> = emptyList(),
    val activeChanges: List<ChangeInfo> = emptyList(),
    val archivedChanges: List<ChangeInfo> = emptyList(),
    val configFile: File? = null,
    val projectFile: File? = null,
    val schema: String? = null,
) {
    val specCount: Int get() = capabilities.size

    val activeCount: Int get() = activeChanges.size

    val archivedCount: Int get() = archivedChanges.size

    /** Open tasks across active changes, which is what a repository row shows. */
    val tasks: TaskStats get() = TaskStats.sum(activeChanges.map { it.tasks })

    val isEmpty: Boolean
        get() = capabilities.isEmpty() && activeChanges.isEmpty() && archivedChanges.isEmpty()

    /** One entry per workflow schema the changes use, in name order. */
    val schemasInUse: List<String>
        get() = (activeChanges + archivedChanges).mapNotNull { it.schema }.distinct().sorted()
}
