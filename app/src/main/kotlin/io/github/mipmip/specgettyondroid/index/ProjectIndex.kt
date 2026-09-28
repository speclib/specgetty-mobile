package io.github.mipmip.specgettyondroid.index

import io.github.mipmip.specgettyondroid.project.ChangeInfo
import io.github.mipmip.specgettyondroid.project.ProjectInfo
import io.github.mipmip.specgettyondroid.tasks.TaskStats
import java.io.File

data class ProjectCounts(
    val specs: Int,
    val active: Int,
    val archived: Int,
    val tasks: TaskStats,
)

/**
 * One change in a filtered list, with the files that made it match.
 * Empty for a change that matched through its name, which is how a row knows
 * whether it has anything to explain.
 */
data class ChangeMatch(
    val change: ChangeInfo,
    val matchedFiles: List<String> = emptyList(),
)

data class SearchResult(
    val query: Query,
    val raw: String,
    val active: List<ChangeMatch>,
    val archived: List<ChangeMatch>,
) {
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()

    /** An empty result from a query is not the same as an empty project. */
    val isFiltered: Boolean get() = !query.isEmpty
}

/**
 * @param readText how a file's text is obtained. Injected so that a test can
 *   count the reads and assert a name query performs none, which is the whole
 *   reason the matchers are separated.
 */
class ProjectIndex(
    private val project: ProjectInfo,
    private val readText: (File) -> String? = { runCatching { it.readText() }.getOrNull() },
) {

    val counts: ProjectCounts
        get() = ProjectCounts(
            specs = project.specCount,
            active = project.activeCount,
            archived = project.archivedCount,
            tasks = project.tasks,
        )

    /** Name order, as the loader produced them. */
    val activeChanges: List<ChangeInfo> get() = project.activeChanges

    /** Newest first, undated last, ties broken by name. */
    val archivedChanges: List<ChangeInfo>
        get() = project.archivedChanges.sortedWith(
            compareByDescending<ChangeInfo> { it.date != null }
                .thenByDescending { it.date ?: "" }
                .thenBy { it.name },
        )

    val capabilityNames: List<String> get() = project.capabilities.map { it.name }

    fun search(raw: String): SearchResult {
        val query = Query.parse(raw)
        if (query.isEmpty) {
            return SearchResult(
                query = query,
                raw = raw,
                active = activeChanges.map { ChangeMatch(it) },
                archived = archivedChanges.map { ChangeMatch(it) },
            )
        }
        return SearchResult(
            query = query,
            raw = raw,
            active = filter(activeChanges, query),
            archived = filter(archivedChanges, query),
        )
    }

    private fun filter(changes: List<ChangeInfo>, query: Query): List<ChangeMatch> =
        when (query.kind) {
            MatchKind.LITERAL_NAME ->
                changes.filter { query.contains(it.name) }.map { ChangeMatch(it) }

            MatchKind.BODY ->
                changes.mapNotNull { change ->
                    val files = matchingFiles(change, query)
                    val byName = query.contains(change.name)
                    if (files.isEmpty() && !byName) null else ChangeMatch(change, files)
                }

            MatchKind.FUZZY_NAME -> {
                val names = changes.map { it.name }
                Fuzzy.find(query.term, names)
                    .filter { match ->
                        !query.caseSensitive ||
                            Fuzzy.exactCase(match, names[match.index], query.term)
                    }
                    .map { ChangeMatch(changes[it.index]) }
            }
        }

    /**
     * Reads the change's artifacts and delta files. Only a body query reaches
     * here, so a name query touches no file.
     */
    private fun matchingFiles(change: ChangeInfo, query: Query): List<String> {
        val bodies = buildList {
            change.artifacts.forEach { add(it.name to it.file) }
            change.capabilities.forEach { add("${it.name}/spec.md" to it.specFile) }
        }.sortedBy { it.first }

        return bodies.mapNotNull { (label, file) ->
            val text = readText(file) ?: return@mapNotNull null
            label.takeIf { query.contains(text) }
        }
    }
}
