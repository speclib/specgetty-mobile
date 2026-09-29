package io.github.mipmip.specgettyondroid.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the app holds to read a private repository.
 *
 * A typed token is a string and nothing else, which is what every credential
 * was before authorizing existed. One obtained by authorizing carries an expiry
 * and the material to renew itself.
 */
sealed interface Credential {

    val token: String

    /** Typed or pasted by the person. Never expires as far as the app knows. */
    data class Typed(override val token: String) : Credential

    /**
     * Obtained by authorizing on github.com.
     *
     * @param expiresAtMillis when the token stops working, or null when the app
     *   was set never to expire them.
     */
    data class GitHub(
        override val token: String,
        val expiresAtMillis: Long?,
        val refreshToken: String?,
    ) : Credential {

        fun isNearExpiry(nowMillis: Long): Boolean {
            val expiry = expiresAtMillis ?: return false
            return nowMillis >= expiry - RENEW_WITHIN_MILLIS
        }

        val canRenew: Boolean get() = !refreshToken.isNullOrBlank()
    }

    companion object {
        /**
         * Renewed this long before it would expire, so a clone that takes a
         * while does not start with a credential that dies halfway.
         */
        const val RENEW_WITHIN_MILLIS = 5 * 60 * 1000L

        private val JSON = Json { ignoreUnknownKeys = true }

        /**
         * Stored as JSON for a GitHub credential, and as the bare token for a
         * typed one. A value written before this change is a bare token, so it
         * reads back as [Typed] without a migration.
         */
        fun encode(credential: Credential): String = when (credential) {
            is Typed -> credential.token
            is GitHub -> JSON.encodeToString(
                StoredGitHub(
                    token = credential.token,
                    expiresAtMillis = credential.expiresAtMillis,
                    refreshToken = credential.refreshToken,
                ),
            )
        }

        fun decode(stored: String): Credential {
            val trimmed = stored.trim()
            if (!trimmed.startsWith("{")) return Typed(stored)
            return runCatching {
                JSON.decodeFromString<StoredGitHub>(trimmed).let {
                    GitHub(it.token, it.expiresAtMillis, it.refreshToken)
                }
            }.getOrElse { Typed(stored) }
        }
    }
}

@Serializable
private data class StoredGitHub(
    val token: String,
    val expiresAtMillis: Long? = null,
    val refreshToken: String? = null,
)
