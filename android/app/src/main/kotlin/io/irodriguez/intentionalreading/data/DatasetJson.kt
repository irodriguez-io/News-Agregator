package io.irodriguez.intentionalreading.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
internal val DatasetJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = true
    isLenient = false
    coerceInputValues = false
    allowTrailingComma = false
    allowComments = false
}
