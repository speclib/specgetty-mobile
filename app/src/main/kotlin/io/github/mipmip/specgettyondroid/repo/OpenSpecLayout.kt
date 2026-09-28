package io.github.mipmip.specgettyondroid.repo

import java.io.File

const val PROJECT_DIR = "openspec"
const val SPECS_DIR = "specs"
const val CHANGES_DIR = "changes"
const val ARCHIVE_DIR = "archive"
const val PROJECT_FILE = "project.md"

/**
 * One project per repository, at [PROJECT_DIR] in the root. The briefing rules
 * out scanning for nested projects and store resolution, so absence here is a
 * final answer rather than a reason to look further.
 */
object OpenSpecLayout {

    private val CONFIG_NAMES = listOf("config.yaml", "config.yml")

    fun projectDir(workingDir: File): RepoResult<File> {
        val dir = File(workingDir, PROJECT_DIR)
        return if (dir.isDirectory) {
            RepoResult.Success(dir)
        } else {
            RepoResult.Failure(
                RepoError.NoOpenSpecProject("no OpenSpec project here"),
            )
        }
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
