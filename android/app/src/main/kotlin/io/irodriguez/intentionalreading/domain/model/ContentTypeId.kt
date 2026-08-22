package io.irodriguez.intentionalreading.domain.model

enum class ContentTypeId(val id: String) {
    STANDARDS_UPDATE("standards_update"),
    OFFICIAL_RELEASE_NOTES("official_release_notes"),
    RESEARCH_REPORTING("research_reporting"),
    REPORTED_SCIENCE("reported_science"),
    ENGINEERING_DEEP_DIVE("engineering_deep_dive"),
    EVIDENCE_BASED_TRAINING("evidence_based_training"),
    HISTORICAL_ESSAY("historical_essay"),
    ENGINEERING_JOURNALISM("engineering_journalism"),
    LITERARY_ESSAY("literary_essay"),
    REPORTED_JOURNALISM("reported_journalism"),
    ;

    companion object {
        fun fromId(id: String): ContentTypeId? = entries.firstOrNull { it.id == id }
    }
}
