package io.github.mipmip.specgettyondroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.tasks.TaskItem
import io.github.mipmip.specgettyondroid.ui.MarkdownText
import io.github.mipmip.specgettyondroid.viewmodel.ArtifactContent
import io.github.mipmip.specgettyondroid.viewmodel.ChangeTab
import io.github.mipmip.specgettyondroid.viewmodel.ChangeViewModel

/**
 * U+25A2 and U+25A3: one cell wide, from the same block, and differing by a
 * filled centre rather than by colour. The same pair specgetty draws.
 */
private const val BOX_EMPTY = "▢"
private const val BOX_FILLED = "▣"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeScreen(
    viewModel: ChangeViewModel,
    onBack: () -> Unit,
    onOpenDelta: (String) -> Unit,
) {
    val change by viewModel.change.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val tabs = viewModel.tabs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.title, style = MaterialTheme.typography.titleMedium)
                        val subtitle = buildList {
                            if (viewModel.isArchived) add("archived")
                            viewModel.date?.let { add(it) }
                            val stats = viewModel.taskStats
                            if (!stats.isEmpty) add("${stats.done}/${stats.total} tasks")
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                subtitle.joinToString("  ·  "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (change == null) {
                Empty("This change is no longer in the project.")
                return@Column
            }

            val selected = tab ?: tabs.firstOrNull() ?: return@Column
            ScrollableTabRow(selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0)) {
                tabs.forEach { entry ->
                    Tab(
                        selected = entry == selected,
                        onClick = { viewModel.selectTab(entry) },
                        text = { Text(entry.label) },
                    )
                }
            }

            when (selected) {
                is ChangeTab.Artifact -> ArtifactTab(viewModel, selected.fileName)
                ChangeTab.Tasks -> TasksTab(viewModel)
                ChangeTab.Specs -> ChangeSpecsTab(viewModel, onOpenDelta)
            }
        }
    }
}

@Composable
private fun ArtifactTab(viewModel: ChangeViewModel, fileName: String) {
    when (val content = viewModel.artifact(fileName)) {
        is ArtifactContent.Markdown -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            MarkdownText(content.text, Modifier.fillMaxWidth())
        }

        is ArtifactContent.Unreadable -> Empty(content.reason)
    }
}

@Composable
private fun TasksTab(viewModel: ChangeViewModel) {
    val stats = viewModel.taskStats
    val items = viewModel.tasks.items

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (stats.isEmpty) "No tasks" else "${stats.done} of ${stats.total} done",
                style = MaterialTheme.typography.titleMedium,
            )
            if (!stats.isEmpty) {
                LinearProgressIndicator(
                    progress = { stats.done.toFloat() / stats.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        HorizontalDivider()

        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            var lastHeading: String? = null
            items.forEachIndexed { i, item ->
                if (item.heading.isNotEmpty() && item.heading != lastHeading) {
                    lastHeading = item.heading
                    item(key = "h-$i") {
                        Text(
                            item.heading,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                }
                item(key = "t-$i") { TaskRow(item) }
            }
        }
    }
}

/** A box, not a checkbox. Ticking one is Phase 2, and nothing here writes. */
@Composable
private fun TaskRow(item: TaskItem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (item.done) BOX_FILLED else BOX_EMPTY,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyLarge,
        )
        Column {
            Text(item.text, style = MaterialTheme.typography.bodyMedium)
            item.continuation.forEach {
                Text(
                    it.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChangeSpecsTab(viewModel: ChangeViewModel, onOpenDelta: (String) -> Unit) {
    val capabilities = viewModel.capabilities
    if (capabilities.isEmpty()) {
        Empty("This change touches no capability.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(capabilities, key = { it.name }) { capability ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDelta(capability.name) }
                    .padding(vertical = 12.dp),
            ) {
                Text(capability.name, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun Empty(message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
