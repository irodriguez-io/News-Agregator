package io.irodriguez.intentionalreading

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.irodriguez.intentionalreading.data.DatasetRefreshResult
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheMetadata
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PipelineMetadata
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.ui.AppViewModel
import io.irodriguez.intentionalreading.ui.IntentionalReadingApp
import io.irodriguez.intentionalreading.ui.screens.settings.SettingsSheet
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers

@RunWith(AndroidJUnit4::class)
class SettingsSheetInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun reducedMotionIsImmediateWhileTheDimmingScrimRemains() = withReducedMotion {
        composeTestRule.mainClock.autoAdvance = false
        val host = setAppHost()

        TEST_SIZES.forEach { size ->
            composeTestRule.runOnIdle {
                host.viewModel.closeSettings()
                host.size.value = size
            }
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.autoAdvance = true
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            waitForSettingsSheet()
            composeTestRule.mainClock.autoAdvance = false
            settleImmediateChange()

            val title = composeTestRule.onNodeWithText("Settings").assertExists()
            val scrim = composeTestRule.onNodeWithContentDescription("Close sheet").assertIsDisplayed()
            val initialBounds = title.fetchSemanticsNode().boundsInRoot
            val initialPixels = fingerprint(scrim.captureToImage())

            composeTestRule.mainClock.advanceTimeBy(175)
            composeTestRule.waitForIdle()

            assertEquals(
                "Reduced motion must apply no sheet translation",
                initialBounds.top,
                title.fetchSemanticsNode().boundsInRoot.top,
                0.5f,
            )
            assertEquals("Reduced motion must apply no sheet fade", initialPixels, fingerprint(scrim.captureToImage()))

            composeTestRule.onNodeWithContentDescription("Close Settings").performClick()
            settleImmediateChange()
            title.assertDoesNotExist()
        }
    }

    @Test
    fun dismissUsesAReverseTuckBeforeRemovingTheModal() {
        composeTestRule.mainClock.autoAdvance = false
        val host = setSettingsHost()

        TEST_SIZES.forEach { size ->
            setSize(host, size)
            composeTestRule.mainClock.autoAdvance = true
            composeTestRule.onNodeWithTag(OPEN_SETTINGS_TAG).performClick()
            waitForSettingsSheet()
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.mainClock.advanceTimeBy(1_000)
            composeTestRule.waitForIdle()

            val title = composeTestRule.onNodeWithText("Settings").assertExists()
            composeTestRule.onNodeWithContentDescription("Close Settings").performClick()
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.waitForIdle()

            title.assertExists("The sheet must remain composed while its reverse-tuck exit begins.")

            composeTestRule.mainClock.advanceTimeBy(175)
            composeTestRule.waitForIdle()
            title.assertExists("The 350 ms reverse tuck must not remove the sheet halfway through.")

            composeTestRule.mainClock.advanceTimeBy(200)
            composeTestRule.waitForIdle()
            title.assertDoesNotExist()
        }
    }

    @Test
    fun focusStaysInTheModalAndReturnsToItsTrigger() {
        val host = setSettingsHost()

        TEST_SIZES.forEach { size ->
            setSize(host, size)
            val trigger = composeTestRule.onNodeWithTag(OPEN_SETTINGS_TAG)
            trigger.assertIsFocused()
            trigger.performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
            composeTestRule.onNode(
                isFocused() and hasAnyDescendant(hasText("Settings")),
                useUnmergedTree = true,
            ).assertExists("Focus must move inside the modal while it is open.")
            composeTestRule.onNodeWithContentDescription("Close Settings").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
            composeTestRule.onNodeWithTag(OPEN_SETTINGS_TAG).assertIsFocused()
        }
    }

    private fun setSettingsHost(): HostState {
        val host = HostState(
            size = mutableStateOf(TEST_SIZES.first()),
            settingsOpen = mutableStateOf(false),
        )
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(host.size.value),
            ) {
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    val triggerFocusRequester = remember { FocusRequester() }
                    val inputModeManager = LocalInputModeManager.current
                    Box(modifier = Modifier.fillMaxSize()) {
                        Button(
                            onClick = { host.settingsOpen.value = true },
                            modifier = Modifier
                                .testTag(OPEN_SETTINGS_TAG)
                                .focusRequester(triggerFocusRequester),
                        ) {
                            Text("Open settings")
                        }
                        if (host.settingsOpen.value) {
                            SettingsSheet(
                                appearance = Appearance.LIGHT,
                                resetInProgress = false,
                                statusMessage = null,
                                generatedAtLabel = "Updated just now",
                                lastRefreshOutcome = "Refresh succeeded",
                                importFileName = null,
                                importInProgress = false,
                                importTooLarge = false,
                                importUnreadable = false,
                                onAppearanceSelected = {},
                                onExport = {},
                                onSelectImport = {},
                                onCancelImport = {},
                                onConfirmImport = {},
                                onReset = { onComplete -> onComplete(true) },
                                onDismiss = { host.settingsOpen.value = false },
                            )
                        }
                    }
                    LaunchedEffect(Unit) {
                        inputModeManager.requestInputMode(InputMode.Keyboard)
                        triggerFocusRequester.requestFocus()
                    }
                }
            }
        }
        return host
    }

    private fun setAppHost(): AppHostState {
        val host = AppHostState(
            size = mutableStateOf(TEST_SIZES.first()),
            viewModel = testViewModel(),
        )
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(host.size.value),
            ) {
                IntentionalReadingApp(viewModel = host.viewModel)
            }
        }
        return host
    }

    private fun setSize(host: HostState, size: DpSize) {
        composeTestRule.runOnIdle {
            host.settingsOpen.value = false
            host.size.value = size
        }
        composeTestRule.waitForIdle()
    }

    private fun settleImmediateChange() {
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
    }

    private fun waitForSettingsSheet() {
        composeTestRule.waitUntil(timeoutMillis = SHEET_APPEAR_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodes(hasText("Settings")).fetchSemanticsNodes().size == 1
        }
    }

    private fun fingerprint(image: ImageBitmap): Long {
        val pixels = image.toPixelMap()
        var fingerprint = 17L
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                fingerprint = 31L * fingerprint + pixels[x, y].toArgb()
            }
        }
        return fingerprint
    }

    private fun testViewModel(): AppViewModel {
        val dataset = ArticleDataset(
            schemaVersion = 1,
            generatedAt = NOW.toString(),
            pipeline = PipelineMetadata(0, 0, 0, 0),
            articles = emptyList(),
        )
        return AppViewModel(
            readCachedDataset = { DatasetCacheRead.Absent },
            refreshDataset = {
                DatasetRefreshResult.Updated(
                    dataset = dataset,
                    metadata = DatasetCacheMetadata("\"settings-sheet-test\"", NOW),
                )
            },
            loadLocalState = {
                LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT)
            },
            saveLocalState = { state ->
                LocalStateResult.Success(state, LocalStateSource.STORAGE)
            },
            resetLocalState = {
                LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT)
            },
            nowProvider = { NOW },
            zoneProvider = { ZoneId.of("America/Managua") },
            localeProvider = { Locale.US },
            loadDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun withReducedMotion(block: () -> Unit) {
        val originalScale = runShellCommand("settings get global animator_duration_scale").trim()
        try {
            runShellCommand("settings put global animator_duration_scale 0")
            block()
        } finally {
            if (originalScale.toFloatOrNull() == null) {
                runShellCommand("settings delete global animator_duration_scale")
            } else {
                runShellCommand("settings put global animator_duration_scale $originalScale")
            }
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    private fun runShellCommand(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
    }

    private data class HostState(
        val size: MutableState<DpSize>,
        val settingsOpen: MutableState<Boolean>,
    )

    private data class AppHostState(
        val size: MutableState<DpSize>,
        val viewModel: AppViewModel,
    )

    private companion object {
        val TEST_SIZES = listOf(
            DpSize(width = 360.dp, height = 800.dp),
            DpSize(width = 411.dp, height = 891.dp),
        )
        val NOW: Instant = Instant.parse("2026-09-02T12:00:00Z")
        const val OPEN_SETTINGS_TAG = "settings-sheet-trigger"
        const val SHEET_APPEAR_TIMEOUT_MILLIS = 30_000L
    }
}
