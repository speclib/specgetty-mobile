package io.github.mipmip.specgettyondroid.data

import io.github.mipmip.specgettyondroid.repo.LocalRemote
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.FakeVault
import io.github.mipmip.specgettyondroid.store.InMemoryPreferences
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var store: RepoStore
    private lateinit var vault: FakeVault
    private lateinit var registry: RepoRegistry
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        store = RepoStore(temp.newFolder("repos"), Dispatchers.Unconfined)
        vault = FakeVault()
        registry = RepoRegistry(InMemoryPreferences(), vault)
        repository = ProjectRepository(store, registry, Dispatchers.Unconfined)
    }

    private fun remote(build: LocalRemote.() -> Unit = {}): LocalRemote =
        LocalRemote(temp.newFolder()).init().apply(build)

    @Test
    fun `a repository with no state yet is absent`() {
        assertEquals(ProjectState.Absent, repository.stateOf("nothing"))
    }

    @Test
    fun `adding clones and loads the project`() = runTest {
        val config = repository.add(remote().url)
        val state = repository.stateOf(config.id)
        assertTrue("$state", state is ProjectState.Loaded)
        assertEquals(1, state.indexOrNull?.counts?.specs)
        assertEquals(1, state.indexOrNull?.counts?.active)
    }

    @Test
    fun `adding puts the repository in the list`() = runTest {
        val config = repository.add(remote().url)
        assertEquals(listOf(config.id), repository.current().repos.map { it.id })
    }

    @Test
    fun `a repository with no openspec directory is its own outcome`() = runTest {
        val bare = LocalRemote(temp.newFolder()).initBare()
        val config = repository.add(bare.url)
        assertEquals(ProjectState.NoProject, repository.stateOf(config.id))
    }

    @Test
    fun `an empty project is loaded, not absent`() = runTest {
        val empty = LocalRemote(temp.newFolder()).initEmptyProject()
        val config = repository.add(empty.url)
        val state = repository.stateOf(config.id)
        assertTrue("$state", state is ProjectState.Loaded)
        assertEquals(0, state.indexOrNull?.counts?.specs)
    }

    @Test
    fun `an unreachable host leaves a network failure`() = runTest {
        val config = repository.add("https://no-such-host.invalid/a/b.git")
        val state = repository.stateOf(config.id)
        assertTrue("$state", state.errorOrNull is RepoError.Network)
    }

    @Test
    fun `a failed clone leaves the repository in the list so it can be retried`() = runTest {
        val config = repository.add("https://no-such-host.invalid/a/b.git")
        assertEquals(listOf(config.id), repository.current().repos.map { it.id })
        assertTrue(repository.stateOf(config.id) is ProjectState.Failed)
    }

    @Test
    fun `a token given at add time is stored for later refreshes`() = runTest {
        val config = repository.add(remote().url, token = "ghp_secret")
        assertEquals("ghp_secret", vault.tokens[config.id])
        assertTrue(repository.current().repos.single().hasToken)
    }

    @Test
    fun `a refresh picks up a new spec`() = runTest {
        val r = remote()
        val config = repository.add(r.url)
        assertEquals(1, repository.stateOf(config.id).indexOrNull?.counts?.specs)

        r.commit("openspec/specs/later/spec.md", LocalRemote.spec("later"), "later")
        repository.refresh(config.id)

        assertEquals(2, repository.stateOf(config.id).indexOrNull?.counts?.specs)
    }

    @Test
    fun `a refresh that fails leaves a failure rather than a stale index`() = runTest {
        val r = remote()
        val config = repository.add(r.url)
        assertTrue(repository.stateOf(config.id) is ProjectState.Loaded)

        // Destroy the remote so the fetch cannot succeed.
        File(r.url.removePrefix("file:")).deleteRecursively()
        repository.refresh(config.id)

        val state = repository.stateOf(config.id)
        assertFalse("$state", state is ProjectState.Loaded)
        assertTrue("$state", state is ProjectState.Failed)
    }

    @Test
    fun `removing forgets the working copy, the entry and the token`() = runTest {
        val config = repository.add(remote().url, token = "ghp_secret")
        repository.remove(config.id)

        assertFalse(store.isCloned(config.id))
        assertTrue(repository.current().repos.isEmpty())
        assertEquals(ProjectState.Absent, repository.stateOf(config.id))
        assertTrue(vault.tokens.isEmpty())
    }

    @Test
    fun `removing the active repository promotes another`() = runTest {
        val first = repository.add(remote().url)
        val second = repository.add(remote().url)
        repository.remove(first.id)
        assertEquals(second.id, repository.current().activeId)
    }

    @Test
    fun `switching to a loaded repository shows it without cloning again`() = runTest {
        val first = repository.add(remote().url)
        val second = repository.add(remote().url)

        val before = repository.stateOf(first.id)
        repository.activate(first.id)

        assertEquals(first.id, repository.current().activeId)
        assertEquals(before, repository.stateOf(first.id))
        assertTrue(repository.stateOf(second.id) is ProjectState.Loaded)
    }

    @Test
    fun `switching to one cloned in an earlier session loads it from disk`() = runTest {
        val r = remote()
        val config = repository.add(r.url)

        // A new repository object over the same store and registry is what a
        // later session looks like: the working copy is there, the state is not.
        val later = ProjectRepository(store, registry, Dispatchers.Unconfined)
        assertEquals(ProjectState.Absent, later.stateOf(config.id))

        later.activate(config.id)
        assertTrue(later.stateOf(config.id) is ProjectState.Loaded)
    }

    @Test
    fun `the state flow carries an entry per repository`() = runTest {
        val first = repository.add(remote().url)
        val second = repository.add(remote().url)
        val states = repository.stateOf(first.id) to repository.stateOf(second.id)
        assertTrue(states.first is ProjectState.Loaded)
        assertTrue(states.second is ProjectState.Loaded)
    }

    @Test
    fun `loading a project parses no spec`() = runTest {
        val config = repository.add(remote().url)
        assertEquals(0, repository.cacheOf(config.id).parses)
    }

    @Test
    fun `opening a spec parses it once and reopening does not parse again`() = runTest {
        val config = repository.add(remote().url)
        val index = repository.stateOf(config.id).indexOrNull!!
        val capability = index.capabilityNames.single()
        val specFile = File(store.workingDir(config.id), "openspec/specs/$capability/spec.md")

        val cache = repository.cacheOf(config.id)
        val first = cache.spec(capability, specFile)
        assertEquals(1, cache.parses)

        val second = cache.spec(capability, specFile)
        assertEquals(1, cache.parses)
        assertEquals(first, second)
    }

    @Test
    fun `a rewritten spec is parsed again`() = runTest {
        val config = repository.add(remote().url)
        val capability = repository.stateOf(config.id).indexOrNull!!.capabilityNames.single()
        val specFile = File(store.workingDir(config.id), "openspec/specs/$capability/spec.md")

        val cache = repository.cacheOf(config.id)
        cache.spec(capability, specFile)
        assertEquals(1, cache.parses)

        specFile.writeText(LocalRemote.spec(capability) + "\nAn extra line.\n")
        specFile.setLastModified(specFile.lastModified() + 2000)

        cache.spec(capability, specFile)
        assertEquals(2, cache.parses)
    }

    @Test
    fun `a refresh drops what was parsed from the old content`() = runTest {
        val r = remote()
        val config = repository.add(r.url)
        val capability = repository.stateOf(config.id).indexOrNull!!.capabilityNames.single()
        val specFile = File(store.workingDir(config.id), "openspec/specs/$capability/spec.md")

        repository.cacheOf(config.id).spec(capability, specFile)
        assertEquals(1, repository.cacheOf(config.id).parses)

        repository.refresh(config.id)
        assertEquals("the cache is new after a refresh", 0, repository.cacheOf(config.id).parses)
    }

    @Test
    fun `a delta is cached separately from a spec of the same name`() = runTest {
        val config = repository.add(remote().url)
        val cache = repository.cacheOf(config.id)
        val deltaFile = File(
            store.workingDir(config.id),
            "openspec/changes/first-change/specs/repo-store/spec.md",
        )
        deltaFile.parentFile.mkdirs()
        deltaFile.writeText(
            "## ADDED Requirements\n\n### Requirement: A\nProse.\n\n" +
                "#### Scenario: s\n- **WHEN** a\n- **THEN** b\n",
        )
        cache.delta("repo-store", deltaFile)
        assertEquals(1, cache.parses)
        cache.delta("repo-store", deltaFile)
        assertEquals(1, cache.parses)
    }
}
