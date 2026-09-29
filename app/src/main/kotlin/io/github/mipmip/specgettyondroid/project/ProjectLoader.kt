package io.github.mipmip.specgettyondroid.project

import io.github.mipmip.specgettyondroid.repo.ARCHIVE_DIR
import io.github.mipmip.specgettyondroid.repo.OpenSpecLayout
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoResult
import io.github.mipmip.specgettyondroid.repo.StoreDeclarations
import io.github.mipmip.specgettyondroid.tasks.TaskParser
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

/**
 * Walks `openspec/` into the model the screens read. Eager about structure and
 * task counts, which every clone and refresh needs; lazy about spec content,
 * which is parsed when a spec is opened.
 */
object ProjectLoader {

    private const val CHANGE_CONFIG = ".openspec.yaml"
    private val DATE_PREFIX = Regex("""^(\d{4})-(\d{2})-(\d{2})-(.+)$""")

    fun load(workingDir: File, path: String = ""): RepoResult<ProjectInfo> =
        when (val found = OpenSpecLayout.projectDir(workingDir, path)) {
            is RepoResult.Failure -> found
            is RepoResult.Success -> {
                val declaration = StoreDeclarations.unresolvable(found.value)
                if (declaration != null) {
                    RepoResult.Failure(
                        RepoError.PointsElsewhere(StoreDeclarations.describe(declaration)),
                    )
                } else {
                    RepoResult.Success(loadProject(found.value))
                }
            }
        }

    fun loadProject(projectDir: File): ProjectInfo {
        val configFile = OpenSpecLayout.configFile(projectDir)
        return ProjectInfo(
            dir = projectDir,
            capabilities = capabilitiesIn(OpenSpecLayout.specsDir(projectDir)),
            activeChanges = activeChanges(projectDir),
            archivedChanges = archivedChanges(projectDir),
            configFile = configFile,
            projectFile = OpenSpecLayout.projectFile(projectDir),
            schema = configFile?.let { schemaIn(it) },
        )
    }

    private fun capabilitiesIn(specsDir: File): List<Capability> =
        specsDir.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { dir ->
                File(dir, "spec.md").takeIf(File::isFile)?.let { Capability(dir.name, it) }
            }
            .sortedBy { it.name }

    private fun activeChanges(projectDir: File): List<ChangeInfo> =
        OpenSpecLayout.changesDir(projectDir).listFiles().orEmpty()
            .filter { it.isDirectory && it.name != ARCHIVE_DIR }
            .map { readChange(it, archived = false) }
            .sortedBy { it.name }

    private fun archivedChanges(projectDir: File): List<ChangeInfo> =
        OpenSpecLayout.archiveDir(projectDir).listFiles().orEmpty()
            .filter { it.isDirectory }
            .map { readChange(it, archived = true) }
            .sortedWith(compareByDescending<ChangeInfo> { it.date ?: "" }.thenBy { it.name })

    private fun readChange(dir: File, archived: Boolean): ChangeInfo {
        val (date, name) = if (archived) splitDate(dir.name) else null to dir.name

        val artifacts = dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".md") }
            .map { Artifact(it.name, it) }
            .sortedBy { it.name }

        val capabilities = capabilitiesIn(File(dir, "specs"))

        val schema = File(dir, CHANGE_CONFIG).takeIf(File::isFile)?.let { schemaIn(it) }

        val tasks = TaskParser.statsOf(artifacts.firstOrNull { it.name == "tasks.md" }?.file)

        return ChangeInfo(
            name = name,
            dir = dir,
            archived = archived,
            date = date,
            artifacts = artifacts,
            capabilities = capabilities,
            schema = schema,
            tasks = tasks,
        )
    }

    /**
     * The date comes from the directory name, not the filesystem: a clone gives
     * every file the time it was written to disk, which would sort the archive
     * by when the phone fetched it.
     */
    internal fun splitDate(dirName: String): Pair<String?, String> {
        val m = DATE_PREFIX.find(dirName) ?: return null to dirName
        val (y, mo, d, rest) = m.destructured
        if (!isRealDate(y.toInt(), mo.toInt(), d.toInt())) return null to dirName
        return "$y-$mo-$d" to rest
    }

    private fun isRealDate(year: Int, month: Int, day: Int): Boolean {
        if (month !in 1..12 || day < 1) return false
        val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        val lengths = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return day <= lengths[month - 1]
    }

    /**
     * `SafeConstructor` deliberately: these files come from a repository
     * somebody else wrote, and a YAML parser that can construct arbitrary types
     * is a way into the app.
     */
    internal fun schemaIn(yamlFile: File): String? = try {
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Any?>(yamlFile.readText())
        (root as? Map<*, *>)?.get("schema") as? String
    } catch (_: Exception) {
        null
    }
}
