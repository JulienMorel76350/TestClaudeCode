package com.veoneer.logisticinventoryapp.core.domain.use_case

import com.veoneer.logisticinventoryapp.core.domain.model.MovexReferenceInventory
import com.veoneer.logisticinventoryapp.core.domain.repository.InventoryRepository
import com.veoneer.logisticinventoryapp.core.presentation.state.PFReference
import javax.inject.Inject

class SendPFInventoryUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        inventoryNumber: String,
        locationCode: String,
        references: List<PFReference>
    ): Result<Unit> {
        return try {
            references.forEach { reference ->
                val response = inventoryRepository.createReferenceInventory(
                    MovexReferenceInventory(
                        inventoryNumber = inventoryNumber,
                        locationCode = locationCode,
                        reference = reference.reference,
                        numberOfUnits = "1",
                        quantityByUnit = reference.totalQuantity.toString(),
                        boxNumber = null
                    )
                )

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Erreur envoi référence ${reference.reference}: ${response.errorBody()?.string()}")
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
