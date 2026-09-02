package io.irodriguez.intentionalreading.ui.screens.discover

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoverScreenTest {
    @Test
    fun `discover layout uses the authored spacing and shape scales`() {
        assertTrue(screenSource.contains("val spacing = LocalIntentionalReadingSpacing.current"))
        assertTrue(screenSource.contains(".padding(spacing.mobileMargin)"))
        assertTrue(screenSource.contains("Arrangement.spacedBy(spacing.sectionGap)"))
        assertTrue(screenSource.contains("shape = shapes.smallContainer"))
        assertTrue(screenSource.contains("color = tokens.container"))
        assertTrue(headerSource.contains("Arrangement.spacedBy(spacing.stackGap)"))
        assertTrue(headerSource.contains("shape = shapes.filledPrimaryButton"))
    }

    @Test
    fun `non-card states retain their truthful copy and routes onward`() {
        assertTrue(screenSource.contains("is DiscoverUiState.Loading -> LoadingPanel(state)"))
        assertTrue(screenSource.contains("text = state.copy"))
        assertTrue(screenSource.contains("is DiscoverUiState.Error -> StatePanel("))
        assertTrue(screenSource.contains("onAction = onRetry.takeUnless"))
        assertTrue(screenSource.contains("is DiscoverUiState.Empty -> StatePanel("))
        assertTrue(screenSource.contains("onAction = onViewReadLater"))
    }

    @Test
    fun `discover files name no colour radius size or font literal`() {
        val forbidden = listOf(
            Regex("""\b\d+(?:\.\d+)?\.(?:dp|sp)\b"""),
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bCircleShape\b"""),
            Regex("""\bFont(?:Family|Style|Weight)\s*\("""),
        )

        forbidden.forEach { pattern ->
            assertFalse(
                pattern.containsMatchIn(screenSource + headerSource),
                "Forbidden literal matched ${pattern.pattern}",
            )
        }
    }

    private val screenSource: String by lazy {
        source("DiscoverScreen.kt")
    }

    private val headerSource: String by lazy {
        source("DiscoverHeader.kt")
    }

    private fun source(fileName: String): String = Path.of(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/discover/$fileName",
    ).readText()
}
