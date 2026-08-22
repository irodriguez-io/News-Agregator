package io.irodriguez.intentionalreading.domain.model

enum class Category(val id: String) {
    SCIENCE("science"),
    TECHNOLOGY("technology"),
    LITERATURE("literature"),
    HISTORY("history"),
    WEIGHTLIFTING("weightlifting"),
    IAM("iam"),
    IDENTITY_AUTOMATION("identity_automation"),
    ;

    companion object {
        fun fromId(id: String): Category? = entries.firstOrNull { it.id == id }
    }
}
