package io.github.mipmip.specgettyondroid.viewmodel

import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.RepoError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The pull-to-refresh gesture, as every screen reading a repository needs it:
 * whether a fetch is in flight, why the last one failed, and the two things a
 * screen can do about either. One of these rather than the same four members
 * copied into five view models.
 *
 * Asking for a refresh does not wait for it. The repository owns the work, so
 * leaving the screen leaves the fetch running.
 */
class RefreshControl(
    private val repository: ProjectRepository,
    private val repoId: String,
    private val scope: CoroutineScope,
) {

    val refreshing: StateFlow<Boolean> =
        repository.refreshingOf(repoId).stateIn(scope, SharingStarted.Eagerly, false)

    val failure: StateFlow<RepoError?> =
        repository.outcomeOf(repoId).stateIn(scope, SharingStarted.Eagerly, null)

    fun refresh() {
        scope.launch { repository.refresh(repoId) }
    }

    /** Told once. A failure already shown is not shown again on returning. */
    fun failureShown() {
        scope.launch { repository.clearOutcome(repoId) }
    }
}

/** A failed refresh, with the working copy it was for, so it can be cleared. */
data class RefreshFailure(val cloneId: String, val error: RepoError)
