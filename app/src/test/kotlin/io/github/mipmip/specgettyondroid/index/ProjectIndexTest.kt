package io.github.mipmip.specgettyondroid.index

import io.github.mipmip.specgettyondroid.project.ProjectBuilder
import io.github.mipmip.specgettyondroid.tasks.TaskStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectIndexTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun builder() = ProjectBuilder(temp.newFolder())

    private fun index(build: ProjectBuilder.() -> Unit): ProjectIndex =
        ProjectIndex(builder().apply(build).load())

    @Test
    fun `the counts are what the project holds`() {
        val idx = index {
            spec("a")
            spec("b")
            change("active-one", artifacts = mapOf("tasks.md" to ProjectBuilder.tasks(3, 5)))
            change("2026-09-21-done", archived = true)
        }
        assertEquals(ProjectCounts(2, 1, 1, TaskStats(3, 5)), idx.counts)
    }

    @Test
    fun `archived tasks are not in the open task figure`() {
        val idx = index {
            change("active", artifacts = mapOf("tasks.md" to ProjectBuilder.tasks(1, 2)))
            change(
                "2026-09-21-done",
                archived = true,
                artifacts = mapOf("tasks.md" to ProjectBuilder.tasks(9, 9)),
            )
        }
        assertEquals(TaskStats(1, 2), idx.counts.tasks)
    }

    @Test
    fun `an empty project counts zero everywhere`() {
        val idx = index { emptyProject() }
        assertEquals(ProjectCounts(0, 0, 0, TaskStats.NONE), idx.counts)
    }

    @Test
    fun `active changes are in name order`() {
        val idx = index {
            change("zebra")
            change("alpha")
            change("middle")
        }
        assertEquals(listOf("alpha", "middle", "zebra"), idx.activeChanges.map { it.name })
    }

    @Test
    fun `archived changes are newest first`() {
        val idx = index {
            change("2026-01-01-oldest", archived = true)
            change("2026-09-21-newest", archived = true)
            change("2026-05-05-middle", archived = true)
        }
        assertEquals(
            listOf("newest", "middle", "oldest"),
            idx.archivedChanges.map { it.name },
        )
    }

    @Test
    fun `two archived on the same day are ordered by name`() {
        val idx = index {
            change("2026-09-21-zebra", archived = true)
            change("2026-09-21-alpha", archived = true)
        }
        assertEquals(listOf("alpha", "zebra"), idx.archivedChanges.map { it.name })
    }

    @Test
    fun `an archived change with no date is listed last`() {
        val idx = index {
            change("undated-one", archived = true)
            change("2026-01-01-dated", archived = true)
        }
        assertEquals(listOf("dated", "undated-one"), idx.archivedChanges.map { it.name })
    }

    @Test
    fun `an empty query filters nothing and keeps the order`() {
        val idx = index {
            change("alpha")
            change("zebra")
            change("2026-01-01-old", archived = true)
        }
        val result = idx.search("")
        assertEquals(listOf("alpha", "zebra"), result.active.map { it.change.name })
        assertEquals(listOf("old"), result.archived.map { it.change.name })
        assertFalse(result.isFiltered)
    }

    @Test
    fun `the fuzzy example from the search spec`() {
        val idx = index {
            change("export-change-as-zip")
            change("something-else")
        }
        val result = idx.search("expzip")
        assertEquals(listOf("export-change-as-zip"), result.active.map { it.change.name })
    }

    @Test
    fun `a literal name query does not match a subsequence`() {
        val idx = index { change("export-change-as-zip") }
        assertTrue(idx.search("'expzip").active.isEmpty())
        assertEquals(1, idx.search("'export").active.size)
    }

    @Test
    fun `a body query matches the text of an artifact and names the file`() {
        val idx = index {
            change(
                "some-change",
                artifacts = mapOf(
                    "proposal.md" to "## Why\n\nBecause of inotify.\n",
                    "tasks.md" to "- [ ] 1.1 Use inotify\n",
                    "design.md" to "## Context\n\nNothing relevant.\n",
                ),
            )
        }
        val match = idx.search(":inotify").active.single()
        assertEquals(listOf("proposal.md", "tasks.md"), match.matchedFiles)
    }

    @Test
    fun `a body query matches the text of a delta and names the capability`() {
        val idx = index { change("some-change", capabilities = listOf("repo-store")) }
        val match = idx.search(":Something new in repo-store").active.single()
        assertEquals(listOf("repo-store/spec.md"), match.matchedFiles)
    }

    @Test
    fun `a change matching on its name alone reports no files`() {
        val idx = index {
            change("inotify-watcher", artifacts = mapOf("proposal.md" to "## Why\n\nNothing.\n"))
        }
        val match = idx.search(":inotify").active.single()
        assertTrue(match.matchedFiles.isEmpty())
    }

    @Test
    fun `a change matching on both still reports the file`() {
        val idx = index {
            change("inotify-watcher", artifacts = mapOf("proposal.md" to "About inotify.\n"))
        }
        assertEquals(listOf("proposal.md"), idx.search(":inotify").active.single().matchedFiles)
    }

    @Test
    fun `a body query that matches nothing returns nothing`() {
        val idx = index { change("a", artifacts = mapOf("proposal.md" to "Nothing here.\n")) }
        assertTrue(idx.search(":zzzz").active.isEmpty())
    }

    @Test
    fun `smart case applies to a literal name query`() {
        val idx = index { change("Export-Change") }
        assertEquals(1, idx.search("'export").active.size)
        assertEquals(0, idx.search("'Export-change").active.size)
        assertEquals(1, idx.search("'Export").active.size)
    }

    @Test
    fun `smart case applies to the fuzzy matcher`() {
        val idx = index { change("export-change") }
        assertEquals(1, idx.search("expc").active.size)
        assertEquals(0, idx.search("Expc").active.size)
    }

    @Test
    fun `fuzzy results come back ranked`() {
        val idx = index {
            change("ab")
            change("a-long-way-around-to-b")
        }
        assertEquals("ab", idx.search("ab").active.first().change.name)
    }

    @Test
    fun `literal results keep the list order`() {
        val idx = index {
            change("alpha-thing")
            change("beta-thing")
            change("gamma-thing")
        }
        assertEquals(
            listOf("alpha-thing", "beta-thing", "gamma-thing"),
            idx.search("'thing").active.map { it.change.name },
        )
    }

    @Test
    fun `a query that matched nothing is distinguishable from an empty project`() {
        val filtered = index { change("alpha") }.search("zzzz")
        assertTrue(filtered.isEmpty)
        assertTrue(filtered.isFiltered)
        assertEquals("zzzz", filtered.raw)

        val bare = index { emptyProject() }.search("")
        assertTrue(bare.isEmpty)
        assertFalse(bare.isFiltered)
    }

    @Test
    fun `a search covers archived changes too`() {
        val idx = index {
            change("2026-01-01-archived-thing", archived = true)
            change("active-thing")
        }
        val result = idx.search("'thing")
        assertEquals(1, result.active.size)
        assertEquals(1, result.archived.size)
    }

    @Test
    fun `capability names are listed`() {
        val idx = index {
            spec("overview-tab")
            spec("change-search")
        }
        assertEquals(listOf("change-search", "overview-tab"), idx.capabilityNames)
    }
}
