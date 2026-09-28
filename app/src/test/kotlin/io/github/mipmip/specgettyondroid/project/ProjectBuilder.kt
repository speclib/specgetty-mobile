package io.github.mipmip.specgettyondroid.project

import java.io.File

/** Builds a project on disk, so a test reads like the directory tree it means. */
class ProjectBuilder(private val root: File) {

    val openspec: File get() = File(root, "openspec")

    fun spec(capability: String, body: String = defaultSpec(capability)) = apply {
        write("openspec/specs/$capability/spec.md", body)
    }

    fun emptySpecDir(capability: String) = apply {
        File(openspec, "specs/$capability").mkdirs()
    }

    fun change(
        name: String,
        archived: Boolean = false,
        artifacts: Map<String, String> = mapOf("proposal.md" to "## Why\n\nBecause.\n"),
        capabilities: List<String> = emptyList(),
        schema: String? = "spec-driven",
    ) = apply {
        val base = if (archived) "openspec/changes/archive/$name" else "openspec/changes/$name"
        File(root, base).mkdirs()
        artifacts.forEach { (file, body) -> write("$base/$file", body) }
        capabilities.forEach { write("$base/specs/$it/spec.md", delta(it)) }
        if (schema != null) write("$base/.openspec.yaml", "schema: $schema\ncreated: 2026-09-28\n")
    }

    fun config(name: String = "config.yaml", body: String = "schema: spec-driven\n") = apply {
        write("openspec/$name", body)
    }

    fun projectMarkdown(body: String = "# The project\n") = apply {
        write("openspec/project.md", body)
    }

    fun emptyProject() = apply { openspec.mkdirs() }

    fun write(path: String, body: String) = apply {
        val f = File(root, path)
        f.parentFile?.mkdirs()
        f.writeText(body)
    }

    fun load(): ProjectInfo = ProjectLoader.loadProject(openspec)

    companion object {
        fun defaultSpec(capability: String) = """
            # $capability Specification

            ## Purpose
            What $capability is for, at enough length to be a purpose.

            ## Requirements

            ### Requirement: It works

            The system SHALL work.

            #### Scenario: It does

            - **WHEN** asked
            - **THEN** it works
        """.trimIndent() + "\n"

        fun delta(capability: String) = """
            ## ADDED Requirements

            ### Requirement: Something new in $capability

            The system SHALL do it.

            #### Scenario: It does

            - **WHEN** asked
            - **THEN** done
        """.trimIndent() + "\n"

        fun tasks(done: Int, total: Int): String = buildString {
            appendLine("## 1. Work")
            appendLine()
            repeat(total) { i ->
                appendLine("- [${if (i < done) "x" else " "}] 1.${i + 1} Task ${i + 1}")
            }
        }
    }
}
