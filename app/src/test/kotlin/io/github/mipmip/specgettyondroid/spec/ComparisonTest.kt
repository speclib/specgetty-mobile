package io.github.mipmip.specgettyondroid.spec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequirementSourceTest {

    private val spec = """
        # thing Specification

        ## Purpose
        A purpose long enough to be one.

        ## Requirements

        ### Requirement: The first thing
        Its prose.

        #### Scenario: One
        - **WHEN** a
        - **THEN** b

        ### Requirement: The second thing
        More prose.

        #### Scenario: Two
        - **WHEN** c
        - **THEN** d

        ## Notes

        Something after the section.
    """.trimIndent()

    @Test
    fun `a requirement is sliced from its heading to the next one`() {
        val text = RequirementSource.extract(spec, "The first thing")!!
        assertTrue(text.startsWith("### Requirement: The first thing"))
        assertTrue(text.contains("Its prose."))
        assertTrue(text.contains("#### Scenario: One"))
        assertFalse("the next requirement is not included", text.contains("The second thing"))
    }

    @Test
    fun `the last requirement of a section ends at the next section heading`() {
        val text = RequirementSource.extract(spec, "The second thing")!!
        assertTrue(text.contains("#### Scenario: Two"))
        assertFalse(text.contains("## Notes"))
        assertFalse(text.contains("Something after the section."))
    }

    @Test
    fun `a requirement at the end of the file runs to the end`() {
        val src = """
            ## Requirements

            ### Requirement: Only one
            Its prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
        """.trimIndent()
        val text = RequirementSource.extract(src, "Only one")!!
        assertTrue(text.trimEnd().endsWith("- **THEN** b"))
    }

    @Test
    fun `a heading inside a fenced block does not end the slice`() {
        val src = """
            ## Requirements

            ### Requirement: Shows its own format
            Prose.

            #### Scenario: The shape
            - **WHEN** drawn
            - **THEN** it looks like this:

            ```markdown
            ## Requirements

            ### Requirement: not a requirement
            ```

            - **AND** the block is content

            ### Requirement: The real next one
            Prose.
        """.trimIndent()
        val text = RequirementSource.extract(src, "Shows its own format")!!
        assertTrue("the fenced block is inside the slice", text.contains("not a requirement"))
        assertTrue(text.contains("the block is content"))
        assertFalse(text.contains("The real next one"))
    }

    @Test
    fun `a name that is not in the file yields nothing`() {
        assertNull(RequirementSource.extract(spec, "Nothing of the sort"))
    }

    @Test
    fun `the name has to match exactly`() {
        assertNull(RequirementSource.extract(spec, "The first"))
    }

    @Test
    fun `a delta's requirement is sliced the same way`() {
        val delta = """
            ## MODIFIED Requirements

            ### Requirement: The first thing
            Its prose, reworded.

            #### Scenario: One
            - **WHEN** a
            - **THEN** something else

            ## ADDED Requirements

            ### Requirement: Brand new
            Prose.
        """.trimIndent()
        val text = RequirementSource.extract(delta, "The first thing")!!
        assertTrue(text.contains("reworded"))
        assertFalse(text.contains("Brand new"))
        assertFalse(text.contains("## ADDED Requirements"))
    }
}

class ComparisonTest {

    private fun comparison(original: String, proposed: String) =
        Comparison("cap", "req", original.trimIndent(), proposed.trimIndent())

    @Test
    fun `identical text produces only common lines`() {
        val text = "### Requirement: A\nProse.\n"
        val c = Comparison("cap", "A", text, text)
        assertTrue(c.isIdentical)
        assertTrue(c.diff.all { it.mark == DiffMark.COMMON })
    }

    @Test
    fun `a changed line appears on both sides`() {
        val c = comparison(
            """
            ### Requirement: A
            The system SHALL do the old thing.
            """,
            """
            ### Requirement: A
            The system SHALL do the new thing.
            """,
        )
        assertFalse(c.isIdentical)
        val removed = c.diff.filter { it.mark == DiffMark.ORIGINAL_ONLY }.map { it.text }
        val added = c.diff.filter { it.mark == DiffMark.PROPOSED_ONLY }.map { it.text }
        assertEquals(listOf("The system SHALL do the old thing."), removed)
        assertEquals(listOf("The system SHALL do the new thing."), added)
    }

    @Test
    fun `an added line is proposed only`() {
        val c = comparison(
            """
            ### Requirement: A
            Prose.
            """,
            """
            ### Requirement: A
            Prose.
            And more of it.
            """,
        )
        assertEquals(
            listOf("And more of it."),
            c.diff.filter { it.mark == DiffMark.PROPOSED_ONLY }.map { it.text },
        )
        assertTrue(c.diff.none { it.mark == DiffMark.ORIGINAL_ONLY })
    }

    @Test
    fun `a removed line is original only`() {
        val c = comparison(
            """
            ### Requirement: A
            Prose.
            Something dropped.
            """,
            """
            ### Requirement: A
            Prose.
            """,
        )
        assertEquals(
            listOf("Something dropped."),
            c.diff.filter { it.mark == DiffMark.ORIGINAL_ONLY }.map { it.text },
        )
    }

    @Test
    fun `every line of both sides is accounted for`() {
        val c = comparison(
            """
            one
            two
            three
            """,
            """
            one
            changed
            three
            four
            """,
        )
        val kept = c.diff.filter { it.mark != DiffMark.PROPOSED_ONLY }.map { it.text }
        assertEquals(listOf("one", "two", "three"), kept)

        val proposed = c.diff.filter { it.mark != DiffMark.ORIGINAL_ONLY }.map { it.text }
        assertEquals(listOf("one", "changed", "three", "four"), proposed)
    }

    @Test
    fun `the common lines keep their order`() {
        val c = comparison(
            """
            a
            b
            c
            d
            """,
            """
            a
            b
            X
            d
            """,
        )
        val common = c.diff.filter { it.mark == DiffMark.COMMON }.map { it.text }
        assertEquals(listOf("a", "b", "d"), common)
    }
}
