package io.github.mipmip.specgettyondroid

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.platform.app.InstrumentationRegistry
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.AndroidGit
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import io.github.mipmip.specgettyondroid.ui.SpecgettyNavHost
import io.github.mipmip.specgettyondroid.ui.theme.SpecgettyTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The gesture on each screen that reads a repository. What the unit tests
 * cannot answer is whether a pull reaches the fetch at all: the indicator rides
 * nested scroll, so a pane that reports no drag has a dead gesture however
 * right the view model is.
 */
class RefreshGestureTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var root: File
    private lateinit var server: GitHttpServer
    private lateinit var remote: LocalRemote
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(context.cacheDir, "refresh-${System.nanoTime()}").apply { mkdirs() }
        AndroidGit.install(File(root, "git-config"))

        remote = LocalRemote(File(root, "remote")).initEmptyProject().apply {
            commit("openspec/config.yaml", "schema: spec-driven\n", "config")
            commit("openspec/specs/thing/spec.md", LocalRemote.spec("thing"), "the main spec")
            commit("openspec/changes/first/proposal.md", "## Why\n\nThe first reason.\n", "p")
        }
        server = GitHttpServer(remote.dir, null).start()

        repository = ProjectRepository(
            store = RepoStore(File(root, "repos")),
            catalog = RepoRegistry(InstrumentedPreferences(), InstrumentedVault()),
        )
    }

    @After
    fun tearDown() {
        server.stop()
        root.deleteRecursively()
    }

    private fun start() {
        compose.setContent {
            SpecgettyTheme {
                SpecgettyNavHost(repository, OfflineAuth.deviceFlow, OfflineAuth.installations)
            }
        }
    }

    private fun addTheRepository() {
        compose.awaitDescription("Add a repository")
        compose.firstWithDescription("Add a repository").performClick()
        compose.awaitDescription("Repository URL")
        compose.firstWithDescription("Repository URL").performTextInput(server.url)
        compose.firstWithDescription("Confirm adding the repository").performClick()
    }

    private fun openTheProject() {
        compose.awaitText("specs", substring = true)
        compose.firstWithText("specs", substring = true).performClick()
        compose.awaitText("Overview")
    }

    /** Starts below the app bar, so the drag lands in the pane and not on it. */
    private fun ComposeContentTestRule.pullDown() {
        onRoot().performTouchInput {
            swipeDown(startY = centerY, endY = bottom - 1f, durationMillis = 400)
        }
    }

    @Test
    fun pullingTheProjectViewFetches() {
        start()
        addTheRepository()
        openTheProject()
        compose.awaitText("1 specs", substring = true)

        remote.commit("openspec/changes/second/proposal.md", "## Why\n\nAnother.\n", "p2")
        remote.commit("openspec/specs/later/spec.md", LocalRemote.spec("later"), "later")
        compose.pullDown()

        // The Overview tab's own list first, then the header, so a failure says
        // whether the pull reached the fetch or only the header stood still.
        compose.awaitText("second")
        compose.awaitText("2 specs", substring = true)
    }

    @Test
    fun pullingTheProjectViewKeepsTheOpenTab() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Changes").performClick()
        compose.awaitText("first")

        remote.commit("openspec/changes/second/proposal.md", "## Why\n\nAnother.\n", "p2")
        compose.pullDown()

        // Still the Changes tab, now listing both.
        compose.awaitText("second")
        compose.awaitText("first")
    }

    /**
     * The Specs tab of a project with none is a centred line with nothing to
     * scroll, which is where a dead gesture would hide.
     */
    @Test
    fun pullingAPaneWithNothingToScrollStillFetches() {
        val bare = LocalRemote(File(root, "bare")).initEmptyProject().apply {
            commit("openspec/config.yaml", "schema: spec-driven\n", "config")
        }
        val bareServer = GitHttpServer(bare.dir, null).start()
        try {
            start()
            compose.awaitDescription("Add a repository")
            compose.firstWithDescription("Add a repository").performClick()
            compose.awaitDescription("Repository URL")
            compose.firstWithDescription("Repository URL").performTextInput(bareServer.url)
            compose.firstWithDescription("Confirm adding the repository").performClick()
            openTheProject()

            compose.firstWithText("Specs").performClick()
            compose.awaitText("This project has no specs yet.")

            bare.commit("openspec/specs/arrived/spec.md", LocalRemote.spec("arrived"), "a spec")
            compose.pullDown()

            compose.awaitText("arrived")
        } finally {
            bareServer.stop()
        }
    }

    /**
     * An artifact renders through a Markwon `TextView`, which Compose's
     * matchers cannot see into, so what is asserted here is the tab row: the
     * pull reached the fetch, and the tabs follow what the change holds now.
     * That an artifact's text is re-read is a view model test.
     */
    @Test
    fun pullingAChangeFetchesAndKeepsTheOpenTab() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Changes").performClick()
        compose.awaitText("first")
        compose.firstWithText("first").performClick()
        compose.awaitText("proposal")

        remote.commit("openspec/changes/first/design.md", "## Context\n\nThings.\n", "d")
        compose.pullDown()

        compose.awaitText("design")
        compose.awaitText("proposal")
    }

    @Test
    fun pullingASpecCardKeepsItOpen() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Specs").performClick()
        compose.awaitText("thing")
        compose.firstWithText("thing").performClick()
        compose.awaitText("Something holds")
        compose.firstWithText("Something holds").performClick()
        compose.awaitText("The system SHALL do the thing.", substring = true)

        remote.commit(
            "openspec/specs/thing/spec.md",
            LocalRemote.spec("thing").replace("do the thing", "do it twice"),
            "reworded",
        )
        compose.pullDown()

        compose.awaitText("The system SHALL do it twice.", substring = true)
    }

    /**
     * The fetch belongs to the repository, so leaving for the project view
     * while it is in flight does not abandon it.
     */
    @Test
    fun aRefreshStartedDeepLandsAfterLeavingTheScreen() {
        start()
        addTheRepository()
        openTheProject()

        compose.firstWithText("Changes").performClick()
        compose.awaitText("first")
        compose.firstWithText("first").performClick()
        compose.awaitText("proposal")

        remote.commit("openspec/specs/later/spec.md", LocalRemote.spec("later"), "later")
        compose.pullDown()
        compose.firstWithDescription("Back").performClick()

        compose.awaitText("2 specs", substring = true)
    }

    /** A refresh that cannot reach the remote keeps the project and says why. */
    @Test
    fun aFailedRefreshKeepsTheProjectAndOffersToTryAgain() {
        start()
        addTheRepository()
        openTheProject()
        compose.awaitText("1 specs", substring = true)

        server.stop()
        compose.pullDown()

        compose.awaitText("The repository could not be reached.")
        compose.awaitText("Try again")
        // The project is still the one on screen behind the message.
        compose.awaitText("1 specs", substring = true)
    }
}
