package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state

import com.veoneer.logisticinventoryapp.core.domain.model.Family

/**
 * État global de l'écran d'inventaire de références
 */
data class ReferenceInventoryState(
    val inventoryNumber: String,
    val locationCode: String,
    val mode: InventoryMode = InventoryMode.Scanning,
    val scannedReferences: List<ReferenceInventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isComplete: Boolean = false
)

/**
 * Modes de l'écran
 */
sealed class InventoryMode {
    object Scanning : InventoryMode()
    data class SelectingType(
        val familyCode: String,
        val types: List<Family>
    ) : InventoryMode()
    data class EditingQuantity(
        val referenceCode: String,
        val referenceType: String
    ) : InventoryMode()
}

/**
 * Item dans la liste des références scannées
 */
data class ReferenceInventoryItem(
    val code: String,
    val quantity: Int,
    val type: String,
    val id: String = java.util.UUID.randomUUID().toString() // ID unique pour chaque entrée
)

/**
 * Groupe de références identiques
 */
data class GroupedReference(
    val reference: String,
    val type: String,
    val itemCount: Int,           // Nombre d'entrées
    val totalQuantity: Int,       // Quantité totale
    val items: List<ReferenceInventoryItem> // Détails de chaque entrée
)

/**
 * Résultat du scan de code-barre
 */
sealed class ScannedItem {
    data class Family(
        val familyCode: String,
        val types: List<com.veoneer.logisticinventoryapp.core.domain.model.Family>
    ) : ScannedItem()

    data class Reference(
        val code: String,
        val type: String
    ) : ScannedItem()
}

/**
 * État temporaire pendant l'ajout d'une référence
 */
sealed class PendingReference {
    data class FromFamily(
        val familyCode: String,
        val availableTypes: List<Family>,
        val selectedReference: String? = null,
        val selectedType: String? = null
    ) : PendingReference()

    data class FromScan(
        val code: String,
        val type: String
    ) : PendingReference()
}

/**
 * Événements UI
 */
sealed class ReferenceInventoryEvent {
    data class BarcodeScanned(val code: String) : ReferenceInventoryEvent()
    data class TypeSelected(val type: Family) : ReferenceInventoryEvent()
    data class QuantityEntered(val quantity: Int) : ReferenceInventoryEvent()
    data class ReferenceRemoved(val index: Int) : ReferenceInventoryEvent()
    data class ReferenceRemovedById(val id: String) : ReferenceInventoryEvent()
    object SendInventory : ReferenceInventoryEvent()
    object DismissError : ReferenceInventoryEvent()
    object ReturnToScanning : ReferenceInventoryEvent()
}
