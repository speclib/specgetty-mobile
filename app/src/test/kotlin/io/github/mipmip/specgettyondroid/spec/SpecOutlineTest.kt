package io.github.mipmip.specgettyondroid.spec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecOutlineTest {

    @Test
    fun `the outline is purpose then requirements each with its scenarios`() {
        val tree = Fixtures.parse("canonical")
        val shape = tree.nodes.map { it.kind to it.title }
        assertEquals(
            listOf(
                NodeKind.PURPOSE to "Purpose",
                NodeKind.REQUIREMENT to "The first thing",
                NodeKind.SCENARIO to "One clause",
                NodeKind.SCENARIO to "A clause over three lines",
                NodeKind.REQUIREMENT to "The second thing",
                NodeKind.SCENARIO to "Under the second",
            ),
            shape,
        )
    }

    @Test
    fun `a requirement's body stops at its first scenario`() {
        val tree = Fixtures.parse("canonical")
        val first = tree.nodes.first { it.kind == NodeKind.REQUIREMENT }
        assertEquals("Its prose, which the card shows.", first.body)
        assertTrue(!first.body.contains("Scenario"))
    }

    @Test
    fun `the purpose body is the text under the heading`() {
        val tree = Fixtures.parse("canonical")
        val purpose = tree.nodes.first { it.kind == NodeKind.PURPOSE }
        assertEquals(
            "What this capability is for, in a sentence long enough to be a purpose.",
            purpose.body,
        )
    }

    @Test
    fun `a node keeps its path across a re-parse`() {
        val before = Fixtures.parse("canonical")
        val path = before.nodes.last().path

        val edited = Fixtures.read("canonical").replace(
            "### Requirement: The first thing",
            "### Requirement: A new one\nIts prose.\n\n#### Scenario: Inserted\n" +
                "- **WHEN** x\n- **THEN** y\n\n### Requirement: The first thing",
        )
        val after = SpecParser.parse("canonical", edited)
        assertTrue("${after.problems}", after.isSpec)
        assertTrue("the path was lost", after.tree.indexOfPath(path) >= 0)
    }

    @Test
    fun `a scenario path carries its requirement, so two of a name do not collide`() {
        val src = """
            ## Purpose
            A purpose long enough to count as one.

            ## Requirements

            ### Requirement: First
            Prose.

            #### Scenario: Shared name
            - **WHEN** a
            - **THEN** b

            ### Requirement: Second
            Prose.

            #### Scenario: Shared name
            - **WHEN** c
            - **THEN** d
        """.trimIndent()
        val tree = SpecParser.parse("x", src).tree
        val paths = tree.nodes.filter { it.kind == NodeKind.SCENARIO }.map { it.path }
        assertEquals(2, paths.toSet().size)
        assertEquals(listOf("req/First/scen/Shared name", "req/Second/scen/Shared name"), paths)
    }

    @Test
    fun `an unknown path is not found`() {
        assertEquals(-1, Fixtures.parse("canonical").indexOfPath("req/nothing"))
    }

    @Test
    fun `counts report what the tree holds`() {
        val tree = Fixtures.parse("canonical")
        assertEquals(2, tree.requirementCount)
        assertEquals(3, tree.scenarioCount)
    }

    @Test
    fun `carriage returns and a byte order mark do not defeat the grammar`() {
        val src = "\uFEFF" + Fixtures.read("canonical").replace("\n", "\r\n")
        val result = SpecParser.parse("x", src)
        assertTrue("${result.problems}", result.isSpec)
        assertEquals(2, result.tree.requirementCount)
    }

    @Test
    fun `trailing whitespace on a heading does not hide it`() {
        val src = Fixtures.read("canonical").replace("## Requirements", "## Requirements   ")
        assertTrue(SpecParser.parse("x", src).isSpec)
    }

    @Test
    fun `a requirements section ends at the next level two heading`() {
        val src = """
            ## Purpose
            A purpose long enough to count as one.

            ## Requirements

            ### Requirement: Inside
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b

            ## Notes

            ### Requirement: Outside
            Prose.

            #### Scenario: t
            - **WHEN** a
            - **THEN** b
        """.trimIndent()
        val result = SpecParser.parse("x", src)
        assertTrue(
            "the requirement below `## Notes` is outside the section",
            result.problems.any { it.text.contains("outside the `## Requirements` section") },
        )
    }
}
