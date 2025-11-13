package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veoneer.logisticinventoryapp.core.data.api.TracabilityDAO
import com.veoneer.logisticinventoryapp.core.domain.model.InventoryState
import com.veoneer.logisticinventoryapp.core.domain.model.InventoryType
import com.veoneer.logisticinventoryapp.core.domain.model.MovexReferenceInventory
import com.veoneer.logisticinventoryapp.core.domain.model.RefInventoryStepEnum
import com.veoneer.logisticinventoryapp.core.domain.model.Reference
import com.veoneer.logisticinventoryapp.core.domain.repository.InventoryRepository
import com.veoneer.logisticinventoryapp.core.domain.repository.RefRepository
import com.veoneer.logisticinventoryapp.core.service.MainService
import com.veoneer.logisticinventoryapp.core.utils.toBase64
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.domain.use_case.NumberOfRefIsValidUseCase
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.referenceInventoryState
import com.veoneer.logisticinventoryapp.scanner.domain.ScannerRepository
import com.veoneer.logisticinventoryapp.scanner.domain.model.BarcodeScanResult
import com.veoneer.logisticinventoryapp.scanner.domain.model.TriggerFeedbackEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferenceInventoryTypeViewModel @Inject constructor(
    private val tracabilityApiService: TracabilityDAO,
    private val scannerRepository: ScannerRepository,
    private val inventoryRepository: InventoryRepository,
    private val refRepository: RefRepository,
    private val numberOfRefIsValid: NumberOfRefIsValidUseCase,
    private val mainService: MainService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /*
    init {
        val json: String = checkNotNull(savedStateHandle["inventory"])
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(InventoryType.ReferenceInventory::class.java).lenient()
        val inventory: InventoryType.ReferenceInventory = jsonAdapter.fromJson(json)!!

        initValues(inventory)
    }
     */
    init {
        val inventory  = InventoryType.ReferenceInventory(
            inventoryNumber = mainService.inventoryNumber,
            locationCode = mainService.inventoryLocation,
            reference = "",
            type = "",
            quantity = "",
            designation = "",
            famille = ""
        )
        initValues(inventory)
    }
    private lateinit var inventory: InventoryType.ReferenceInventory

    var referenceInventoryState: referenceInventoryState by mutableStateOf(referenceInventoryState())
        private set
    var inventoryState by mutableStateOf(
        InventoryState().copy(
            inventory = inventory,
            actionMessage = "Scanner un produit"
        )
    )
    var error = mutableStateOf("")
        private set

//    var barcodeScanState: BarcodeScanState by mutableStateOf(barcodeScannerHandler.barcodeScanState)
//        private set

    val scannerStateFlow = scannerRepository.scannerStateFlow

    var barcodeScanState = scannerRepository.barcodeScanState

    private fun initValues(referenceInventory: InventoryType.ReferenceInventory) {
        this.inventory = referenceInventory
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun handleBarcodeScan(barcodeScanResult: BarcodeScanResult) {
        viewModelScope.launch {
//            barcodeScanState = barcodeScannerHandler.handleScanResult(barcodeScanResult)
            when (mainService.refInventoryStepState) {
                RefInventoryStepEnum.Start -> {
                    error.value = ""
                    if (barcodeScanResult.barcodeContent.substring(0, 3) == "30S") {
                        getFamily(barcodeScanResult.barcodeContent)
                    } else {
                        getReference(barcodeScanResult.barcodeContent)
                    }
                }

                RefInventoryStepEnum.GetFamily -> {
                    error.value = ""
                }

                RefInventoryStepEnum.GetType -> {
                    error.value = ""
                }

                RefInventoryStepEnum.GetQuantity -> {
                    error.value = ""
                }

                RefInventoryStepEnum.Send -> {
                    error.value = ""
                }

                else -> {}
            }

        }
    }

    fun onInputChange(newValue: String, someEvent: (String) -> Unit) {
        try {
            if (
                newValue.isNotEmpty()
            ) {
                someEvent(newValue)
            } else {
                throw Exception("Le champ ne peut être vide")
            }
        } catch (e: Exception) {
            inventoryState = inventoryState.copy(errorMessage = e.message)
        }
    }

    fun getFamily(data: String) {
        viewModelScope.launch {
            inventoryState = try {
                inventoryState = inventoryState.copy(isLoading = true)
                val response = refRepository.getFamily(data.toBase64())

                if (!response.isSuccessful) {
                    throw Exception(response.errorBody()!!.string())
                }
                referenceInventoryState.familyList.clear()
                if(response.body()!!.isEmpty()) {
                    throw Exception("Aucune information trouvée")
                }
                response.body()!!.forEach() {
                    if (it.typeOfProduct == "SO") {
                        referenceInventoryState.familyList.add(it)
                    } else if (it.typeOfProduct == "SF") {
                        referenceInventoryState.familyList.add(it)
                    }
                }
                inventory.famille = data
                mainService.refInventoryStepState = RefInventoryStepEnum.GetType
                error.value = ""
                inventoryState.copy(
                    actionMessage = "Famille en cours $data",
                    inventory = inventory,
                    isLoading = false,
                    isSnackbarShowing = true,
                    snackbarMessage = "Produit $data ajoutée !",
                    errorMessage = null,
                    isError = false,
                )
            } catch (e: Exception) {
                error.value = e.message!!
                inventoryState.copy(
                    isSnackbarShowing = true,
                    snackbarMessage = e.message!!,
                    isLoading = false,
                    isError = true,
                )
            }
        }
    }

    fun getType(data: String) {
        try {
            if (
                data.isNotEmpty() && data != "Choisir le type de produit"
            ) {
                referenceInventoryState.familyList.forEach() {
                    if (it.designation == data) {
                        inventory =
                            inventory.copy(reference = it.reference, designation = it.designation)
                    }
                }
                mainService.refInventoryStepState = RefInventoryStepEnum.GetQuantity
                inventoryState = inventoryState.copy(
                    inventory = inventory,
                    actionMessage = "Ref°${inventory.reference}"
                )
            } else {
                throw Exception("Le champs ne peut être vide")
            }
        } catch (e: Exception) {
            inventoryState = inventoryState.copy(errorMessage = e.message)
        }
    }

    fun getReference(data: String) {
        viewModelScope.launch {
            inventoryState = try {
                inventoryState = inventoryState.copy(isLoading = true)
                val response = tracabilityApiService.getReferenceAndProductType(data)
                if (!response.isSuccessful) {
                    throw Exception(response.errorBody()!!.string())
                }

                mainService.refInventoryStepState = RefInventoryStepEnum.GetQuantityByScan
                inventory = inventory.copy(
                    reference = response.body()!!.Response,
                    type = response.body()!!.Type
                )
                error.value = ""
                inventoryState.copy(
                    isLoading = false,
                    isSnackbarShowing = true,
                    snackbarMessage = "Produit $data ajoutée !",
                    errorMessage = null,
                    actionMessage = "Ref°${response.body()!!.Response}",
                    isError = false,
                )
            } catch (e: Exception) {
                scannerRepository.triggerFeedback(TriggerFeedbackEnum.ERROR)
                error.value = e.message!!
                inventoryState.copy(
                    isSnackbarShowing = true,
                    snackbarMessage = e.message!!,
                    isError = true,
                    isLoading = false
                )
            }
        }
    }

    fun getQuantityRef() {
        inventoryState.copy(isLoading = true)
        viewModelScope.launch {
            inventoryState = try {
                if (referenceInventoryState.userInput.isEmpty()) {
                    throw Exception("Les champs ne peuvent pas être vide")
                }
                inventory = inventory.copy(quantity = referenceInventoryState.userInput)

                referenceInventoryState = referenceInventoryState.copy(userInput = "")
                referenceInventoryState.refList.add(
                    Reference(
                        code = inventory.reference,
                        quantity = inventory.quantity.toInt(),
                        type = if (inventory.designation == "") inventory.type else inventory.designation.substring(
                            0,
                            3
                        )
                    )
                )
                inventory = inventory.copy(designation = "")
                mainService.refInventoryStepState = RefInventoryStepEnum.Start
                error.value = ""
                inventoryState.copy(
                    inventory = inventory,
                    errorMessage = null,
                    isLoading = false,
                    isError = false,
                    actionMessage = "Scanner un produit"
                )
            } catch (e: Exception) {
                error.value = e.message!!
                inventoryState.copy(
                    isSnackbarShowing = true,
                    snackbarMessage = e.message!!,
                    isLoading = false,
                    isError = true,
                )
            }
        }
    }

    fun sendInventory() {
        viewModelScope.launch {
            inventoryState = inventoryState.copy(isLoading = true)
            inventoryState = try {
                referenceInventoryState.refList.forEach {
                    val response = inventoryRepository.createReferenceInventory(
                        MovexReferenceInventory(
                            inventoryNumber = inventory.inventoryNumber,
                            locationCode = inventory.locationCode,
                            reference = it.code,
                            numberOfUnits = "1",
                            quantityByUnit = it.quantity.toString()
                        )
                    )
                    if (!response.isSuccessful) {
                        inventoryState.copy(
                            isSnackbarShowing = true,
                            snackbarMessage = response.errorBody()!!.string(),
                            isError = true,
                        )
                    }
                }
                referenceInventoryState.refList.clear()
                resetState()
                mainService.refInventoryStepState = RefInventoryStepEnum.Start
                error.value = ""
                inventoryState.copy(
                    isLoading = false,
                    isSnackbarShowing = true,
                    snackbarMessage = "Référence inventoriée !",
                    isError = false,
                    actionMessage = "Scanner un nouveau produit.",
                )
            } catch (e: Exception) {
                scannerRepository.triggerFeedback(TriggerFeedbackEnum.ERROR)
                error.value = e.message!!
                inventoryState.copy(
                    errorMessage = e.localizedMessage,
                    isSnackbarShowing = true,
                    snackbarMessage = e.message!!,
                    isError = true,
                )
            }
        }
    }

    fun onQuantityInputChange(newValue: String) {
        try {
            referenceInventoryState = referenceInventoryState.copy(userInput = newValue)
        } catch (e: Exception) {
            inventoryState = inventoryState.copy(errorMessage = e.message)
        }
    }

    fun dismissSnackbar() {
        inventoryState = inventoryState.copy(isSnackbarShowing = false)
    }

    fun resetState() {
        inventoryState = inventoryState.copy(
            alertDialogIsOpen = false,
            errorMessage = null,
        )
        referenceInventoryState.familyList.clear()
        referenceInventoryState = referenceInventoryState.copy(userInput = "")
        scannerRepository.resetBarcodeScanResult()
    }

    fun onRefInputChange(newValue: String) {
        referenceInventoryState = referenceInventoryState.copy(userInput = newValue)
    }

    fun validStep() {
        if (mainService.refInventoryStepState == RefInventoryStepEnum.Start) {
            inventoryState = inventoryState.copy(alertDialogIsOpen = true, errorMessage = "", isError = false)
        }
    }
}