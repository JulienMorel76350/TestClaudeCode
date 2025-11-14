package com.veoneer.logisticinventoryapp.core.data

import com.veoneer.logisticinventoryapp.core.data.api.LocationStockDAO
import com.veoneer.logisticinventoryapp.core.domain.model.LocationContentRequest
import com.veoneer.logisticinventoryapp.core.domain.model.LocationContentResponse
import com.veoneer.logisticinventoryapp.core.domain.repository.LocationStockRepository
import com.veoneer.logisticinventoryapp.core.utils.ApiResult
import javax.inject.Inject

class LocationStockRepositoryImpl @Inject constructor(
    private val apiService: LocationStockDAO
) : LocationStockRepository {

    override suspend fun getLocationContent(
        request: LocationContentRequest
    ): Result<LocationContentResponse> {
        return try {
            val response = apiService.getLocationContent(request)

            when {
                response.isSuccessful -> {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        Result.success(responseBody)
                    } else {
                        Result.failure(Exception("Réponse vide de l'API"))
                    }
                }
                else -> {
                    Result.failure(Exception("Erreur HTTP: ${response.code()} ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
