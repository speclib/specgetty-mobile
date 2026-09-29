package io.github.mipmip.specgettyondroid.auth

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The real transport, over the JDK's own HTTP client.
 *
 * No new dependency: JGit already brings this, and the two calls are a form
 * POST and a bearer GET. Excluded from coverage, like the other framework glue,
 * because everything it decides is decided in [DeviceFlow] and [Installations]
 * against a fake.
 */
class HttpAuthTransport(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthTransport {

    override suspend fun post(url: String, form: Map<String, String>): String =
        withContext(dispatcher) {
            val body = form.entries.joinToString("&") { (k, v) ->
                "${encode(k)}=${encode(v)}"
            }
            connection(url).run {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                readBody()
            }
        }

    override suspend fun get(url: String, bearer: String): String = withContext(dispatcher) {
        connection(url).run {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $bearer")
            readBody()
        }
    }

    private fun connection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("User-Agent", USER_AGENT)
        }

    /**
     * The body either way. GitHub answers a refusal with JSON that names the
     * reason, and that reason is the whole point, so an error body is read
     * rather than thrown away.
     */
    private fun HttpURLConnection.readBody(): String = try {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        stream?.bufferedReader()?.use { it.readText() } ?: throw IOException("empty response")
    } finally {
        disconnect()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val TIMEOUT_MILLIS = 30_000
        const val USER_AGENT = "specgetty-on-droid"
    }
}
