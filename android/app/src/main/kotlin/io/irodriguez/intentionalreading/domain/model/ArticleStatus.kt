package io.irodriguez.intentionalreading.domain.model

enum class ArticleStatus(val wireValue: String?) {
    UNSEEN(null),
    OPENED("opened"),
    SAVED("saved"),
    DISMISSED("dismissed"),
    READ("read"),
    ;

    companion object {
        fun fromWireValue(value: String): ArticleStatus? = entries.firstOrNull { it.wireValue == value }
    }
}
