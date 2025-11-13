package com.veoneer.logisticinventoryapp.core.domain.use_case.feeder

import com.veoneer.logisticinventoryapp.core.domain.model.FeederQuantities
import com.veoneer.logisticinventoryapp.core.domain.model.Reel
import com.veoneer.logisticinventoryapp.core.domain.repository.ReelRepository
import javax.inject.Inject

/**
 * UseCase pour récupérer une bobine depuis l'ERP (fallback)
 */
class GetReelFromERPUseCase @Inject constructor(
    private val reelRepository: ReelRepository
) {
    suspend operator fun invoke(reelCode: String): Result<FeederQuantities> {
        return try {
            val response = reelRepository.getReel(reelCode)

            if (response.isSuccessful && response.body() != null) {
                val reel = response.body()!!

                // Mapper Reel → FeederQuantities
                val feederQuantities = FeederQuantities(
                    id = reel.uniqueCode,
                    materialID = reel.reference,
                    originalQuantity = reel.quantity.toInt()  ,
                    currentQuantity = reel.quantity.toInt() ,
                    difference = 0
                )

                Result.success(feederQuantities)
            } else {
                Result.failure(Exception("Bobine introuvable dans l'ERP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}