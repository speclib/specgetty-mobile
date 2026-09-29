package io.github.mipmip.specgettyondroid.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.repoDataStore: DataStore<Preferences> by preferencesDataStore("repos")

private val REPOS = stringPreferencesKey("repos")

interface RepoCatalog {
    val repos: Flow<RepoList>

    suspend fun current(): RepoList

    suspend fun add(url: String, label: String, token: String?, path: String = ""): RepoConfig

    suspend fun remove(id: String)

    suspend fun activate(id: String)

    suspend fun tokenFor(id: String): String?
}

class RepoRegistry(
    private val dataStore: DataStore<Preferences>,
    private val vault: TokenVault,
) : RepoCatalog {

    constructor(context: Context, vault: TokenVault) :
        this(context.applicationContext.repoDataStore, vault)

    override val repos: Flow<RepoList> = dataStore.data.map { prefs -> decode(prefs[REPOS]) }

    override suspend fun current(): RepoList = repos.first()

    override suspend fun add(
        url: String,
        label: String,
        token: String?,
        path: String,
    ): RepoConfig {
        val trimmed = url.trim()
        val cleaned = normalisePath(path)
        // The credential belongs to the repository, so it is keyed by the clone.
        if (!token.isNullOrBlank()) vault.put(repoIdFor(trimmed), token)
        val config = RepoConfig(
            id = entryIdFor(trimmed, cleaned),
            url = trimmed,
            label = label.ifBlank { if (cleaned.isEmpty()) labelFor(trimmed) else cleaned },
            hasToken = !token.isNullOrBlank() || vault.get(repoIdFor(trimmed)) != null,
            path = cleaned,
        )
        update { it.add(config) }
        return config
    }

    /**
     * The credential goes with the last entry on a repository, not the first.
     * Removing one of several would otherwise lock the siblings out of the
     * repository they still point at.
     */
    override suspend fun remove(id: String) {
        val list = current()
        val going = list.repos.firstOrNull { it.id == id }
        if (going != null && !list.othersShare(id)) vault.remove(going.cloneId)
        update { it.remove(id) }
    }

    override suspend fun activate(id: String) = update { it.activate(id) }

    override suspend fun tokenFor(id: String): String? {
        val cloneId = current().repos.firstOrNull { it.id == id }?.cloneId ?: id
        return vault.get(cloneId)?.let { Credential.decode(it).token }
    }

    private suspend fun update(transform: (RepoList) -> RepoList) {
        dataStore.edit { prefs ->
            prefs[REPOS] = JSON.encodeToString(transform(decode(prefs[REPOS])))
        }
    }

    private fun decode(raw: String?): RepoList = when {
        raw.isNullOrBlank() -> RepoList()
        else -> runCatching { JSON.decodeFromString<RepoList>(raw) }.getOrDefault(RepoList())
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
