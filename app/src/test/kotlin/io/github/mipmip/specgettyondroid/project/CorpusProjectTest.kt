package io.github.mipmip.specgettyondroid.project

import io.github.mipmip.specgettyondroid.spec.Corpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The vendored specgetty project, loaded as the app would load it. */
class CorpusProjectTest {

    private val info: ProjectInfo by lazy { ProjectLoader.loadProject(Corpus.root) }

    @Test
    fun `the corpus project loads with the shape it has on disk`() {
        assertEquals(
            "capabilities",
            Corpus.mainSpecs().size,
            info.specCount,
        )
        assertTrue("only ${info.archivedCount} archived changes", info.archivedCount >= 50)
        println(
            "corpus project: ${info.specCount} specs, ${info.activeCount} active, " +
                "${info.archivedCount} archived, tasks ${info.tasks.done}/${info.tasks.total}, " +
                "schemas ${info.schemasInUse}",
        )
    }

    @Test
    fun `every archived change in the corpus carries a date`() {
        val undated = info.archivedChanges.filter { it.date == null }
        assertTrue("archived changes with no date: ${undated.map { it.name }}", undated.isEmpty())
    }

    @Test
    fun `the archive is newest first`() {
        val dates = info.archivedChanges.mapNotNull { it.date }
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun `the corpus uses both workflow schemas`() {
        assertEquals(listOf("spec-driven", "tinychange"), info.schemasInUse)
    }

    @Test
    fun `every change in the corpus has at least one artifact`() {
        (info.activeChanges + info.archivedChanges).forEach {
            assertTrue("${it.name} has no artifact", it.artifacts.isNotEmpty())
        }
    }

    @Test
    fun `the project configuration is found`() {
        assertEquals("config.yaml", info.configFile?.name)
        assertEquals("spec-driven", info.schema)
    }

    @Test
    fun `the delta capabilities of the corpus match the files on disk`() {
        val fromLoader = (info.activeChanges + info.archivedChanges).sumOf { it.capabilities.size }
        assertEquals(Corpus.deltaSpecs().size, fromLoader)
    }

    @Test
    fun `the corpus project is not empty`() {
        assertTrue(!info.isEmpty)
    }
}
