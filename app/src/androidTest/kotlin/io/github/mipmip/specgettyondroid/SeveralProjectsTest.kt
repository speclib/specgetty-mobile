package io.github.mipmip.specgettyondroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.AndroidGit
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import io.github.mipmip.specgettyondroid.store.repoIdFor
import io.github.mipmip.specgettyondroid.ui.SpecgettyNavHost
import io.github.mipmip.specgettyondroid.ui.theme.SpecgettyTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * `nivis-openspec-stores` holds four projects, one per top-level directory.
 * Every one of them used to be unreachable, because the app looked at the
 * repository root and nowhere else.
 */
class SeveralProjectsTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var root: File
    private lateinit var server: GitHttpServer
    private lateinit var store: RepoStore
    private lateinit var catalog: RepoRegistry
    private lateinit var repository: ProjectRepository

    private val names = listOf("nivis", "nivis-demos", "nivis-tunnel", "registry")

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(context.cacheDir, "several-${System.nanoTime()}").apply { mkdirs() }
        AndroidGit.install(File(root, "git-config"))

        // initBare, so the root itself holds no project and the four below it
        // are the whole answer.
        val remote = LocalRemote(File(root, "remote")).initBare().apply {
            names.forEach { name ->
                commit("$name/openspec/config.yaml", "schema: spec-driven\n", "config $name")
                commit(
                    "$name/openspec/specs/cap-$name/spec.md",
                    LocalRemote.spec("cap-$name"),
                    "spec $name",
                )
            }
            commit("untangle/README.md", "no project here\n", "untangle")
        }
        server = GitHttpServer(remote.dir, null).start()

        store = RepoStore(File(root, "repos"))
        catalog = RepoRegistry(InstrumentedPreferences(), InstrumentedVault())
        repository = ProjectRepository(store = store, catalog = catalog)
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

    @Test
    fun aRepositoryHoldingSeveralProjectsOffersThemAll() {
        start()
        addTheRepository()

        compose.awaitDescription("Choose which projects to add")
        // Scrolled to, because four of them do not fit a sheet on a short
        // screen, which is what made the last one unreachable.
        names.forEach {
            compose.firstWithDescription("Project $it").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun choosingTwoOfFourAddsTwoRows() {
        start()
        addTheRepository()
        compose.awaitDescription("Choose which projects to add")

        compose.firstWithDescription("Project nivis").performScrollTo().performClick()
        compose.firstWithDescription("Project registry").performScrollTo().performClick()
        compose.firstWithDescription("Add the chosen projects").performScrollTo().performClick()

        compose.awaitText("nivis")
        compose.awaitText("registry")
        assertEquals(2, runBlocking { catalog.current().repos.size })
    }

    @Test
    fun eachRowSaysWhereInTheRepositoryItCameFrom() {
        start()
        addTheRepository()
        compose.awaitDescription("Choose which projects to add")
        compose.firstWithDescription("Project nivis").performScrollTo().performClick()
        compose.firstWithDescription("Add the chosen projects").performScrollTo().performClick()

        compose.awaitDescription("Where nivis came from")
        compose.firstWithDescription("Where nivis came from").assertIsDisplayed()
        compose.onNodeWithText("nivis in ${server.url}").assertIsDisplayed()
    }

    /** Each row carries its own project, not a total of the repository's. */
    @Test
    fun eachRowCarriesItsOwnStatistics() {
        start()
        addTheRepository()
        compose.awaitDescription("Choose which projects to add")
        compose.firstWithDescription("Project nivis").performScrollTo().performClick()
        compose.firstWithDescription("Project registry").performScrollTo().performClick()
        compose.firstWithDescription("Add the chosen projects").performScrollTo().performClick()

        compose.awaitText("1 specs", substring = true)
        val showing = compose.onAllNodesWithText("1 specs", substring = true, useUnmergedTree = true)
        assertEquals(2, showing.fetchSemanticsNodes().size)
    }

    @Test
    fun abandoningTheChoiceAddsNothing() {
        start()
        addTheRepository()
        compose.awaitDescription("Choose which projects to add")

        compose.firstWithText("Cancel").performClick()
        compose.waitForIdle()

        assertTrue(runBlocking { catalog.current().repos.isEmpty() })
    }

    /**
     * The working copy belongs to the repository, so it outlives the row that
     * made it for as long as another row reads from it.
     */
    @Test
    fun removingOneOfTwoKeepsTheOtherLoading() {
        start()
        addTheRepository()
        compose.awaitDescription("Choose which projects to add")
        compose.firstWithDescription("Project nivis").performScrollTo().performClick()
        compose.firstWithDescription("Project registry").performScrollTo().performClick()
        compose.firstWithDescription("Add the chosen projects").performScrollTo().performClick()
        compose.awaitText("registry")

        compose.firstWithDescription("Remove nivis").performClick()
        compose.awaitText("Remove nivis?")
        compose.onNodeWithText("Only this project is removed", substring = true).assertIsDisplayed()
        compose.firstWithText("Remove").performClick()

        compose.waitForIdle()
        val remaining = runBlocking { catalog.current().repos }
        assertEquals(1, remaining.size)
        assertEquals("registry", remaining.single().path)
        assertTrue(store.isCloned(remaining.single().cloneId))
    }

    @Test
    fun theTwoRowsShareOneWorkingCopy() {
        start()
        addTheRepository()
        compose.awaitDescription("Choose which projects to add")
        compose.firstWithDescription("Project nivis").performScrollTo().performClick()
        compose.firstWithDescription("Project registry").performScrollTo().performClick()
        compose.firstWithDescription("Add the chosen projects").performScrollTo().performClick()
        compose.awaitText("registry")

        assertEquals(1, File(root, "repos").listFiles().orEmpty().size)
    }

    /**
     * A repository that points at a store is a real project whose content is
     * held on another machine. Reporting it as absent sends a person looking
     * for the wrong thing.
     */
    @Test
    fun aRepositoryThatPointsAtAStoreSaysSo() {
        val pointing = LocalRemote(File(root, "pointing")).initBare().apply {
            commit("openspec/config.yaml", "store: nivis-tunnel\n", "a pointer")
        }
        val other = GitHttpServer(pointing.dir, null).start()
        try {
            start()
            compose.awaitDescription("Add a repository")
            compose.firstWithDescription("Add a repository").performClick()
            compose.awaitDescription("Repository URL")
            compose.firstWithDescription("Repository URL").performTextInput(other.url)
            compose.firstWithDescription("Confirm adding the repository").performClick()

            compose.awaitText("nivis-tunnel", substring = true)
            compose.onNodeWithText("openspec/config.yaml", substring = true).assertIsDisplayed()
        } finally {
            other.stop()
        }
    }

    /**
     * A list written before entries carried a path keeps every id it had, so
     * the working copy already on the device is still the one it uses.
     */
    @Test
    fun aListStoredBeforePathsExistedReusesItsWorkingCopy() {
        val single = LocalRemote(File(root, "single")).init()
        val served = GitHttpServer(single.dir, null).start()
        try {
            val oldStyle = runBlocking {
                catalog.add(served.url, "", null, "")
            }
            // Exactly what the id was before paths existed.
            assertEquals(repoIdFor(served.url), oldStyle.id)
            assertEquals("", oldStyle.path)

            runBlocking { repository.load(oldStyle.id) }
            val cloneDirs = File(root, "repos").listFiles().orEmpty().map { it.name }
            assertEquals(listOf(repoIdFor(served.url)), cloneDirs)

            // Loading again finds the copy rather than fetching a second one.
            runBlocking { repository.load(oldStyle.id) }
            assertEquals(1, File(root, "repos").listFiles().orEmpty().size)
        } finally {
            served.stop()
        }
    }
}
