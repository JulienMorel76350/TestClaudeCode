package com.veoneer.logisticinventoryapp.core.domain.use_case

import com.veoneer.logisticinventoryapp.core.domain.use_case.rack.CreateRackUseCase
import com.veoneer.logisticinventoryapp.core.service.location.LocationService
import com.veoneer.logisticinventoryapp.core.domain.repository.RackRepository
import com.veoneer.logisticinventoryapp.core.utils.ApiResult
import javax.inject.Inject

/**
 * UseCase pour vérifier et préparer un Rack ou Feeder
 *
 * Logique :
 * 1. Vérifier si l'item existe dans Movex (checkEmplacement avec concat)
 * 2. Si n'existe PAS → Le créer
 * 3. Si existe → Vérifier s'il a déjà été inventorié dans cette session
 */
class CheckAndPrepareInventoryItemUseCase @Inject constructor(
    private val locationService: LocationService,
    private val rackRepository: RackRepository,
    private val createRackUseCase: CreateRackUseCase
) {
    suspend operator fun invoke(
        inventoryNumber: String,
        emplacement: String,
        itemNumber: String
    ): ApiResult<Unit> {
        return try {
            // Concaténer emplacement + numéro (ex: "ZONE-A01" + "RACK-1" = "ZONE-A01-RACK-1")
            val fullItemId = "$emplacement!$itemNumber"

            // Étape 1 : Vérifier si l'item existe dans Movex
            val itemExistsInMovex = try {
                locationService.checkEmplacement(fullItemId)
                true // Si pas d'exception, l'item existe
            } catch (e: Exception) {
                false // Item n'existe pas dans Movex
            }

            if (!itemExistsInMovex) {
                // Étape 2 : L'item n'existe pas dans Movex → Le créer
                val createResult = createRackUseCase(
                    rackNumber = itemNumber,
                    location = "PROD"
                )

                return when (createResult) {
                    is ApiResult.Success -> ApiResult.Success(Unit)
                    is ApiResult.Failure -> ApiResult.Failure(
                        Exception("Erreur lors de la création de l'item dans Movex")
                    )
                }
            }

            // Étape 3 : L'item existe dans Movex → Vérifier s'il a déjà été inventorié
            val response = rackRepository.checkRackAlreadyScannedInInventory(
                nInventaire = inventoryNumber,
                nRack = itemNumber
            )

            if (!response.isSuccessful) {
                // Déjà inventorié dans cette session
                return ApiResult.Failure(Exception("Ce feeder $itemNumber a déjà été inventorié"))
            }

            // Pas encore inventorié, c'est bon !
            ApiResult.Success(Unit)

        } catch (e: Exception) {
            ApiResult.Failure(e)
        }
    }
}