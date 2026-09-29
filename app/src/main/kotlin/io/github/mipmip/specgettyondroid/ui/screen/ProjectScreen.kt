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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.index.ChangeMatch
import io.github.mipmip.specgettyondroid.index.MatchKind
import io.github.mipmip.specgettyondroid.project.ChangeInfo
import io.github.mipmip.specgettyondroid.ui.MarkdownText
import io.github.mipmip.specgettyondroid.viewmodel.ProjectTab
import io.github.mipmip.specgettyondroid.viewmodel.Properties
import io.github.mipmip.specgettyondroid.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    viewModel: ProjectViewModel,
    label: String,
    onBack: () -> Unit,
    onOpenChange: (ChangeInfo) -> Unit,
    onOpenSpec: (String) -> Unit,
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val counts = viewModel.index?.counts

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        counts?.let {
                            val parts = buildList {
                                add("${it.specs} specs")
                                add("${it.active} active")
                                add("${it.archived} archived")
                                if (!it.tasks.isEmpty) {
                                    add("tasks ${it.tasks.done}/${it.tasks.total}")
                                }
                            }
                            Text(
                                parts.joinToString("  ·  "),
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
            val message = viewModel.message
            if (message != null) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(message, style = MaterialTheme.typography.titleMedium)
                }
                return@Column
            }

            ProjectTabs(selected = tab, onSelect = viewModel::selectTab)

            when (tab) {
                ProjectTab.OVERVIEW -> OverviewTab(viewModel, onOpenChange)
                ProjectTab.CHANGES -> ChangesTab(viewModel, onOpenChange)
                ProjectTab.SPECS -> SpecsTab(viewModel, onOpenSpec)
                ProjectTab.PROPERTIES -> PropertiesTab(viewModel)
            }
        }
    }
}

/**
 * Scrollable, not equal-width. `TabRow` gives each of the four a quarter of the
 * screen, which is narrower than "Properties" on a phone, and Compose then
 * breaks the word because there is no space in it to break at. Each tab takes
 * the width its label needs, and the row scrolls when they do not all fit.
 *
 * Internal rather than private so the test can render the row the app renders,
 * instead of a copy of it that could drift.
 */
@Composable
internal fun ProjectTabs(selected: ProjectTab, onSelect: (ProjectTab) -> Unit) {
    ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 0.dp) {
        ProjectTab.entries.forEach { entry ->
            Tab(
                selected = entry == selected,
                onClick = { onSelect(entry) },
                text = { Text(entry.label, maxLines = 1, softWrap = false) },
            )
        }
    }
}

@Composable
private fun OverviewTab(viewModel: ProjectViewModel, onOpenChange: (ChangeInfo) -> Unit) {
    val index = viewModel.index ?: return
    val active = index.activeChanges
    val archived = index.archivedChanges.take(10)

    if (index.project.isEmpty) {
        Empty("This project holds no specs and no changes yet.")
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { SectionHeading("Active changes") }
        if (active.isEmpty()) {
            item { Text("Nothing is being worked on.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(active, key = { "a-${it.name}" }) { change ->
            ChangeRow(change, emptyList()) { onOpenChange(change) }
        }

        item { SectionHeading("Recently archived") }
        if (archived.isEmpty()) {
            item { Text("Nothing archived yet.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(archived, key = { "r-${it.date}-${it.name}" }) { change ->
            ChangeRow(change, emptyList()) { onOpenChange(change) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangesTab(viewModel: ProjectViewModel, onOpenChange: (ChangeInfo) -> Unit) {
    val changes by viewModel.changes.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = changes.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Search changes") },
            singleLine = true,
            trailingIcon = {
                if (changes.query.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearQuery) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear the search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val matcher = viewModel.matcher
            MatcherChip("Name", MatchKind.FUZZY_NAME, matcher, viewModel)
            MatcherChip("Exact name", MatchKind.LITERAL_NAME, matcher, viewModel)
            MatcherChip("In the text", MatchKind.BODY, matcher, viewModel)
        }

        if (changes.isEmpty) {
            Empty(
                if (changes.isFiltered) {
                    "No changes match \"${changes.query}\""
                } else {
                    "This project has no changes."
                },
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (changes.active.isNotEmpty()) {
                item { SectionHeading("Active") }
                items(changes.active, key = { "a-${it.change.name}" }) { match ->
                    ChangeRow(match.change, match.matchedFiles) { onOpenChange(match.change) }
                }
            }
            if (changes.archived.isNotEmpty()) {
                item { SectionHeading("Archived") }
                items(changes.archived, key = { "r-${it.change.date}-${it.change.name}" }) { match ->
                    ChangeRow(match.change, match.matchedFiles) { onOpenChange(match.change) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatcherChip(
    label: String,
    kind: MatchKind,
    selected: MatchKind,
    viewModel: ProjectViewModel,
) {
    FilterChip(
        selected = kind == selected,
        onClick = { viewModel.selectMatcher(kind) },
        label = { Text(label) },
    )
}

@Composable
private fun SpecsTab(viewModel: ProjectViewModel, onOpenSpec: (String) -> Unit) {
    val names = viewModel.index?.capabilityNames.orEmpty()
    if (names.isEmpty()) {
        Empty("This project has no specs yet.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(names, key = { it }) { name ->
            Column(
                Modifier.fillMaxWidth().clickable { onOpenSpec(name) }.padding(vertical = 12.dp),
            ) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PropertiesTab(viewModel: ProjectViewModel) {
    val properties = viewModel.properties
    val schemas = viewModel.schemasInUse

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (properties) {
            is Properties.Description -> {
                SectionHeading("project.md")
                MarkdownText(properties.markdown, Modifier.fillMaxWidth())
            }

            is Properties.Configuration -> {
                SectionHeading(properties.fileName)
                YamlText(properties.yaml)
            }

            Properties.None -> Text(
                "This project has no project.md and no configuration file.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SectionHeading("Workflow schemas")
        if (schemas.isEmpty()) {
            Text("No change names a schema.", style = MaterialTheme.typography.bodyMedium)
        } else {
            schemas.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

/**
 * Monospaced, with comments dimmed and keys emphasised. `BRIEFING.md` asks for
 * YAML highlighting, and for a configuration file of a dozen lines this is as
 * far as that needs to go.
 */
@Composable
private fun YamlText(yaml: String) {
    Column {
        yaml.lines().forEach { line ->
            val trimmed = line.trimStart()
            val isComment = trimmed.startsWith("#")
            val isKey = !isComment && trimmed.contains(':')
            Text(
                text = line.ifEmpty { " " },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isComment -> MaterialTheme.colorScheme.onSurfaceVariant
                    isKey -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun ChangeRow(change: ChangeInfo, matchedFiles: List<String>, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 10.dp)) {
        Text(change.name, style = MaterialTheme.typography.bodyLarge)

        val parts = buildList {
            if (!change.tasks.isEmpty) add("${change.tasks.done}/${change.tasks.total} tasks")
            add("${change.capabilities.size} specs")
            change.date?.let { add(it) }
        }
        Text(
            parts.joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (matchedFiles.isNotEmpty()) {
            Text(
                "matched in ${matchedFiles.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
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
