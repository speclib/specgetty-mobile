package io.github.mipmip.specgettyondroid

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.github.mipmip.specgettyondroid.store.TokenVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The real `RepoRegistry` against storage that starts empty every test. A
 * DataStore on disk would carry one test's list into the next.
 */
class InstrumentedPreferences : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())
    private val lock = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = lock.withLock {
        val next = transform(state.value)
        state.value = next
        next
    }
}

class InstrumentedVault : TokenVault {

    val tokens = mutableMapOf<String, String>()

    override suspend fun put(id: String, token: String) {
        tokens[id] = token
    }

    override suspend fun get(id: String): String? = tokens[id]

    override suspend fun remove(id: String) {
        tokens.remove(id)
    }
}
