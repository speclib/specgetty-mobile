package io.github.mipmip.specgettyondroid.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.auth.GITHUB_INSTALL_URL
import io.github.mipmip.specgettyondroid.store.Credential
import io.github.mipmip.specgettyondroid.viewmodel.AuthState
import io.github.mipmip.specgettyondroid.viewmodel.AuthorizeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizeScreen(
    viewModel: AuthorizeViewModel,
    onAuthorized: (Credential.GitHub) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.begin() }

    LaunchedEffect(state) {
        (state as? AuthState.Approved)?.let { onAuthorized(it.credential) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authorize with GitHub") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.cancel()
                            onDismiss()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                AuthState.Idle, AuthState.RequestingCode -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Asking GitHub for a code")
                }

                is AuthState.Waiting -> Waiting(s)
                is AuthState.Approved -> Granted(s)
                is AuthState.NeedsInstalling -> NeedsInstalling(s)
                is AuthState.Failed -> Failed(s.message, viewModel::begin, onDismiss)
            }
        }
    }
}

@Composable
private fun Waiting(state: AuthState.Waiting) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val code = state.code

    Text("Type this code on GitHub", style = MaterialTheme.typography.titleMedium)

    Text(
        code.userCode,
        style = MaterialTheme.typography.displaySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.semantics { contentDescription = "The code to type" },
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = { clipboard.setText(AnnotatedString(code.userCode)) },
            modifier = Modifier.semantics { contentDescription = "Copy the code" },
        ) {
            Text("Copy")
        }
        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUri))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            modifier = Modifier.semantics { contentDescription = "Open GitHub" },
        ) {
            Text("Open GitHub")
        }
    }

    Text(code.verificationUri, style = MaterialTheme.typography.bodySmall)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(
            if (state.reconnecting) {
                "Waiting for the connection to come back. The code is still good."
            } else {
                "Waiting for you to approve"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    Text(
        "Approving is only half of it: GitHub also asks which repositories this " +
            "app may read. Choose them on the same visit.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Granted(state: AuthState.Approved) {
    Text("Authorized", style = MaterialTheme.typography.titleMedium)
    Text(
        describe(state.reach),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "What was granted" },
    )
    Text(
        "It may read them and nothing else. It cannot write.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NeedsInstalling(state: AuthState.NeedsInstalling) {
    val context = LocalContext.current

    Text("Almost there", style = MaterialTheme.typography.titleMedium)
    Text(
        if (state.reach.reachesNothing) {
            "GitHub accepted the authorization, but no repository has been chosen for " +
                "it yet, so it can read nothing."
        } else {
            "The authorization does not include this repository. " + describe(state.reach)
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { contentDescription = "What is still needed" },
    )
    Text(
        "Open GitHub, choose this repository for the app, then try again. A " +
            "repository owned by an organization needs the app installed on that " +
            "organization, which an owner there may have to approve.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_INSTALL_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    ) {
        Text("Choose repositories")
    }
}

@Composable
private fun Failed(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Text("Could not authorize", style = MaterialTheme.typography.titleMedium)
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { contentDescription = "Why it failed" },
    )
    Text(
        "Typing an access token still works.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onDismiss) { Text("Back") }
        Button(
            onClick = onRetry,
            modifier = Modifier.semantics { contentDescription = "Start again" },
        ) {
            Text("Start again")
        }
    }
}

/**
 * Names the accounts covered wholesale rather than saying "everything". An
 * installation on one account says nothing about an organization's repositories,
 * and the old wording promised otherwise.
 */
private fun describe(reach: io.github.mipmip.specgettyondroid.auth.Reach): String {
    val parts = mutableListOf<String>()
    if (reach.wholeAccounts.isNotEmpty()) {
        parts += "every repository in " + reach.wholeAccounts.joinToString(" and ")
    }
    if (reach.repositories.isNotEmpty()) {
        parts += reach.repositories.joinToString(", ")
    }
    return if (parts.isEmpty()) {
        "This app may read nothing yet."
    } else {
        "This app may read " + parts.joinToString("; ") + "."
    }
}


