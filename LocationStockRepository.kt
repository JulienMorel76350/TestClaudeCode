package com.veoneer.logisticinventoryapp.core.domain.repository

import com.veoneer.logisticinventoryapp.core.domain.model.LocationContentRequest
import com.veoneer.logisticinventoryapp.core.domain.model.LocationContentResponse
import com.veoneer.logisticinventoryapp.core.utils.ApiResult

interface LocationStockRepository {
    suspend fun getLocationContent(request: LocationContentRequest): Result<LocationContentResponse>
}