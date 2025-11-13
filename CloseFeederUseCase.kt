package com.veoneer.logisticinventoryapp.core.domain.use_case.feeder

import com.veoneer.logisticinventoryapp.core.service.inventory.InventoryService
import com.veoneer.logisticinventoryapp.core.utils.ApiResult
import javax.inject.Inject

/**
 * UseCase pour fermer un feeder après validation complète
 */
class CloseFeederUseCase @Inject constructor(
    private val inventoryService: InventoryService
) {
    /**
     * Ferme le feeder dans le système
     * @param password Mot de passe de validation
     * @param feederId ID complet du feeder (emplacement + numéro)
     * @return ApiResult<Unit>
     */
    suspend operator fun invoke(
        password: String,
        feederId: String
    ): ApiResult<Unit> {
        return try {
            inventoryService.closeZone(
                password = password,
                zone = feederId
            )
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Failure(e)
        }
    }
}
