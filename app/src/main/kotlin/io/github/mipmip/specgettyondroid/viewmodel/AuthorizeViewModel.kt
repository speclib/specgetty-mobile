package io.github.mipmip.specgettyondroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mipmip.specgettyondroid.auth.AuthOutcome
import io.github.mipmip.specgettyondroid.auth.DeviceCode
import io.github.mipmip.specgettyondroid.auth.DeviceFlow
import io.github.mipmip.specgettyondroid.auth.Installations
import io.github.mipmip.specgettyondroid.auth.Reach
import io.github.mipmip.specgettyondroid.store.Credential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where an authorization has got to. */
sealed interface AuthState {
    data object Idle : AuthState

    data object RequestingCode : AuthState

    /**
     * The code is on screen and the app is waiting for GitHub to say yes.
     *
     * @param reconnecting the last ask did not reach the host. The code is
     *   still good, so it stays on screen and the app keeps trying.
     */
    data class Waiting(val code: DeviceCode, val reconnecting: Boolean = false) : AuthState

    /** Approved, and the credential reaches the repository being added. */
    data class Approved(val credential: Credential.GitHub, val reach: Reach) : AuthState

    /**
     * Approved, and the credential reaches nothing useful. Not a success:
     * saying so here is the difference between a person knowing they must still
     * install the app and discovering it when a clone fails.
     */
    data class NeedsInstalling(
        val credential: Credential.GitHub,
        val reach: Reach,
        val repositoryUrl: String?,
    ) : AuthState

    data class Failed(val message: String) : AuthState
}

/**
 * Drives the device flow for one repository URL.
 *
 * The polling loop lives here rather than in the screen so that leaving the app
 * for the browser and coming back does not restart it.
 */
class AuthorizeViewModel(
    private val deviceFlow: DeviceFlow,
    private val installations: Installations,
    private val repositoryUrl: String?,
    injectedScope: CoroutineScope? = null,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val scope: CoroutineScope = injectedScope ?: viewModelScope

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var polling: Job? = null

    fun begin() {
        if (polling?.isActive == true) return
        polling = scope.launch {
            _state.value = AuthState.RequestingCode

            val code = deviceFlow.requestCode().getOrElse { error ->
                _state.value = AuthState.Failed(explain(error))
                return@launch
            }
            _state.value = AuthState.Waiting(code)

            // Bounded by the expiry GitHub gave us. Polling past it would ask
            // forever for an answer that can no longer come, and the host is
            // not obliged to say `expired_token` promptly.
            var interval = code.intervalSeconds
            var waited = 0
            var unreachable = 0
            while (waited < code.expiresInSeconds) {
                delay(interval * 1000L)
                waited += interval
                when (val outcome = deviceFlow.poll(code.deviceCode, interval)) {
                    is AuthOutcome.Pending -> {
                        unreachable = 0
                        interval = outcome.intervalSeconds
                        _state.value = AuthState.Waiting(code)
                    }

                    // A phone in a browser loses its network, or hands over
                    // between cells. The code is good for a quarter of an hour,
                    // so keep asking rather than throwing it away.
                    is AuthOutcome.Unreachable -> {
                        unreachable++
                        if (unreachable >= GIVE_UP_AFTER_UNREACHABLE) {
                            _state.value = AuthState.Failed(
                                "Could not reach GitHub: ${outcome.reason}. " +
                                    "Check the connection and start again.",
                            )
                            return@launch
                        }
                        _state.value = AuthState.Waiting(code, reconnecting = true)
                    }
                    is AuthOutcome.Approved -> {
                        finish(outcome)
                        return@launch
                    }

                    AuthOutcome.Expired -> {
                        _state.value = AuthState.Failed(
                            "That code expired. Start again for a fresh one.",
                        )
                        return@launch
                    }

                    AuthOutcome.Declined -> {
                        _state.value = AuthState.Failed("Authorization was declined on GitHub.")
                        return@launch
                    }

                    AuthOutcome.Unavailable -> {
                        _state.value = AuthState.Failed(
                            "This app cannot authorize with GitHub. Type an access token instead.",
                        )
                        return@launch
                    }

                    is AuthOutcome.Failed -> {
                        _state.value = AuthState.Failed("GitHub answered: ${outcome.reason}")
                        return@launch
                    }
                }
            }
            _state.value = AuthState.Failed("That code expired. Start again for a fresh one.")
        }
    }

    private suspend fun finish(outcome: AuthOutcome.Approved) {
        val credential = Credential.GitHub(
            token = outcome.credential.token,
            expiresAtMillis = outcome.credential.expiresInSeconds?.let { now() + it * 1000L },
            refreshToken = outcome.credential.refreshToken,
        )
        val reach = installations.reachOf(credential.token).getOrElse {
            // The credential is good; only the listing failed. Treat it as
            // reaching nothing, which asks rather than assumes.
            Reach(emptyList())
        }

        val covered = repositoryUrl?.let { reach.covers(it) } ?: !reach.reachesNothing
        _state.value = if (covered) {
            AuthState.Approved(credential, reach)
        } else {
            AuthState.NeedsInstalling(credential, reach, repositoryUrl)
        }
    }

    /** Abandoning stores nothing, and stops the app asking GitHub anything more. */
    fun cancel() {
        polling?.cancel()
        polling = null
        _state.value = AuthState.Idle
    }

    override fun onCleared() {
        polling?.cancel()
        super.onCleared()
    }

    private fun explain(error: Throwable): String = when {
        error.message?.contains("device_flow_disabled") == true ->
            "This app cannot authorize with GitHub. Type an access token instead."

        else -> "Could not reach GitHub. Check the connection, or type an access token."
    }

    companion object {
        /**
         * Consecutive failures to reach the host before giving up. At a five
         * second interval this is half a minute of trying, which outlasts a
         * cell handover and a screen lock without outlasting the person's
         * patience.
         */
        private const val GIVE_UP_AFTER_UNREACHABLE = 6

        fun factory(
            deviceFlow: DeviceFlow,
            installations: Installations,
            repositoryUrl: String?,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthorizeViewModel(deviceFlow, installations, repositoryUrl) as T
        }
    }
}
