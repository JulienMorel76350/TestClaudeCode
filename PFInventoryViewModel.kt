package com.veoneer.logisticinventoryapp.core.service.pf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veoneer.logisticinventoryapp.core.domain.use_case.GetLocationContentUseCase
import com.veoneer.logisticinventoryapp.core.presentation.event.PFInventoryEvent
import com.veoneer.logisticinventoryapp.core.presentation.state.PFCarton
import com.veoneer.logisticinventoryapp.core.presentation.state.PFInventoryState
import com.veoneer.logisticinventoryapp.core.presentation.state.PFReference
import com.veoneer.logisticinventoryapp.core.service.MainService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PFInventoryViewModel @Inject constructor(
    application: Application,
    private val getLocationContentUseCase: GetLocationContentUseCase,
    private val mainService: MainService
) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    private val _state = MutableStateFlow(PFInventoryState())
    val state: StateFlow<PFInventoryState> = _state.asStateFlow()

    private val _events = Channel<PFInventoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private fun emitEvent(event: PFInventoryEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    init {
        val location = mainService.inventoryLocationTotal
        if (location.code.isNotEmpty()) {
            _state.value = _state.value.copy(location = location)

            if (!location.isMultireference && location.referenceEmpl.isNotEmpty()) {
                loadLocationContent(location.referenceEmpl, location.code)
            }
        }
    }

    private fun loadLocationContent(reference: String, locationCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = "")
                mainService.loaderRunning = true
            }

            val result = getLocationContentUseCase(reference, locationCode)

            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    val references = response.boxGroups.map { boxGroup ->
                        val allReels = boxGroup.lotGroups.flatMap { it.reels }
                        val quantities = allReels.map { it.quantity }
                        val standardQty = quantities.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                        val nonStandardCount = if (standardQty != null) {
                            quantities.count { it != standardQty }
                        } else 0

                        PFReference(
                            reference = boxGroup.boxCustomerNumber,
                            totalQuantity = boxGroup.totalQuantity,
                            cartonCount = allReels.size,
                            nonStandardCount = nonStandardCount,
                            standardQuantity = standardQty
                        )
                    }
                    _state.value = _state.value.copy(
                        references = references,
                        isLoading = false
                    )
                }.onFailure { error ->
                    _state.value = _state.value.copy(
                        errorMessage = "Erreur: ${error.message}",
                        isLoading = false
                    )
                    emitEvent(PFInventoryEvent.PlayErrorSound)
                }
                mainService.loaderRunning = false
            }
        }
    }

    fun processScan(scanResult: String, label: String) {
        when {
            scanResult.startsWith("P", ignoreCase = true) -> {
                val reference = scanResult.substring(1).uppercase()
                handleReferenceScan(reference)
            }
            else -> {
                val location = _state.value.location
                if (location != null && location.isMultireference) {
                    handleReferenceScan(scanResult)
                }
            }
        }
    }

    private fun handleReferenceScan(reference: String) {
        val location = _state.value.location ?: return
        loadLocationContent(reference, location.code)
        emitEvent(PFInventoryEvent.NavigateToDetails(reference))
    }

    fun navigateToDetails(reference: String) {
        val location = _state.value.location ?: return

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isLoading = true)
                mainService.loaderRunning = true
            }

            val result = getLocationContentUseCase(reference, location.code)

            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    val boxGroup = response.boxGroups.find { it.boxCustomerNumber == reference }
                    if (boxGroup != null) {
                        val cartons = boxGroup.lotGroups.flatMap { lotGroup ->
                            lotGroup.reels.map { reel ->
                                PFCarton(
                                    reelGaliaNumber = reel.reelGaliaNumber,
                                    quantity = reel.quantity,
                                    isConfirmed = false,
                                    isManual = false
                                )
                            }
                        }
                        _state.value = _state.value.copy(
                            currentDetailReference = reference,
                            cartons = cartons,
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    _state.value = _state.value.copy(
                        errorMessage = "Erreur: ${error.message}",
                        isLoading = false
                    )
                    emitEvent(PFInventoryEvent.PlayErrorSound)
                }
                mainService.loaderRunning = false
            }
        }
    }

    fun navigateBackToMain() {
        _state.value = _state.value.copy(
            currentDetailReference = null,
            cartons = emptyList()
        )
    }

    fun toggleCartonConfirmation(carton: PFCarton) {
        val updatedCartons = _state.value.cartons.map {
            if (it.reelGaliaNumber == carton.reelGaliaNumber) {
                it.copy(isConfirmed = !it.isConfirmed)
            } else it
        }
        _state.value = _state.value.copy(cartons = updatedCartons)
    }

    fun startEditCarton(carton: PFCarton) {
        _state.value = _state.value.copy(
            editingCarton = carton,
            editingQuantity = carton.quantity.toString()
        )
    }

    fun cancelEditCarton() {
        _state.value = _state.value.copy(
            editingCarton = null,
            editingQuantity = ""
        )
    }

    fun updateEditingQuantity(value: String) {
        _state.value = _state.value.copy(editingQuantity = value)
    }

    fun confirmEditCarton() {
        val editing = _state.value.editingCarton ?: return
        val newQty = _state.value.editingQuantity.toIntOrNull()

        if (newQty == null || newQty <= 0) {
            emitEvent(PFInventoryEvent.PlayErrorSound)
            return
        }

        val updatedCartons = _state.value.cartons.map {
            if (it.reelGaliaNumber == editing.reelGaliaNumber) {
                it.copy(quantity = newQty)
            } else it
        }

        _state.value = _state.value.copy(
            cartons = updatedCartons,
            editingCarton = null,
            editingQuantity = ""
        )
        emitEvent(PFInventoryEvent.PlaySuccessSound)
    }

    fun startDeleteCarton(carton: PFCarton) {
        _state.value = _state.value.copy(deletingCarton = carton)
    }

    fun cancelDeleteCarton() {
        _state.value = _state.value.copy(deletingCarton = null)
    }

    fun confirmDeleteCarton() {
        val deleting = _state.value.deletingCarton ?: return
        val updatedCartons = _state.value.cartons.filter {
            it.reelGaliaNumber != deleting.reelGaliaNumber
        }
        _state.value = _state.value.copy(
            cartons = updatedCartons,
            deletingCarton = null
        )
        emitEvent(PFInventoryEvent.PlaySuccessSound)
    }

    fun updateManualRefInput(value: String) {
        _state.value = _state.value.copy(manualRefInput = value.uppercase())
    }

    fun updateManualSerialInput(value: String) {
        _state.value = _state.value.copy(manualSerialInput = value.uppercase())
    }

    fun updateManualQtyInput(value: String) {
        _state.value = _state.value.copy(manualQtyInput = value)
    }

    fun addManualCarton() {
        val serial = _state.value.manualSerialInput.trim()
        val qtyStr = _state.value.manualQtyInput.trim()

        if (serial.isEmpty()) {
            emitEvent(PFInventoryEvent.PlayErrorSound)
            emitEvent(PFInventoryEvent.ShowToast("Veuillez saisir un numéro de série"))
            return
        }

        val qty = qtyStr.toIntOrNull()
        if (qty == null || qty <= 0) {
            emitEvent(PFInventoryEvent.PlayErrorSound)
            emitEvent(PFInventoryEvent.ShowToast("Veuillez saisir une quantité valide"))
            return
        }

        val isDuplicate = _state.value.cartons.any { it.reelGaliaNumber == serial }
        if (isDuplicate) {
            emitEvent(PFInventoryEvent.PlayErrorSound)
            emitEvent(PFInventoryEvent.ShowToast("Ce numéro de série existe déjà"))
            return
        }

        val newCarton = PFCarton(
            reelGaliaNumber = serial,
            quantity = qty,
            isConfirmed = false,
            isManual = true
        )

        val updatedCartons = _state.value.cartons.toMutableList()
        updatedCartons.add(newCarton)

        _state.value = _state.value.copy(
            cartons = updatedCartons,
            manualSerialInput = "",
            manualQtyInput = ""
        )

        emitEvent(PFInventoryEvent.PlaySuccessSound)
        emitEvent(PFInventoryEvent.HideKeyboard)
    }

    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessage = "")
    }

    fun showValidationDialog() {
        _state.value = _state.value.copy(showValidationDialog = true)
    }

    fun hideValidationDialog() {
        _state.value = _state.value.copy(showValidationDialog = false)
    }

    fun validateAndSend() {
        emitEvent(PFInventoryEvent.ShowToast("Validation en cours..."))
    }
}