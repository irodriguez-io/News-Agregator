package io.irodriguez.intentionalreading.ui

internal enum class DestinationSlideDirection {
    FROM_LEFT,
    FROM_RIGHT,
}

internal fun destinationSlideDirection(
    current: Destination,
    target: Destination,
): DestinationSlideDirection {
    require(current != target) { "A destination transition requires two different destinations" }
    return if (target.ordinal < current.ordinal) {
        DestinationSlideDirection.FROM_LEFT
    } else {
        DestinationSlideDirection.FROM_RIGHT
    }
}
