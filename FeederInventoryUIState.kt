package com.veoneer.logisticinventoryapp.core.presentation.state

import com.veoneer.logisticinventoryapp.core.domain.model.FeederQuantities
import com.veoneer.logisticinventoryapp.core.domain.model.Location

data class FeederInventoryUIState(
    // Location scannée
    val location: Location? = null,
    
    // Feeder en cours d'inventaire
    val currentFeederNumber: String = "",
    val currentFeederId: String = "", // emplacement + feederNumber
    
    // Bobines scannées dans le feeder actuel (HIDDEN from UI during scanning)
    val scannedReels: List<FeederQuantities> = emptyList(),
    
    // Toast feedback (dernier scan) - visible brièvement après chaque scan
    val lastScannedReel: FeederQuantities? = null,
    val showScanToast: Boolean = false,
    
    // Workflow state
    val workflowStep: FeederWorkflowStep = FeederWorkflowStep.SCAN_FEEDER,
    
    // Validation du nombre de bobines (BLIND VALIDATION)
    val expectedReelCount: String = "",
    val actualReelCount: Int = 0,
    val validationAttempts: Int = 0,
    val showReelCountDialog: Boolean = false,
    val reelCountError: String? = null,
    
    // Auto-reset après 2 erreurs
    val isResetting: Boolean = false,
    val resetCountdown: Int = 3,
    
    // UI state
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val blindValidation: Boolean = true, // Toujours true dans ce workflow
    
    // Editing (pour les corrections superviseur sur web uniquement)
    val editingReel: FeederQuantities? = null,
    val editingQuantity: String = "",
    val deletingReel: FeederQuantities? = null
)

enum class FeederWorkflowStep {
    SCAN_FEEDER,        // Scanner le numéro du feeder
    SCANNING_REELS,     // Scanner les bobines (liste cachée)
    VALIDATING_COUNT,   // Valider le nombre de bobines (blind)
    FEEDER_COMPLETED    // Feeder terminé
}
