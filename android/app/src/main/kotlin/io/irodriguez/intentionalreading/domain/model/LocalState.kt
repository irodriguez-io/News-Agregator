package io.irodriguez.intentionalreading.domain.model

data class LocalState(
    val schemaVersion: Int,
    val preferences: Preferences,
    val articles: Map<String, ArticleRecord>,
    val settings: Settings,
    val session: Session,
) {
    data class Preferences(
        val sources: Map<String, PreferenceEntry>,
        val topics: Map<String, PreferenceEntry>,
    )

    data class Settings(
        val appearance: Appearance,
    )

    data class Session(
        val lastCategory: Category?,
    )

    companion object {
        const val SCHEMA_VERSION = 1

        fun default(): LocalState = LocalState(
            schemaVersion = SCHEMA_VERSION,
            preferences = Preferences(sources = emptyMap(), topics = emptyMap()),
            articles = emptyMap(),
            settings = Settings(appearance = Appearance.SYSTEM),
            session = Session(lastCategory = null),
        )
    }
}

data class PreferenceEntry(
    val weight: Double,
    val interactions: Int,
)

data class SignalsApplied(
    val opened: Boolean,
    val saved: Boolean,
    val dismissed: Boolean,
    val read: Boolean,
) {
    companion object {
        fun derivedForAndroid(status: ArticleStatus, openedAtPresent: Boolean): SignalsApplied =
            SignalsApplied(
                opened = openedAtPresent,
                saved = false,
                dismissed = false,
                read = status == ArticleStatus.READ,
            )
    }
}
