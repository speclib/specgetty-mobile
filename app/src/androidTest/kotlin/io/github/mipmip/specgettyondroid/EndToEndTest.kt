package io.github.mipmip.specgettyondroid

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
import io.github.mipmip.specgettyondroid.store.labelFor
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
 * The journey `BRIEFING.md` names: add a repo, browse changes, open a delta
 * diff, open a spec. Against a real repository served over HTTP on loopback, so
 * the clone is a real shallow clone rather than a copy.
 *
 * An in-memory catalog rather than DataStore and the Keystore: those are their
 * own concern, and mixing them in would make a failure ambiguous between the
 * journey and the storage.
 */
class EndToEndTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var root: File
    private lateinit var server: GitHttpServer
    private lateinit var repository: ProjectRepository

    private val delta = """
        ## MODIFIED Requirements

        ### Requirement: A settled thing
        The system SHALL do the new thing.

        #### Scenario: It does
        - **WHEN** asked
        - **THEN** the new outcome
    """.trimIndent() + "\n"

    private val mainSpec = """
        # thing Specification

        ## Purpose
        What thing is for, at enough length to count as a purpose.

        ## Requirements

        ### Requirement: A settled thing
        The system SHALL do the old thing.

        #### Scenario: It does
        - **WHEN** asked
        - **THEN** the old outcome
    """.trimIndent() + "\n"

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(context.cacheDir, "e2e-${System.nanoTime()}").apply { mkdirs() }
        AndroidGit.install(File(root, "git-config"))

        // Built from an empty project rather than the shared fixture, so the
        // numbers this test asserts are the ones written here and nowhere else.
        val remote = LocalRemote(File(root, "remote")).initEmptyProject().apply {
            commit("openspec/config.yaml", "schema: spec-driven\n", "config")
            commit("openspec/specs/thing/spec.md", mainSpec, "the main spec")
            commit("openspec/changes/reword-the-thing/proposal.md", "## Why\n\nBecause.\n", "p")
            commit(
                "openspec/changes/reword-the-thing/tasks.md",
                "## 1. Work\n\n- [x] 1.1 Done\n- [ ] 1.2 Not yet\n",
                "t",
            )
            commit("openspec/changes/reword-the-thing/specs/thing/spec.md", delta, "d")
        }
        server = GitHttpServer(remote.dir, null).start()

        repository = ProjectRepository(
            store = RepoStore(File(root, "repos")),
            catalog = InMemoryCatalog(),
        )
    }

    @After
    fun tearDown() {
        server.stop()
        root.deleteRecursively()
    }

    private fun start() {
        compose.setContent {
            SpecgettyTheme { SpecgettyNavHost(repository) }
        }
    }

    /** Add the served repository and wait until its statistics are on screen. */
    private fun addTheRepository(url: String = server.url) {
        compose.awaitDescription("Add a repository")
        compose.firstWithDescription("Add a repository").performClick()

        compose.awaitDescription("Repository URL")
        compose.firstWithDescription("Repository URL").performTextInput(url)
        compose.firstWithDescription("Confirm adding the repository").performClick()
    }

    private fun openTheProject() {
        compose.awaitText("specs", substring = true)
        compose.firstWithText("specs", substring = true).performClick()
        compose.awaitText("Overview")
    }

    @Test
    fun addARepository() {
        start()
        compose.awaitText("No repositories yet")
        addTheRepository()

        // One spec, one active change, no archive, and one of two tasks done.
        compose.awaitText("1 specs", substring = true)
        compose.awaitText("tasks 1/2", substring = true)
    }

    @Test
    fun browseTheChanges() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Changes").performClick()
        compose.awaitText("reword-the-thing")
        compose.awaitText("1/2 tasks", substring = true)
    }

    @Test
    fun openADeltaDiff() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Changes").performClick()
        compose.awaitText("reword-the-thing")
        compose.firstWithText("reword-the-thing").performClick()

        // The change's own Specs tab lists the capability it touches.
        compose.awaitText("Specs")
        compose.firstWithText("Specs").performClick()
        compose.awaitText("thing")
        compose.firstWithText("thing").performClick()

        // The delta outline, marked MODIFIED, and its comparison.
        compose.awaitText("MODIFIED")
        compose.firstWithText("A settled thing").performClick()
        compose.awaitText("Difference")
        compose.awaitText("Original")
        compose.awaitText("Proposed")
    }

    @Test
    fun theDiffShowsBothSides() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Changes").performClick()
        compose.awaitText("reword-the-thing")
        compose.firstWithText("reword-the-thing").performClick()
        compose.firstWithText("Specs").performClick()
        compose.awaitText("thing")
        compose.firstWithText("thing").performClick()
        compose.awaitText("MODIFIED")
        compose.firstWithText("A settled thing").performClick()

        // The old line and the new line are both on the difference.
        compose.awaitText("the old outcome", substring = true)
        compose.awaitText("the new outcome", substring = true)
    }

    @Test
    fun openASpec() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Specs").performClick()
        compose.awaitText("thing")
        compose.firstWithText("thing").performClick()

        // The outline of the spec itself.
        compose.awaitText("Purpose")
        compose.awaitText("A settled thing")

        compose.firstWithText("It does").performClick()
        compose.awaitText("the old outcome", substring = true)
    }

    @Test
    fun aSpecCardStepsToTheNextAndPreviousNode() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Specs").performClick()
        compose.awaitText("thing")
        compose.firstWithText("thing").performClick()

        // Open the first node's card, then walk forward through the outline.
        compose.awaitText("Purpose")
        compose.firstWithText("Purpose").performClick()
        compose.awaitText("Next", substring = true)

        compose.firstWithText("Next", substring = true).performClick()
        compose.awaitText("A settled thing")

        compose.firstWithText("Next", substring = true).performClick()
        compose.awaitText("It does")

        // And back again.
        compose.firstWithText("Previous", substring = true).performClick()
        compose.awaitText("A settled thing")
    }

    @Test
    fun aChangeShowsItsTasksAsBoxes() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Changes").performClick()
        compose.awaitText("reword-the-thing")
        compose.firstWithText("reword-the-thing").performClick()

        compose.awaitText("Tasks")
        compose.firstWithText("Tasks").performClick()
        compose.awaitText("1 of 2 done")
        compose.awaitText("1.1 Done")
        compose.awaitText("1.2 Not yet")
    }

    @Test
    fun aRepositoryWithNoOpenSpecProjectSaysSo() {
        val bare = LocalRemote(File(root, "bare")).initBare()
        val bareServer = GitHttpServer(bare.dir, null).start()
        try {
            start()
            addTheRepository(bareServer.url)
            compose.awaitText("No OpenSpec project here")
        } finally {
            bareServer.stop()
        }
    }

    @Test
    fun anUnreachableHostSaysSo() {
        start()
        addTheRepository("https://no-such-host.invalid/a/b.git")
        compose.awaitText("The repository could not be reached.")
    }

    @Test
    fun anSshUrlIsRefusedBeforeAnythingIsAttempted() {
        start()
        compose.awaitDescription("Add a repository")
        compose.firstWithDescription("Add a repository").performClick()
        compose.awaitDescription("Repository URL")
        compose.firstWithDescription("Repository URL")
            .performTextInput("git@github.com:speclib/specgetty.git")
        compose.firstWithDescription("Confirm adding the repository").performClick()

        compose.awaitText("SSH URLs are not supported", substring = true)
    }
}

/** The repository list, without DataStore. Persistence is its own concern. */
private class InMemoryCatalog : RepoCatalog {

    private val state = MutableStateFlow(RepoList())
    private val tokens = mutableMapOf<String, String>()

    override val repos: Flow<RepoList> = state

    override suspend fun current(): RepoList = repos.first()

    override suspend fun add(url: String, label: String, token: String?): RepoConfig {
        val trimmed = url.trim()
        val config = RepoConfig(
            id = repoIdFor(trimmed),
            url = trimmed,
            label = label.ifBlank { labelFor(trimmed) },
            hasToken = !token.isNullOrBlank(),
        )
        if (!token.isNullOrBlank()) tokens[config.id] = token
        state.update { it.add(config) }
        return config
    }

    override suspend fun remove(id: String) {
        tokens.remove(id)
        state.update { it.remove(id) }
    }

    override suspend fun activate(id: String) = state.update { it.activate(id) }

    override suspend fun tokenFor(id: String): String? = tokens[id]
}
