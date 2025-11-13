package com.veoneer.logisticinventoryapp.core.presentation.event

sealed class FeederInventoryEvent {
    object PlaySuccessSound : FeederInventoryEvent()
    object PlayErrorSound : FeederInventoryEvent()
    object PlayBeepSound : FeederInventoryEvent()

    data class ShowError(val message: String) : FeederInventoryEvent()
    data class ShowSuccess(val message: String) : FeederInventoryEvent()
    object NavigateBack : FeederInventoryEvent()
    object ShowReelCountDialog : FeederInventoryEvent()
}
