package io.irodriguez.intentionalreading.ui.components

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.ui.gesture.SwipeGesture
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingSpacing
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArticleCardTest {
    @Test
    fun `the exit uses Android's curve not the browser's`() {
        // Given normal motion, when the card selects its swipe animation spec.
        val selected = assertIs<TweenSpec<Float>>(articleSwipeMotionSpec(reducedMotion = false))

        // Then the selected spec uses the duration recorded in 023 D1 and item 021's curve.
        assertEquals(300, selected.durationMillis)
        assertEquals(0, selected.delay)
        assertSame(SwipeGesture.ExitEmphasizedEasing, selected.easing)
        assertNotEquals(CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f), selected.easing)
        assertTrue(functionSource("ArticleCard").contains("motionSpec = articleSwipeMotionSpec(reducedMotionEnabled)"))
    }

    @Test
    fun `the card fades as it leaves with unchanged translate and rotation`() {
        // Given either direction, covering both the viewport distance and the minimum distance.
        listOf(Triple(-1f, 1_000f, -820f), Triple(1f, 500f, 620f)).forEach { (direction, width, exit) ->
            val gesture = swipeGesture(viewportWidthPx = width)
            assertEquals(1f, gesture.alpha)
            gesture.down(0f, 0f)
            gesture.move(direction * SwipeGesture.THRESHOLD_DP, 0f)
            assertEquals(1f, gesture.alpha)

            // When the swipe is committed.
            gesture.release()

            // Then only the exit adds transparency, without changing travel or rotation.
            assertEquals(0f, gesture.alpha)
            assertEquals(exit, gesture.translationX)
            assertEquals(direction * 4.5f, gesture.rotationDegrees)
            gesture.releaseCommitLock()
            assertEquals<Float>(0f, gesture.alpha, "releasing the commit lock must not flash the departing card")

            // And a failed persistence restores the card's opacity along with its position.
            gesture.restore()
            assertEquals(1f, gesture.alpha)
            assertEquals(0f, gesture.translationX)
            assertEquals(0f, gesture.rotationDegrees)
        }
    }

    @Test
    fun `a reduced-motion preference removes the exit and fade immediately`() {
        // Given reduced motion, when the card selects its spec and commits either direction.
        val selected = assertIs<SnapSpec<Float>>(articleSwipeMotionSpec(reducedMotion = true))
        val spec = selected.vectorize(Float.VectorConverter)
        listOf(-1f, 1f).forEach { direction ->
            val gesture = swipeGesture(reducedMotion = true)
            gesture.down(0f, 0f)
            gesture.move(direction * SwipeGesture.THRESHOLD_DP, 0f)
            val action = gesture.release()

            // Then the state remains clear with no travel, rotation, fade, or animation time.
            assertEquals(if (direction < 0f) SwipeGesture.Action.DISMISS else SwipeGesture.Action.SAVE, action)
            assertEquals(0f, gesture.translationX)
            assertEquals(0f, gesture.rotationDegrees)
            assertEquals(1f, gesture.alpha)
            val start = AnimationVector1D(direction * SwipeGesture.THRESHOLD_DP)
            val end = AnimationVector1D(0f)
            val velocity = AnimationVector1D(0f)
            assertEquals(0L, spec.getDurationNanos(start, end, velocity))
            assertEquals(0f, spec.getValueFromNanos(0L, start, end, velocity).value)
        }
    }

    @Test
    fun `an uncommitted swipe keeps the card opaque`() {
        // Given a card dragged short of commitment.
        val gesture = swipeGesture()
        gesture.down(0f, 0f)
        gesture.move(SwipeGesture.THRESHOLD_DP - 1f, 0f)
        // When it is released, then it returns without a fade.
        gesture.release()
        assertEquals(1f, gesture.alpha)

        // Given a longer drag, when cancelled, then it also stays opaque.
        gesture.down(0f, 0f)
        gesture.move(SwipeGesture.THRESHOLD_DP, 0f)
        gesture.cancel()
        assertEquals(1f, gesture.alpha)
    }

    @Test
    fun `the exit fade shares the animation completion before the state action`() {
        // Given the one card and its existing coordinated animation.
        val animation = functionSource("ArticleGestureValues.animateToGestureState")
        val surface = articleCardSurface()
        // When it animates, then alpha joins the same scope and draw-time layer.
        assertTrue(animation.contains("coroutineScope {"))
        assertTrue(animation.contains("launch { translationX.animateTo(gestureState.translationX, motionSpec) }"))
        assertTrue(animation.contains("launch { rotationDegrees.animateTo(gestureState.rotationDegrees, motionSpec) }"))
        assertTrue(animation.contains("launch { alpha.animateTo(gestureState.alpha, motionSpec) }"))
        assertTrue(surface.contains("alpha = gestureValues.alpha.value"))
        assertTrue(functionSource("ArticleCard").contains("alpha = Animatable(1f)"))
        assertTrue(
            Regex("""gesture\.animateToGestureState\(\)\s+currentOnSwipeCommit\(gesture\.article, articleAction\)""")
                .containsMatchIn(surface),
            "the state action must still wait for the entire exit",
        )
    }

    @Test
    fun `nothing bounces pulses or celebrates during the exit`() {
        // Given the selected finite tween and the exact two-cubic curve required by 023's brief.
        val selected = assertIs<TweenSpec<Float>>(articleSwipeMotionSpec(reducedMotion = false))
        assertSame(SwipeGesture.ExitEmphasizedEasing, selected.easing)
        val gestureSource = Path.of(
            "src/main/kotlin/io/irodriguez/intentionalreading/ui/gesture/SwipeGesture.kt",
        ).readText()
        // Then the path remains item 021's monotone, bounded Emphasized curve.
        assertTrue(
            Regex(
                """PathEasing\(\s*Path\(\)\.apply\s*\{\s*moveTo\(0f, 0f\)\s*cubicTo\(0\.05f, 0f, 0\.133333f, 0\.06f, 0\.166666f, 0\.4f\)\s*cubicTo\(0\.208333f, 0\.82f, 0\.25f, 1f, 1f, 1f\)\s*},?\s*\)""",
            ).containsMatchIn(gestureSource),
            "the exit must retain item 021's exact two-cubic path",
        )
        // And the card adds no second card, repeating animation, or scale.
        listOf("AnimatedContent(", "Crossfade(", "infiniteRepeatable(", "repeatable(", "spring(", "scaleX =", "scaleY =")
            .forEach { assertFalse(source.contains(it), it) }
    }

    private fun swipeGesture(viewportWidthPx: Float = 1_000f, reducedMotion: Boolean = false) = SwipeGesture.State(
        thresholdPx = SwipeGesture.THRESHOLD_DP,
        intentSlopPx = SwipeGesture.INTENT_SLOP_DP,
        viewportWidthPx = viewportWidthPx,
        exitMinimumPx = SwipeGesture.EXIT_MINIMUM_DP,
        reducedMotion = reducedMotion,
    )

    @Test
    fun `the headline text clamps at three lines and ellipsises`() {
        val headline = textCall("text = article.title")

        assertEquals(3, maxLines(headline), "headline maxLines")
        assertEquals("Ellipsis", assignedMember(headline, "overflow"), "headline overflow")
    }

    @Test
    fun `the excerpt text clamps at two lines and ellipsises`() {
        val excerpt = textCall("text = article.excerpt")

        assertEquals(2, maxLines(excerpt), "excerpt maxLines")
        assertEquals("Ellipsis", assignedMember(excerpt, "overflow"), "excerpt overflow")
    }

    @Test
    fun `the deck card uses the authored primary card shape`() {
        assertEquals("primaryCard", assignedScale(articleCardSurface(), "shape", "shapes"))
    }

    @Test
    fun `the deck card uses the card fill`() {
        assertEquals("card", assignedScale(articleCardSurface(), "color", "tokens"))
    }

    @Test
    fun `the deck card shadow uses the theme shadow tint for both shadow channels`() {
        val surface = articleCardSurface()

        assertTrue(
            surface.contains("ambientColor = MaterialTheme.colorScheme.surfaceTint"),
            "ambient shadow tint was absent",
        )
        assertTrue(
            surface.contains("spotColor = MaterialTheme.colorScheme.surfaceTint"),
            "spot shadow tint was absent",
        )
    }

    @Test
    fun `the headline is the authored editorial headline style`() {
        assertEquals("headlineLarge", assignedMember(textCall("text = article.title"), "style"))
    }

    @Test
    fun `an empty excerpt is omitted without placeholder copy`() {
        val body = functionSource("ArticleCard")

        assertTrue(body.contains("if (article.excerpt.isNotEmpty())"))
        assertFalse(body.contains("No description available"))
    }

    @Test
    fun `the badge preserves the content type label and uses badge roles`() {
        val metadata = functionSource("ArticleMetadata")

        assertTrue(metadata.contains("article.contentType.label.uppercase(Locale.ROOT)"))
        assertEquals("badge", assignedScale(metadata, "shape", "shapes"))
        assertEquals("primarySoft", assignedScale(metadata, "color", "tokens"))
        assertEquals("primary", assignedScale(metadata, "contentColor", "tokens"))
    }

    @Test
    fun `unknown reading time and publication age are omitted`() {
        val metadata = functionSource("ArticleMetadata")

        assertTrue(metadata.contains("if (publicationAge.isNotEmpty()) MetadataText(publicationAge)"))
        assertTrue(metadata.contains("if (readingTime.isNotEmpty()) MetadataText(readingTime)"))
        assertFalse(metadata.contains("?: 0"))
    }

    @Test
    fun `tags are capped neutral outlined pills without interaction`() {
        val tags = functionSource("TopicTags")

        assertTrue(tags.contains("article.tags.take(5)"), "tag cap was not five")
        assertTrue(tags.contains("tokens.outlineVariant"), "neutral decorative outline was absent")
        assertTrue(tags.contains("shapes.pill"), "authored pill shape was absent")
        assertFalse(Regex("\\b(?:clickable|selectable|toggleable)\\s*\\(").containsMatchIn(tags))
        assertFalse(Regex("\\bcolor\\s*=\\s*tokens\\.").containsMatchIn(tags), "tags carried a fill")
    }

    @Test
    fun `the card reserves no image or media region`() {
        val body = functionSource("ArticleCard")

        listOf("Image(", "AsyncImage(", "thumbnail", "mediaPlaceholder").forEach { forbidden ->
            assertFalse(body.contains(forbidden), "reserved media marker found: $forbidden")
        }
    }

    @Test
    fun `the action rail uses item 018 shared controls with the existing callbacks`() {
        val rail = articleActionRail()

        assertEquals(
            listOf("CircularTriageControl", "FilledPrimaryControl", "CircularTriageControl"),
            Regex(
                """\b(CircularTriageControl|FilledPrimaryControl|RoundTriageAction|Button)\s*\(""",
            ).findAll(rail).map { it.groupValues[1] }.toList(),
            "action rail control types",
        )

        val controls = listOf("CircularTriageControl", "FilledPrimaryControl").flatMap { name ->
            callBlocks(rail, name)
        }.sortedBy(rail::indexOf)
        assertTrue(controls[0].contains("onClick = { onDismiss(article) }"))
        assertTrue(controls[1].contains("onClick = { onReadArticle(article) }"))
        assertTrue(controls[2].contains("onClick = { onSave(article) }"))
    }

    @Test
    fun `the adopted triage controls keep compliant targets and accessible names`() {
        val triageControls = callBlocks(articleActionRail(), "CircularTriageControl")
        val layout = sharedControlLayout(IntentionalReadingSpacing, IntentionalReadingShapes)

        assertEquals(2, triageControls.size, "circular triage control count")
        assertTrue(layout.triageSize >= 48.dp, "triage target was smaller than 48 dp")
        assertEquals(
            listOf("notInterestedLabel", "saveForLaterLabel"),
            triageControls.map { assignedIdentifier(it, "accessibleName") },
            "triage accessible names",
        )
    }

    @Test
    fun `the swipe cue uses the Android small container roles`() {
        val cue = functionSource("SwipeCue")

        assertEquals("smallContainer", assignedScale(cue, "shape", "shapes"))
        assertEquals("container", assignedScale(cue, "color", "tokens"))
        assertEquals("fg", assignedScale(cue, "contentColor", "tokens"))
    }

    @Test
    fun `the opened acknowledgment uses the Android small container roles`() {
        val acknowledgment = functionSource("OpenedAcknowledgment")

        assertEquals("smallContainer", assignedScale(acknowledgment, "shape", "shapes"))
        assertEquals("container", assignedScale(acknowledgment, "color", "tokens"))
        assertEquals("fg", assignedScale(acknowledgment, "contentColor", "tokens"))
    }

    @Test
    fun `the whole card source names no colour radius size or font literal`() {
        val forbidden = listOf(
            Regex("""\b\d+(?:\.\d+)?\.(?:dp|sp)\b"""),
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bCircleShape\b"""),
            Regex("""\bFont(?:Family|Style|Weight)\s*\("""),
            Regex("""\b\d+(?:\.\d+)?\.sp\b"""),
        )

        forbidden.forEach { pattern ->
            assertFalse(
                pattern.containsMatchIn(source),
                "Forbidden literal matched ${pattern.pattern}",
            )
        }
    }

    private fun maxLines(textCall: String): Int {
        val assigned = Regex("""\bmaxLines\s*=\s*([A-Za-z0-9_.]+)""")
            .find(textCall)
            ?.groupValues
            ?.get(1)
            ?: return Int.MAX_VALUE
        return assigned.toIntOrNull() ?: namedInt(assigned)
    }

    private fun namedInt(name: String): Int = Regex(
        """\b(?:private\s+|internal\s+)?const\s+val\s+$name\s*=\s*(\d+)""",
    ).find(source)?.groupValues?.get(1)?.toInt()
        ?: error("No integer value was declared for $name")

    private fun assignedMember(block: String, property: String): String? = Regex(
        """\b$property\s*=\s*(?:[A-Za-z0-9_]+\.)+([A-Za-z0-9_]+)""",
    ).find(block)?.groupValues?.get(1)

    private fun assignedScale(block: String, property: String, scale: String): String? = Regex(
        """\b$property\s*=\s*$scale\.([A-Za-z0-9_]+)""",
    ).find(block)?.groupValues?.get(1)

    private fun assignedIdentifier(block: String, property: String): String? = Regex(
        """\b$property\s*=\s*([A-Za-z0-9_]+)""",
    ).find(block)?.groupValues?.get(1)

    private fun articleActionRail(): String {
        val actions = functionSource("ArticleActions")
        val start = actions.indexOf("Row(")
        check(start >= 0) { "ArticleActions has no action rail Row" }
        val parametersStart = start + "Row".length
        val parametersEnd = balancedEnd(actions, parametersStart, '(', ')')
        val bodyStart = actions.indexOf('{', parametersEnd + 1)
        check(bodyStart >= 0) { "ArticleActions action rail Row has no body" }
        return balancedBlock(actions, bodyStart, '{', '}')
    }

    private fun callBlocks(block: String, name: String): List<String> = Regex("""\b$name\s*\(""")
        .findAll(block)
        .map { match -> balancedBlock(block, match.range.last, '(', ')') }
        .toList()

    private fun articleCardSurface(): String {
        val body = functionSource("ArticleCard")
        val start = body.indexOf("Surface(")
        check(start >= 0) { "ArticleCard has no Surface" }
        return balancedBlock(body, start + "Surface".length, '(', ')')
    }

    private fun textCall(marker: String): String {
        val body = functionSource("ArticleCard")
        val markerIndex = body.indexOf(marker)
        check(markerIndex >= 0) { "No text element carries $marker" }
        val start = body.lastIndexOf("Text(", markerIndex)
        check(start >= 0) { "No Text call carries $marker" }
        return balancedBlock(body, start + "Text".length, '(', ')')
    }

    private fun functionSource(name: String): String {
        val signature = "fun $name("
        val start = source.indexOf(signature)
        check(start >= 0) { "No function named $name" }
        val parametersStart = source.indexOf('(', start)
        val parametersEnd = balancedEnd(source, parametersStart, '(', ')')
        val bodyStart = source.indexOf('{', parametersEnd + 1)
        check(bodyStart >= 0) { "$name has no body" }
        return balancedBlock(source, bodyStart, '{', '}')
    }

    private fun balancedBlock(text: String, start: Int, open: Char, close: Char): String {
        val end = balancedEnd(text, start, open, close)
        return text.substring(start, end + 1)
    }

    private fun balancedEnd(text: String, start: Int, open: Char, close: Char): Int {
        check(text[start] == open) { "Expected $open at $start" }
        var depth = 0
        for (index in start until text.length) {
            when (text[index]) {
                open -> depth += 1
                close -> {
                    depth -= 1
                    if (depth == 0) return index
                }
            }
        }
        error("Unbalanced $open$close block")
    }

    private val source: String by lazy {
        Path.of(
            "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/ArticleCard.kt",
        ).readText()
    }
}
