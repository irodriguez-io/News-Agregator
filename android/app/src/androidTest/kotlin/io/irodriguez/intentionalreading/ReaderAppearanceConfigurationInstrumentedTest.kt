package io.irodriguez.intentionalreading

import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.ui.AppViewModel
import io.irodriguez.intentionalreading.ui.Destination
import io.irodriguez.intentionalreading.ui.theme.darkTokens
import io.irodriguez.intentionalreading.ui.theme.lightTokens
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderAppearanceConfigurationInstrumentedTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun choosingADifferentAppearanceDoesNotRestartTheScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as IntentionalReadingApplication
        val repository = application.container.localStateRepository
        val originalState = runBlocking { repository.load() } as LocalStateResult.Success

        try {
            // Given Light is established in storage and on the platform, independent of the phone's theme.
            assertTrue(
                runBlocking {
                    repository.save(originalState.state.copy(settings = LocalState.Settings(Appearance.LIGHT)))
                } is LocalStateResult.Success,
            )
            instrumentation.runOnMainSync { application.container.applyNightMode(Appearance.LIGHT) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var viewModel: AppViewModel
                scenario.onActivity { activity ->
                    viewModel = ViewModelProvider(activity)[AppViewModel::class.java]
                }
                composeTestRule.waitUntil(10_000) { viewModel.localStateReady.value }
                assertEquals(Appearance.LIGHT, viewModel.appearance.value)
                awaitNightConfiguration(scenario, Configuration.UI_MODE_NIGHT_NO)
                scenario.onActivity { viewModel.selectDestination(Destination.HISTORY) }
                assertRenderedBackground(lightTokens().bg)

                lateinit var originalActivity: MainActivity
                scenario.onActivity { originalActivity = it }

                // When the reader selects Dark through the same entry point as the Settings control.
                // MainActivity's real ViewModel uses the production repository and night-mode applier.
                scenario.onActivity { viewModel.launchAppearanceChange(Appearance.DARK) }

                // Then the platform configuration AND the rendered scheme actually change.
                awaitNightConfiguration(scenario, Configuration.UI_MODE_NIGHT_YES)
                assertRenderedBackground(darkTokens().bg)
                assertEquals(Appearance.DARK, viewModel.appearance.value)
                assertEquals(
                    Appearance.DARK,
                    (runBlocking { repository.load() } as LocalStateResult.Success).state.settings.appearance,
                )

                // And the same Activity survives, still on the reader's destination.
                scenario.onActivity { currentActivity ->
                    assertSame(
                        "Choosing a different appearance must not recreate MainActivity",
                        originalActivity,
                        currentActivity,
                    )
                    assertFalse("The original Activity must survive", originalActivity.isDestroyed)
                    assertEquals(
                        Destination.HISTORY,
                        ViewModelProvider(currentActivity)[AppViewModel::class.java].destination.value,
                    )
                }
            }
        } finally {
            // Restore app state and its persisted platform mode even if the lifecycle assertion fails.
            // The phone's system night mode and animation settings are never changed by this test.
            try {
                assertTrue(runBlocking { repository.save(originalState.state) } is LocalStateResult.Success)
            } finally {
                instrumentation.runOnMainSync {
                    application.container.applyNightMode(originalState.state.settings.appearance)
                }
            }
        }
    }

    private fun awaitNightConfiguration(scenario: ActivityScenario<MainActivity>, expected: Int) {
        composeTestRule.waitUntil(10_000) {
            var actual = Configuration.UI_MODE_NIGHT_UNDEFINED
            scenario.onActivity { activity ->
                actual = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            }
            actual == expected
        }
        composeTestRule.waitForIdle()
    }

    private fun assertRenderedBackground(expected: Color) {
        // As in the system-toggle guard, this pixel is the Scaffold background, clear of controls.
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onRoot().captureToImage().toPixelMap()[1, 1].toArgb() == expected.toArgb()
        }
        assertEquals(
            "The composed background must resolve to the selected appearance",
            expected.toArgb(),
            composeTestRule.onRoot().captureToImage().toPixelMap()[1, 1].toArgb(),
        )
    }
}
