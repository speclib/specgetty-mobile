package io.github.mipmip.specgettyondroid

import android.os.Looper
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.capture.CaptureResult
import io.github.mipmip.specgettyondroid.capture.DecodeRelay
import io.github.mipmip.specgettyondroid.capture.UrlCapture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * The crash this change fixes: the camera's analyser ran the decode callback on
 * its own thread, and that callback navigated. Navigation off the main thread
 * throws, which is why the app died the instant a code was recognised.
 *
 * These assert the shape `ScannerScreen` now uses: the analyser only offers,
 * and the composition acts.
 */
class DecodeThreadTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aDecodeOfferedFromABackgroundThreadIsActedOnTheMainThread() {
        val relay = DecodeRelay()
        val ranOn = AtomicReference<String>()
        val url = AtomicReference<String>()

        compose.setContent {
            val decoded by relay.decoded.collectAsStateWithLifecycle()
            LaunchedEffect(decoded) {
                val text = decoded ?: return@LaunchedEffect
                ranOn.set(
                    if (Looper.myLooper() == Looper.getMainLooper()) "main" else "background",
                )
                (UrlCapture.capture(text) as? CaptureResult.Found)?.let { url.set(it.url) }
            }
        }

        val pool = Executors.newSingleThreadExecutor()
        pool.submit {
            check(Looper.myLooper() != Looper.getMainLooper()) { "the offer must be off-main" }
            relay.offer("scan this https://github.com/speclib/specgetty/issues/12 please")
        }
        compose.waitUntil(10_000) { ranOn.get() != null }
        pool.shutdownNow()

        assertEquals("the decode was acted on off the main thread", "main", ranOn.get())
        assertEquals("https://github.com/speclib/specgetty", url.get())
    }

    @Test
    fun severalFramesRecognisingTheSameCodeActOnce() {
        val relay = DecodeRelay()
        val acted = AtomicInteger(0)
        val seen = AtomicBoolean(false)

        compose.setContent {
            val decoded by relay.decoded.collectAsStateWithLifecycle()
            LaunchedEffect(decoded) {
                if (decoded != null) {
                    acted.incrementAndGet()
                    seen.set(true)
                }
            }
        }

        val pool = Executors.newFixedThreadPool(8)
        repeat(40) { pool.submit { relay.offer("https://example.test/a") } }
        compose.waitUntil(10_000) { seen.get() }
        compose.waitForIdle()
        pool.shutdownNow()

        assertEquals("forty frames, one action", 1, acted.get())
    }

    @Test
    fun aCodeThatIsNotAUrlLetsScanningContinue() {
        val relay = DecodeRelay()
        val notices = AtomicInteger(0)

        compose.setContent {
            val decoded by relay.decoded.collectAsStateWithLifecycle()
            LaunchedEffect(decoded) {
                val text = decoded ?: return@LaunchedEffect
                if (UrlCapture.capture(text) is CaptureResult.NoUrl) {
                    notices.incrementAndGet()
                    relay.rearm()
                }
            }
        }

        relay.offer("nothing useful here")
        compose.waitUntil(10_000) { notices.get() == 1 }

        // Rearmed, so the next code still gets through.
        relay.offer("also not a url")
        compose.waitUntil(10_000) { notices.get() == 2 }
        assertTrue(notices.get() == 2)
    }
}
