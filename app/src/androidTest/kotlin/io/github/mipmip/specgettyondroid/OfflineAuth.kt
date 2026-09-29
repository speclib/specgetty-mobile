package io.github.mipmip.specgettyondroid

import io.github.mipmip.specgettyondroid.auth.AuthTransport
import io.github.mipmip.specgettyondroid.auth.DeviceFlow
import io.github.mipmip.specgettyondroid.auth.Installations

/**
 * For the tests that render the whole nav host without authorizing. Reaching
 * github.com from a test would make it depend on the network and on a real
 * account, so the transport refuses instead.
 */
object OfflineAuth : AuthTransport {

    override suspend fun post(url: String, form: Map<String, String>): String =
        error("this test does not authorize")

    override suspend fun get(url: String, bearer: String): String =
        error("this test does not authorize")

    val deviceFlow: DeviceFlow get() = DeviceFlow(this, "test")

    val installations: Installations get() = Installations(this)
}
