package io.irodriguez.intentionalreading

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTokens
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import io.irodriguez.intentionalreading.ui.theme.darkTokens
import io.irodriguez.intentionalreading.ui.theme.intentionalReadingColorScheme
import io.irodriguez.intentionalreading.ui.theme.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeReducedMotionInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule(
        // Establish the animation clock's scale instead of inheriting the emulator setting.
        effectContext = object : MotionDurationScale {
            override val scaleFactor: Float = 1f
        },
    )

    @Test
    fun theThemeHonoursTheInjectedReducedMotionPreferenceInBothDirections() {
        // Given a real theme composition and a fake preference, independent of Application and device state.
        val appearance = mutableStateOf(Appearance.LIGHT)
        val reducedMotion = mutableStateOf(false)
        lateinit var observed: Palette
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            IntentionalReadingTheme(
                appearance = appearance.value,
                reducedMotion = { reducedMotion.value },
            ) {
                val tokens = LocalIntentionalReadingTokens.current
                val scheme = MaterialTheme.colorScheme
                SideEffect { observed = Palette(tokens, scheme.background, scheme.surfaceTint) }
            }
        }
        composeTestRule.runOnIdle { assertEquals(palette(Appearance.LIGHT), observed) }

        // Reuse the same theme and callback while changing the returned flag, so it cannot be cached once.
        listOf(false, true, false).forEach { immediate ->
            listOf(Appearance.DARK, Appearance.LIGHT).forEach { target ->
                val start = palette(if (target == Appearance.DARK) Appearance.LIGHT else Appearance.DARK)
                val end = palette(target)
                // When the injected preference and resolved appearance change.
                composeTestRule.runOnIdle {
                    reducedMotion.value = immediate
                    appearance.value = target
                }
                composeTestRule.mainClock.advanceTimeByFrame()
                composeTestRule.waitForIdle()

                if (immediate) {
                    // Then the next composition already exposes every target token and the derived tint.
                    composeTestRule.runOnIdle {
                        assertEquals("Injected reduced motion must select the endpoint immediately", end, observed)
                    }
                } else {
                    // Then normal motion actually travels through an intermediate palette.
                    composeTestRule.mainClock.advanceTimeBy(100)
                    composeTestRule.runOnIdle {
                        assertNotEquals("Tokens must leave the initial palette", start.tokens, observed.tokens)
                        assertNotEquals("Tokens must not snap to the target palette", end.tokens, observed.tokens)
                        assertNotEquals(start.background, observed.background)
                        assertNotEquals(end.background, observed.background)
                        assertNotEquals(start.surfaceTint, observed.surfaceTint)
                        assertNotEquals(end.surfaceTint, observed.surfaceTint)
                    }
                }
                composeTestRule.mainClock.advanceTimeBy(350)
                composeTestRule.runOnIdle {
                    assertEquals("Both branches must finish at the authored endpoint", end, observed)
                }
            }
        }
    }

    private fun palette(appearance: Appearance): Palette {
        val dark = appearance == Appearance.DARK
        val tokens = if (dark) darkTokens() else lightTokens()
        val scheme = intentionalReadingColorScheme(tokens, dark)
        return Palette(tokens, scheme.background, scheme.surfaceTint)
    }

    private data class Palette(
        val tokens: IntentionalReadingTokens,
        val background: Color,
        val surfaceTint: Color,
    )
}
