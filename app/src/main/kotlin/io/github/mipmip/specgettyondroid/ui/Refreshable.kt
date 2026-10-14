package io.github.mipmip.specgettyondroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mipmip.specgettyondroid.repo.RepoError
import io.github.mipmip.specgettyondroid.repo.RepoRemedy
import io.github.mipmip.specgettyondroid.repo.explanation
import io.github.mipmip.specgettyondroid.repo.remedy
import io.github.mipmip.specgettyondroid.viewmodel.RefreshControl

/**
 * The gesture and its report, written once for the five screens that need it.
 * Each has a `Scaffold`, a repository to fetch and the same three parts: a pull
 * to start it, an indicator while it runs, and a word about it when it fails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Refreshable(
    control: RefreshControl,
    host: SnackbarHostState,
    modifier: Modifier = Modifier,
    onAuthorize: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val refreshing by control.refreshing.collectAsStateWithLifecycle()
    val failure by control.failure.collectAsStateWithLifecycle()

    ReportFailure(
        failure = failure,
        host = host,
        onShown = control::failureShown,
        onTryAgain = control::refresh,
        onAuthorize = onAuthorize,
    )

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = control::refresh,
        modifier = modifier,
        content = content,
    )
}

/**
 * Tells a failed refresh and offers what can be done about it. Separate from
 * [Refreshable] because the repository list has its own gesture and still wants
 * this.
 */
@Composable
fun ReportFailure(
    failure: RepoError?,
    host: SnackbarHostState,
    onShown: () -> Unit,
    onTryAgain: () -> Unit,
    onAuthorize: (() -> Unit)? = null,
) {
    LaunchedEffect(failure) {
        val error = failure ?: return@LaunchedEffect
        val remedy = error.remedy?.takeIf { it != RepoRemedy.AUTHORIZE || onAuthorize != null }
        val result = host.showSnackbar(
            message = error.explanation,
            actionLabel = remedy?.label,
            duration = SnackbarDuration.Long,
        )
        onShown()
        if (result == SnackbarResult.ActionPerformed) {
            when (remedy) {
                RepoRemedy.TRY_AGAIN -> onTryAgain()
                RepoRemedy.AUTHORIZE -> onAuthorize?.invoke()
                null -> Unit
            }
        }
    }
}

/**
 * A centred explanation that still answers a pull. Material's pull-to-refresh
 * rides nested scroll, so a pane with nothing to scroll reports no drag and the
 * gesture would be dead exactly where a fetch is most wanted. Scrolling alone
 * would push the words to the top, so the content is held a screen tall and
 * centred within that.
 */
@Composable
fun PullableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val aScreen = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = aScreen)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
