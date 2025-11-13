package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case

import com.veoneer.logisticinventoryapp.core.domain.model.Reference

class ReferenceInventoryIsOnGoingUseCase() {
    operator fun invoke(reference: String, listOfOnGoingReferences: List<Reference>): Boolean {
        return listOfOnGoingReferences.any {it.code == reference}
    }
}