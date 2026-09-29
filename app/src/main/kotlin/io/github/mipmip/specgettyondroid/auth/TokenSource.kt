package io.github.mipmip.specgettyondroid.auth

import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.store.TokenVault

/** Why a credential could not be produced. */
sealed interface TokenProblem {
    /** Renewal was refused; the repository has to be authorized again. */
    data class NeedsAuthorizing(val reason: String) : TokenProblem

    /**
     * Renewal could not reach the host. The credential may well be fine, so
     * this must not send anyone off to authorize again over a lost signal.
     */
    data class Unreachable(val reason: String) : TokenProblem
}

data class TokenResult(val token: String?, val problem: TokenProblem? = null)

/**
 * Hands out the string `RepoStore` sends, renewing first when the stored
 * credential is close to expiring.
 *
 * Renewing before use rather than retrying after a failure: a refused read
 * returns 403, and 403 also means "this repository is not in the installation",
 * so a retry loop would have to tell those apart from the outside. Checking the
 * clock first avoids the ambiguity entirely.
 */
class TokenSource(
    private val vault: TokenVault,
    private val deviceFlow: DeviceFlow,
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun current(id: String): TokenResult {
        val stored = vault.get(id) ?: return TokenResult(null)
        val credential = Credential.decode(stored)

        if (credential !is Credential.GitHub) return TokenResult(credential.token)
        if (!credential.isNearExpiry(now())) return TokenResult(credential.token)

        val refreshToken = credential.refreshToken
        if (refreshToken.isNullOrBlank()) {
            return TokenResult(
                null,
                TokenProblem.NeedsAuthorizing("the credential expired and cannot be renewed"),
            )
        }

        return when (val outcome = deviceFlow.refresh(refreshToken)) {
            is AuthOutcome.Approved -> {
                val renewed = Credential.GitHub(
                    token = outcome.credential.token,
                    expiresAtMillis = outcome.credential.expiresInSeconds
                        ?.let { now() + it * 1000L },
                    refreshToken = outcome.credential.refreshToken ?: refreshToken,
                )
                vault.put(id, Credential.encode(renewed))
                TokenResult(renewed.token)
            }

            is AuthOutcome.Unreachable -> TokenResult(
                null,
                TokenProblem.Unreachable(outcome.reason),
            )

            else -> TokenResult(
                null,
                TokenProblem.NeedsAuthorizing("renewing the credential was refused"),
            )
        }
    }

    suspend fun store(id: String, credential: Credential) {
        vault.put(id, Credential.encode(credential))
    }

    /** From what the device flow just returned, stamped against the clock now. */
    fun fromApproval(credential: GitHubCredential): Credential.GitHub = Credential.GitHub(
        token = credential.token,
        expiresAtMillis = credential.expiresInSeconds?.let { now() + it * 1000L },
        refreshToken = credential.refreshToken,
    )
}
