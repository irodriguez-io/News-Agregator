package io.irodriguez.intentionalreading.domain.model

enum class Appearance(val wireValue: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"),
    ;

    companion object {
        fun fromWireValue(value: String): Appearance? = entries.firstOrNull { it.wireValue == value }
    }
}
