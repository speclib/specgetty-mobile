package io.github.mipmip.specgettyondroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

/** Which arrangement a screen is in, so it can answer back correctly. */
enum class PaneArrangement { TWO_LEVELS, SIDE_BY_SIDE }

@Composable
fun paneArrangement(): PaneArrangement {
    val width = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    // The window's width, not the device: a phone-sized freeform window on a
    // large screen is narrow, and should be laid out as narrow.
    return if (width == WindowWidthSizeClass.COMPACT) {
        PaneArrangement.TWO_LEVELS
    } else {
        PaneArrangement.SIDE_BY_SIDE
    }
}

/**
 * An outline and the card for whatever is selected in it.
 *
 * Two navigation levels when the window is narrow, two panes when it is wide.
 * Back differs between them and that is the point of [PaneArrangement]: side by
 * side there is no level to return from, because the outline never went away,
 * so a back press that cleared the selection would appear to do nothing.
 */
@Composable
fun ListDetail(
    hasSelection: Boolean,
    onClearSelection: () -> Unit,
    onLeave: () -> Unit,
    emptyDetailMessage: String,
    modifier: Modifier = Modifier,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    val arrangement = paneArrangement()

    BackHandler(enabled = true) {
        if (arrangement == PaneArrangement.TWO_LEVELS && hasSelection) {
            onClearSelection()
        } else {
            onLeave()
        }
    }

    when (arrangement) {
        PaneArrangement.TWO_LEVELS -> Box(modifier.fillMaxSize()) {
            if (hasSelection) detail() else list()
        }

        PaneArrangement.SIDE_BY_SIDE -> Row(modifier.fillMaxSize()) {
            Box(Modifier.width(360.dp).fillMaxHeight()) { list() }
            VerticalDivider()
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (hasSelection) {
                    detail()
                } else {
                    EmptyDetail(emptyDetailMessage)
                }
            }
        }
    }
}

@Composable
private fun EmptyDetail(message: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
