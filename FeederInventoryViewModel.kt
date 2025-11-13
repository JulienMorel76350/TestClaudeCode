package com.veoneer.logisticinventoryapp.core.service.feeder

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veoneer.logisticinventoryapp.core.domain.model.FeederQuantities
import com.veoneer.logisticinventoryapp.core.domain.use_case.CheckAndPrepareInventoryItemUseCase
import com.veoneer.logisticinventoryapp.core.domain.use_case.feeder.ValidateQuantityUseCase
import com.veoneer.logisticinventoryapp.core.domain.use_case.feeder.CloseFeederUseCase
import com.veoneer.logisticinventoryapp.core.domain.use_case.feeder.GetFeederQuatityUseCase
import com.veoneer.logisticinventoryapp.core.domain.use_case.feeder.GetReelFromERPUseCase
import com.veoneer.logisticinventoryapp.core.domain.use_case.feeder.NormalizeMovexCodeUseCase
import com.veoneer.logisticinventoryapp.core.domain.use_case.feeder.SendFeederInventoryUseCase
import com.veoneer.logisticinventoryapp.core.presentation.event.FeederInventoryEvent
import com.veoneer.logisticinventoryapp.core.presentation.state.FeederInventoryUIState
import com.veoneer.logisticinventoryapp.core.presentation.state.FeederWorkflowStep
import com.veoneer.logisticinventoryapp.core.service.MainService
import com.veoneer.logisticinventoryapp.core.utils.onFailure
import com.veoneer.logisticinventoryapp.core.utils.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FeederInventoryViewModel @Inject constructor(
    application: Application,
    private val mainService: MainService,
    private val checkAndPrepareItemUseCase: CheckAndPrepareInventoryItemUseCase,
    private val normalizeMovexCodeUseCase: NormalizeMovexCodeUseCase,
    private val getFeederQuantityUseCase: GetFeederQuatityUseCase,
    private val validateQuantityUseCase: ValidateQuantityUseCase,
    private val sendFeederInventoryUseCase: SendFeederInventoryUseCase,
    private val closeFeederUseCase: CloseFeederUseCase,
    private val getReelFromERPUseCase: GetReelFromERPUseCase
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private val _state = MutableStateFlow(FeederInventoryUIState())
    val state: StateFlow<FeederInventoryUIState> = _state.asStateFlow()

    private val _events = Channel<FeederInventoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var toastJob: Job? = null
    private var resetCountdownJob: Job? = null

    private fun emitEvent(event: FeederInventoryEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    init {
        val location = mainService.inventoryLocationTotal
        if (location.code.isNotEmpty()) {
            _state.value = _state.value.copy(
                location = location,
                workflowStep = FeederWorkflowStep.SCAN_FEEDER
            )
        }
    }

    fun processScan(scanResult: String, label: String) {
        val currentStep = _state.value.workflowStep

        when (currentStep) {
            FeederWorkflowStep.SCAN_FEEDER -> scanFeeder(scanResult)
            FeederWorkflowStep.SCANNING_REELS -> scanReel(scanResult)
            else -> {}
        }
    }

    private fun scanFeeder(feederNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = "")
                mainService.loaderRunning = true
            }

            try {
                val location = _state.value.location ?: throw Exception("Emplacement non défini")

                // Étape 1 : Normaliser le code scanné (max 10 caractères pour Movex)
                val normalizeResult = normalizeMovexCodeUseCase(feederNumber)

                val normalizedCode = normalizeResult.getOrElse {
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = "Code feeder invalide : ${it.message}"
                        )
                        mainService.loaderRunning = false
                        emitEvent(FeederInventoryEvent.PlayErrorSound)
                    }
                    return@launch
                }

                // Étape 2 : Vérifier et préparer le feeder
                val result = checkAndPrepareItemUseCase(
                    inventoryNumber = location.inventoryNumber,
                    emplacement = location.code,
                    itemNumber = normalizedCode
                )

                withContext(Dispatchers.Main) {
                    result.onSuccess {
                        val feederId = "${location.code}!$normalizedCode"
                        _state.value = _state.value.copy(
                            isLoading = false,
                            currentFeederId = feederId,
                            currentFeederNumber = normalizedCode,
                            workflowStep = FeederWorkflowStep.SCANNING_REELS,
                            scannedReels = emptyList(),
                            validationAttempts = 0,
                            actualReelCount = 0
                        )
                        mainService.loaderRunning = false
                        emitEvent(FeederInventoryEvent.PlaySuccessSound)
                    }.onFailure(context) { error ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Erreur inconnue"
                        )
                        mainService.loaderRunning = false
                        emitEvent(FeederInventoryEvent.PlayErrorSound)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur inconnue"
                    )
                    mainService.loaderRunning = false
                    emitEvent(FeederInventoryEvent.PlayErrorSound)
                }
            }
        }
    }

    private fun scanReel(dataMatrix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = "")
                mainService.loaderRunning = true
            }

            val decoded = mainService.decodeDatamatrix(dataMatrix)
            val reelCode = decoded.getMaterialManagerDatamatrix()
            val reelCodebIS = decoded.getDefaultBarcodeValue()

            // Tentative 1 : Base de données machines
            getFeederQuantityUseCase(reelCode)
                .onSuccess { feederQuantities ->
                    handleReelSuccess(feederQuantities)
                }
                .onFailure { exception ->
                    // Tentative 2 : Fallback ERP
                    getReelFromERPUseCase(reelCodebIS)
                        .onSuccess { feederQuantities ->
                            handleReelSuccess(feederQuantities)
                        }
                        .onFailure { erpException ->
                            withContext(Dispatchers.Main) {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    errorMessage = "Bobine introuvable (DB machines + ERP)"
                                )
                                mainService.loaderRunning = false
                                emitEvent(FeederInventoryEvent.PlayErrorSound)
                            }
                        }
                }
        }
    }

    /**
     * Fonction helper pour gérer le succès du scan d'une bobine
     * Affiche un toast temporaire au lieu d'ajouter à une liste visible
     */
    private suspend fun handleReelSuccess(feederQuantities: FeederQuantities) {
        // Vérifier les doublons
        val isDuplicate = _state.value.scannedReels.any { it.id == feederQuantities.id }

        withContext(Dispatchers.Main) {
            if (isDuplicate) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Cette bobine a déjà été scannée"
                )
                mainService.loaderRunning = false
                emitEvent(FeederInventoryEvent.PlayErrorSound)
            } else {
                val updatedReels = _state.value.scannedReels + feederQuantities
                
                // ✅ Ajouter à la liste cachée et afficher toast temporaire
                _state.value = _state.value.copy(
                    isLoading = false,
                    scannedReels = updatedReels,
                    actualReelCount = updatedReels.size,
                    lastScannedReel = feederQuantities,
                    showScanToast = true
                )
                mainService.loaderRunning = false
                emitEvent(FeederInventoryEvent.PlayBeepSound)
                
                // Auto-hide toast after 1.5 seconds
                toastJob?.cancel()
                toastJob = viewModelScope.launch {
                    delay(1500)
                    _state.value = _state.value.copy(showScanToast = false)
                }
            }
        }
    }

    fun requestReelCountValidation() {
        if (_state.value.scannedReels.isEmpty()) {
            _state.value = _state.value.copy(
                errorMessage = "Aucune bobine n'a été scannée"
            )
            emitEvent(FeederInventoryEvent.PlayErrorSound)
            return
        }

        _state.value = _state.value.copy(
            showReelCountDialog = true,
            expectedReelCount = "",
            reelCountError = null,
            workflowStep = FeederWorkflowStep.VALIDATING_COUNT,
            blindValidation = true
        )
    }

    fun updateExpectedReelCount(count: String) {
        _state.value = _state.value.copy(
            expectedReelCount = count.filter { it.isDigit() }
        )
    }

    fun validateReelCount() {
        val manualCount = _state.value.expectedReelCount.toIntOrNull()

        if (manualCount == null || manualCount <= 0) {
            _state.value = _state.value.copy(
                reelCountError = "Veuillez saisir un nombre valide"
            )
            emitEvent(FeederInventoryEvent.PlayErrorSound)
            return
        }

        val actualCount = _state.value.actualReelCount
        val currentAttempts = _state.value.validationAttempts

        if (manualCount == actualCount) {
            // ✅ SUCCÈS - Validation correcte
            _state.value = _state.value.copy(
                showReelCountDialog = false,
                reelCountError = null,
                validationAttempts = 0
            )
            emitEvent(FeederInventoryEvent.PlaySuccessSound)
            sendInventoryAndCloseFeeder()
        } else {
            // ❌ ERREUR - Nombre incorrect
            val newAttempts = currentAttempts + 1

            if (newAttempts >= 2) {
                // ⚠️ 2 ERREURS = AUTO-RESET
                _state.value = _state.value.copy(
                    showReelCountDialog = false,
                    isResetting = true,
                    resetCountdown = 3,
                    reelCountError = null
                )
                emitEvent(FeederInventoryEvent.PlayErrorSound)
                startResetCountdown()
            } else {
                // ⚠️ 1ère ERREUR - Donner une 2ème chance
                _state.value = _state.value.copy(
                    validationAttempts = newAttempts,
                    expectedReelCount = "",
                    reelCountError = "❌ Nombre incorrect ! Il reste 1 tentative avant réinitialisation."
                )
                emitEvent(FeederInventoryEvent.PlayErrorSound)
            }
        }
    }

    private fun startResetCountdown() {
        resetCountdownJob?.cancel()
        resetCountdownJob = viewModelScope.launch {
            repeat(3) { i ->
                val remainingSeconds = 3 - i
                _state.value = _state.value.copy(resetCountdown = remainingSeconds)
                delay(1000)
            }
            
            // Auto-reset après 3 secondes
            _state.value = _state.value.copy(
                isResetting = false,
                scannedReels = emptyList(),
                actualReelCount = 0,
                expectedReelCount = "",
                validationAttempts = 0,
                workflowStep = FeederWorkflowStep.SCANNING_REELS,
                errorMessage = ""
            )
            emitEvent(FeederInventoryEvent.ShowSuccess("Feeder réinitialisé - Rescannez les bobines"))
        }
    }

    fun cancelReelCountValidation() {
        _state.value = _state.value.copy(
            showReelCountDialog = false,
            expectedReelCount = "",
            reelCountError = null,
            workflowStep = FeederWorkflowStep.SCANNING_REELS
        )
    }

    private fun sendInventoryAndCloseFeeder() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isLoading = true)
                mainService.loaderRunning = true
            }

            try {
                val location = _state.value.location ?: throw Exception("Emplacement non défini")

                val sendResult = sendFeederInventoryUseCase(
                    inventoryNumber = location.inventoryNumber,
                    locationCode = _state.value.currentFeederNumber,
                    feederId = _state.value.currentFeederId,
                    reels = _state.value.scannedReels
                )

                sendResult.onSuccess {
                    val closeResult = closeFeederUseCase(
                        password = mainService.inventoryNumber,
                        feederId = _state.value.currentFeederNumber
                    )

                    closeResult.onSuccess {
                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                workflowStep = FeederWorkflowStep.FEEDER_COMPLETED
                            )
                            mainService.loaderRunning = false
                            emitEvent(FeederInventoryEvent.PlaySuccessSound)
                            emitEvent(FeederInventoryEvent.ShowSuccess("Feeder ${_state.value.currentFeederNumber} inventorié avec succès"))
                        }
                    }.onFailure(context) { error ->
                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                errorMessage = "Erreur lors de la fermeture du feeder"
                            )
                            mainService.loaderRunning = false
                            emitEvent(FeederInventoryEvent.PlayErrorSound)
                        }
                    }
                }.onFailure(context) { error ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = "Erreur lors de l'envoi de l'inventaire"
                        )
                        mainService.loaderRunning = false
                        emitEvent(FeederInventoryEvent.PlayErrorSound)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erreur inconnue"
                    )
                    mainService.loaderRunning = false
                    emitEvent(FeederInventoryEvent.PlayErrorSound)
                }
            }
        }
    }

    fun resetForNewFeeder() {
        resetCountdownJob?.cancel()
        toastJob?.cancel()
        
        _state.value = _state.value.copy(
            currentFeederId = "",
            currentFeederNumber = "",
            scannedReels = emptyList(),
            actualReelCount = 0,
            expectedReelCount = "",
            validationAttempts = 0,
            showReelCountDialog = false,
            reelCountError = null,
            isResetting = false,
            resetCountdown = 3,
            workflowStep = FeederWorkflowStep.SCAN_FEEDER,
            errorMessage = "",
            lastScannedReel = null,
            showScanToast = false
        )
    }

    fun startEditingReel(reel: FeederQuantities) {
        _state.value = _state.value.copy(
            editingReel = reel,
            editingQuantity = reel.currentQuantity.toString()
        )
    }

    fun updateEditingQuantity(quantity: String) {
        _state.value = _state.value.copy(editingQuantity = quantity)
    }

    fun confirmEditReel() {
        val editing = _state.value.editingReel ?: return
        val newQuantity = _state.value.editingQuantity.toIntOrNull() ?: return

        val updatedReels = _state.value.scannedReels.map { reel ->
            if (reel.id == editing.id) {
                reel.copy(currentQuantity = newQuantity)
            } else {
                reel
            }
        }

        _state.value = _state.value.copy(
            scannedReels = updatedReels,
            editingReel = null,
            editingQuantity = ""
        )
    }

    fun cancelEditReel() {
        _state.value = _state.value.copy(
            editingReel = null,
            editingQuantity = ""
        )
    }

    fun requestDeleteReel(reel: FeederQuantities) {
        _state.value = _state.value.copy(deletingReel = reel)
    }

    fun confirmDeleteReel() {
        val deleting = _state.value.deletingReel ?: return

        val updatedReels = _state.value.scannedReels.toMutableList()
        updatedReels.remove(deleting)

        _state.value = _state.value.copy(
            scannedReels = updatedReels,
            actualReelCount = updatedReels.size,
            deletingReel = null
        )
    }

    fun undoLastScannedReel() {
        val list = _state.value.scannedReels
        if (list.isEmpty()) return

        val updated = list.dropLast(1)
        _state.value = _state.value.copy(
            scannedReels = updated,
            actualReelCount = updated.size
        )
    }

    fun cancelDeleteReel() {
        _state.value = _state.value.copy(deletingReel = null)
    }

    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessage = "")
    }

    override fun onCleared() {
        super.onCleared()
        resetCountdownJob?.cancel()
        toastJob?.cancel()
    }
}
