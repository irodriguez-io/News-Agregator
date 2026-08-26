package io.irodriguez.intentionalreading.ui.format

data class CategoryOption(
    val id: String,
    val label: String,
)

object Labels {
    const val DISCOVER_LOADING_COPY = "Gathering a thoughtful queue…"
    const val DISCOVER_ERROR_TITLE = "Discover is unavailable right now"
    const val DISCOVER_ERROR_COPY =
        "Your saved reading and History remain on this device. Try loading the current reading queue again when you are ready."
    const val DISCOVER_ERROR_ACTION = "Try again"
    const val DISCOVER_EMPTY_TITLE = "Nothing needs your attention right now"
    const val DISCOVER_EMPTY_COPY =
        "You are caught up for this category. Leave without missing anything, or return to your saved reading."
    const val DISCOVER_EMPTY_ACTION = "View Read Later"
    const val DEGRADED_NOTICE = "Some sources were unavailable when this content was gathered."
    const val DISCOVER_REFRESH_FAILED = "Refresh failed. Showing the last available content."
    const val CONTENT_GENERATION_UNAVAILABLE = "Content generated · Unavailable"
    const val LAST_REFRESH_IDLE = "Last refresh · Not requested yet"
    const val LAST_REFRESH_REFRESHING = "Last refresh · Refreshing now…"
    const val LAST_REFRESH_UPDATED = "Last refresh · Updated"
    const val LAST_REFRESH_CURRENT = "Last refresh · Already current"
    const val LAST_REFRESH_FAILED = "Last refresh · Failed. Saved reading and History are unchanged."

    val categoryOptions: List<CategoryOption> = listOf(
        CategoryOption("all", "All"),
        CategoryOption("science", "Science"),
        CategoryOption("technology", "Technology"),
        CategoryOption("literature", "Literature"),
        CategoryOption("history", "History"),
        CategoryOption("weightlifting", "Weightlifting"),
        CategoryOption("iam", "IAM"),
        CategoryOption("identity_automation", "Identity Automation"),
    )

    fun categoryLabel(id: String): String = categoryOptions.firstOrNull { it.id == id }?.label.orEmpty()

    fun contentFreshness(relativeAge: String): String = "Content age · $relativeAge"

    fun generatedAt(localDateTime: String): String = "Content generated · $localDateTime"

    fun remainingChoices(remainingCount: Int): String? = when {
        remainingCount <= 0 -> null
        remainingCount == 1 -> "1 more choice waits quietly behind this one."
        else -> "$remainingCount more choices wait quietly behind this one."
    }
}
