package io.github.mipmip.specgettyondroid

import android.app.Application
import io.github.mipmip.specgettyondroid.auth.DeviceFlow
import io.github.mipmip.specgettyondroid.auth.HttpAuthTransport
import io.github.mipmip.specgettyondroid.auth.Installations
import io.github.mipmip.specgettyondroid.auth.TokenSource
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.AndroidGit
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.KeystoreTokenVault
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import java.io.File

class SpecgettyApplication : Application() {

    lateinit var projects: ProjectRepository
        private set

    lateinit var deviceFlow: DeviceFlow
        private set

    lateinit var installations: Installations
        private set

    override fun onCreate() {
        super.onCreate()
        AndroidGit.install(File(filesDir, "git-config"))

        val vault = KeystoreTokenVault(this)
        val transport = HttpAuthTransport()
        deviceFlow = DeviceFlow(transport)
        installations = Installations(transport)

        projects = ProjectRepository(
            store = RepoStore(File(filesDir, "repos")),
            catalog = RepoRegistry(this, vault),
            tokens = TokenSource(vault, deviceFlow),
        )
    }
}
