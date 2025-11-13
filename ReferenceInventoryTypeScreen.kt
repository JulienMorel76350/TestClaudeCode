package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veoneer.logisticinventoryapp.core.presentation.components.SystemBroadcastReceiverHandler
import com.veoneer.logisticinventoryapp.core.router.model.Screen
import com.veoneer.logisticinventoryapp.core.service.MainViewModel
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.components.*
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.InventoryMode
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryEvent
import com.veoneer.logisticinventoryapp.scanner.data.zebra.ZebraDatawedgeActions

/**
 * Écran principal d'inventaire de références
 * Architecture moderne avec un seul écran et modes différents
 */
@Composable
fun ReferenceInventoryTypeScreen(
    referenceInventoryTypeViewModel: ReferenceInventoryTypeViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val mainService = mainViewModel.getMainService()
    val context = LocalContext.current

    // États
    val state by referenceInventoryTypeViewModel.state.collectAsState()
    val scannerStateFlow by referenceInventoryTypeViewModel.scannerStateFlow.observeAsState()

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Gestion du scanner Zebra
    SystemBroadcastReceiverHandler(
        state = scannerStateFlow,
        handleBarcodeScan = referenceInventoryTypeViewModel::handleBarcodeScan
    )

    // Navigation après succès
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            mainViewModel.playSucces()
            mainService.navigateTo(Screen.HomeScreen)
        }
    }

    // Affichage des messages
    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            referenceInventoryTypeViewModel.onEvent(ReferenceInventoryEvent.DismissError)
        }
        state.successMessage?.let { success ->
            snackbarHostState.showSnackbar(
                message = success,
                duration = SnackbarDuration.Short
            )
            referenceInventoryTypeViewModel.onEvent(ReferenceInventoryEvent.DismissError)
        }
    }

    // Activer le scanner en mode Scanning
    LaunchedEffect(state.mode) {
        if (state.mode is InventoryMode.Scanning) {
            ZebraDatawedgeActions.enableScan(context)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                inventoryNumber = state.inventoryNumber,
                locationCode = state.locationCode
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    backgroundColor = if (state.error != null) Color.Red else Color.Black,
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val mode = state.mode) {
                is InventoryMode.Scanning -> {
                    ScanningModeContent(
                        state = state,
                        onEvent = referenceInventoryTypeViewModel::onEvent,
                        context = context
                    )
                }
                is InventoryMode.SelectingType -> {
                    TypeSelectionModal(
                        familyCode = mode.familyCode,
                        types = mode.types,
                        onTypeSelected = { type ->
                            referenceInventoryTypeViewModel.onEvent(
                                ReferenceInventoryEvent.TypeSelected(type)
                            )
                        },
                        onDismiss = {
                            referenceInventoryTypeViewModel.onEvent(
                                ReferenceInventoryEvent.ReturnToScanning
                            )
                        }
                    )
                }
                is InventoryMode.EditingQuantity -> {
                    QuantityInputModal(
                        referenceCode = mode.referenceCode,
                        referenceType = mode.referenceType,
                        onQuantityConfirmed = { quantity ->
                            referenceInventoryTypeViewModel.onEvent(
                                ReferenceInventoryEvent.QuantityEntered(quantity)
                            )
                        },
                        onDismiss = {
                            referenceInventoryTypeViewModel.onEvent(
                                ReferenceInventoryEvent.ReturnToScanning
                            )
                        }
                    )
                }
            }

            // Loader
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * TopBar avec informations d'inventaire
 */
@Composable
private fun TopBar(
    inventoryNumber: String,
    locationCode: String
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = Color.White,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Inventaire Références",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Location: $locationCode | Inv: $inventoryNumber",
                style = MaterialTheme.typography.caption
            )
        }
    }
}

/**
 * Contenu en mode Scanning
 */
@Composable
private fun ScanningModeContent(
    state: com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryState,
    onEvent: (ReferenceInventoryEvent) -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Instruction
        ScanInstructionCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Liste ou message vide
        if (state.scannedReferences.isEmpty()) {
            EmptyStateMessage()
        } else {
            ScannedReferencesList(
                references = state.scannedReferences,
                onRemove = { index ->
                    onEvent(ReferenceInventoryEvent.ReferenceRemoved(index))
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton envoyer
            SendInventoryButton(
                count = state.scannedReferences.size,
                enabled = !state.isLoading,
                onClick = {
                    onEvent(ReferenceInventoryEvent.SendInventory)
                }
            )
        }
    }
}
