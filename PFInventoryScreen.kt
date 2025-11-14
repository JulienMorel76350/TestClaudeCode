package com.veoneer.logisticinventoryapp.core.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veoneer.logisticinventoryapp.core.presentation.event.PFInventoryEvent
import com.veoneer.logisticinventoryapp.core.presentation.state.PFCarton
import com.veoneer.logisticinventoryapp.core.presentation.state.PFInventoryState
import com.veoneer.logisticinventoryapp.core.service.MainViewModel
import com.veoneer.logisticinventoryapp.core.service.pf.PFInventoryViewModel
import com.veoneer.logisticinventoryapp.core.utils.SoundPlayer
import kotlinx.coroutines.delay

@Composable
fun PFInventoryScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    pfInventoryViewModel: PFInventoryViewModel = hiltViewModel()
) {
    val mainService = mainViewModel.getMainService()
    val context = LocalContext.current
    val state by pfInventoryViewModel.state.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    mainService.ScannerHandler(
        onSuccess = { scan, label ->
            pfInventoryViewModel.processScan(scan, label)
        }
    )

    LaunchedEffect(Unit) {
        pfInventoryViewModel.events.collect { event ->
            when (event) {
                is PFInventoryEvent.PlaySuccessSound -> {
                    SoundPlayer.playSuccessSound(context)
                }
                is PFInventoryEvent.PlayErrorSound -> {
                    SoundPlayer.playErrorSound(context)
                }
                is PFInventoryEvent.HideKeyboard -> {
                    keyboardController?.hide()
                }
                is PFInventoryEvent.NavigateToDetails -> {
                    pfInventoryViewModel.navigateToDetails(event.reference)
                }
                is PFInventoryEvent.NavigateBackToMain -> {
                    pfInventoryViewModel.navigateBackToMain()
                }
                is PFInventoryEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    if (state.errorMessage.isNotEmpty()) {
        LaunchedEffect(state.errorMessage) {
            delay(3000)
            pfInventoryViewModel.clearErrorMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                if (state.errorMessage.isNotEmpty()) {
                    ErrorBanner(message = state.errorMessage)
                }

                if (state.currentDetailReference != null) {
                    PFDetailPage(
                        reference = state.currentDetailReference!!,
                        viewModel = pfInventoryViewModel,
                        state = state,
                        onBack = { pfInventoryViewModel.navigateBackToMain() }
                    )
                } else {
                    PFMainContent(
                        viewModel = pfInventoryViewModel,
                        state = state
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }

    if (state.editingCarton != null) {
        EditCartonDialog(
            carton = state.editingCarton!!,
            currentQuantity = state.editingQuantity,
            onQuantityChange = { pfInventoryViewModel.updateEditingQuantity(it) },
            onConfirm = { pfInventoryViewModel.confirmEditCarton() },
            onDismiss = { pfInventoryViewModel.cancelEditCarton() }
        )
    }

    if (state.deletingCarton != null) {
        DeleteCartonDialog(
            carton = state.deletingCarton!!,
            onConfirm = { pfInventoryViewModel.confirmDeleteCarton() },
            onDismiss = { pfInventoryViewModel.cancelDeleteCarton() }
        )
    }

    if (state.showValidationDialog) {
        ValidationDialog(
            referencesCount = state.references.size,
            totalCartons = state.references.sumOf { it.cartonCount },
            onConfirm = {
                pfInventoryViewModel.validateAndSend()
                pfInventoryViewModel.hideValidationDialog()
            },
            onDismiss = { pfInventoryViewModel.hideValidationDialog() }
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFEF4444)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PFMainContent(
    viewModel: PFInventoryViewModel,
    state: PFInventoryState
) {
    val location = state.location

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header info compact
        if (location != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Produits Finis",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = location.code,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF111827)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatBadge(
                            value = state.references.size.toString(),
                            label = "réf."
                        )
                        StatBadge(
                            value = state.references.sumOf { it.cartonCount }.toString(),
                            label = "cartons"
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (location?.isMultireference == true && state.references.isEmpty()) {
                item {
                    InfoCard(
                        text = "Scannez ou saisissez une référence pour charger ses cartons"
                    )
                }
            }

            items(state.references) { reference ->
                ReferenceCard(
                    reference = reference,
                    onClick = { viewModel.navigateToDetails(reference.reference) }
                )
            }

            if (state.references.isEmpty() && location?.isMultireference == false) {
                item {
                    EmptyState()
                }
            }
        }

        if (state.references.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { viewModel.showValidationDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Valider l'inventaire",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF93C5FD))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color(0xFF1E40AF)
            )
        }
    }
}

@Composable
private fun ReferenceCard(
    reference: com.veoneer.logisticinventoryapp.core.presentation.state.PFReference,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reference.reference,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${reference.cartonCount} cartons",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${reference.totalQuantity} pcs",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                if (reference.nonStandardCount > 0 && reference.standardQuantity != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⚠ ${reference.nonStandardCount} carton(s) différent(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Voir détails",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "Aucun carton chargé",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PFDetailPage(
    reference: String,
    viewModel: PFInventoryViewModel,
    state: PFInventoryState,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header détails
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color(0xFF111827)
                        )
                    }

                    Column {
                        Text(
                            text = "Référence",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = reference,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF111827)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBadge(
                        value = state.cartons.size.toString(),
                        label = "cartons"
                    )
                    StatBadge(
                        value = state.cartons.sumOf { it.quantity }.toString(),
                        label = "pcs"
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ManualEntryCard(
                    serialInput = state.manualSerialInput,
                    qtyInput = state.manualQtyInput,
                    onSerialChange = { viewModel.updateManualSerialInput(it) },
                    onQtyChange = { viewModel.updateManualQtyInput(it) },
                    onAdd = { viewModel.addManualCarton() },
                    keyboardController = keyboardController,
                    focusManager = focusManager
                )
            }

            items(state.cartons) { carton ->
                CartonCard(
                    carton = carton,
                    onEdit = { viewModel.startEditCarton(carton) },
                    onDelete = { viewModel.startDeleteCarton(carton) }
                )
            }
        }
    }
}

@Composable
private fun ManualEntryCard(
    serialInput: String,
    qtyInput: String,
    onSerialChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onAdd: () -> Unit,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ajouter un carton",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = serialInput,
                    onValueChange = onSerialChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text("N° Série", fontSize = 13.sp) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFD1D5DB)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = onQtyChange,
                    modifier = Modifier
                        .weight(0.5f)
                        .height(56.dp),
                    placeholder = { Text("Qté", fontSize = 13.sp) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onAdd()
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFD1D5DB)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ajouter",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CartonCard(
    carton: PFCarton,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = carton.reelGaliaNumber,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )

                    if (carton.isManual) {
                        Surface(
                            color = Color(0xFFDEEFFF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Manuel",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${carton.quantity} pièces",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditCartonDialog(
    carton: PFCarton,
    currentQuantity: String,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modifier la quantité",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Carton : ${carton.reelGaliaNumber}",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = currentQuantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Nouvelle quantité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun DeleteCartonDialog(
    carton: PFCarton,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFFEF4444)
            )
        },
        title = {
            Text(
                text = "Supprimer ce carton ?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Voulez-vous vraiment supprimer le carton ${carton.reelGaliaNumber} ?",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFEF4444)
                )
            ) {
                Text("Supprimer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun ValidationDialog(
    referencesCount: Int,
    totalCartons: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF3B82F6)
            )
        },
        title = {
            Text(
                text = "Valider l'inventaire ?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Vous allez envoyer :",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "• $referencesCount référence(s)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "• $totalCartons carton(s) au total",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
