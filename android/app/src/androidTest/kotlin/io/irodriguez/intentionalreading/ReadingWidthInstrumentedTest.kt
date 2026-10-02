package io.irodriguez.intentionalreading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.data.DatasetRefreshErrorCode
import io.irodriguez.intentionalreading.data.DatasetRefreshResult
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.ui.AggregateUiState
import io.irodriguez.intentionalreading.ui.AppViewModel
import io.irodriguez.intentionalreading.ui.Destination
import io.irodriguez.intentionalreading.ui.IntentionalReadingApp
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryScreen
import io.irodriguez.intentionalreading.ui.screens.history.HistoryUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterScreen
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterUiState
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingWidthInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun handsetsAreUnchanged_GivenDISCOVER_360Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.DISCOVER, 360, 18)

    @Test
    fun handsetsAreUnchanged_GivenREAD_LATER_360Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.READ_LATER, 360, 18)

    @Test
    fun handsetsAreUnchanged_GivenHISTORY_360Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.HISTORY, 360, 18)

    @Test
    fun handsetsAreUnchanged_GivenDISCOVER_390Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.DISCOVER, 390, 18)

    @Test
    fun handsetsAreUnchanged_GivenREAD_LATER_390Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.READ_LATER, 390, 18)

    @Test
    fun handsetsAreUnchanged_GivenHISTORY_390Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.HISTORY, 390, 18)

    @Test
    fun handsetsAreUnchanged_GivenDISCOVER_430Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.DISCOVER, 430, 18)

    @Test
    fun handsetsAreUnchanged_GivenREAD_LATER_430Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.READ_LATER, 430, 18)

    @Test
    fun handsetsAreUnchanged_GivenHISTORY_430Dp_WhenLaidOut_ThenInsetsAre18Dp() =
        assertReadingInsets(Destination.HISTORY, 430, 18)

    @Test
    fun wideScreensKeepAReadingWidth_GivenDISCOVER_600Dp_WhenLaidOut_ThenInsetsAre24Dp() =
        assertReadingInsets(Destination.DISCOVER, 600, 24)

    @Test
    fun wideScreensKeepAReadingWidth_GivenREAD_LATER_600Dp_WhenLaidOut_ThenInsetsAre24Dp() =
        assertReadingInsets(Destination.READ_LATER, 600, 24)

    @Test
    fun wideScreensKeepAReadingWidth_GivenHISTORY_600Dp_WhenLaidOut_ThenInsetsAre24Dp() =
        assertReadingInsets(Destination.HISTORY, 600, 24)

    @Test
    fun wideScreensKeepAReadingWidth_GivenDISCOVER_768Dp_WhenLaidOut_ThenInsetsAre44Dp() =
        assertReadingInsets(Destination.DISCOVER, 768, 44)

    @Test
    fun wideScreensKeepAReadingWidth_GivenREAD_LATER_768Dp_WhenLaidOut_ThenInsetsAre44Dp() =
        assertReadingInsets(Destination.READ_LATER, 768, 44)

    @Test
    fun wideScreensKeepAReadingWidth_GivenHISTORY_768Dp_WhenLaidOut_ThenInsetsAre44Dp() =
        assertReadingInsets(Destination.HISTORY, 768, 44)

    @Test
    fun wideScreensKeepAReadingWidth_Given768Dp_WhenFullAppIsLaidOut_ThenNavigationSpansFullWidth() {
        val viewModel = AppViewModel(
            readCachedDataset = { DatasetCacheRead.Absent },
            refreshDataset = { DatasetRefreshResult.Failed(DatasetRefreshErrorCode.FETCH) },
            loadLocalState = { LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT) },
            saveLocalState = { LocalStateResult.Success(it, LocalStateSource.STORAGE) },
            resetLocalState = { LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT) },
            nowProvider = { Instant.parse("2026-10-02T12:00:00Z") },
            zoneProvider = { ZoneOffset.UTC },
            localeProvider = { Locale.US },
            loadDispatcher = Dispatchers.Unconfined,
        )
        var density = 0f
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(768.dp, 1000.dp))) {
                density = LocalDensity.current.density
                Box(Modifier.fillMaxSize().testTag("host")) {
                    IntentionalReadingApp(viewModel = viewModel)
                }
            }
        }
        val host = compose.onNodeWithTag("host").fetchSemanticsNode().boundsInRoot
        val navigation = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup),
        ).fetchSemanticsNode().boundsInRoot
        assertEquals("ForcedSize really supplies 768 dp", 768f, host.width / density, 1f)
        assertEquals("Navigation left", host.left, navigation.left, density)
        assertEquals("Navigation right", host.right, navigation.right, density)
        println("READING_WIDTH full app: host=${host.width / density} dp navigation=${navigation.width / density} dp")
    }

    private fun assertReadingInsets(destination: Destination, width: Int, expectedInset: Int) {
        // Given an actual available width, independent of the physical device's width.
        val height = mutableStateOf(1000.dp)
        var density = 0f
        val aggregate = AggregateUiState(0, 0, 0, null, null)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.value))) {
                density = LocalDensity.current.density
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    Box(Modifier.fillMaxSize().testTag("host")) {
                        when (destination) {
                            Destination.DISCOVER -> DiscoverScreen(
                                state = DiscoverUiState.Loading("Loading fixture", null, null, DiscoverRefreshAffordance.HIDDEN),
                                degraded = false,
                                selectedCategory = null,
                                onCategorySelected = {}, onRetry = {}, onViewReadLater = {},
                                onDismiss = {}, onReadArticle = {}, onSave = {}, onMarkRead = {},
                                onSwipeCommit = { _, _, complete -> complete(true) },
                                reducedMotion = { true },
                            )
                            Destination.READ_LATER -> ReadLaterScreen(
                                ReadLaterUiState(emptyList(), aggregate), {}, {}, {}, {},
                            )
                            Destination.HISTORY -> HistoryScreen(
                                HistoryUiState(emptyList(), aggregate), {}, {}, {}, {},
                            )
                        }
                    }
                }
            }
        }
        // When the real screen has been laid out, measure its full-width content surface.
        val host = compose.onNodeWithTag("host").fetchSemanticsNode().boundsInRoot
        val content = when (destination) {
            Destination.DISCOVER -> compose.onNodeWithTag("discover-masthead")
            Destination.READ_LATER -> compose.onNodeWithText("Your reading queue is open", useUnmergedTree = true).onParent()
            Destination.HISTORY -> compose.onNodeWithText("No reading history yet", useUnmergedTree = true).onParent()
        }.fetchSemanticsNode().boundsInRoot
        val left = (content.left - host.left) / density
        val right = (host.right - content.right) / density
        println("READING_WIDTH $destination $width dp: left=$left right=$right content=${content.width / density} dp")
        // Then both insets match the specified result, within one dp (including pixel rounding).
        assertEquals("ForcedSize width", width.toFloat(), host.width / density, 1f)
        assertEquals("$destination $width dp left inset", expectedInset.toFloat(), left, 1f)
        assertEquals("$destination $width dp right inset", expectedInset.toFloat(), right, 1f)

        // Given a short viewport, both outer gutters must still respond to scrolling.
        compose.runOnIdle { height.value = 240.dp }
        val scroll = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        fun position() = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        val before = position()
        scroll.performTouchInput {
            swipe(Offset(density, this.height * 0.8f), Offset(density, this.height * 0.2f), 400)
        }
        val afterLeft = position()
        assertTrue("$destination scroll responds at left edge", afterLeft > before)
        scroll.performTouchInput {
            swipe(Offset(this.width - density, this.height * 0.2f), Offset(this.width - density, this.height * 0.8f), 400)
        }
        assertTrue("$destination scroll responds at right edge", position() < afterLeft)
    }
}
