package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.data.local.DatasetSource
import io.irodriguez.intentionalreading.domain.validation.DatasetErrorCode
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.DatasetValidator

class DatasetRepository(
    private val source: DatasetSource,
    private val validator: DatasetValidator = DatasetValidator(),
) {
    fun load(): DatasetResult = try {
        validator.validate(source.read())
    } catch (exception: Exception) {
        DatasetResult.Failure(
            code = DatasetErrorCode.MALFORMED_DATASET,
            message = "The bundled dataset could not be read",
        )
    }
}
