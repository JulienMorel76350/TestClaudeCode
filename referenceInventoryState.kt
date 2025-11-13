package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state

import androidx.compose.runtime.mutableStateListOf
import com.veoneer.logisticinventoryapp.core.domain.model.Family
import com.veoneer.logisticinventoryapp.core.domain.model.Reference

data class referenceInventoryState(
    val userInput : String = "",
    val familyList : MutableList<Family> = mutableStateListOf(),
    val refList : MutableList<Reference> = mutableStateListOf(),
)
