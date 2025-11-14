package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veoneer.logisticinventoryapp.core.domain.model.Family
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.ReferenceInventoryItem

/**
 * Message d'instruction pour le scan
 */
@Composable
fun ScanInstructionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Scannez un code-barre produit ou entrez une famille (IL, VW, AA...)",
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Message d'état vide
 */
@Composable
fun EmptyStateMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucune référence scannée",
            style = MaterialTheme.typography.h6,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Commencez par scanner un code-barre",
            style = MaterialTheme.typography.body2,
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
