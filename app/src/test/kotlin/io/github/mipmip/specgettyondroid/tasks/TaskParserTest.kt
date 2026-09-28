package io.github.mipmip.specgettyondroid.tasks

import io.github.mipmip.specgettyondroid.spec.Corpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TaskParserTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun parse(content: String) = TaskParser.parse(content.trimIndent())

    @Test
    fun `the two shapes that count`() {
        val list = parse(
            """
            - [ ] 1.1 Not done
            - [x] 1.2 Done
            """,
        )
        assertEquals(2, list.items.size)
        assertFalse(list.items[0].done)
        assertTrue(list.items[1].done)
        assertEquals("1.1 Not done", list.items[0].text)
        assertEquals("1.2 Done", list.items[1].text)
    }

    @Test
    fun `an indented checkbox is not a task of its own`() {
        val list = parse(
            """
            - [ ] 1.1 A task
              - [ ] 1.1.1 Indented
            """,
        )
        assertEquals(1, list.items.size)
        assertEquals(listOf("  - [ ] 1.1.1 Indented"), list.items.single().continuation)
    }

    @Test
    fun `another list marker is not a task`() {
        listOf("* [ ] a", "+ [ ] a", "* [x] a").forEach {
            assertTrue(it, TaskParser.parse(it).items.isEmpty())
        }
    }

    @Test
    fun `an upper case cross is not a task`() {
        assertTrue(TaskParser.parse("- [X] 1.1 Done").items.isEmpty())
    }

    @Test
    fun `a checkbox with no space after it is not a task`() {
        assertTrue(TaskParser.parse("- [x]1.1 Done").items.isEmpty())
        assertTrue(TaskParser.parse("- [ ]1.1 Done").items.isEmpty())
    }

    @Test
    fun `a line that merely mentions a checkbox is not a task`() {
        assertTrue(TaskParser.parse("Write it as - [ ] like this").items.isEmpty())
    }

    @Test
    fun `a wrapped task keeps its continuation`() {
        val list = parse(
            """
            - [ ] 1.1 Add the workflow running on push and on
                  pull request, installing nix and nothing else
            """,
        )
        assertEquals(1, list.items.size)
        assertEquals(1, list.items.single().continuation.size)
        assertTrue(list.items.single().continuation.single().contains("pull request"))
    }

    @Test
    fun `a blank line ends a task`() {
        val list = parse(
            """
            - [ ] 1.1 A task

              not a continuation, a blank line came first
            """,
        )
        assertEquals(1, list.items.size)
        assertTrue(list.items.single().continuation.isEmpty())
    }

    @Test
    fun `the next checkbox ends a task`() {
        val list = parse(
            """
            - [ ] 1.1 First
            - [x] 1.2 Second
            """,
        )
        assertTrue(list.items[0].continuation.isEmpty())
        assertEquals(2, list.items.size)
    }

    @Test
    fun `an unindented line ends a task`() {
        val list = parse(
            """
            - [ ] 1.1 A task
            Some prose at column zero.
            """,
        )
        assertTrue(list.items.single().continuation.isEmpty())
    }

    @Test
    fun `counts are done and total`() {
        val list = parse(
            """
            - [x] 1.1 a
            - [x] 1.2 b
            - [x] 1.3 c
            - [ ] 1.4 d
            - [ ] 1.5 e
            """,
        )
        assertEquals(TaskStats(3, 5), list.stats)
    }

    @Test
    fun `a file with no tasks counts zero of zero`() {
        assertEquals(TaskStats(0, 0), TaskParser.parse("# Tasks\n\nNothing yet.\n").stats)
        assertTrue(TaskParser.parse("").stats.isEmpty)
    }

    @Test
    fun `an absent file counts zero of zero`() {
        assertEquals(TaskStats.NONE, TaskParser.statsOf(null))
        assertEquals(TaskStats.NONE, TaskParser.statsOf(File(temp.root, "nothing.md")))
    }

    @Test
    fun `a real file is read from disk`() {
        val f = temp.newFile("tasks.md")
        f.writeText("- [x] 1.1 a\n- [ ] 1.2 b\n")
        assertEquals(TaskStats(1, 2), TaskParser.statsOf(f))
    }

    @Test
    fun `counts add up across changes`() {
        val aggregate = TaskStats.sum(listOf(TaskStats(3, 5), TaskStats(2, 4)))
        assertEquals(TaskStats(5, 9), aggregate)
    }

    @Test
    fun `nothing to add is zero of zero`() {
        assertEquals(TaskStats.NONE, TaskStats.sum(emptyList()))
        assertEquals(TaskStats.NONE, TaskStats.sum(listOf(TaskStats.NONE, TaskStats.NONE)))
    }

    @Test
    fun `each task records the heading above it`() {
        val list = parse(
            """
            ## 1. First section

            - [x] 1.1 a
            - [ ] 1.2 b

            ## 2. Second section

            - [ ] 2.1 c
            """,
        )
        assertEquals(
            listOf("1. First section", "1. First section", "2. Second section"),
            list.items.map { it.heading },
        )
    }

    @Test
    fun `a task before any heading records none`() {
        val list = parse(
            """
            - [ ] 0.1 Before everything

            ## 1. A section

            - [ ] 1.1 After it
            """,
        )
        assertEquals("", list.items[0].heading)
        assertEquals("1. A section", list.items[1].heading)
    }

    @Test
    fun `a task records its source line index`() {
        val list = parse(
            """
            # Tasks

            ## 1. Section
            - [ ] 1.1 The fourth line
            """,
        )
        assertEquals(3, list.items.single().index)
    }

    @Test
    fun `reading changes nothing on disk`() {
        val f = temp.newFile("tasks.md")
        val content = "- [x] 1.1 a\n- [ ] 1.2 b\n"
        f.writeText(content)
        TaskParser.statsOf(f)
        assertEquals(content, f.readText())
    }

    @Test
    fun `carriage returns do not defeat the parser`() {
        assertEquals(TaskStats(1, 2), TaskParser.parse("- [x] a\r\n- [ ] b\r\n").stats)
    }

    @Test
    fun `every tasks file in the corpus is read without raising`() {
        val files = Corpus.changesDir.walkTopDown()
            .filter { it.isFile && it.name == "tasks.md" }
            .toList()
        assertTrue("only ${files.size} tasks files in the corpus", files.size >= 40)

        var items = 0
        var done = 0
        files.forEach { f ->
            val stats = TaskParser.statsOf(f)
            items += stats.total
            done += stats.done
            assertTrue("${f.path}: done exceeds total", stats.done <= stats.total)
        }
        assertTrue("only $items tasks found across the corpus", items >= 300)
        println("corpus: $done/$items tasks done across ${files.size} tasks.md files")
    }

    @Test
    fun `an archived change in the corpus is fully done`() {
        val archived = Corpus.archiveDir.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { File(it, "tasks.md").takeIf(File::isFile) }
        assertTrue(archived.isNotEmpty())
        val allDone = archived.count { TaskParser.statsOf(it).let { s -> s.done == s.total } }
        assertTrue(
            "$allDone of ${archived.size} archived changes have every task ticked",
            allDone >= archived.size * 3 / 4,
        )
    }
}
