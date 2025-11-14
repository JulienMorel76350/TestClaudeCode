package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veoneer.logisticinventoryapp.core.domain.model.Family
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.GroupedReference
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryItem

/**
 * Message d'instruction pour le scan - Version compacte
 */
@Composable
fun ScanInstructionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Scanner un produit ou entrer une famille",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Input compact personnalisé pour la saisie manuelle
 */
@Composable
fun CompactFamilyInput(
    onSubmit: (String) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }

    OutlinedTextField(
        value = inputValue,
        onValueChange = { inputValue = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = {
            Text(
                text = "Famille (IL, VW, AA...)",
                style = MaterialTheme.typography.body2
            )
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    if (inputValue.isNotEmpty()) {
                        onSubmit(inputValue)
                        inputValue = ""
                    }
                },
                enabled = inputValue.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Valider",
                    tint = if (inputValue.isNotEmpty())
                        MaterialTheme.colors.primary
                    else
                        Color.Gray
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = Color(0xFFE5E7EB)
        ),
        textStyle = MaterialTheme.typography.body2
    )
}

/**
 * Message d'état vide
 */
@Composable
fun EmptyStateMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aucune référence",
            style = MaterialTheme.typography.subtitle1,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scanner ou entrer une famille",
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Liste des références scannées
 */
@Composable
fun ScannedReferencesList(
    references: List<ReferenceInventoryItem>,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Références scannées (${references.size})",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(references) { index, item ->
                ReferenceListItem(
                    item = item,
                    onRemove = { onRemove(index) }
                )
            }
        }
    }
}

/**
 * Item de référence dans la liste
 */
@Composable
fun ReferenceListItem(
    item: ReferenceInventoryItem,
    onRemove: () -> Unit
) {
    val openDialog = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF5551FF))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge quantité
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.h6,
                        color = Color(0xFF5551FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Infos référence
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.code,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Bouton supprimer
            IconButton(onClick = { openDialog.value = true }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Supprimer",
                    tint = Color.White
                )
            }
        }
    }

    // Dialog de confirmation
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = { Text("Confirmer la suppression") },
            text = { Text("Voulez-vous vraiment supprimer cette référence ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        onRemove()
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog.value = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * Bouton d'envoi
 */
@Composable
fun SendInventoryButton(
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val openDialog = remember { mutableStateOf(false) }

    Button(
        onClick = { openDialog.value = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && count > 0,
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Send, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Envoyer ($count)",
            style = MaterialTheme.typography.button
        )
    }

    // Dialog de confirmation
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = { Text("Confirmer l'envoi") },
            text = { Text("Voulez-vous envoyer l'inventaire de $count référence(s) ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        onClick()
                    }
                ) {
                    Text("Envoyer")
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog.value = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * Modal de sélection de type
 */
@Composable
fun TypeSelectionModal(
    familyCode: String,
    types: List<Family>,
    onTypeSelected: (Family) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Sélectionner un type")
                Text(
                    text = "Famille: $familyCode",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        },
        text = {
            LazyColumn {
                itemsIndexed(types) { _, type ->
                    TextButton(
                        onClick = { onTypeSelected(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = type.designation,
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Réf: ${type.reference} | Type: ${type.typeOfProduct}",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray
                            )
                        }
                    }
                    Divider()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Modal de saisie de quantité
 */
@Composable
fun QuantityInputModal(
    referenceCode: String,
    referenceType: String,
    onQuantityConfirmed: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Entrer la quantité")
                Text(
                    text = "Réf: $referenceCode",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
                Text(
                    text = "Type: $referenceType",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        error = null
                    },
                    label = { Text("Quantité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val quantity = quantityText.toIntOrNull()
                    when {
                        quantity == null -> error = "Quantité invalide"
                        quantity <= 0 -> error = "La quantité doit être > 0"
                        else -> {
                            onQuantityConfirmed(quantity)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Fonction helper pour grouper les références par code
 */
fun groupReferencesByCode(references: List<ReferenceInventoryItem>): List<GroupedReference> {
    return references
        .groupBy { it.code }
        .map { (code, items) ->
            GroupedReference(
                reference = code,
                type = items.first().type,
                itemCount = items.size,
                totalQuantity = items.sumOf { it.quantity },
                items = items
            )
        }
        .sortedByDescending { it.totalQuantity }
}

/**
 * Liste des références scannées avec groupement
 */
@Composable
fun ScannedReferencesListGrouped(
    references: List<ReferenceInventoryItem>,
    onRemoveItem: (String) -> Unit, // Supprime par ID
    modifier: Modifier = Modifier
) {
    val groupedReferences = remember(references) {
        groupReferencesByCode(references)
    }

    var selectedGroup by remember { mutableStateOf<GroupedReference?>(null) }

    Column(modifier = modifier) {
        Text(
            text = "Références (${groupedReferences.size} • ${references.size} entrées)",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(groupedReferences) { _, group ->
                GroupedReferenceCard(
                    group = group,
                    onClick = { selectedGroup = group }
                )
            }
        }
    }

    // Modal de détails
    selectedGroup?.let { group ->
        ReferenceDetailsModal(
            group = group,
            onDismiss = { selectedGroup = null },
            onRemoveItem = onRemoveItem
        )
    }
}

/**
 * Carte pour une référence groupée - Version compacte sur une ligne
 */
@Composable
fun GroupedReferenceCard(
    group: GroupedReference,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        color = Color.White,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Référence et type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.reference,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = group.type,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }

            // Nombre d'entrées
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF667EEA).copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = group.itemCount.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF667EEA)
                    )
                    Text(
                        text = "×",
                        fontSize = 11.sp,
                        color = Color(0xFF667EEA)
                    )
                }
            }

            // Quantité totale
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF10B981).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = group.totalQuantity.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "pcs",
                        fontSize = 10.sp,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier
                    .size(18.dp)
                    .padding(start = 4.dp)
            )
        }
    }
}

/**
 * Modal de détails pour une référence groupée
 */
@Composable
fun ReferenceDetailsModal(
    group: GroupedReference,
    onDismiss: () -> Unit,
    onRemoveItem: (String) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = group.reference,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${group.itemCount} entrée(s) • ${group.totalQuantity} pièces total",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(group.items) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Entrée #${index + 1}",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${item.quantity} pièces",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF667EEA)
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = item.type,
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { itemToDelete = item.id }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Supprimer",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
        dismissButton = null
    )

    // Dialogue de confirmation de suppression
    itemToDelete?.let { itemId ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Voulez-vous vraiment supprimer cette entrée ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveItem(itemId)
                        itemToDelete = null
                        // Si c'était la dernière entrée du groupe, fermer le modal
                        if (group.items.size == 1) {
                            onDismiss()
                        }
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
