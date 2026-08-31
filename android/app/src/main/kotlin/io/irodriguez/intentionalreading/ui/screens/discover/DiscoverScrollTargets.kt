package io.irodriguez.intentionalreading.ui.screens.discover

object DiscoverScrollTargets {
    /**
     * The scroll value that brings the bottom of the Discover card — and therefore its action rail —
     * into view without scrolling past it into whatever follows the card.
     */
    fun revealCardActions(cardBottomOffset: Int, viewportHeight: Int, maxValue: Int): Int =
        (cardBottomOffset - viewportHeight).coerceAtLeast(0).coerceAtMost(maxValue)
}
