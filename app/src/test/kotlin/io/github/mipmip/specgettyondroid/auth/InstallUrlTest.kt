package io.github.mipmip.specgettyondroid.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The button used to open `github.com/settings/installations`, which lists what
 * is already installed on your own account and gives no way to add an
 * organization. That is the one thing a person reaching this screen needs.
 */
class InstallUrlTest {

    @Test
    fun `the install address offers a new installation, not a listing`() {
        assertEquals(
            "https://github.com/apps/specgetty-on-droid/installations/new",
            GITHUB_INSTALL_URL,
        )
    }

    @Test
    fun `it is not the settings listing`() {
        assertFalse(GITHUB_INSTALL_URL.contains("settings/installations"))
    }

    @Test
    fun `the slug is the one the address is built from`() {
        assertEquals("specgetty-on-droid", GITHUB_APP_SLUG)
        assertEquals(true, GITHUB_INSTALL_URL.contains(GITHUB_APP_SLUG))
    }
}
