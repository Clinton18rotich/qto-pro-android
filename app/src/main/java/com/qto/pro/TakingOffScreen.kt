package com.qto.pro

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakingOffScreen(data: QTOData, onUpdate: (QTOData) -> Unit, ctx: Context) {
    var selectedSectionIndex by remember { mutableStateOf(-1) }
    var showAddSection by remember { mutableStateOf(false) }
    var newSectionTitle by remember { mutableStateOf("") }
    var showAddItem by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TakingOffItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    if (selectedSectionIndex == -1) {
        // Section list view
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A1628))) {
            // Project info header
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PROJECT: ${data.projectName.ifEmpty { "[Tap to set]" }}", 
                        color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (data.contractNo.isNotEmpty()) Text("Contract: ${data.contractNo}", 
                        color = Color.Gray, fontSize = 12.sp)
                    Text("Sheet ${data.sheetNo} of ${data.totalSheets} | ${data.date}", 
                        color = Color.Gray, fontSize = 11.sp)
                }
            }

            // Sections list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(data.sections.size) { i ->
                    val section = data.sections[i]
                    val totalQty = section.items.sumOf { calculateQuantity(it) }
                    val deductions = section.items.filter { it.isDeduction }.sumOf { calculateQuantity(it) }
                    val netQty = totalQty - deductions

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { selectedSectionIndex = i },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${i+1}. ${section.title}", color = Color.White, 
                                    fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("${section.items.size} items | Net: ${formatQuantity(netQty)} m³", 
                                    color = Color.Gray, fontSize = 11.sp)
                            }
                            IconButton(onClick = { 
                                val new = data.copy(sections = data.sections.toMutableList().also { it.removeAt(i) })
                                onUpdate(new)
                                if (selectedSectionIndex >= new.sections.size) selectedSectionIndex = -1
                            }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }

            // Add section buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddSection = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("+ Add Section") }

                OutlinedButton(
                    onClick = {
                        val new = data.copy(sections = data.sections.toMutableList().also { 
                            it.add(TakingOffSection(title = KENYAN_SECTIONS.first()))
                        })
                        onUpdate(new)
                        selectedSectionIndex = new.sections.size - 1
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Quick Add", fontSize = 12.sp) }
            }
        }

        // Add section dialog
        if (showAddSection) {
            AlertDialog(
                onDismissRequest = { showAddSection = false },
                title = { Text("Add Section", color = Color.White) },
                text = {
                    Column {
                        KENYAN_SECTIONS.forEach { title ->
                            TextButton(
                                onClick = {
                                    val new = data.copy(sections = data.sections.toMutableList().also {
                                        it.add(TakingOffSection(title = title))
                                    })
                                    onUpdate(new)
                                    selectedSectionIndex = new.sections.size - 1
                                    showAddSection = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(title, color = Color(0xFF2196F3), fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddSection = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1A2744)
            )
        }
    } else {
        // 3-column taking-off view for selected section
        val section = data.sections[selectedSectionIndex]
        TakingOffDetailView(
            section = section,
            onBack = { selectedSectionIndex = -1 },
            onUpdateSection = { updated ->
                val new = data.copy(sections = data.sections.toMutableList().also { it[selectedSectionIndex] = updated })
                onUpdate(new)
            },
            onAddItem = { showAddItem = true; editingItem = null },
            onEditItem = { editingItem = it; showAddItem = true },
            onDeleteItem = { itemId ->
                val updated = section.copy(items = section.items.toMutableList().also { it.removeAll { i -> i.id == itemId } })
                val new = data.copy(sections = data.sections.toMutableList().also { it[selectedSectionIndex] = updated })
                onUpdate(new)
            },
            ctx = ctx
        )

        // Add/Edit item dialog
        if (showAddItem) {
            ItemEditDialog(
                item = editingItem,
                onDismiss = { showAddItem = false; editingItem = null },
                onSave = { item ->
                    val items = section.items.toMutableList()
                    val idx = items.indexOfFirst { it.id == item.id }
                    if (idx >= 0) items[idx] = item else items.add(item)
                    val updated = section.copy(items = items)
                    val new = data.copy(sections = data.sections.toMutableList().also { it[selectedSectionIndex] = updated })
                    onUpdate(new)
                    showAddItem = false
                    editingItem = null
                }
            )
        }
    }
}

@Composable
fun TakingOffDetailView(
    section: TakingOffSection,
    onBack: () -> Unit,
    onUpdateSection: (TakingOffSection) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (TakingOffItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    ctx: Context
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A1628))) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Text(section.title, color = Color(0xFF2196F3), 
                fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, "Add Item", tint = Color(0xFF4CAF50))
            }
        }

        // Column headers
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF152040)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dimensions (L×W×H)", color = Color(0xFF2196F3), fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            Text("Waste/\nCross-ref", color = Color(0xFF2196F3), fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
            Text("Description / Remarks", color = Color(0xFF2196F3), fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
        }
        Divider(color = Color(0xFF2196F3), thickness = 1.dp)

        // Items list
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(section.items.size) { i ->
                val item = section.items[i]
                val qty = calculateQuantity(item)
                val bgColor = if (item.isDeduction) Color(0xFF2A1010) else Color(0xFF1A2744)

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable { onEditItem(item) },
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Column 1: Dimensions
                        Column(modifier = Modifier.weight(1.2f)) {
                            if (item.isDeduction) {
                                Text("LESS:", color = Color(0xFFFF5252), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${item.itemNo}", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                "${item.length} × ${item.width} × ${item.height}",
                                color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "= ${formatQuantity(qty)} ${item.unit}",
                                color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Column 2: Waste/Cross-ref
                        Column(
                            modifier = Modifier.weight(0.8f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (item.wasteMultiplier.isNotEmpty()) {
                                Text("×${item.wasteMultiplier}", color = Color(0xFFFF9800), 
                                    fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (item.crossRef.isNotEmpty()) {
                                Text("ref: ${item.crossRef}", color = Color(0xFF64B5F6), 
                                    fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Column 3: Description
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                item.description.ifEmpty { "[No description]" },
                                color = Color.White, fontSize = 10.sp, maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.remarks.isNotEmpty()) {
                                Text("Note: ${item.remarks}", color = Color.Gray, fontSize = 8.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }

            // Totals row
            item {
                val totalQty = section.items.filter { !it.isDeduction }.sumOf { calculateQuantity(it) }
                val deductions = section.items.filter { it.isDeduction }.sumOf { calculateQuantity(it) }
                val netQty = totalQty - deductions

                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF152040)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("SECTION TOTALS", color = Color(0xFF2196F3), 
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gross:", color = Color.White, fontSize = 11.sp)
                            Text("${formatQuantity(totalQty)} m³", color = Color.White, 
                                fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        if (deductions > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Less:", color = Color(0xFFFF5252), fontSize = 11.sp)
                                Text("-${formatQuantity(deductions)} m³", color = Color(0xFFFF5252), 
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Divider(color = Color(0xFF2196F3), modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NET:", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${formatQuantity(netQty)} m³", color = Color(0xFF4CAF50), 
                                fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Bottom action bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddItem,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(8.dp)
            ) { Text("+ Item", fontSize = 12.sp) }

            OutlinedButton(
                onClick = {
                    val updated = section.copy(items = section.items.toMutableList().also {
                        it.add(TakingOffItem(
                            isDeduction = true,
                            description = "DEDUCTION: ",
                            itemNo = "D${it.size + 1}"
                        ))
                    })
                    onUpdateSection(updated)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) { Text("+ Deduction", fontSize = 12.sp) }

            IconButton(onClick = {
                exportToPDF(section, ctx)
            }) {
                Icon(Icons.Default.PictureAsPdf, "Export PDF", tint = Color(0xFFFF5252))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditDialog(
    item: TakingOffItem?,
    onDismiss: () -> Unit,
    onSave: (TakingOffItem) -> Unit
) {
    val isNew = item == null
    var itemNo by remember { mutableStateOf(item?.itemNo ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var unit by remember { mutableStateOf(item?.unit ?: "m³") }
    var length by remember { mutableStateOf(item?.length ?: "") }
    var width by remember { mutableStateOf(item?.width ?: "") }
    var height by remember { mutableStateOf(item?.height ?: "") }
    var waste by remember { mutableStateOf(item?.wasteMultiplier ?: "") }
    var crossRef by remember { mutableStateOf(item?.crossRef ?: "") }
    var isDeduction by remember { mutableStateOf(item?.isDeduction ?: false) }
    var remarks by remember { mutableStateOf(item?.remarks ?: "") }
    var unitExpanded by remember { mutableStateOf(false) }

    val calcQty = calculateQuantity(TakingOffItem(
        length = length, width = width, height = height, 
        wasteMultiplier = waste, unit = unit
    ))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Item" else "Edit Item", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Item number
                OutlinedTextField(
                    value = itemNo, onValueChange = { itemNo = it },
                    label = { Text("Item No.") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    colors = textFieldColors(),
                    maxLines = 4
                )

                // Unit dropdown
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it }
                ) {
                    OutlinedTextField(
                        value = unit, onValueChange = {},
                        readOnly = true, label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = textFieldColors()
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        KENYAN_UNITS.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = { unit = u; unitExpanded = false }
                            )
                        }
                    }
                }

                // Dimensions
                Text("Dimensions", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = length, onValueChange = { length = it },
                        label = { Text("Length") },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = width, onValueChange = { width = it },
                        label = { Text("Width") },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = height, onValueChange = { height = it },
                        label = { Text("Height") },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                // Calculated quantity preview
                if (length.isNotEmpty() || width.isNotEmpty() || height.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2137)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Calculated: ${formatQuantity(calcQty)} $unit",
                            modifier = Modifier.padding(8.dp),
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Waste multiplier
                OutlinedTextField(
                    value = waste, onValueChange = { waste = it },
                    label = { Text("Waste Multiplier") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("e.g., 1.05 for 5% waste", fontSize = 10.sp, color = Color.Gray) }
                )

                // Cross reference
                OutlinedTextField(
                    value = crossRef, onValueChange = { crossRef = it },
                    label = { Text("Cross Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    singleLine = true
                )

                // Deduction toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDeduction,
                        onCheckedChange = { isDeduction = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5252))
                    )
                    Text("This is a deduction (LESS)", color = if (isDeduction) Color(0xFFFF5252) else Color.Gray, fontSize = 12.sp)
                }

                // Remarks
                OutlinedTextField(
                    value = remarks, onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newItem = TakingOffItem(
                        id = item?.id ?: java.util.UUID.randomUUID().toString(),
                        itemNo = itemNo,
                        description = description,
                        unit = unit,
                        length = length,
                        width = width,
                        height = height,
                        wasteMultiplier = waste,
                        crossRef = crossRef,
                        isDeduction = isDeduction,
                        quantity = formatQuantity(calcQty),
                        remarks = remarks
                    )
                    onSave(newItem)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF1A2744)
    )
}

@Composable
fun SettingsScreen(data: QTOData, onUpdate: (QTOData) -> Unit) {
    var projectName by remember { mutableStateOf(data.projectName) }
    var contractNo by remember { mutableStateOf(data.contractNo) }
    var sheetNo by remember { mutableStateOf(data.sheetNo) }
    var totalSheets by remember { mutableStateOf(data.totalSheets) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A1628)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Project Settings", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 18.sp)

        OutlinedTextField(
            value = projectName, onValueChange = { projectName = it },
            label = { Text("Project Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            singleLine = true
        )

        OutlinedTextField(
            value = contractNo, onValueChange = { contractNo = it },
            label = { Text("Contract No.") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = sheetNo, onValueChange = { sheetNo = it },
                label = { Text("Sheet No.") },
                modifier = Modifier.weight(1f),
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = totalSheets, onValueChange = { totalSheets = it },
                label = { Text("Total Sheets") },
                modifier = Modifier.weight(1f),
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Button(
            onClick = {
                onUpdate(data.copy(
                    projectName = projectName,
                    contractNo = contractNo,
                    sheetNo = sheetNo,
                    totalSheets = totalSheets
                ))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(8.dp)
        ) { Text("Save Settings") }

        Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

        Text("About", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("QTO Pro v1.0", color = Color.White, fontSize = 14.sp)
        Text("Kenyan Quantity Take-Off App", color = Color.Gray, fontSize = 12.sp)
        Text("Traditional 3-Column Taking-Off Format", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF2196F3),
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = Color(0xFF2196F3),
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color(0xFF2196F3)
)

fun exportToPDF(section: TakingOffSection, ctx: Context) {
    try {
        val file = File(ctx.cacheDir, "QTO_${section.title.replace(" ", "_")}.csv")
        FileOutputStream(file).use { out ->
            out.write("\uFEFF".toByteArray()) // BOM for Excel
            out.write("QTO PRO - KENYAN TAKING-OFF SHEET\n".toByteArray())
            out.write("Section: ${section.title}\n".toByteArray())
            out.write("Item No.,Description,Unit,Dimensions (LxWxH),Waste,Cross-ref,Qty,Deduction,Remarks\n".toByteArray())
            section.items.forEach { item ->
                val dims = "${item.length}x${item.width}x${item.height}"
                val qty = calculateQuantity(item)
                val line = "\"${item.itemNo}\",\"${item.description}\",${item.unit},\"${dims}\",${item.wasteMultiplier},\"${item.crossRef}\",${formatQuantity(qty)},${item.isDeduction},\"${item.remarks}\"\n"
                out.write(line.toByteArray())
            }
            val total = section.items.filter { !it.isDeduction }.sumOf { calculateQuantity(it) }
            val ded = section.items.filter { it.isDeduction }.sumOf { calculateQuantity(it) }
            out.write("\"\",\"NET TOTAL\",\"\",\"\",\"\",\"\",${formatQuantity(total - ded)},\"\",\"\"\n".toByteArray())
        }
        android.widget.Toast.makeText(ctx, "Exported to: ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(ctx, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
