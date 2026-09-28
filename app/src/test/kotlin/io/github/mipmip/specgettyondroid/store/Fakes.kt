package io.github.mipmip.specgettyondroid.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryPreferences : DataStore<Preferences> {

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

class FakeVault : TokenVault {

    val tokens = mutableMapOf<String, String>()
    var puts = 0
        private set

    override suspend fun put(id: String, token: String) {
        puts++
        tokens[id] = token
    }

    override suspend fun get(id: String): String? = tokens[id]

    override suspend fun remove(id: String) {
        tokens.remove(id)
    }
}
