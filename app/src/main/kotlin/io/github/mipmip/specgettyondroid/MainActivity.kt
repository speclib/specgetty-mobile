package io.github.mipmip.specgettyondroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.ui.SpecgettyNavHost
import io.github.mipmip.specgettyondroid.ui.theme.SpecgettyTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /**
     * A share that arrives while the app is open reaches [onNewIntent] rather
     * than a fresh start, which is probably the common case: you are likely to
     * have had the app open recently.
     */
    private val shared = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shared.value = sharedText(intent)
        val projects = (application as SpecgettyApplication).projects
        setContent {
            SpecgettyTheme {
                val incoming by shared.collectAsStateWithLifecycle()
                SpecgettyNavHost(
                    projects = projects,
                    sharedText = incoming,
                    onSharedTextHandled = { shared.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText(intent)?.let { shared.value = it }
    }

    /** Only `ACTION_SEND` with plain text is claimed, and nothing else. */
    private fun sharedText(intent: Intent?): String? = when {
        intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" ->
            intent.getStringExtra(Intent.EXTRA_TEXT)

        else -> null
    }
}
