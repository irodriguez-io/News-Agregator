package io.irodriguez.intentionalreading

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLaunchSmokeTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun appLaunchesAndPrimaryNavigationWorks() {
        launchAppOrFail().use { scenario ->
            val labels = listOf("Read Later", "Discover", "History")
            val destinations = labels.map(::destination)
            val horizontalPositions = destinations.map { it.position }
            assertTrue(
                "Expected destination labels in Read Later / Discover / History order, " +
                    "but their horizontal positions were $horizontalPositions",
                horizontalPositions.zipWithNext().all { (left, right) -> left < right },
            )

            composeTestRule.onNodeWithText("A FINITE READING QUEUE").assertExists()
            destinations.first().interaction.performClick()
            composeTestRule
                .onNodeWithText("YOUR DELIBERATE QUEUE")
                .assertExists("Expected selecting Read Later to present the Read Later screen.")
            composeTestRule.onNodeWithText("A FINITE READING QUEUE").assertDoesNotExist()

            assertEquals(LAUNCH_FAILURE_MESSAGE, Lifecycle.State.RESUMED, scenario.state)
        }
    }

    private fun launchAppOrFail(): ActivityScenario<MainActivity> {
        val scenario = try {
            ActivityScenario.launch(MainActivity::class.java)
        } catch (cause: Throwable) {
            throw AssertionError(LAUNCH_FAILURE_MESSAGE, cause)
        }

        try {
            composeTestRule.waitForIdle()
            assertEquals(LAUNCH_FAILURE_MESSAGE, Lifecycle.State.RESUMED, scenario.state)
        } catch (cause: Throwable) {
            scenario.close()
            throw AssertionError(LAUNCH_FAILURE_MESSAGE, cause)
        }
        return scenario
    }

    private fun destination(label: String): DestinationNode {
        val matches = composeTestRule.onAllNodes(
            hasClickAction() and hasAnyDescendant(hasText(label)),
            useUnmergedTree = true,
        )
        val nodes = matches.fetchSemanticsNodes()
        val bottomMostIndex = nodes.indices.maxByOrNull { nodes[it].boundsInRoot.top }
            ?: throw AssertionError("Expected the $label destination label to render.")
        val interaction = matches[bottomMostIndex]
        assertTrue("Expected the $label destination label to render.", interaction.isDisplayed())
        return DestinationNode(interaction, nodes[bottomMostIndex].boundsInRoot.center.x)
    }

    private data class DestinationNode(
        val interaction: androidx.compose.ui.test.SemanticsNodeInteraction,
        val position: Float,
    )

    private companion object {
        const val LAUNCH_FAILURE_MESSAGE =
            "The app did not launch and remain resumed; a startup crash is the likely cause."
    }
}
