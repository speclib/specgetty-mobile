package io.github.mipmip.specgettyondroid.repo

import org.eclipse.jgit.api.Git
import java.io.File

/** A real git repository on disk, served over `file://`, built with JGit itself. */
class LocalRemote(private val dir: File) {

    val url: String get() = dir.toURI().toString()

    fun init(): LocalRemote {
        dir.mkdirs()
        Git.init().setDirectory(dir).setInitialBranch("main").call().use { git ->
            write("openspec/config.yaml", "schema: spec-driven\n")
            write("openspec/specs/repo-store/spec.md", spec("repo-store"))
            write("openspec/changes/first-change/proposal.md", "## Why\n\nBecause.\n")
            write("openspec/changes/first-change/tasks.md", "- [x] 1.1 Done\n- [ ] 1.2 Not done\n")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("first").setSign(false).call()
        }
        return this
    }

    fun initBare(): LocalRemote {
        dir.mkdirs()
        Git.init().setDirectory(dir).setInitialBranch("main").call().use { git ->
            write("README.md", "Nothing to see here.\n")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("only a readme").setSign(false).call()
        }
        return this
    }

    fun initEmptyProject(): LocalRemote {
        dir.mkdirs()
        Git.init().setDirectory(dir).setInitialBranch("main").call().use { git ->
            write("openspec/.keep", "")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("an empty project").setSign(false).call()
        }
        return this
    }

    fun commit(path: String, content: String, message: String) {
        Git.open(dir).use { git ->
            write(path, content)
            git.add().addFilepattern(".").call()
            git.commit().setMessage(message).setSign(false).call()
        }
    }

    fun removeAndCommit(path: String, message: String) {
        Git.open(dir).use { git ->
            File(dir, path).delete()
            git.add().addFilepattern(".").setUpdate(true).call()
            git.commit().setMessage(message).setSign(false).call()
        }
    }

    fun commitCount(): Int = Git.open(dir).use { it.log().call().count() }

    private fun write(path: String, content: String) {
        val file = File(dir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    companion object {
        fun spec(capability: String) = """
            # $capability Specification

            ## Purpose
            The purpose of $capability.

            ## Requirements

            ### Requirement: Something holds

            The system SHALL do the thing.

            #### Scenario: It does

            - **WHEN** asked
            - **THEN** it does
        """.trimIndent() + "\n"
    }
}
