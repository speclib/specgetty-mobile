package io.github.mipmip.specgettyondroid

import android.app.Application
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.repo.AndroidGit
import io.github.mipmip.specgettyondroid.repo.RepoStore
import io.github.mipmip.specgettyondroid.store.KeystoreTokenVault
import io.github.mipmip.specgettyondroid.store.RepoRegistry
import java.io.File

class SpecgettyApplication : Application() {

    lateinit var projects: ProjectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AndroidGit.install(File(filesDir, "git-config"))
        projects = ProjectRepository(
            store = RepoStore(File(filesDir, "repos")),
            catalog = RepoRegistry(this, KeystoreTokenVault(this)),
        )
    }
}
