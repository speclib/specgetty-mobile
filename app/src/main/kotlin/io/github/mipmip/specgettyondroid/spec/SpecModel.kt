package io.github.mipmip.specgettyondroid.spec

enum class NodeKind {
    PURPOSE,
    REQUIREMENT,
    SCENARIO,

    /** Roots one delta file inside a change. A main spec never produces one. */
    CAPABILITY,
}

enum class PartKind { CLAUSE, PROSE }

/**
 * One piece of a scenario's content, in the order the file gives it. Ordered
 * rather than clauses beside a body: the two interleave in real files, and
 * grouping them would reorder a behaviour contract.
 */
data class SpecPart(
    val kind: PartKind,
    val keyword: String = "",
    val text: String = "",
)

data class SpecNode(
    val kind: NodeKind,
    val title: String,
    /** Prose under a requirement heading, or the Purpose text. */
    val body: String = "",
    val parts: List<SpecPart> = emptyList(),
    /** Identifies the node across a re-parse. */
    val path: String,
    /** The delta operation a requirement carries. Empty outside a change. */
    val op: String = "",
    /** The delta file this node came from. Empty outside a change. */
    val capability: String = "",
)

data class SpecTree(
    val name: String,
    val nodes: List<SpecNode> = emptyList(),
) {
    val requirementCount: Int get() = nodes.count { it.kind == NodeKind.REQUIREMENT }

    val scenarioCount: Int get() = nodes.count { it.kind == NodeKind.SCENARIO }

    fun indexOfPath(path: String): Int = nodes.indexOfFirst { it.path == path }
}

/**
 * One reason a file is not a spec, and the line it is on. Several rather than
 * one: a file that does not fit usually does not fit in more than one way.
 *
 * @param line 1-based, or 0 when the fault is that something is absent.
 */
data class SpecProblem(val line: Int, val text: String)

data class ParsedSpec(val tree: SpecTree, val problems: List<SpecProblem>) {
    val isSpec: Boolean get() = problems.isEmpty()
}

object Op {
    const val ADDED = "ADDED"
    const val MODIFIED = "MODIFIED"
    const val REMOVED = "REMOVED"
    const val RENAMED = "RENAMED"

    val known = setOf(ADDED, MODIFIED, REMOVED, RENAMED)
}
