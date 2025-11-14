package com.veoneer.logisticinventoryapp.core.presentation.state

import com.veoneer.logisticinventoryapp.core.domain.model.Location

data class PFInventoryState(
    val location: Location? = null,
    
    val references: List<PFReference> = emptyList(),
    
    val currentDetailReference: String? = null,
    val cartons: List<PFCarton> = emptyList(),
    
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    
    val editingCarton: PFCarton? = null,
    val editingQuantity: String = "",
    val deletingCarton: PFCarton? = null,
    
    val manualRefInput: String = "",
    val manualSerialInput: String = "",
    val manualQtyInput: String = "",
    val manualSerialCounter: Int = 1,
    
    val showValidationDialog: Boolean = false
)

data class PFReference(
    val reference: String,
    val totalQuantity: Int,
    val cartonCount: Int,
    val nonStandardCount: Int,
    val standardQuantity: Int?
)

data class PFCarton(
    val reelGaliaNumber: String,
    val quantity: Int,
    val isConfirmed: Boolean = false,
    val isManual: Boolean = false
)
