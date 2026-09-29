package io.github.mipmip.specgettyondroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.index.ProjectCounts
import io.github.mipmip.specgettyondroid.viewmodel.ProjectChoice
import io.github.mipmip.specgettyondroid.viewmodel.RepoListViewModel
import io.github.mipmip.specgettyondroid.viewmodel.RepoRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    viewModel: RepoListViewModel,
    onOpenRepo: (String) -> Unit,
    onScan: () -> Unit = {},
    onAuthorize: (String) -> Unit = {},
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val formOpen by viewModel.formOpen.collectAsStateWithLifecycle()
    val pendingRemoval by viewModel.pendingRemoval.collectAsStateWithLifecycle()
    val choice by viewModel.choice.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Specgetty on Droid") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openForm() },
                modifier = Modifier.semantics { contentDescription = "Add a repository" },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (rows.isEmpty()) {
                EmptyRepoList()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows, key = { it.config.id }) { row ->
                        RepoCard(
                            row = row,
                            onOpen = {
                                viewModel.activate(row.config.id)
                                if (row.canOpen) onOpenRepo(row.config.id)
                            },
                            onRemove = { viewModel.askToRemove(row.config.id) },
                            onRetry = { viewModel.refresh(row.config.id) },
                        )
                    }
                }
            }
        }
    }

    if (formOpen) {
        AddRepoSheet(viewModel, onScan, onAuthorize)
    }

    choice?.let { ChooseProjectsSheet(viewModel, it) }

    pendingRemoval?.let { id ->
        val going = rows.firstOrNull { it.config.id == id }
        val label = going?.config?.label ?: id
        // The downloaded copy belongs to the repository, not to the row, so it
        // outlives this removal whenever another row still reads from it.
        val shared = going != null && rows.count { it.config.url == going.config.url } > 1
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoval,
            title = { Text("Remove $label?") },
            text = {
                Text(
                    if (shared) {
                        "Only this project is removed. The downloaded copy and the " +
                            "access token stay, because another project in the same " +
                            "repository still uses them."
                    } else {
                        "Its downloaded copy and its access token are deleted from " +
                            "this device."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRemoval) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRemoval) { Text("Keep") }
            },
        )
    }
}

@Composable
private fun EmptyRepoList() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No repositories yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Add one by its HTTPS clone URL to read the OpenSpec project in it.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RepoCard(
    row: RepoRow,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = row.canOpen, onClick = onOpen),
        colors = if (row.isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.config.label, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.semantics {
                        contentDescription = "Remove ${row.config.label}"
                    },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
            Text(
                if (row.config.path.isEmpty()) {
                    row.config.url
                } else {
                    "${row.config.path} in ${row.config.url}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Where ${row.config.label} came from"
                },
            )

            row.counts?.let { Statistics(it) }

            row.message?.let { message ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (row.isLoading) {
                        CircularProgressIndicator(Modifier.padding(2.dp))
                    }
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
                if (!row.isLoading) {
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
            }
        }
    }
}

@Composable
private fun Statistics(counts: ProjectCounts) {
    val parts = buildList {
        add("${counts.specs} specs")
        add("${counts.active} active")
        add("${counts.archived} archived")
        // A project with no tasks shows no task figure rather than `0/0`.
        if (!counts.tasks.isEmpty) add("tasks ${counts.tasks.done}/${counts.tasks.total}")
    }
    Text(
        parts.joinToString("  ·  "),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { contentDescription = parts.joinToString(", ") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRepoSheet(
    viewModel: RepoListViewModel,
    onScan: () -> Unit,
    onAuthorize: (String) -> Unit,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = viewModel::closeForm) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add a repository", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = form.url,
                onValueChange = viewModel::onUrlChanged,
                label = { Text("HTTPS clone URL") },
                singleLine = true,
                isError = form.error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Repository URL" },
            )
            OutlinedTextField(
                value = form.label,
                onValueChange = viewModel::onLabelChanged,
                label = { Text("Name (optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Repository name" },
            )
            if (form.hasAuthorized) {
                Row(
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = "Authorized with GitHub"
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Authorized with GitHub", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::discardAuthorization) { Text("Forget") }
                }
            } else {
                OutlinedTextField(
                    value = form.token,
                    onValueChange = viewModel::onTokenChanged,
                    label = { Text("Access token (private repositories only)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Access token" },
                )

                if (form.canAuthorize) {
                    TextButton(
                        onClick = { onAuthorize(form.url.trim()) },
                        modifier = Modifier.semantics {
                            contentDescription = "Authorize with GitHub"
                        },
                    ) {
                        Text("Or authorize with GitHub instead")
                    }
                }
            }

            form.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        viewModel.closeForm()
                        onScan()
                    },
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Text("  Scan a code")
                }
                Row {
                    TextButton(onClick = viewModel::closeForm) { Text("Cancel") }
                    TextButton(
                        onClick = viewModel::submit,
                        enabled = !form.busy,
                        modifier = Modifier.semantics {
                            contentDescription = "Confirm adding the repository"
                        },
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

/**
 * One repository, several projects. Nothing is in the list while this stands,
 * so backing out leaves the catalog exactly as it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseProjectsSheet(viewModel: RepoListViewModel, choice: ProjectChoice) {
    // Fully expanded, not the half height a sheet opens at: this one exists to
    // show a list, and a partly open sheet clips the end of it.
    ModalBottomSheet(
        onDismissRequest = viewModel::cancelChoice,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // Scrollable as well, for a repository holding more than a screenful.
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "This repository holds several projects",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics {
                    contentDescription = "Choose which projects to add"
                },
            )
            Text(
                "Choose the ones to add. Each becomes its own row, and they share " +
                    "one downloaded copy.",
                style = MaterialTheme.typography.bodyMedium,
            )

            choice.projects.forEach { path ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { viewModel.toggleChoice(path) }
                        .padding(vertical = 4.dp)
                        .semantics { contentDescription = "Project ${choice.nameOf(path)}" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = path in choice.selected,
                        onCheckedChange = { viewModel.toggleChoice(path) },
                    )
                    Text(choice.nameOf(path), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = viewModel::cancelChoice) { Text("Cancel") }
                TextButton(
                    onClick = viewModel::confirmChoice,
                    enabled = choice.canConfirm,
                    modifier = Modifier.semantics {
                        contentDescription = "Add the chosen projects"
                    },
                ) {
                    Text("Add")
                }
            }
        }
    }
}
