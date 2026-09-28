package io.github.mipmip.specgettyondroid.data

import io.github.mipmip.specgettyondroid.index.ProjectIndex
import io.github.mipmip.specgettyondroid.repo.RepoError

/**
 * What a repository's project is doing. Five values rather than a handful of
 * booleans, so that a screen reads a state instead of deducing one and
 * inventing a sixth by accident.
 */
sealed interface ProjectState {

    /** Added, but nothing has been read yet. */
    data object Absent : ProjectState

    /** A clone, a refresh or a load is in flight. */
    data object Loading : ProjectState

    data class Loaded(val index: ProjectIndex) : ProjectState

    /**
     * The clone worked and there is no `openspec/` at the root. Its own outcome
     * rather than a failure: nothing went wrong, the repository simply is not
     * an OpenSpec project.
     */
    data object NoProject : ProjectState

    data class Failed(val error: RepoError) : ProjectState
}

val ProjectState.indexOrNull: ProjectIndex?
    get() = (this as? ProjectState.Loaded)?.index

val ProjectState.isLoading: Boolean get() = this is ProjectState.Loading

val ProjectState.errorOrNull: RepoError?
    get() = (this as? ProjectState.Failed)?.error
