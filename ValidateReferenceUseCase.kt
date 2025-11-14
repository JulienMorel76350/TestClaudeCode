package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case

import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryItem
import javax.inject.Inject

/**
 * Use case pour valider qu'une référence peut être ajoutée à l'inventaire
 */
class ValidateReferenceUseCase @Inject constructor() {

    operator fun invoke(
        reference: String,
        existingReferences: List<ReferenceInventoryItem>
    ): Result<Unit> = runCatching {
        require(reference.isNotBlank()) {
            "La référence ne peut pas être vide"
        }
        // Note: On permet les doublons car le système de groupement
        // est fait pour gérer plusieurs entrées de la même référence
    }
}
