package com.veoneer.logisticinventoryapp.core.domain.use_case

import com.veoneer.logisticinventoryapp.core.domain.model.LocationContentRequest
import com.veoneer.logisticinventoryapp.core.domain.model.LocationContentResponse
import com.veoneer.logisticinventoryapp.core.domain.repository.LocationStockRepository
import com.veoneer.logisticinventoryapp.core.utils.ApiResult
import javax.inject.Inject

class GetLocationContentUseCase @Inject constructor(
    private val repository: LocationStockRepository
) {
    suspend operator fun invoke(
        reference: String,
        location: String
    ): Result<LocationContentResponse> {
        return repository.getLocationContent(
            LocationContentRequest(
                reference = reference,
                location = location,
                startingNumber = null
            )
        )
    }
}
