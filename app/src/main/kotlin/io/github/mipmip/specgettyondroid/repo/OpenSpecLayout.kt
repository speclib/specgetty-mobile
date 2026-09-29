package io.github.mipmip.specgettyondroid.repo

import java.io.File

const val PROJECT_DIR = "openspec"
const val SPECS_DIR = "specs"
const val CHANGES_DIR = "changes"
const val ARCHIVE_DIR = "archive"
const val PROJECT_FILE = "project.md"

/** One project found inside a repository, and where it sits. */
data class DiscoveredProject(
    /** Relative to the working copy root, empty for the root itself. */
    val path: String,
    val dir: File,
) {
    /**
     * Named by its directory, and never by `.openspec-store/store.yaml`. An
     * identity file alone does not make a store: a clone keeps its copy of that
     * file, and a clone is what this holds.
     */
    val name: String get() = path.substringAfterLast('/').ifEmpty { dir.name }
}

/**
 * Where a project sits in a working copy, and which directories hold one.
 *
 * Two rules, answering two questions. [projectDir] answers "what is at this
 * path", loosely, so that a bare `openspec/` still loads as an empty project.
 * [qualifies] answers "is this worth offering", strictly, so that a sweep does
 * not turn every stray directory named `openspec` into a row. The strict rule
 * is specgetty's scanner's.
 */
object OpenSpecLayout {

    private val CONFIG_NAMES = listOf("config.yaml", "config.yml")

    private val MARKER_NAMES = CONFIG_NAMES + PROJECT_FILE

    private const val GIT_DIR = ".git"

    fun projectDir(workingDir: File, path: String = ""): RepoResult<File> {
        val base = if (path.isBlank()) workingDir else File(workingDir, path.trim().trim('/'))
        val dir = File(base, PROJECT_DIR)
        return if (dir.isDirectory) {
            RepoResult.Success(dir)
        } else {
            RepoResult.Failure(
                RepoError.NoOpenSpecProject(
                    if (path.isBlank()) {
                        "no OpenSpec project here"
                    } else {
                        "no OpenSpec project at ${path.trim().trim('/')}"
                    },
                ),
            )
        }
    }

    /**
     * Whether a directory holds a project worth offering: an `openspec/` child
     * holding a configuration or a project file. Content alone does not
     * qualify, and a directory named `openspec` holding neither never does.
     */
    fun qualifies(dir: File): Boolean {
        val openspec = File(dir, PROJECT_DIR)
        if (!openspec.isDirectory) return false
        return MARKER_NAMES.any { File(openspec, it).isFile }
    }

    /**
     * Every qualifying directory in a working copy, the root first and the rest
     * by path. Ordered rather than left to the filesystem, so that the same
     * repository always offers the same list in the same order.
     */
    fun discover(workingDir: File): List<DiscoveredProject> {
        val found = mutableListOf<DiscoveredProject>()

        fun walk(dir: File, path: String) {
            if (qualifies(dir)) found += DiscoveredProject(path, File(dir, PROJECT_DIR))
            dir.listFiles().orEmpty()
                .filter { it.isDirectory && it.name != GIT_DIR && it.name != PROJECT_DIR }
                .forEach { walk(it, if (path.isEmpty()) it.name else "$path/${it.name}") }
        }

        walk(workingDir, "")
        return found.sortedBy { it.path }
    }

    fun specsDir(projectDir: File): File = File(projectDir, SPECS_DIR)

    fun changesDir(projectDir: File): File = File(projectDir, CHANGES_DIR)

    fun archiveDir(projectDir: File): File = File(changesDir(projectDir), ARCHIVE_DIR)

    fun configFile(projectDir: File): File? =
        CONFIG_NAMES.map { File(projectDir, it) }.firstOrNull { it.isFile }

    fun projectFile(projectDir: File): File? =
        File(projectDir, PROJECT_FILE).takeIf { it.isFile }

    /** A project that exists but holds nothing. Not the same as no project. */
    fun isEmpty(projectDir: File): Boolean {
        val specs = specsDir(projectDir).listFiles()?.any { it.isDirectory } ?: false
        val changes = changesDir(projectDir).listFiles()
            ?.any { it.isDirectory && it.name != ARCHIVE_DIR } ?: false
        val archived = archiveDir(projectDir).listFiles()?.any { it.isDirectory } ?: false
        return !specs && !changes && !archived
    }
}
