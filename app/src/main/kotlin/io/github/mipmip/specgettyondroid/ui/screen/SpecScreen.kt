package io.github.mipmip.specgettyondroid.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.spec.NodeKind
import io.github.mipmip.specgettyondroid.spec.PartKind
import io.github.mipmip.specgettyondroid.spec.SpecNode
import io.github.mipmip.specgettyondroid.spec.SpecProblem
import io.github.mipmip.specgettyondroid.ui.ListDetail
import io.github.mipmip.specgettyondroid.ui.MarkdownText
import io.github.mipmip.specgettyondroid.viewmodel.SpecScreenState
import io.github.mipmip.specgettyondroid.viewmodel.SpecViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecScreen(viewModel: SpecViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val showingRaw by viewModel.showingRaw.collectAsStateWithLifecycle()
    val rawText by viewModel.rawText.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.capability, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                showingRaw -> viewModel.showRaw(false)
                                selected != null -> viewModel.clearSelection()
                                else -> onBack()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showingRaw) {
                // Its own level: back returns to the report rather than leaving
                // the spec, whatever the window's width.
                BackHandler(enabled = true) { viewModel.showRaw(false) }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    MarkdownText(rawText, Modifier.fillMaxWidth())
                }
                return@Column
            }

            ListDetail(
                hasSelection = selected != null,
                onClearSelection = viewModel::clearSelection,
                onLeave = onBack,
                emptyDetailMessage = "Choose a requirement or a scenario from the outline.",
                list = {
                    when (val s = state) {
                        SpecScreenState.Loading -> Centred("Loading")
                        is SpecScreenState.Unreadable -> Centred(s.reason)
                        is SpecScreenState.Report ->
                            ProblemReport(s.problems) { viewModel.showRaw(true) }

                        is SpecScreenState.Outline ->
                            SpecOutline(s.nodes) { viewModel.select(it) }
                    }
                },
                detail = { selected?.let { NodeDetail(it) } },
            )
        }
    }
}

@Composable
private fun SpecOutline(nodes: List<SpecNode>, onSelect: (SpecNode) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(nodes.size) { i ->
            val node = nodes[i]
            val indent = if (node.kind == NodeKind.SCENARIO) 24.dp else 0.dp
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(node) }
                    .padding(start = indent, top = 10.dp, bottom = 10.dp),
            ) {
                Text(
                    node.title,
                    style = when (node.kind) {
                        NodeKind.SCENARIO -> MaterialTheme.typography.bodyMedium
                        else -> MaterialTheme.typography.bodyLarge
                    },
                    // Drawn differently, not merely indented: a wrapped label
                    // occupies the indentation the level below it would use.
                    fontWeight = when (node.kind) {
                        NodeKind.SCENARIO -> FontWeight.Normal
                        else -> FontWeight.Medium
                    },
                    color = when (node.kind) {
                        NodeKind.SCENARIO -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun NodeDetail(node: SpecNode) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                node.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        if (node.body.isNotEmpty()) {
            item { Text(node.body, style = MaterialTheme.typography.bodyMedium) }
        }
        items(node.parts.size) { i ->
            val part = node.parts[i]
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (part.kind == PartKind.CLAUSE) {
                    Text(
                        part.keyword,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        part.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                    )
                } else {
                    Text(part.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ProblemReport(problems: List<SpecProblem>, onShowRaw: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text(
                    "This file is not a spec OpenSpec can read",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Every reason is listed, so it can be repaired in one pass.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(problems.size) { i ->
            val problem = problems[i]
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    if (problem.line > 0) "line ${problem.line}" else "the file as a whole",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(problem.text, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
        item {
            TextButton(onClick = onShowRaw, modifier = Modifier.padding(top = 12.dp)) {
                Text("Read the file anyway")
            }
        }
    }
}

@Composable
private fun Centred(message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
