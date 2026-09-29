package io.github.mipmip.specgettyondroid

import androidx.test.platform.app.InstrumentationRegistry
import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.store.KeystoreTokenVault
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Against the real vault, not a fake: the encryption, the Keystore key and the
 * file on disk are the parts that could keep a credential after a removal, and
 * none of them exist off a device.
 */
class AuthorizedRemovalTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val vault = KeystoreTokenVault(context)

    private val id = "authorized-removal-test"

    private val credential = Credential.GitHub(
        token = "ghu_instrumented",
        expiresAtMillis = 1_700_000_000_000L,
        refreshToken = "ghr_instrumented",
    )

    @Before
    fun clean() = runBlocking { vault.remove(id) }

    @Test
    fun anAuthorizedCredentialRoundTripsThroughTheRealVault() = runBlocking {
        vault.put(id, Credential.encode(credential))

        val read = Credential.decode(vault.get(id)!!)
        assertEquals(credential, read)
        vault.remove(id)
    }

    @Test
    fun removingLeavesNeitherTheTokenNorTheRenewalMaterial() = runBlocking {
        vault.put(id, Credential.encode(credential))
        assertNotNull(vault.get(id))

        vault.remove(id)

        assertNull(vault.get(id))
    }

    /**
     * The stored form is what a person pulling the file off the device would
     * see. Neither half of the credential may be readable in it.
     */
    @Test
    fun nothingReadableIsLeftInTheStoredForm() = runBlocking {
        vault.put(id, Credential.encode(credential))

        val onDisk = context.filesDir.parentFile!!.walkTopDown()
            .filter { it.isFile }
            .joinToString("\n") { runCatching { it.readText() }.getOrDefault("") }

        assertFalse(onDisk.contains("ghu_instrumented"))
        assertFalse(onDisk.contains("ghr_instrumented"))
        vault.remove(id)
    }
}
