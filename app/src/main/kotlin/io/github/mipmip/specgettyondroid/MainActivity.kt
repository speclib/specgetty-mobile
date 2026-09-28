package io.github.mipmip.specgettyondroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.mipmip.specgettyondroid.ui.SpecgettyNavHost
import io.github.mipmip.specgettyondroid.ui.theme.SpecgettyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val projects = (application as SpecgettyApplication).projects
        setContent {
            SpecgettyTheme {
                SpecgettyNavHost(projects)
            }
        }
    }
}
