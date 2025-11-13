package com.veoneer.logisticinventoryapp.core.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veoneer.logisticinventoryapp.core.domain.model.FeederQuantities
import com.veoneer.logisticinventoryapp.core.presentation.event.FeederInventoryEvent
import com.veoneer.logisticinventoryapp.core.presentation.state.FeederWorkflowStep
import com.veoneer.logisticinventoryapp.core.service.MainViewModel
import com.veoneer.logisticinventoryapp.core.service.feeder.FeederInventoryViewModel
import com.veoneer.logisticinventoryapp.core.utils.SoundPlayer
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeederInventoryScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    viewModel: FeederInventoryViewModel = hiltViewModel()
) {
    val mainService = mainViewModel.getMainService()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is FeederInventoryEvent.PlaySuccessSound -> {
                    SoundPlayer.playSuccessSound(context)
                }
                is FeederInventoryEvent.PlayErrorSound -> {
                    SoundPlayer.playErrorSound(context)
                }
                is FeederInventoryEvent.PlayBeepSound -> {
                    SoundPlayer.playBeepSound(context)
                }
                else -> {}
            }
        }
    }

    // Scanner handler
    mainService.ScannerHandler(
        onSuccess = { scan, label ->
            viewModel.processScan(scan, label)
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Indicateur de workflow
            WorkflowStepIndicator(
                currentStep = state.workflowStep,
                feederNumber = state.currentFeederNumber
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Message d'erreur global
            if (state.errorMessage.isNotEmpty()) {
                ErrorBanner(
                    message = state.errorMessage,
                    onDismiss = { viewModel.clearErrorMessage() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Contenu principal selon l'étape
            when {
                state.isResetting -> {
                    // État 5: Auto-reset en cours
                    ResetCountdownScreen(countdown = state.resetCountdown)
                }
                state.workflowStep == FeederWorkflowStep.SCAN_FEEDER -> {
                    ScanFeederPrompt()
                }
                state.workflowStep == FeederWorkflowStep.SCANNING_REELS -> {
                    // État 2: Scan aveugle - PAS DE LISTE ni COMPTEUR
                    BlindScanningWorkspace(
                        feederNumber = state.currentFeederNumber
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { viewModel.requestReelCountValidation() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.actualReelCount > 0
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("J'ai scanné toutes les bobines")
                    }
                }
                state.workflowStep == FeederWorkflowStep.VALIDATING_COUNT -> {
                    // Affichage pendant validation
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.workflowStep == FeederWorkflowStep.FEEDER_COMPLETED -> {
                    CompletedFeederCard(
                        feederNumber = state.currentFeederNumber,
                        reelCount = state.actualReelCount,
                        onNewFeeder = { viewModel.resetForNewFeeder() }
                    )
                }
            }
        }

        // Toast overlay pour le feedback de scan (visible brièvement)
        AnimatedVisibility(
            visible = state.showScanToast && state.lastScannedReel != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            ScanToast(reel = state.lastScannedReel!!)
        }
    }

    // Dialog de validation (SANS indication du nombre)
    if (state.showReelCountDialog) {
        BlindReelCountValidationDialog(
            manualCount = state.expectedReelCount,
            errorMessage = state.reelCountError,
            validationAttempts = state.validationAttempts,
            onCountChanged = viewModel::updateExpectedReelCount,
            onConfirm = viewModel::validateReelCount,
            onDismiss = viewModel::cancelReelCountValidation
        )
    }
}

@Composable
fun WorkflowStepIndicator(
    currentStep: FeederWorkflowStep,
    feederNumber: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (currentStep) {
                FeederWorkflowStep.SCAN_FEEDER -> {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "📱 Scannez le feeder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                FeederWorkflowStep.SCANNING_REELS -> {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Feeder: $feederNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scannez toutes les bobines",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                FeederWorkflowStep.VALIDATING_COUNT -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Validation en cours...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                FeederWorkflowStep.FEEDER_COMPLETED -> {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Feeder terminé !",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScanFeederPrompt() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "En attente de scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pointez le scanner vers le code du feeder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * État 2: Zone de scan aveugle - PAS DE LISTE ni COMPTEUR
 * L'utilisateur doit compter mentalement les bobines qu'il scanne
 */
@Composable
fun BlindScanningWorkspace(feederNumber: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Scannez toutes les bobines du feeder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Un son confirme chaque scan ✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Toast de feedback après chaque scan (visible 1.5 secondes)
 */
@Composable
fun ScanToast(reel: FeederQuantities) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Bobine scannée ✓",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${reel.materialID} - Qté: ${reel.currentQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * État 3: Dialog de validation SANS indication du nombre attendu
 * L'utilisateur doit saisir le nombre qu'il a compté mentalement
 */
@Composable
fun BlindReelCountValidationDialog(
    manualCount: String,
    errorMessage: String?,
    validationAttempts: Int,
    onCountChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (validationAttempts == 0) "Validation du nombre de bobines"
                else "⚠️ Réessayez"
            ) 
        },
        text = {
            Column {
                if (validationAttempts > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "❌ Nombre incorrect !",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = "Combien de bobines avez-vous scanné ?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = manualCount,
                    onValueChange = { input ->
                        onCountChanged(input.filter { it.isDigit() })
                    },
                    label = { Text("Nombre de bobines") },
                    placeholder = { Text("Entrez le nombre") },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Entrez le nombre exact de bobines scannées")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { 
            Button(onClick = onConfirm, enabled = manualCount.isNotEmpty()) { 
                Text("Confirmer") 
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/**
 * État 5: Écran de reset automatique avec countdown
 */
@Composable
fun ResetCountdownScreen(countdown: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "❌ Feeder réinitialisé",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Le nombre de bobines ne correspond toujours pas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Les bobines de ce feeder ont été effacées.\nRescannez toutes les bobines à nouveau.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Retour au scan dans ${countdown}s...",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CompletedFeederCard(
    feederNumber: String,
    reelCount: Int,
    onNewFeeder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "✅ Feeder validé !",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Feeder $feederNumber",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "$reelCount bobines inventoriées",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNewFeeder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Passer au feeder suivant")
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
