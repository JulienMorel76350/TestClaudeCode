package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case

import com.veoneer.logisticinventoryapp.core.domain.model.MovexReferenceInventory
import com.veoneer.logisticinventoryapp.core.domain.repository.InventoryRepository
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryItem
import javax.inject.Inject

/**
 * Use case pour envoyer l'inventaire au backend
 */
class SendInventoryUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    suspend operator fun invoke(
        inventoryNumber: String,
        locationCode: String,
        references: List<ReferenceInventoryItem>
    ): Result<Unit> = runCatching {
        if (references.isEmpty()) {
            throw Exception("Aucune référence à envoyer")
        }

        references.forEach { item ->
            val response = inventoryRepository.createReferenceInventory(
                MovexReferenceInventory(
                    inventoryNumber = inventoryNumber,
                    locationCode = locationCode,
                    reference = item.code,
                    numberOfUnits = "1",
                    quantityByUnit = item.quantity.toString()
                )
            )

            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string()
                    ?: "Erreur lors de l'envoi de la référence ${item.code}"
                throw Exception(errorMsg)
            }
        }
    }
}
