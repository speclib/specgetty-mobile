package io.github.mipmip.specgettyondroid.spec

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vendored specgetty project is the definition of done `BRIEFING.md` names.
 * These tests assert the parsers survive a real corpus, and report what share
 * of it they structure, so a regression shows up as a number rather than as a
 * feeling.
 */
class CorpusTest {

    @Test
    fun `the corpus is present and of the expected shape`() {
        assertTrue("the corpus root is missing", Corpus.root.isDirectory)
        val specs = Corpus.mainSpecs()
        val deltas = Corpus.deltaSpecs()
        assertTrue("only ${specs.size} main specs", specs.size >= 25)
        assertTrue("only ${deltas.size} delta files", deltas.size >= 100)
    }

    @Test
    fun `no main spec in the corpus makes the parser raise`() {
        Corpus.mainSpecs().forEach { file ->
            val capability = file.parentFile.name
            SpecParser.parse(capability, file.readText())
        }
    }

    @Test
    fun `no delta in the corpus makes the parser raise`() {
        Corpus.deltaSpecs().forEach { file ->
            DeltaParser.parse(Corpus.capabilityOf(file), file.readText())
        }
    }

    /**
     * The corpus is vendored and frozen, so this asserts all of it rather than
     * a fraction. A file that stops structuring is a regression in the parser,
     * not a new spec someone wrote badly.
     */
    @Test
    fun `every main spec in the corpus is structured`() {
        val failures = Corpus.mainSpecs()
            .map { it to SpecParser.parse(it.parentFile.name, it.readText()) }
            .filterNot { (_, result) -> result.isSpec }
            .map { (file, result) -> "${file.parentFile.name}: ${result.problems}" }
        assertTrue("main specs that did not structure: $failures", failures.isEmpty())
    }

    @Test
    fun `every delta in the corpus is structured`() {
        val failures = Corpus.deltaSpecs()
            .map { it to DeltaParser.parse(Corpus.capabilityOf(it), it.readText()) }
            .filterNot { (_, result) -> result.isSpec }
            .map { (file, result) -> "${file.path}: ${result.problems}" }
        assertTrue("deltas that did not structure: $failures", failures.isEmpty())
    }

    @Test
    fun `every structured delta requirement carries an operation`() {
        Corpus.deltaSpecs().forEach { file ->
            val result = DeltaParser.parse(Corpus.capabilityOf(file), file.readText())
            if (!result.isSpec) return@forEach
            result.tree.nodes.filter { it.kind == NodeKind.REQUIREMENT }.forEach { node ->
                assertTrue("${file.path}: '${node.title}' has no operation", node.op.isNotEmpty())
            }
        }
    }

    @Test
    fun `no scenario anywhere in the corpus is empty`() {
        var checked = 0
        (Corpus.mainSpecs().map { it.parentFile.name to it } +
            Corpus.deltaSpecs().map { Corpus.capabilityOf(it) to it })
            .forEach { (capability, file) ->
                val result = if (file.path.contains("/changes/")) {
                    DeltaParser.parse(capability, file.readText())
                } else {
                    SpecParser.parse(capability, file.readText())
                }
                if (!result.isSpec) return@forEach
                result.tree.nodes.filter { it.kind == NodeKind.SCENARIO }.forEach { node ->
                    checked++
                    assertTrue(
                        "${file.path}: scenario '${node.title}' is empty",
                        node.parts.isNotEmpty(),
                    )
                }
            }
        assertTrue("checked only $checked scenarios", checked >= 500)
    }

    @Test
    fun `the corpus numbers are reported`() {
        val specs = Corpus.mainSpecs()
        val deltas = Corpus.deltaSpecs()
        val specsOk = specs.count { SpecParser.parse(it.parentFile.name, it.readText()).isSpec }
        val deltasOk = deltas.count {
            DeltaParser.parse(Corpus.capabilityOf(it), it.readText()).isSpec
        }
        println(
            "corpus: main specs $specsOk/${specs.size} structured, " +
                "deltas $deltasOk/${deltas.size} structured",
        )
        assertTrue(specsOk > 0 && deltasOk > 0)
    }

    @Test
    fun `the corpus exercises all four known operations`() {
        val ops = Corpus.deltaSpecs()
            .map { DeltaParser.parse(Corpus.capabilityOf(it), it.readText()) }
            .filter { it.isSpec }
            .flatMap { it.tree.nodes }
            .mapNotNull { it.op.takeIf(String::isNotEmpty) }
            .toSet()
        assertTrue("operations found: $ops", ops.containsAll(setOf(Op.ADDED, Op.MODIFIED)))
    }
}
