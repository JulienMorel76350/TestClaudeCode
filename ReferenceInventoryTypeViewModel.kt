package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veoneer.logisticinventoryapp.core.data.api.TracabilityDAO
import com.veoneer.logisticinventoryapp.core.domain.model.Family
import com.veoneer.logisticinventoryapp.core.domain.repository.InventoryRepository
import com.veoneer.logisticinventoryapp.core.domain.repository.RefRepository
import com.veoneer.logisticinventoryapp.core.service.MainService
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case.ScanBarcodeUseCase
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case.SendInventoryUseCase
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case.ValidateReferenceUseCase
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.InventoryMode
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.PendingReference
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryEvent
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryItem
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryState
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ScannedItem
import com.veoneer.logisticinventoryapp.scanner.domain.ScannerRepository
import com.veoneer.logisticinventoryapp.scanner.domain.model.BarcodeScanResult
import com.veoneer.logisticinventoryapp.scanner.domain.model.TriggerFeedbackEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel pour l'écran d'inventaire de références
 * Architecture moderne avec State + Events
 */
@HiltViewModel
class ReferenceInventoryTypeViewModel @Inject constructor(
    private val tracabilityDAO: TracabilityDAO,
    private val inventoryRepository: InventoryRepository,
    private val refRepository: RefRepository,
    private val scannerRepository: ScannerRepository,
    private val scanBarcodeUseCase: ScanBarcodeUseCase,
    private val validateReferenceUseCase: ValidateReferenceUseCase,
    private val sendInventoryUseCase: SendInventoryUseCase,
    private val mainService: MainService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // État central
    private val _state = MutableStateFlow(
        ReferenceInventoryState(
            inventoryNumber = mainService.inventoryNumber,
            locationCode = mainService.inventoryLocation
        )
    )
    val state = _state.asStateFlow()

    // États du scanner (réutilisation de l'existant)
    val scannerStateFlow = scannerRepository.scannerStateFlow
    val barcodeScanState = scannerRepository.barcodeScanState

    // Référence en cours d'ajout
    private var pendingReference: PendingReference? = null

    /**
     * Point d'entrée unique pour tous les événements UI
     */
    fun onEvent(event: ReferenceInventoryEvent) {
        when (event) {
            is ReferenceInventoryEvent.BarcodeScanned -> handleBarcodeScan(event.code)
            is ReferenceInventoryEvent.TypeSelected -> setPendingReferenceType(event.type)
            is ReferenceInventoryEvent.QuantityEntered -> addReferenceWithQuantity(event.quantity)
            is ReferenceInventoryEvent.ReferenceRemoved -> removeReference(event.index)
            is ReferenceInventoryEvent.SendInventory -> sendInventory()
            is ReferenceInventoryEvent.DismissError -> dismissError()
            is ReferenceInventoryEvent.ReturnToScanning -> returnToScanning()
        }
    }

    /**
     * Gère le scan de code-barre depuis le scanner Zebra
     */
    fun handleBarcodeScan(barcodeScanResult: BarcodeScanResult) {
        onEvent(ReferenceInventoryEvent.BarcodeScanned(barcodeScanResult.barcodeContent))
    }

    /**
     * Traite le code-barre scanné
     */
    private fun handleBarcodeScan(code: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        scanBarcodeUseCase(code)
            .onSuccess { scannedItem ->
                when (scannedItem) {
                    is ScannedItem.Family -> {
                        pendingReference = PendingReference.FromFamily(
                            familyCode = scannedItem.familyCode,
                            availableTypes = scannedItem.types
                        )
                        _state.update {
                            it.copy(
                                mode = InventoryMode.SelectingType(
                                    familyCode = scannedItem.familyCode,
                                    types = scannedItem.types
                                ),
                                isLoading = false
                            )
                        }
                        scannerRepository.triggerFeedback(TriggerFeedbackEnum.SUCCESS)
                    }

                    is ScannedItem.Reference -> {
                        pendingReference = PendingReference.FromScan(
                            code = scannedItem.code,
                            type = scannedItem.type
                        )
                        _state.update {
                            it.copy(
                                mode = InventoryMode.EditingQuantity(
                                    referenceCode = scannedItem.code,
                                    referenceType = scannedItem.type
                                ),
                                isLoading = false
                            )
                        }
                        scannerRepository.triggerFeedback(TriggerFeedbackEnum.SUCCESS)
                    }
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Erreur de scan"
                    )
                }
                scannerRepository.triggerFeedback(TriggerFeedbackEnum.ERROR)
            }
    }

    /**
     * Définit le type sélectionné pour une famille
     */
    private fun setPendingReferenceType(type: Family) {
        val pending = pendingReference as? PendingReference.FromFamily ?: return

        pendingReference = pending.copy(
            selectedReference = type.reference,
            selectedType = type.designation
        )

        _state.update {
            it.copy(
                mode = InventoryMode.EditingQuantity(
                    referenceCode = type.reference,
                    referenceType = type.designation
                )
            )
        }
    }

    /**
     * Ajoute la référence avec la quantité saisie
     */
    private fun addReferenceWithQuantity(quantity: Int) = viewModelScope.launch {
        val pending = pendingReference ?: return@launch

        if (quantity <= 0) {
            _state.update { it.copy(error = "La quantité doit être supérieure à 0") }
            return@launch
        }

        val newItem = when (pending) {
            is PendingReference.FromFamily -> {
                val ref = pending.selectedReference
                val type = pending.selectedType
                if (ref == null || type == null) {
                    _state.update { it.copy(error = "Veuillez sélectionner un type") }
                    return@launch
                }
                ReferenceInventoryItem(
                    code = ref,
                    quantity = quantity,
                    type = type.take(3)
                )
            }

            is PendingReference.FromScan -> ReferenceInventoryItem(
                code = pending.code,
                quantity = quantity,
                type = pending.type
            )
        }

        validateReferenceUseCase(newItem.code, _state.value.scannedReferences)
            .onSuccess {
                _state.update { state ->
                    state.copy(
                        scannedReferences = state.scannedReferences + newItem,
                        mode = InventoryMode.Scanning,
                        successMessage = "Référence ${newItem.code} ajoutée"
                    )
                }
                pendingReference = null
            }
            .onFailure { error ->
                _state.update { it.copy(error = error.message) }
            }
    }

    /**
     * Supprime une référence de la liste
     */
    private fun removeReference(index: Int) {
        _state.update { state ->
            state.copy(
                scannedReferences = state.scannedReferences.filterIndexed { i, _ -> i != index }
            )
        }
    }

    /**
     * Envoie l'inventaire au backend
     */
    private fun sendInventory() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        val currentState = _state.value

        sendInventoryUseCase(
            inventoryNumber = currentState.inventoryNumber,
            locationCode = currentState.locationCode,
            references = currentState.scannedReferences
        )
            .onSuccess {
                mainService.informationMessage = "Envoi réussi"
                _state.update {
                    it.copy(
                        isLoading = false,
                        isComplete = true,
                        successMessage = "Inventaire envoyé avec succès"
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Erreur d'envoi"
                    )
                }
                scannerRepository.triggerFeedback(TriggerFeedbackEnum.ERROR)
            }
    }

    /**
     * Efface le message d'erreur
     */
    private fun dismissError() {
        _state.update { it.copy(error = null, successMessage = null) }
    }

    /**
     * Retour au mode scan
     */
    private fun returnToScanning() {
        pendingReference = null
        _state.update { it.copy(mode = InventoryMode.Scanning) }
    }
}
