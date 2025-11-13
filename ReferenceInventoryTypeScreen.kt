package com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.veoneer.logisticinventoryapp.core.domain.model.InventoryState
import com.veoneer.logisticinventoryapp.core.domain.model.InventoryType
import com.veoneer.logisticinventoryapp.core.domain.model.RefInventoryStepEnum
import com.veoneer.logisticinventoryapp.core.domain.model.Reference
import com.veoneer.logisticinventoryapp.core.presentation.components.AlertDialogExample
import com.veoneer.logisticinventoryapp.core.presentation.components.SystemBroadcastReceiverHandler
import com.veoneer.logisticinventoryapp.core.presentation.components.TextFieldEntry
import com.veoneer.logisticinventoryapp.core.presentation.components.global.RoudButton
import com.veoneer.logisticinventoryapp.core.presentation.components.global.ScannerInput
import com.veoneer.logisticinventoryapp.core.router.model.Screen
import com.veoneer.logisticinventoryapp.core.service.MainService
import com.veoneer.logisticinventoryapp.core.service.MainViewModel
import com.veoneer.logisticinventoryapp.referenceInventoryType_feature.presentation.state.referenceInventoryState
import com.veoneer.logisticinventoryapp.scanner.data.zebra.ZebraDatawedgeActions
import kotlinx.coroutines.launch


@Composable
fun ReferenceInventoryTypeScreen(
    referenceInventoryTypeViewModel: ReferenceInventoryTypeViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val mainService : MainService = mainViewModel.getMainService()
    val barcodeScanState by referenceInventoryTypeViewModel.barcodeScanState.collectAsState()
    val scannerStateFlow by referenceInventoryTypeViewModel.scannerStateFlow.observeAsState()
    val inventoryState = referenceInventoryTypeViewModel.inventoryState.inventory as InventoryType.ReferenceInventory
    val snackbarHostState = remember { SnackbarHostState() }
    val error by referenceInventoryTypeViewModel.error

    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState){
                Snackbar(
                    snackbarData = it,
                    backgroundColor = if (!referenceInventoryTypeViewModel.inventoryState.isError) {
                        Color.Black
                    }else {
                        Color.Red
                    },
                    contentColor = Color.White,
                )
            }
        },

//        topBar = {
//            TopBar(
//                title = "Emplacement : ${(referenceInventoryTypeViewModel.inventoryState.inventory as InventoryType.ReferenceInventory).locationCode}",
//                buttonIcon = Icons.Filled.ArrowBack,
//                onButtonClicked = {
//                    navigateBack()
//                }
//            )
//        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(paddingValues = contentPadding)) {
            /*
            NavDestinationHelper(
                shouldNavigate = { referenceInventoryTypeViewModel.inventoryState.isComplete },
                destination = navigateBack
            )

             */
            if (referenceInventoryTypeViewModel.inventoryState.isComplete) {
                mainService.refInventoryStepState = RefInventoryStepEnum.Start
                mainService.informationMessage = "Envoie réussie"
                mainViewModel.playSucces()
                mainService.navigateTo(Screen.HomeScreen)
            }

