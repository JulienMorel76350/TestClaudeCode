package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case

import com.veoneer.logisticinventoryapp.core.data.api.TracabilityDAO
import com.veoneer.logisticinventoryapp.core.domain.repository.RefRepository
import com.veoneer.logisticinventoryapp.core.utils.toBase64
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ScannedItem
import javax.inject.Inject

/**
 * Use case pour gérer le scan d'un code-barre
 * Détermine si c'est une famille (30S) ou une référence directe
 */
class ScanBarcodeUseCase @Inject constructor(
    private val tracabilityDAO: TracabilityDAO,
    private val refRepository: RefRepository
) {

    suspend operator fun invoke(code: String): Result<ScannedItem> = runCatching {
        when {
            // C'est un code famille (moins de 8 caractères : IL, VW, AA, 30, 30S, etc.)
            code.length < 8 -> {
                val response = refRepository.getFamily(code.toBase64())

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }
                    throw Exception(errorMsg ?: "Famille inconnue ou introuvable")
                }

                val body = response.body()
                if (body == null || body.isEmpty()) {
                    throw Exception("Famille '$code' introuvable")
                }

                val familyList = body.filter {
                    it.typeOfProduct == "SO" || it.typeOfProduct == "SF"
                }

                if (familyList.isEmpty()) {
                    throw Exception("Aucun produit trouvé pour la famille '$code'")
                }

                ScannedItem.Family(
                    familyCode = code,
                    types = familyList
                )
            }
            // C'est une référence produit (au moins 8 caractères)
            else -> {
                val response = tracabilityDAO.getReferenceAndProductType(code)

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }
                    throw Exception(errorMsg ?: "Référence produit introuvable")
                }

                val body = response.body()
                    ?: throw Exception("Référence '$code' introuvable")

                ScannedItem.Reference(
                    code = body.Response,
                    type = body.Type
                )
            }
        }
    }
}
