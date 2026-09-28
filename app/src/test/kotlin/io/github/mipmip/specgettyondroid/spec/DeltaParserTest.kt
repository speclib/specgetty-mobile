package io.github.mipmip.specgettyondroid.spec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeltaParserTest {

    private fun parse(content: String, capability: String = "thing") =
        DeltaParser.parse(capability, content.trimIndent())

    private fun requirements(result: ParsedSpec) =
        result.tree.nodes.filter { it.kind == NodeKind.REQUIREMENT }

    @Test
    fun `a requirement carries the operation it sits under`() {
        val result = parse(
            """
            ## ADDED Requirements

            ### Requirement: New thing
            The system SHALL do it.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        assertEquals(Op.ADDED, requirements(result).single().op)
    }

    @Test
    fun `each section gives its own requirements their operation`() {
        val result = parse(
            """
            ## ADDED Requirements

            ### Requirement: A
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b

            ## MODIFIED Requirements

            ### Requirement: B
            Prose.

            #### Scenario: t
            - **WHEN** a
            - **THEN** b

            ## REMOVED Requirements

            ### Requirement: C
            Gone because it was never used.
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        assertEquals(
            listOf(Op.ADDED, Op.MODIFIED, Op.REMOVED),
            requirements(result).map { it.op },
        )
    }

    @Test
    fun `an unrecognised operation is carried through as written`() {
        val result = parse(
            """
            ## DEPRECATED Requirements

            ### Requirement: Old thing
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        assertEquals("DEPRECATED", requirements(result).single().op)
        assertFalse(Op.known.contains(requirements(result).single().op))
    }

    @Test
    fun `a requirement's body stops at the next operation heading`() {
        val result = parse(
            """
            ## REMOVED Requirements

            ### Requirement: Gone
            No longer needed.

            ## ADDED Requirements

            ### Requirement: New
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        val removed = requirements(result).first { it.op == Op.REMOVED }
        assertEquals("No longer needed.", removed.body)
        assertFalse(removed.body.contains("ADDED"))
    }

    @Test
    fun `a requirement under no delta header is reported`() {
        val result = parse(
            """
            ### Requirement: Orphan
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b

            ## ADDED Requirements

            ### Requirement: Fine
            Prose.

            #### Scenario: t
            - **WHEN** a
            - **THEN** b
            """,
        )
        val problem = result.problems.firstOrNull { it.text.contains("under no delta header") }
        assertTrue("${result.problems}", problem != null)
        assertTrue(problem!!.line > 0)
        listOf("ADDED", "MODIFIED", "REMOVED", "RENAMED").forEach {
            assertTrue("the reason should name $it", problem.text.contains(it))
        }
        assertEquals("a file with problems yields no outline", 0, result.tree.nodes.size)
    }

    @Test
    fun `a delta with no requirements is reported`() {
        val result = parse("## ADDED Requirements\n")
        assertTrue(result.problems.any { it.text.contains("no requirements") })
    }

    @Test
    fun `a main spec heading in a change is reported`() {
        val result = parse(
            """
            ## Requirements

            ### Requirement: A
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
        )
        val problem = result.problems.firstOrNull {
            it.text.contains("heading a main spec uses")
        }
        assertTrue("${result.problems}", problem != null)
        assertTrue(problem!!.text.contains("archived"))
    }

    @Test
    fun `a delta with no purpose is fine`() {
        val result = parse(
            """
            ## ADDED Requirements

            ### Requirement: A
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        assertTrue(result.tree.nodes.none { it.kind == NodeKind.PURPOSE })
    }

    @Test
    fun `a purpose for a new capability becomes the first node`() {
        val result = parse(
            """
            ## Purpose
            What this new capability is for.

            ## ADDED Requirements

            ### Requirement: A
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        assertEquals(NodeKind.PURPOSE, result.tree.nodes.first().kind)
        assertEquals("What this new capability is for.", result.tree.nodes.first().body)
    }

    @Test
    fun `an empty purpose heading is reported`() {
        val result = parse(
            """
            ## Purpose

            ## ADDED Requirements

            ### Requirement: A
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
        )
        val problem = result.problems.firstOrNull { it.text.contains("Purpose` section is empty") }
        assertTrue("${result.problems}", problem != null)
        assertTrue(problem!!.text.contains("archive copies it"))
    }

    @Test
    fun `an added requirement with no scenario is reported`() {
        val result = parse(
            """
            ## ADDED Requirements

            ### Requirement: A
            Prose and nothing else.
            """,
        )
        assertTrue("${result.problems}", result.problems.any { it.text.contains("no scenario") })
    }

    @Test
    fun `a modified requirement with no scenario is reported`() {
        val result = parse(
            """
            ## MODIFIED Requirements

            ### Requirement: A
            Prose and nothing else.
            """,
        )
        assertTrue("${result.problems}", result.problems.any { it.text.contains("no scenario") })
    }

    @Test
    fun `a removed or renamed requirement may go without a scenario`() {
        listOf(Op.REMOVED, Op.RENAMED).forEach { op ->
            val result = parse(
                """
                ## $op Requirements

                ### Requirement: A
                The reason it went.
                """,
            )
            assertTrue("$op: ${result.problems}", result.isSpec)
            assertEquals(op, requirements(result).single().op)
        }
    }

    @Test
    fun `every node names the capability it came from`() {
        val result = parse(
            """
            ## Purpose
            A purpose.

            ## ADDED Requirements

            ### Requirement: A
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
            """,
            capability = "spec-parsing",
        )
        assertTrue("${result.problems}", result.isSpec)
        assertTrue(result.tree.nodes.all { it.capability == "spec-parsing" })
        assertTrue(result.tree.nodes.all { it.path.startsWith("cap/spec-parsing/") })
    }

    @Test
    fun `two deltas naming the same requirement do not collide`() {
        val body = """
            ## ADDED Requirements

            ### Requirement: Shared
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
        """
        val one = parse(body, capability = "first").tree
        val two = parse(body, capability = "second").tree
        val a = one.nodes.first { it.kind == NodeKind.REQUIREMENT }.path
        val b = two.nodes.first { it.kind == NodeKind.REQUIREMENT }.path
        assertFalse(a == b)
    }

    @Test
    fun `scenario content follows the same rules as a main spec`() {
        val result = parse(
            """
            ## ADDED Requirements

            ### Requirement: A
            Prose.

            #### Scenario: mixed
            - **WHEN** the first thing

            **Rationale**: because.

            - **THEN** the second thing

            #### Scenario: empty heading

            #### Scenario: last
            - **WHEN** a
            - **THEN** b
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        val scenarios = result.tree.nodes.filter { it.kind == NodeKind.SCENARIO }
        assertEquals(listOf("mixed", "last"), scenarios.map { it.title })
        assertEquals(3, scenarios.first().parts.size)
        assertEquals(PartKind.PROSE, scenarios.first().parts[1].kind)
    }

    @Test
    fun `a heading inside a fence is not structure`() {
        val result = parse(
            """
            ## ADDED Requirements

            ### Requirement: A
            Prose.

            #### Scenario: s
            - **WHEN** a page is generated
            - **THEN** it looks like this:

            ```markdown
            ## MODIFIED Requirements

            ### Requirement: not a requirement
            ```

            - **AND** nothing in the block is structure
            """,
        )
        assertTrue("${result.problems}", result.isSpec)
        assertEquals(1, result.tree.requirementCount)
        assertEquals(Op.ADDED, requirements(result).single().op)
    }
}