//            LaunchedEffect(key1 = state){
//                scannerViewModel.backStackIsTrue.value = false
//            }

            SystemBroadcastReceiverHandler(
                state = scannerStateFlow,
                handleBarcodeScan = referenceInventoryTypeViewModel::handleBarcodeScan
            )

            if (referenceInventoryTypeViewModel.inventoryState.isSnackbarShowing) {
                LaunchedEffect(key1 = referenceInventoryTypeViewModel.inventoryState.isSnackbarShowing) {
                    scope.launch {
                        val result = snackbarHostState
                            .showSnackbar(
                                message = referenceInventoryTypeViewModel.inventoryState.snackbarMessage,
                                duration = SnackbarDuration.Short
                            )
                        when (result) {
                            SnackbarResult.ActionPerformed -> referenceInventoryTypeViewModel.dismissSnackbar()
                            SnackbarResult.Dismissed -> referenceInventoryTypeViewModel.dismissSnackbar()
                        }
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                /*
                ReferenceInventoryContainer(
                    state = referenceInventoryTypeViewModel.inventoryState,
                    context = LocalContext.current
                )

                 */
                if (referenceInventoryTypeViewModel.inventoryState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                if (error.isNotEmpty()){
                    Row() {
                        Text(
                            text = error,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )
                    }
                }
                InventoryContainer(
                    context = LocalContext.current,
                    onUserInputChange = referenceInventoryTypeViewModel::onRefInputChange ,
                    state = referenceInventoryTypeViewModel.inventoryState,
                    refInventoryStepEnum = mainService.refInventoryStepState,
                    refInventoryState = referenceInventoryTypeViewModel.referenceInventoryState,
                    referenceInventoryTypeViewModel = referenceInventoryTypeViewModel
                )
                when (mainService.refInventoryStepState) {
                    RefInventoryStepEnum.Start -> {

                    }

                    RefInventoryStepEnum.GetFamily -> {

                    }

                    RefInventoryStepEnum.GetType -> {
                        ScannerInput(
                            "Entrer une famille",
                            referenceInventoryTypeViewModel::onInputChange,
                            referenceInventoryTypeViewModel::getFamily
                        )
                        MyUI(referenceInventoryTypeViewModel)
                    }

                    RefInventoryStepEnum.GetQuantity -> {
                        MyUI(referenceInventoryTypeViewModel)
                        TextFieldEntry(
                            description = "Quantité de produit",
                            hint = "Quantité de produit",
                            leadingIcon = Icons.Default.Inventory,
                            textValue = referenceInventoryTypeViewModel.referenceInventoryState.userInput,
                            onValueChanged = referenceInventoryTypeViewModel::onQuantityInputChange
                        )
                        Button(onClick = referenceInventoryTypeViewModel::getQuantityRef) {
                            Text(text = "Valider")
                        }
                    }

                    RefInventoryStepEnum.GetQuantityByScan -> {
                        TextFieldEntry(
                            description = "Quantité de produit",
                            hint = "Quantité de produit",
                            leadingIcon = Icons.Default.Inventory,
                            textValue = referenceInventoryTypeViewModel.referenceInventoryState.userInput,
                            onValueChanged = referenceInventoryTypeViewModel::onQuantityInputChange
                        )
                        Button(onClick = referenceInventoryTypeViewModel::getQuantityRef) {
                            Text(text = "Valider")
                        }
                    }

                    RefInventoryStepEnum.Send -> {

                    }
                }
            }


        }
    }
}

@Composable
fun ReferenceInventoryContainer(
    state: InventoryState,
    context: Context
) {
    Column () {
        InstructionAndInfo(actionMessage = state.actionMessage, errorMessage = state.errorMessage)
    }
}


