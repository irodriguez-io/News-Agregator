package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.data.local.DatasetSource
import io.irodriguez.intentionalreading.domain.validation.DatasetErrorCode
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.DatasetValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatasetRepository(
    private val source: DatasetSource,
    private val validator: DatasetValidator = DatasetValidator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun load(): DatasetResult = withContext(ioDispatcher) {
        try {
            validator.validate(source.read())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            DatasetResult.Failure(
                code = DatasetErrorCode.MALFORMED_DATASET,
                message = "The bundled dataset could not be read",
            )
        }
    }
}
