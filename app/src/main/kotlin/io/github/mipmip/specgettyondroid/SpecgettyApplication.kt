package io.github.mipmip.specgettyondroid

import android.app.Application
import io.github.mipmip.specgettyondroid.repo.AndroidGit
import java.io.File

class SpecgettyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidGit.install(File(filesDir, "git-config"))
    }
}