@Composable
fun InstructionAndInfo(
    actionMessage: String,
    errorMessage: String?
) {
    Column(
    ) {
        Text(
            text = actionMessage,
            style = MaterialTheme.typography.h2,
            fontWeight = FontWeight.Bold,
        )

        if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = "Erreur : $errorMessage",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InstructionAndInfoPreview() {
    InstructionAndInfo("Scanner un produit", null)
}

@Composable
fun InventoriedReferencesList(
    references: MutableList<Reference>,
    referenceInventoryTypeViewModel: ReferenceInventoryTypeViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)) {
        Text(text = "Listes des références",fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .height(60.dp)
                .background(color = Color(0xFF5551FF), shape = RoundedCornerShape(10.dp))
        ) {
            Text(text = "Quantité", color = Color.White, style = MaterialTheme.typography.body1, modifier = Modifier
                .weight(.28f)
                .padding(start = 8.dp))
            Text(text = "FAB/CIE", color = Color.White, style = MaterialTheme.typography.body1, modifier = Modifier.weight(.3f))
            Text(text = "Référence", color = Color.White, style = MaterialTheme.typography.body1, modifier = Modifier.weight(.48f))
        }
        LazyVerticalGrid(
            modifier = Modifier.height(height = 220.dp),
            columns = GridCells.Adaptive(minSize = 380.dp)
        ) {
            itemsIndexed(references) {index, it ->
                InventoriedReference(code = it.code, quantity = it.quantity, type = it.type,index,referenceInventoryTypeViewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InventoriedReferencesListPreview() {
    val list = mutableListOf<Reference>(
        Reference("02154545A", 10, "FAB"),
        Reference("08484851A", 20, "FAB"),
        Reference("99874120A", 60, "FAB"),
        Reference("64888110A", 5, "FAB"),
        Reference("98978410A", 1, "FAB"),
        Reference("02156598A", 10000, "CIE"),
        Reference("21544545A", 300, "CIE"),
        Reference("99874111A", 12, "FAB")
    )
}

@Composable
fun InventoriedReference(
    code: String,
    quantity: Int,
    type: String,
    index : Int,
    referenceInventoryTypeViewModel: ReferenceInventoryTypeViewModel
) {
        val openAlertDialog = remember { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .height(60.dp)
                .background(color = Color(0xFF5551FF), shape = RoundedCornerShape(10.dp))
                .fillMaxWidth()
            ,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,

                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(text = "$quantity", color = Color.White, style = MaterialTheme.typography.body1, modifier = Modifier.weight(.3f))
                Text(text = "$type", color = Color.White, style = MaterialTheme.typography.body1, modifier = Modifier.weight(.3f))
                Text(text = "$code", color = Color.White, style = MaterialTheme.typography.body1, modifier = Modifier.weight(.4f))
                IconButton(onClick = {openAlertDialog.value = true}){
                    Icon(Icons.Filled.Close, contentDescription = "Supprimer")
                }
            }
        }
    if (openAlertDialog.value) {
        AlertDialogExample(
            onDismissRequest = { openAlertDialog.value = false },
            onConfirmation = {
                openAlertDialog.value = false
                referenceInventoryTypeViewModel.referenceInventoryState.refList.removeAt(index) // Add logic here to handle confirmation.
            },
            dialogTitle = "Confirmer",
            dialogText = "Voulez-vous vraiment supprimer cette référence.",
            icon = Icons.Default.Info
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InventoriedReferencePreview() {

}

@Composable
fun InventoryContainer(
    state: InventoryState,
    refInventoryStepEnum: RefInventoryStepEnum,
    referenceInventoryTypeViewModel : ReferenceInventoryTypeViewModel,
    refInventoryState: referenceInventoryState,
    onUserInputChange: (String) -> Unit,
    context: Context,
) {
    val openAlertDialog = remember { mutableStateOf(false) }
    Column {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (refInventoryStepEnum) {
                RefInventoryStepEnum.Start -> {
                    ZebraDatawedgeActions.enableScan(context)
                    Text(text = "Scanner un produit ou entrer une famille",style = MaterialTheme.typography.h2, fontWeight = FontWeight.Bold)
                    ScannerInput(
                        "Entrer une famille",
                        referenceInventoryTypeViewModel::onInputChange,
                        referenceInventoryTypeViewModel::getFamily
                    )
                    if (referenceInventoryTypeViewModel.referenceInventoryState.refList.isNotEmpty()) {
                        InventoriedReferencesList(references = referenceInventoryTypeViewModel.referenceInventoryState.refList, referenceInventoryTypeViewModel)
                        RoudButton(label = "Valider Zone" , onclick = {
                            openAlertDialog.value = true
                        } )
                    }
                    if (openAlertDialog.value) {
                        AlertDialogExample(
                            onDismissRequest = { openAlertDialog.value = false },
                            onConfirmation = {
                                openAlertDialog.value = false
                                referenceInventoryTypeViewModel.sendInventory()
                                },
                            dialogTitle = "Confirmer",
                            dialogText = "Voulez-vous vraiment envoyer les références.",
                            icon = Icons.Default.Info
                        )
                    }
                }
                RefInventoryStepEnum.GetFamily -> {
                    Text(text = "Famille en cours ${(state.inventory as InventoryType.ReferenceInventory).famille}",style = MaterialTheme.typography.h2, fontWeight = FontWeight.Bold)
                }
                RefInventoryStepEnum.GetType -> {
                    Text(text = "Famille en cours ${(state.inventory as InventoryType.ReferenceInventory).famille}",style = MaterialTheme.typography.h2, fontWeight = FontWeight.Bold)
                }
                RefInventoryStepEnum.GetQuantity -> {
                    Text(text = "REF° ${(state.inventory as InventoryType.ReferenceInventory).reference}",style = MaterialTheme.typography.h2, fontWeight = FontWeight.Bold)

                }

                RefInventoryStepEnum.Send -> {
                    ZebraDatawedgeActions.enableScan(context)

                }
                else -> {

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyUI(referenceInventoryTypeViewModel: ReferenceInventoryTypeViewModel) {
    val listItems = mutableListOf("Choisir le type de produit")
    referenceInventoryTypeViewModel.referenceInventoryState.familyList.forEach() {
        listItems.add(it.designation)
    }

    val contextForToast = LocalContext.current.applicationContext

    // state of the menu
    var expanded by remember {
        mutableStateOf(false)
    }

    // remember the selected item
    var selectedItem by remember {
        mutableStateOf(listItems[0])
    }
    if ((referenceInventoryTypeViewModel.inventoryState.inventory as InventoryType.ReferenceInventory).designation.isNotEmpty()) {
        selectedItem =
            (referenceInventoryTypeViewModel.inventoryState.inventory as InventoryType.ReferenceInventory).designation
    }
    // box
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "TYPE") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )

        // menu
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listItems.forEach { selectedOption ->
                DropdownMenuItem(onClick = {
                    selectedItem = selectedOption
                    Toast.makeText(contextForToast, selectedOption, Toast.LENGTH_SHORT).show()
                    expanded = false
                    referenceInventoryTypeViewModel.getType(selectedItem)
                }) {
                    Text(text = selectedOption)
                }
            }
        }
    }
}
