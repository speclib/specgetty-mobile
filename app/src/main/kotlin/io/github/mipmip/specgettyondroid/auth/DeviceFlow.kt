package io.github.mipmip.specgettyondroid.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/**
 * The public identifier of this project's GitHub App.
 *
 * Public by design. GitHub's web flow needs a client secret to exchange a code,
 * and PKCE does not replace it, so a FOSS app cannot use that flow at all. The
 * device flow needs only this, and it is meant to be published. A fork that
 * wants its own app changes this one constant.
 */
const val GITHUB_CLIENT_ID = "Iv23lihDe3mB1mbVXUKC"

/**
 * The same app's name in a URL. Public for the same reason, and kept beside the
 * client id because a fork has to change both or neither.
 */
const val GITHUB_APP_SLUG = "specgetty-on-droid"

/**
 * Where a person chooses which repositories the app may read.
 *
 * Not `github.com/settings/installations`, which lists only what is already
 * installed on your own account and offers no way to add an organization. This
 * one does, which is the whole point of showing it.
 */
const val GITHUB_INSTALL_URL = "https://github.com/apps/$GITHUB_APP_SLUG/installations/new"

/** What GitHub was asked and what it answered, so the flow can be driven by a fake. */
interface AuthTransport {
    /** Form-encoded POST returning the body. */
    suspend fun post(url: String, form: Map<String, String>): String

    /** GET with a bearer credential, returning the body. */
    suspend fun get(url: String, bearer: String): String
}

/** The codes GitHub issues to begin a device authorization. */
data class DeviceCode(
    val deviceCode: String,
    /** The short code the person types. GitHub does not offer a prefilled URL. */
    val userCode: String,
    val verificationUri: String,
    val intervalSeconds: Int,
    val expiresInSeconds: Int,
)

/** A credential, and what is needed to renew it. */
data class GitHubCredential(
    val token: String,
    /** Seconds from issue, or null when the app was set never to expire tokens. */
    val expiresInSeconds: Int?,
    val refreshToken: String?,
)

/**
 * Every way waiting can end. Reported rather than thrown: an unfinished
 * authorization is not a failure, and telling the two apart is the whole job.
 */
sealed interface AuthOutcome {
    data class Approved(val credential: GitHubCredential) : AuthOutcome

    /** Not yet. Keep waiting, after [intervalSeconds]. */
    data class Pending(val intervalSeconds: Int) : AuthOutcome

    data object Expired : AuthOutcome

    data object Declined : AuthOutcome

    /** The app is not set up for this, which the person cannot fix. */
    data object Unavailable : AuthOutcome

    /**
     * The host could not be reached. Not an answer, and so not an ending: a
     * phone loses its network while its owner is in the browser approving, and
     * throwing the authorization away over that would be the app's fault rather
     * than GitHub's.
     */
    data class Unreachable(val reason: String) : AuthOutcome

    /** GitHub answered, and its answer was one this app does not know. */
    data class Failed(val reason: String) : AuthOutcome
}

/**
 * GitHub's device flow. Two calls and a poll, with no client secret anywhere.
 */
class DeviceFlow(
    private val transport: AuthTransport,
    private val clientId: String = GITHUB_CLIENT_ID,
) {

    suspend fun requestCode(): Result<DeviceCode> = runCatching {
        val body = transport.post(DEVICE_CODE_URL, mapOf("client_id" to clientId))
        val json = Json.parseToJsonElement(body).jsonObject

        json["error"]?.let { error ->
            throw AuthError(error.jsonPrimitive.content)
        }
        DeviceCode(
            deviceCode = json.string("device_code"),
            userCode = json.string("user_code"),
            verificationUri = json.string("verification_uri"),
            intervalSeconds = json.int("interval", DEFAULT_INTERVAL),
            expiresInSeconds = json.int("expires_in", DEFAULT_EXPIRY),
        )
    }

    /**
     * Asks once whether the person has approved yet.
     *
     * @param interval the interval currently in use. A `slow_down` answer
     *   returns a longer one, which the caller must adopt, or GitHub starts
     *   refusing.
     */
    suspend fun poll(deviceCode: String, interval: Int): AuthOutcome = try {
        val body = transport.post(
            TOKEN_URL,
            mapOf(
                "client_id" to clientId,
                "device_code" to deviceCode,
                "grant_type" to DEVICE_GRANT,
            ),
        )
        val json = Json.parseToJsonElement(body).jsonObject

        when (val error = json["error"]?.jsonPrimitive?.content) {
            null -> AuthOutcome.Approved(
                GitHubCredential(
                    token = json.string("access_token"),
                    expiresInSeconds = json["expires_in"]?.jsonPrimitive?.content?.toIntOrNull(),
                    refreshToken = json["refresh_token"]?.jsonPrimitive?.content,
                ),
            )

            "authorization_pending" -> AuthOutcome.Pending(interval)
            "slow_down" -> AuthOutcome.Pending(
                json.int("interval", interval + SLOW_DOWN_STEP).coerceAtLeast(interval + SLOW_DOWN_STEP),
            )

            "expired_token" -> AuthOutcome.Expired
            "access_denied" -> AuthOutcome.Declined
            "device_flow_disabled", "unauthorized_client" -> AuthOutcome.Unavailable
            else -> AuthOutcome.Failed(error)
        }
    } catch (e: IOException) {
        AuthOutcome.Unreachable(e.message ?: e::class.java.simpleName)
    } catch (e: Exception) {
        AuthOutcome.Failed(e.message ?: e::class.java.simpleName)
    }

    /**
     * Exchanges a refresh token for a fresh credential. No client secret: the
     * spike confirmed a public client may do this, which is what makes the
     * eight-hour expiry affordable.
     */
    suspend fun refresh(refreshToken: String): AuthOutcome = try {
        val body = transport.post(
            TOKEN_URL,
            mapOf(
                "client_id" to clientId,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
            ),
        )
        val json = Json.parseToJsonElement(body).jsonObject
        when (val error = json["error"]?.jsonPrimitive?.content) {
            null -> AuthOutcome.Approved(
                GitHubCredential(
                    token = json.string("access_token"),
                    expiresInSeconds = json["expires_in"]?.jsonPrimitive?.content?.toIntOrNull(),
                    refreshToken = json["refresh_token"]?.jsonPrimitive?.content,
                ),
            )

            else -> AuthOutcome.Failed(error)
        }
    } catch (e: IOException) {
        AuthOutcome.Unreachable(e.message ?: e::class.java.simpleName)
    } catch (e: Exception) {
        AuthOutcome.Failed(e.message ?: e::class.java.simpleName)
    }

    private companion object {
        const val DEVICE_CODE_URL = "https://github.com/login/device/code"
        const val TOKEN_URL = "https://github.com/login/oauth/access_token"
        const val DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"
        const val DEFAULT_INTERVAL = 5
        const val DEFAULT_EXPIRY = 900
        const val SLOW_DOWN_STEP = 5
    }
}

class AuthError(val code: String) : Exception(code)

private fun kotlinx.serialization.json.JsonObject.string(key: String): String =
    this[key]?.jsonPrimitive?.content ?: throw AuthError("missing_$key")

private fun kotlinx.serialization.json.JsonObject.int(key: String, fallback: Int): Int =
    this[key]?.jsonPrimitive?.content?.toIntOrNull() ?: fallback
