package io.github.mipmip.specgettyondroid

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText

/**
 * A clone and a parse happen off the main thread, so a test that asserts
 * immediately after a tap asserts against the screen before it. These wait for
 * what should appear rather than sleeping for a guess at how long it takes.
 */
fun ComposeContentTestRule.awaitText(
    text: String,
    substring: Boolean = false,
    timeoutMillis: Long = 60_000,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

fun ComposeContentTestRule.awaitDescription(
    description: String,
    timeoutMillis: Long = 60_000,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithContentDescription(description, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
}

/** The first node with this text, so a label and its field do not collide. */
fun ComposeContentTestRule.firstWithText(
    text: String,
    substring: Boolean = false,
): SemanticsNodeInteraction =
    onAllNodesWithText(text, substring = substring, useUnmergedTree = true)[0]

fun ComposeContentTestRule.firstWithDescription(
    description: String,
): SemanticsNodeInteraction =
    onAllNodesWithContentDescription(description, useUnmergedTree = true)[0]
