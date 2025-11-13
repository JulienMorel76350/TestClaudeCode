package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case

import com.veoneer.logisticinventoryapp.core.domain.model.Reference

class NumberOfRefIsValidUseCase {
    operator fun invoke( userInput: String, List: MutableList<Reference> ) : Boolean {
        var nombreOfTotalPanel = 0
        var count = List.count()
        return count == userInput.toInt()
    }
}