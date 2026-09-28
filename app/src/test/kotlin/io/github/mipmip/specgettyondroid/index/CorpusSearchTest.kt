package io.github.mipmip.specgettyondroid.index

import io.github.mipmip.specgettyondroid.project.ProjectLoader
import io.github.mipmip.specgettyondroid.spec.Corpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Search over the vendored specgetty project. The timings are reported rather
 * than asserted tightly: a unit test on a build machine is not a phone, and a
 * threshold tuned to one would be noise on the other. Milestone 09 measures the
 * device.
 */
class CorpusSearchTest {

    private var reads = 0

    private val index by lazy {
        ProjectIndex(ProjectLoader.loadProject(Corpus.root)) { file ->
            reads++
            runCatching { file.readText() }.getOrNull()
        }
    }

    @Test
    fun `the corpus index reports the project's counts`() {
        val counts = index.counts
        assertEquals(28, counts.specs)
        assertTrue(counts.archived >= 50)
        println("index: $counts")
    }

    @Test
    fun `a fuzzy query over the whole archive finds what it should`() {
        val names = index.search("expzip").archived.map { it.change.name }
        assertTrue("got $names", names.contains("export-change-as-zip"))
        assertEquals("the best match is first", "export-change-as-zip", names.first())
    }

    @Test
    fun `a name query reads no file from disk`() {
        index.search("arch")
        index.search("'archive")
        assertEquals("a name query must not read artifacts", 0, reads)

        index.search(":archive")
        assertTrue("a body query does read them", reads > 0)
    }

    @Test
    fun `a body query over the corpus completes and reports its time`() {
        val started = System.nanoTime()
        val result = index.search(":inotify")
        val millis = (System.nanoTime() - started) / 1_000_000

        val hits = result.active + result.archived
        println(
            "body search ':inotify' over ${index.counts.archived + index.counts.active} " +
                "changes: ${hits.size} hits in ${millis}ms",
        )
        assertTrue("a body search found nothing at all", hits.isNotEmpty())
        assertTrue("every hit names the files it matched", hits.any { it.matchedFiles.isNotEmpty() })
    }

    @Test
    fun `a body query finds text that a name query cannot`() {
        val byName = index.search("inotify")
        val byBody = index.search(":inotify")
        val nameHits = (byName.active + byName.archived).size
        val bodyHits = (byBody.active + byBody.archived).size
        assertTrue("body $bodyHits should exceed name $nameHits", bodyHits > nameHits)
    }

    @Test
    fun `every archived change in the corpus is reachable by a literal name query`() {
        index.archivedChanges.forEach { change ->
            val found = index.search("'${change.name}").archived.map { it.change.name }
            assertTrue("${change.name} was not found by its own name", found.contains(change.name))
        }
    }

}
