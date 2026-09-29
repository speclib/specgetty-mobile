package io.github.mipmip.specgettyondroid.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The handoff from the camera's analyser thread to the composition.
 *
 * The analyser runs on an executor of its own, and a decoded code used to be
 * acted on there: navigation and Compose state were touched off the main
 * thread, which is a crash rather than a race. [offer] may be called from any
 * thread; [decoded] is read by the composition, which then acts.
 *
 * It also accepts one decode at a time. Several frames in a row recognise the
 * same code, and the guard stays closed until [rearm], so the scanner acts once
 * rather than once per frame.
 */
class DecodeRelay {

    private val _decoded = MutableStateFlow<String?>(null)
    val decoded: StateFlow<String?> = _decoded.asStateFlow()

    private val open = AtomicBoolean(true)

    /**
     * From the camera's thread. Accepted only while the relay is open.
     *
     * @return whether this call was the one that got through. Reported rather
     *   than inferred, because with several frames arriving at once there is no
     *   way for a caller to tell from the outside.
     */
    fun offer(text: String): Boolean {
        if (!open.compareAndSet(true, false)) return false
        _decoded.value = text
        return true
    }

    /** From the composition, once a decode has been dealt with and was not acted on. */
    fun rearm() {
        _decoded.value = null
        open.set(true)
    }
}
