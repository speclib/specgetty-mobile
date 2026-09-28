package io.github.mipmip.specgettyondroid.tasks

import java.io.File

data class TaskItem(
    /** The checkbox line verbatim, less its prefix. */
    val text: String,
    val done: Boolean,
    /** The heading this task sits under, empty before the first one. */
    val heading: String,
    /** The checkbox line's position in the source, counting from zero. */
    val index: Int,
    /** The indented lines that continue this task, in file order. */
    val continuation: List<String> = emptyList(),
)

data class TaskStats(val done: Int, val total: Int) {
    val isEmpty: Boolean get() = total == 0

    operator fun plus(other: TaskStats) = TaskStats(done + other.done, total + other.total)

    companion object {
        val NONE = TaskStats(0, 0)

        fun sum(all: Iterable<TaskStats>): TaskStats = all.fold(NONE) { a, b -> a + b }
    }
}

data class TaskList(val items: List<TaskItem>) {
    val stats: TaskStats get() = TaskStats(items.count { it.done }, items.size)
}

/**
 * The prefixes the counter matches, at column zero. Matching them exactly is
 * deliberate: an indented or `*` prefixed checkbox is not counted in the
 * totals, so drawing it as a box would make the display disagree with the
 * numbers beside it.
 */
private const val UNCHECKED = "- [ ] "
private const val CHECKED = "- [x] "

object TaskParser {

    fun isTaskLine(line: String): Boolean =
        line.startsWith(UNCHECKED) || line.startsWith(CHECKED)

    /**
     * A task is rarely one line: OpenSpec's generators wrap a task and indent
     * the rest. A continuation is indented and not blank; everything else
     * starts something new.
     */
    fun continuesTask(line: String): Boolean {
        if (isTaskLine(line)) return false
        if (line.isBlank()) return false
        return line[0] == ' ' || line[0] == '\t'
    }

    fun parse(content: String): TaskList {
        val lines = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split("\n")

        val items = mutableListOf<TaskItem>()
        var heading = ""
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!isTaskLine(line)) {
                val level = line.takeWhile { it == '#' }.length
                if (level in 1..6 && line.length > level && line[level] == ' ') {
                    heading = line.substring(level + 1).trim()
                }
                i++
                continue
            }

            val done = line.startsWith(CHECKED)
            val text = line.substring(UNCHECKED.length)

            var last = i
            while (last + 1 < lines.size && continuesTask(lines[last + 1])) last++

            items.add(
                TaskItem(
                    text = text,
                    done = done,
                    heading = heading,
                    index = i,
                    continuation = lines.subList(i + 1, last + 1).toList(),
                ),
            )
            i = last + 1
        }
        return TaskList(items)
    }

    /** Zero of zero for an absent file, rather than an error. */
    fun statsOf(tasksFile: File?): TaskStats =
        if (tasksFile == null || !tasksFile.isFile) {
            TaskStats.NONE
        } else {
            parse(tasksFile.readText()).stats
        }
}
