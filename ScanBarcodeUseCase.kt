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
            // C'est un code famille (commence par "30" et moins de 8 caractères OU commence par "30S")
            code.startsWith("30") && (code.length < 8 || code.startsWith("30S")) -> {
                val response = refRepository.getFamily(code.toBase64())

                if (!response.isSuccessful) {
                    throw Exception(response.errorBody()?.string() ?: "Erreur lors de la récupération de la famille")
                }

                val familyList = response.body()?.filter {
                    it.typeOfProduct == "SO" || it.typeOfProduct == "SF"
                } ?: emptyList()

                if (familyList.isEmpty()) {
                    throw Exception("Aucune information trouvée pour cette famille")
                }

                ScannedItem.Family(
                    familyCode = code,
                    types = familyList
                )
            }
            // C'est une référence produit (doit avoir au moins 8 caractères)
            code.length >= 8 -> {
                val response = tracabilityDAO.getReferenceAndProductType(code)

                if (!response.isSuccessful) {
                    throw Exception(response.errorBody()?.string() ?: "Référence invalide")
                }

                val body = response.body()
                    ?: throw Exception("Aucune donnée reçue pour cette référence")

                ScannedItem.Reference(
                    code = body.Response,
                    type = body.Type
                )
            }
            // Code trop court pour être un produit
            else -> {
                throw Exception("Code invalide : les références produit doivent avoir au moins 8 caractères")
            }
        }
    }
}
