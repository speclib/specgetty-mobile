package io.github.mipmip.specgettyondroid.data

import io.github.mipmip.specgettyondroid.spec.DeltaParser
import io.github.mipmip.specgettyondroid.spec.ParsedSpec
import io.github.mipmip.specgettyondroid.spec.SpecParser
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * A spec is parsed when it is opened and then kept: the same spec is opened,
 * left and reopened as a reader moves between the outline and its cards.
 *
 * Keyed by path, length and modification time, so a refresh that rewrote a file
 * reparses it and one that left it alone does not.
 */
class SpecCache {

    private data class Key(val path: String, val length: Long, val modified: Long, val delta: Boolean)

    private val entries = ConcurrentHashMap<Key, ParsedSpec>()

    var parses: Int = 0
        private set

    val size: Int get() = entries.size

    fun spec(capability: String, file: File): ParsedSpec =
        cached(file, delta = false) { SpecParser.parse(capability, file.readText()) }

    fun delta(capability: String, file: File): ParsedSpec =
        cached(file, delta = true) { DeltaParser.parse(capability, file.readText()) }

    private fun cached(file: File, delta: Boolean, parse: () -> ParsedSpec): ParsedSpec {
        val key = Key(file.path, file.length(), file.lastModified(), delta)
        entries[key]?.let { return it }
        val parsed = parse()
        parses++
        entries[key] = parsed
        return parsed
    }

    fun clear() {
        entries.clear()
    }
}
