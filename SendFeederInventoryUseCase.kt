package com.veoneer.logisticinventoryapp.core.domain.use_case.feeder

import android.util.Log

import com.veoneer.logisticinventoryapp.core.domain.model.FeederQuantities
import com.veoneer.logisticinventoryapp.core.domain.model.MovexReferenceInventory
import com.veoneer.logisticinventoryapp.core.domain.repository.InventoryRepository
import com.veoneer.logisticinventoryapp.core.utils.ApiResult
import javax.inject.Inject

/**
 * UseCase pour envoyer l'inventaire des bobines d'un feeder vers Movex
 * Regroupe les bobines par référence et envoie la somme des quantités
 */
class SendFeederInventoryUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    companion object {
        private const val TAG = "SendFeederInventory"
    }

    /**
     * Envoie toutes les bobines d'un feeder vers Movex
     * @param inventoryNumber Numéro d'inventaire
     * @param locationCode Code emplacement
     * @param feederId ID du feeder
     * @param reels Liste des bobines scannées
     * @return ApiResult<Unit>
     */
    suspend operator fun invoke(
        inventoryNumber: String,
        locationCode: String,
        feederId: String,
        reels: List<FeederQuantities>
    ): ApiResult<Unit> {
        return try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Début envoi inventaire feeder")
            Log.d(TAG, "Feeder ID: $feederId")
            Log.d(TAG, "Inventory Number: $inventoryNumber")
            Log.d(TAG, "Location Code: $locationCode")
            Log.d(TAG, "Nombre total de bobines scannées: ${reels.size}")
            Log.d(TAG, "========================================")

            // Étape 1 : Regrouper les bobines par référence (materialID)
            val groupedByReference = reels.groupBy { it.materialID }

            Log.d(TAG, "Nombre de références uniques: ${groupedByReference.size}")

            // Étape 2 : Calculer la quantité totale par référence
            val referenceQuantities = groupedByReference.map { (materialID, bobines) ->
                val totalQuantity = bobines.sumOf { it.currentQuantity }

                Log.d(TAG, "")
                Log.d(TAG, "Référence: $materialID")
                Log.d(TAG, "  - Nombre de bobines: ${bobines.size}")
                bobines.forEachIndexed { index, bobine ->
                    Log.d(TAG, "    Bobine ${index + 1}: ID=${bobine.id}, Qty=${bobine.currentQuantity}")
                }
                Log.d(TAG, "  - Quantité totale: $totalQuantity")

                materialID to totalQuantity
            }

            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Envoi vers Movex...")
            Log.d(TAG, "========================================")

            // Étape 3 : Envoyer chaque référence vers Movex
            referenceQuantities.forEachIndexed { index, (reference, totalQuantity) ->
                Log.d(TAG, "")
                Log.d(TAG, "Envoi ${index + 1}/${referenceQuantities.size}")
                Log.d(TAG, "  Reference: $reference")
                Log.d(TAG, "  InventoryNumber: $inventoryNumber")
                Log.d(TAG, "  LocationCode: $locationCode")
                Log.d(TAG, "  NumberOfUnits: 1")
                Log.d(TAG, "  QuantityByUnit: $totalQuantity")

                val movexInventory = MovexReferenceInventory(
                    reference = reference,
                    inventoryNumber = inventoryNumber,
                    locationCode = locationCode,
                    numberOfUnits = "1",
                    quantityByUnit = totalQuantity.toString()
                )

                val response = inventoryRepository.createReferenceInventory(movexInventory)

                if (!response.isSuccessful) {
                    Log.e(TAG, "  ❌ Échec envoi pour $reference")
                } else {
                    Log.d(TAG, "  ✅ Envoi réussi pour $reference")

                }

            }

            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ Tous les envois terminés avec succès")
            Log.d(TAG, "Total références envoyées: ${referenceQuantities.size}")
            Log.d(TAG, "========================================")

            ApiResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "")
            Log.e(TAG, "========================================")
            Log.e(TAG, "❌ ERREUR lors de l'envoi inventaire")
            Log.e(TAG, "Message: ${e.message}")
            Log.e(TAG, "Type: ${e.javaClass.simpleName}")
            Log.e(TAG, "========================================", e)

            ApiResult.Failure(e)
        }
    }
}