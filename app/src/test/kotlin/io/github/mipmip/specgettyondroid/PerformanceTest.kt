package io.github.mipmip.specgettyondroid

import io.github.mipmip.specgettyondroid.data.SpecCache
import io.github.mipmip.specgettyondroid.index.ProjectIndex
import io.github.mipmip.specgettyondroid.project.ProjectInfo
import io.github.mipmip.specgettyondroid.project.ProjectLoader
import io.github.mipmip.specgettyondroid.spec.Corpus
import io.github.mipmip.specgettyondroid.spec.DeltaParser
import io.github.mipmip.specgettyondroid.spec.SpecParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What reading a real project costs. The thresholds are deliberately loose: a
 * tight one on a shared machine fails on a busy afternoon and teaches everyone
 * to ignore it. These catch an order-of-magnitude regression, which is the kind
 * that matters.
 *
 * What the app draws is not measured here. A build machine is not a phone, and a
 * number from one says nothing about the other, so scrolling is checked on a
 * device instead.
 */
class PerformanceTest {

    private fun <T> timed(label: String, block: () -> T): Pair<T, Long> {
        // One warm run first, so the figure is not mostly class loading.
        block()
        val started = System.nanoTime()
        val result = block()
        val millis = (System.nanoTime() - started) / 1_000_000
        println("perf: $label took ${millis}ms")
        return result to millis
    }

    private fun load(): ProjectInfo = ProjectLoader.loadProject(Corpus.root)

    @Test
    fun `loading the project`() {
        val (info, millis) = timed("load the project") { load() }
        assertEquals(28, info.specCount)
        assertTrue("only ${info.archivedCount} archived", info.archivedCount >= 50)
        assertTrue("loading took ${millis}ms", millis < 4000)
    }

    @Test
    fun `building the index and reading its counts`() {
        val info = load()
        val (counts, millis) = timed("index and counts") { ProjectIndex(info).counts }
        assertEquals(28, counts.specs)
        assertTrue("indexing took ${millis}ms", millis < 1000)
    }

    @Test
    fun `a name query over every change reads no file`() {
        val index = ProjectIndex(load()) { error("a name query must read no file") }
        val (result, millis) = timed("name query") { index.search("arch") }
        assertTrue(result.active.isNotEmpty() || result.archived.isNotEmpty())
        assertTrue("a name query took ${millis}ms", millis < 1000)
    }

    @Test
    fun `a text query over every change`() {
        var reads = 0
        val index = ProjectIndex(load()) { file ->
            reads++
            runCatching { file.readText() }.getOrNull()
        }
        val (result, millis) = timed("text query") { index.search(":requirement") }
        println("perf: the text query read $reads files")
        assertTrue(result.active.isNotEmpty() || result.archived.isNotEmpty())
        assertTrue("a text query took ${millis}ms", millis < 8000)
    }

    @Test
    fun `parsing every spec and every delta`() {
        val specs = Corpus.mainSpecs()
        val deltas = Corpus.deltaSpecs()

        val (_, millis) = timed("parse ${specs.size} specs and ${deltas.size} deltas") {
            specs.forEach { SpecParser.parse(it.parentFile.name, it.readText()) }
            deltas.forEach { DeltaParser.parse(Corpus.capabilityOf(it), it.readText()) }
        }
        println("perf: ${specs.size + deltas.size} files parsed")
        assertTrue("parsing everything took ${millis}ms", millis < 10000)
    }

    @Test
    fun `parsing the same spec again through the cache costs nothing`() {
        val cache = SpecCache()
        val spec = Corpus.mainSpecs().first { it.length() > 10_000 }
        val capability = spec.parentFile.name

        val first = System.nanoTime()
        cache.spec(capability, spec)
        val firstMillis = (System.nanoTime() - first) / 1_000_000

        val second = System.nanoTime()
        repeat(100) { cache.spec(capability, spec) }
        val hundredMillis = (System.nanoTime() - second) / 1_000_000

        println(
            "perf: first parse of $capability ${firstMillis}ms, " +
                "100 cached reads ${hundredMillis}ms",
        )
        assertEquals("only the first was a parse", 1, cache.parses)
        assertTrue("100 cached reads took ${hundredMillis}ms", hundredMillis < 500)
    }

    @Test
    fun `the whole of what a refresh does, end to end`() {
        val (_, millis) = timed("load, index and count as a refresh does") {
            ProjectIndex(load()).counts
        }
        assertTrue("a refresh's work took ${millis}ms", millis < 5000)
    }
}
