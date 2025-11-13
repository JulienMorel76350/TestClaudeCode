package com.veoneer.logisticinventoryapp.core.domain.use_case.feeder

import com.veoneer.logisticinventoryapp.core.domain.model.FeederQuantities
import com.veoneer.logisticinventoryapp.core.domain.repository.FeederRepository

class GetFeederQuatityUseCase(
    private val lotRepository: FeederRepository
) {
    suspend operator fun invoke(dataMatrix: String): Result<FeederQuantities> {
        return try {
            lotRepository.getLotQuantitiesByDataMatrix(dataMatrix)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}