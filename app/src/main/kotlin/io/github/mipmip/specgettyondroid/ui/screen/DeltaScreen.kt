package io.github.mipmip.specgettyondroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.spec.DiffMark
import io.github.mipmip.specgettyondroid.ui.ListDetail
import io.github.mipmip.specgettyondroid.spec.NodeKind
import io.github.mipmip.specgettyondroid.spec.PartKind
import io.github.mipmip.specgettyondroid.spec.SpecNode
import io.github.mipmip.specgettyondroid.viewmodel.ComparisonView
import io.github.mipmip.specgettyondroid.viewmodel.DeltaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeltaScreen(viewModel: DeltaViewModel, onBack: () -> Unit) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (viewModel.isArchived) "archived · spec deltas" else "spec deltas",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) viewModel.clearSelection() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListDetail(
                hasSelection = selected != null,
                onClearSelection = viewModel::clearSelection,
                onLeave = onBack,
                emptyDetailMessage = "Choose a requirement or a scenario from the outline.",
                list = {
                    if (sections.isEmpty()) {
                        CentredMessage("This change touches no capability.")
                    } else {
                        Outline(viewModel, sections)
                    }
                },
                detail = { selected?.let { NodeCard(viewModel, it) } },
            )
        }
    }
}

@Composable
private fun Outline(
    viewModel: DeltaViewModel,
    sections: List<io.github.mipmip.specgettyondroid.viewmodel.DeltaSection>,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        sections.forEach { section ->
            item(key = "cap-${section.capability}") {
                Text(
                    section.capability,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                )
            }

            if (!section.isSpec) {
                section.problems.forEachIndexed { i, problem ->
                    item(key = "p-${section.capability}-$i") {
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                if (problem.line > 0) "line ${problem.line}" else "this file",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(problem.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                return@forEach
            }

            section.nodes.forEachIndexed { i, node ->
                item(key = "n-${node.path}-$i") {
                    OutlineRow(node) { viewModel.select(node) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutlineRow(node: SpecNode, onSelect: () -> Unit) {
    val indent = when (node.kind) {
        NodeKind.SCENARIO -> 24.dp
        else -> 0.dp
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = indent, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.kind == NodeKind.REQUIREMENT && node.op.isNotEmpty()) {
            AssistChip(onClick = onSelect, label = { Text(node.op) })
        }
        Text(
            node.title,
            style = when (node.kind) {
                NodeKind.SCENARIO -> MaterialTheme.typography.bodyMedium
                else -> MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (node.kind == NodeKind.SCENARIO) FontWeight.Normal else FontWeight.Medium,
        )
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeCard(viewModel: DeltaViewModel, node: SpecNode) {
    val comparison = viewModel.comparison
    val view by viewModel.view.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (node.op.isNotEmpty()) AssistChip(onClick = {}, label = { Text(node.op) })
            Text(node.title, style = MaterialTheme.typography.titleMedium)
        }

        if (comparison != null) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComparisonView.entries.forEach { entry ->
                    FilterChip(
                        selected = entry == view,
                        onClick = { viewModel.showView(entry) },
                        label = {
                            Text(
                                when (entry) {
                                    ComparisonView.DIFFERENCE -> "Difference"
                                    ComparisonView.ORIGINAL -> "Original"
                                    ComparisonView.PROPOSED -> "Proposed"
                                },
                            )
                        },
                    )
                }
            }
            when (view) {
                ComparisonView.DIFFERENCE -> DiffBody(comparison.diff)
                ComparisonView.ORIGINAL -> SourceBody(comparison.original)
                ComparisonView.PROPOSED -> SourceBody(comparison.proposed)
            }
            return@Column
        }

        NodeBody(node)
    }
}

@Composable
private fun NodeBody(node: SpecNode) {
    LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
        if (node.body.isNotEmpty()) {
            item { Text(node.body, style = MaterialTheme.typography.bodyMedium) }
        }
        node.parts.forEachIndexed { i, part ->
            item(key = "part-$i") {
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    if (part.kind == PartKind.CLAUSE) {
                        Text(
                            part.keyword,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            part.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    } else {
                        Text(part.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffBody(lines: List<io.github.mipmip.specgettyondroid.spec.DiffLine>) {
    LazyColumn(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        items(lines.size) { i ->
            val line = lines[i]
            val (prefix, background) = when (line.mark) {
                DiffMark.ORIGINAL_ONLY -> "-" to Color(0x33FF5252)
                DiffMark.PROPOSED_ONLY -> "+" to Color(0x332E7D5B)
                DiffMark.COMMON -> " " to Color.Transparent
            }
            Text(
                "$prefix ${line.text}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().background(background).padding(2.dp),
            )
        }
    }
}

@Composable
private fun SourceBody(text: String) {
    LazyColumn(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        val lines = text.lines()
        items(lines.size) { i ->
            Text(
                lines[i].ifEmpty { " " },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CentredMessage(message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
