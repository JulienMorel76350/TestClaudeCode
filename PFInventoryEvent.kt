package com.veoneer.logisticinventoryapp.core.presentation.event

sealed class PFInventoryEvent {
    object PlaySuccessSound : PFInventoryEvent()
    object PlayErrorSound : PFInventoryEvent()
    object HideKeyboard : PFInventoryEvent()
    data class NavigateToDetails(val reference: String) : PFInventoryEvent()
    object NavigateBackToMain : PFInventoryEvent()
    data class ShowToast(val message: String) : PFInventoryEvent()
}
