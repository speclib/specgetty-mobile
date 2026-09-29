package io.github.mipmip.specgettyondroid.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DecodeRelayTest {

    @Test
    fun `nothing has been decoded to begin with`() {
        assertNull(DecodeRelay().decoded.value)
    }

    @Test
    fun `an offered decode is readable, and the offer says it was taken`() {
        val relay = DecodeRelay()
        assertTrue(relay.offer("https://example.test/a"))
        assertEquals("https://example.test/a", relay.decoded.value)
    }

    @Test
    fun `a second offer is refused until the relay is rearmed`() {
        val relay = DecodeRelay()
        assertTrue(relay.offer("first"))
        assertFalse(relay.offer("second"))
        assertEquals("first", relay.decoded.value)
    }

    @Test
    fun `rearming clears the decode and accepts another`() {
        val relay = DecodeRelay()
        relay.offer("first")
        relay.rearm()
        assertNull(relay.decoded.value)

        assertTrue(relay.offer("second"))
        assertEquals("second", relay.decoded.value)
    }

    /**
     * Several frames in a row recognise the same code, and the analyser runs on
     * its own thread. Exactly one of them may get through.
     */
    @Test
    fun `many threads offering at once produce exactly one decode`() {
        repeat(50) {
            val relay = DecodeRelay()
            val threads = 16
            val pool = Executors.newFixedThreadPool(threads)
            val start = CountDownLatch(1)
            val done = CountDownLatch(threads)
            val accepted = java.util.concurrent.atomic.AtomicInteger(0)

            repeat(threads) { i ->
                pool.submit {
                    start.await()
                    if (relay.offer("code-$i")) accepted.incrementAndGet()
                    done.countDown()
                }
            }
            start.countDown()
            done.await(5, TimeUnit.SECONDS)
            pool.shutdownNow()

            assertEquals("one decode landed", 1, accepted.get())
            assertEquals(true, relay.decoded.value?.startsWith("code-"))
        }
    }

    @Test
    fun `rearming while offers are in flight still admits one at a time`() {
        val relay = DecodeRelay()
        relay.offer("a")
        assertEquals("a", relay.decoded.value)
        relay.offer("b")
        assertEquals("a", relay.decoded.value)
        relay.rearm()
        relay.offer("c")
        relay.offer("d")
        assertEquals("c", relay.decoded.value)
    }
}
