package io.github.mipmip.specgettyondroid

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.AndroidGit
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.RepoCatalog
import io.github.mipmip.specgettyondroid.store.RepoConfig
import io.github.mipmip.specgettyondroid.store.RepoList
import io.github.mipmip.specgettyondroid.store.repoIdFor
import io.github.mipmip.specgettyondroid.ui.SpecgettyNavHost
import io.github.mipmip.specgettyondroid.ui.theme.SpecgettyTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Takes the store screenshots by driving the real app, so what is published is
 * what the app draws rather than a mock-up of it.
 *
 * The project it photographs is a small invented one rather than somebody's
 * real repository: a screenshot is published, and a real project's contents
 * are not this project's to publish.
 *
 * Files land in `/data/local/tmp`; `scripts/screenshots.sh` pulls them into the
 * Fastlane metadata.
 */
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var root: File
    private lateinit var server: GitHttpServer
    private lateinit var repository: ProjectRepository
    private var ime: String? = null

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(context.cacheDir, "shots-${System.nanoTime()}").apply { mkdirs() }
        AndroidGit.install(File(root, "git-config"))
        disableTheKeyboard()

        val remote = LocalRemote(File(root, "remote")).initEmptyProject().apply {
            commit("openspec/config.yaml", "schema: spec-driven\n", "config")
            commit("openspec/specs/repo-url-capture/spec.md", capture, "s1")
            commit("openspec/specs/spec-parsing/spec.md", parsing, "s2")
            commit("openspec/specs/task-parsing/spec.md", tasks, "s3")
            commit("openspec/changes/reword-the-capture/proposal.md", proposal, "p")
            commit("openspec/changes/reword-the-capture/tasks.md", taskList, "t")
            commit("openspec/changes/reword-the-capture/specs/repo-url-capture/spec.md", delta, "d")
            commit("openspec/changes/archive/2026-09-21-add-the-task-parser/proposal.md", proposal, "a1")
            commit("openspec/changes/archive/2026-08-14-add-the-spec-parser/proposal.md", proposal, "a2")
        }
        server = GitHttpServer(remote.dir, null).start()

        repository = ProjectRepository(
            store = RepoStore(File(root, "repos")),
            catalog = ShotCatalog(),
        )
    }

    @After
    fun tearDown() {
        restoreTheKeyboard()
        server.stop()
        root.deleteRecursively()
    }

    /**
     * Through `screencap` rather than `takeScreenshot`, because the file has to
     * outlive the run. Gradle uninstalls both APKs when the suite finishes, and
     * anything written into the app's own storage goes with them. `screencap`
     * runs as the shell user and writes somewhere the uninstall does not touch.
     */
    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
            it.readBytes().toString(Charsets.UTF_8).trim()
        }
    }

    /**
     * The soft keyboard covers half of what a store screenshot is meant to
     * show, and there is no gentle way to dismiss it here: `closeSoftKeyboard`
     * needs an Espresso view root that a Compose-only activity does not have,
     * and BACK finishes the test's host activity rather than the keyboard. So
     * the input method is turned off for the run and turned back on after.
     */
    private fun disableTheKeyboard() {
        ime = shell("settings get secure default_input_method").takeIf { it.isNotBlank() }
        ime?.let { shell("ime disable $it") }
    }

    private fun restoreTheKeyboard() {
        ime?.let { shell("ime enable $it") }
    }

    private fun shoot(name: String) {
        compose.waitForIdle()
        val target = "$SHELL_DIR/$PREFIX$name.png"
        // Read to the end, so the command has finished before the next step
        // changes what is on screen.
        shell("screencap -p $target")
    }

    companion object {
        const val SHELL_DIR = "/data/local/tmp"
        const val PREFIX = "specgetty-"
    }

    @Test
    fun takeTheScreenshots() {
        compose.setContent { SpecgettyTheme { SpecgettyNavHost(repository) } }

        compose.awaitDescription("Add a repository")
        compose.firstWithDescription("Add a repository").performClick()
        compose.awaitDescription("Repository URL")
        compose.firstWithDescription("Repository URL").performTextInput(server.url)
        compose.firstWithDescription("Confirm adding the repository").performClick()

        compose.awaitText("3 specs", substring = true)
        shoot("1_repositories")

        compose.firstWithText("specs", substring = true).performClick()
        compose.awaitText("Overview")
        shoot("2_overview")

        compose.firstWithText("Changes").performClick()
        compose.awaitText("reword-the-capture")
        shoot("3_changes")

        compose.firstWithText("reword-the-capture").performClick()
        compose.awaitText("Tasks")
        compose.firstWithText("Tasks").performClick()
        compose.awaitText("done", substring = true)
        shoot("4_tasks")

        compose.firstWithText("Specs").performClick()
        compose.awaitText("repo-url-capture")
        compose.firstWithText("repo-url-capture").performClick()
        compose.awaitText("MODIFIED")
        compose.firstWithText("A web address is extracted from arbitrary text").performClick()
        compose.awaitText("Difference")
        shoot("5_difference")
    }

    private val capture = """
        # repo-url-capture Specification

        ## Purpose
        Getting a repository URL into the app without typing it.

        ## Requirements

        ### Requirement: A web address is extracted from arbitrary text

        The system SHALL find the first `http` or `https` address in a piece of
        text and SHALL report that no address was found when there is none.

        #### Scenario: Text surrounding a URL

        - **WHEN** the text is `Look at https://example.test/a nice one`
        - **THEN** the extracted address is `https://example.test/a`

        #### Scenario: No address

        - **WHEN** the text contains no `http` or `https` address
        - **THEN** the result reports that none was found
    """.trimIndent() + "\n"

    private val parsing = """
        # spec-parsing Specification

        ## Purpose
        Reading a spec by the rules OpenSpec's own parser uses.

        ## Requirements

        ### Requirement: A scenario's content is never dropped

        The system SHALL carry a scenario's content in full and in the order the
        file gives it.

        #### Scenario: A scenario written as prose

        - **WHEN** a scenario's content is a paragraph carrying no keyword
        - **THEN** that paragraph is in the parsed content
    """.trimIndent() + "\n"

    private val tasks = """
        # task-parsing Specification

        ## Purpose
        Counting the checkboxes of a tasks file.

        ## Requirements

        ### Requirement: A task is a checkbox at column zero

        The system SHALL count a line as a task only when it begins, at column
        zero, with an unchecked or checked box.

        #### Scenario: An indented checkbox

        - **WHEN** a checkbox line is indented
        - **THEN** it is not a task of its own
    """.trimIndent() + "\n"

    private val delta = """
        ## MODIFIED Requirements

        ### Requirement: A web address is extracted from arbitrary text

        The system SHALL find the first `http` or `https` address in a piece of
        text, SHALL trim trailing punctuation from it, and SHALL report that no
        address was found when there is none.

        #### Scenario: Text surrounding a URL

        - **WHEN** the text is `Look at https://example.test/a nice one`
        - **THEN** the extracted address is `https://example.test/a`

        #### Scenario: Trailing punctuation

        - **WHEN** an address is followed by a full stop or a closing bracket
        - **THEN** that character is not part of the extracted address
    """.trimIndent() + "\n"

    private val proposal = """
        ## Why

        A QR code generated from a browser carries the page URL, not the clone
        URL, and a clone of the page URL fails thirty seconds later.

        ## What Changes

        - Trim trailing punctuation from an extracted address.
        - Say so in the spec, rather than leaving it to the reader.
    """.trimIndent() + "\n"

    private val taskList = """
        ## 1. Extraction

        - [x] 1.1 Find the first address in arbitrary text
        - [x] 1.2 Trim trailing punctuation from the match
        - [ ] 1.3 Report absence rather than an empty string

        ## 2. Proof

        - [ ] 2.1 A table of every punctuation mark that can follow a URL
    """.trimIndent() + "\n"
}

private class ShotCatalog : RepoCatalog {

    private val state = MutableStateFlow(RepoList())

    override val repos: Flow<RepoList> = state

    override suspend fun current(): RepoList = repos.first()

    override suspend fun add(url: String, label: String, token: String?): RepoConfig {
        val trimmed = url.trim()
        val config = RepoConfig(
            id = repoIdFor(trimmed),
            // A name a reader recognises, rather than `repo.git` from the URL.
            label = label.ifBlank { "specgetty" },
            url = trimmed,
        )
        state.update { it.add(config) }
        return config
    }

    override suspend fun remove(id: String) = state.update { it.remove(id) }

    override suspend fun activate(id: String) = state.update { it.activate(id) }

    override suspend fun tokenFor(id: String): String? = null
}
