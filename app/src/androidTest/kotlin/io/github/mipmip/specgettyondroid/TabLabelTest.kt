package io.github.mipmip.specgettyondroid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mipmip.specgettyondroid.ui.screen.ProjectTabs
import io.github.mipmip.specgettyondroid.ui.theme.SpecgettyTheme
import io.github.mipmip.specgettyondroid.viewmodel.ProjectTab
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * "Properties" is the longest of the four labels, and an equal-width row gave it
 * a quarter of the screen. On a Fairphone that is narrower than the word, so
 * Compose broke it mid-word: `propertie` / `s`.
 *
 * These render the row the project view uses at widths and font scales narrow
 * enough to have caused that, and assert every label is present whole. A label
 * that wrapped would not match its own text.
 */
class TabLabelTest {

    @get:Rule
    val compose = createComposeRule()

    private fun renderAt(widthDp: Int, fontScale: Float) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = fontScale),
            ) {
                SpecgettyTheme {
                    // The app's own row, not a copy: a revert would fail these.
                    Box(Modifier.width(widthDp.dp).fillMaxHeight()) {
                        ProjectTabs(selected = ProjectTab.OVERVIEW, onSelect = {})
                    }
                }
            }
        }
    }

    /**
     * Whole, not necessarily on screen. A scrollable row is allowed to put a
     * later tab past the edge, which is what the spec asks for: scroll rather
     * than squeeze a label into a column narrower than the word.
     *
     * This asserts the label is present, and nothing more. Compose's semantics
     * carry the whole string even when it is drawn broken across two lines, so
     * presence cannot detect the bug. [noLabelIsDrawnOverTwoLines] measures
     * instead.
     */
    private fun assertEveryLabelWhole() {
        ProjectTab.entries.forEach { entry ->
            compose.onNodeWithText(entry.label, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun everyLabelIsWholeOnANarrowScreen() {
        renderAt(widthDp = 320, fontScale = 1.0f)
        assertEveryLabelWhole()
    }

    @Test
    fun everyLabelIsWholeAtALargeFontScale() {
        renderAt(widthDp = 360, fontScale = 1.5f)
        assertEveryLabelWhole()
    }

    @Test
    fun everyLabelIsWholeAtTheLargestFontScaleAndroidOffers() {
        renderAt(widthDp = 320, fontScale = 2.0f)
        assertEveryLabelWhole()
    }

    @Test
    fun everyLabelIsWholeOnAWideScreen() {
        renderAt(widthDp = 840, fontScale = 1.0f)
        assertEveryLabelWhole()
    }

    /**
     * The word the bug broke, on its own, at the width that broke it. `TabRow`
     * would have given it 320/4 = 80dp.
     */
    @Test
    fun propertiesSurvivesTheWidthThatBrokeIt() {
        renderAt(widthDp = 320, fontScale = 1.3f)
        compose.onNodeWithText("Properties", useUnmergedTree = true).assertExists()
    }

    /**
     * The bug itself, measured rather than inferred.
     *
     * A label broken across two lines is about twice as tall as one that fits.
     * The shortest label is the yardstick: at a width where none of them wraps
     * they are all the same height, so a label half again as tall as the
     * shortest is one that wrapped.
     *
     * 360dp at the ordinary font scale, measured rather than picked: with an
     * equal-width row that is where "Properties" alone goes to two lines, 40dp
     * against 20dp for the rest. Wider and nothing wraps; narrower and they all
     * do, which makes the comparison blind because there is no unwrapped label
     * left to measure against. It is also the width of an ordinary phone.
     */
    @Test
    fun noLabelIsDrawnOverTwoLines() {
        renderAt(widthDp = 360, fontScale = 1.0f)

        val heights: List<Pair<String, Dp>> = ProjectTab.entries.map { entry ->
            val bounds = compose.onNodeWithText(entry.label, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            entry.label to (bounds.bottom - bounds.top)
        }
        val shortest: Dp = heights.minOf { it.second }

        heights.forEach { (label, height) ->
            assertTrue(
                "$label is $height tall against a shortest label of $shortest: it wrapped",
                height < shortest * 1.5f,
            )
        }
    }

    /** Off the edge is fine; unreachable is not. */
    @Test
    fun aLabelPastTheEdgeCanBeScrolledTo() {
        renderAt(widthDp = 320, fontScale = 2.0f)
        compose.onNodeWithText("Properties", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
