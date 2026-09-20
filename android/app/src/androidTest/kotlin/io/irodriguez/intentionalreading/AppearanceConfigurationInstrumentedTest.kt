package io.irodriguez.intentionalreading

import android.content.res.Configuration
import android.os.ParcelFileDescriptor
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
class AppearanceConfigurationInstrumentedTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun systemStillFollowsThePhoneWhileTheAppIsOpenWithoutRecreatingTheActivity() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext
            .applicationContext as IntentionalReadingApplication
        val repository = application.container.localStateRepository
        val originalState = runBlocking { repository.load() } as LocalStateResult.Success
        val originalNightMode = shell("cmd uimode night").trim().removePrefix("Night mode: ")

        try {
            // Given Appearance.SYSTEM and a real, running MainActivity composing the light scheme.
            shell("cmd uimode night no")
            assertTrue(
                runBlocking {
                    repository.save(originalState.state.copy(settings = LocalState.Settings(Appearance.SYSTEM)))
                } is LocalStateResult.Success,
            )
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var originalActivity: MainActivity
                lateinit var viewModel: AppViewModel
                scenario.onActivity { activity ->
                    originalActivity = activity
                    viewModel = ViewModelProvider(activity)[AppViewModel::class.java]
                }
                composeTestRule.waitUntil(10_000) { viewModel.localStateReady.value }
                assertEquals(Appearance.SYSTEM, viewModel.appearance.value)
                awaitNightConfiguration(scenario, Configuration.UI_MODE_NIGHT_NO)
                assertRenderedBackground(lightTokens().bg)

                listOf(
                    Triple("yes", Configuration.UI_MODE_NIGHT_YES, darkTokens().bg),
                    Triple("no", Configuration.UI_MODE_NIGHT_NO, lightTokens().bg),
                ).forEach { (systemMode, nightMask, background) ->
                    // When the phone changes theme, let the platform deliver the configuration.
                    shell("cmd uimode night $systemMode")
                    awaitNightConfiguration(scenario, nightMask)

                    // Then the actual Compose screen follows System in both directions.
                    assertRenderedBackground(background)
                    assertEquals(Appearance.SYSTEM, viewModel.appearance.value)

                    // And it is the original Activity, not a replacement with restored state.
                    scenario.onActivity { currentActivity ->
                        assertSame(
                            "A system uiMode change must not recreate MainActivity",
                            originalActivity,
                            currentActivity,
                        )
                        assertFalse("The original Activity must survive", originalActivity.isDestroyed)
                    }
                }
            }
        } finally {
            // Restore device and app preferences even when the lifecycle assertion fails.
            assertTrue(runBlocking { repository.save(originalState.state) } is LocalStateResult.Success)
            application.container.applyNightMode(originalState.state.settings.appearance)
            shell("cmd uimode night $originalNightMode")
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
        // The root's top-left pixel is the Scaffold background, clear of text and controls.
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onRoot().captureToImage().toPixelMap()[1, 1].toArgb() == expected.toArgb()
        }
        assertEquals(
            "The composed background must resolve to the phone's scheme",
            expected.toArgb(),
            composeTestRule.onRoot().captureToImage().toPixelMap()[1, 1].toArgb(),
        )
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
