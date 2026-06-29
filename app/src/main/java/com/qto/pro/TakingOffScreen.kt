package com.qto.pro

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakingOffScreen(data: QTOData, onUpdate: (QTOData) -> Unit, ctx: Context) {
    var selectedSection by remember { mutableStateOf<TakingOffSection?>(null) }
    var showAddSection by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    if (selectedSection != null) {
        SectionDetailScreen(selectedSection!!, { updated ->
            val nd = data.copy(); val idx = nd.sections.indexOfFirst { it.id == updated.id }
            if (idx >= 0) { nd.sections[idx] = updated; onUpdate(nd) }; selectedSection = updated
        }, { selectedSection = null })
        return
    }
    if (showPreview) { DocumentPreviewScreen(data, ctx) { showPreview = false }; return }

    Scaffold(
        topBar = { TopAppBar(title = { Text("📐 Taking Off", fontWeight = FontWeight.Bold) },
            actions = {
                TextButton(onClick = { showAddSection = true }) { Text("+ Section", color = Color(0xFF2196F3)) }
                TextButton(onClick = { showPreview = true }) { Text("📄", fontSize = 20.sp) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A1628))) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Card(Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("QUANTITY TAKE-OFF SHEET", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Row { Text("PROJECT: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray); BasicTextField(value = data.projectName, onValueChange = { onUpdate(data.copy(projectName = it)) }, textStyle = TextStyle(fontSize = 12.sp, color = Color.White), modifier = Modifier.weight(1f).background(Color(0xFF2A3755)).padding(4.dp), singleLine = true) }
                        Row(Modifier.padding(top = 4.dp)) { Text("CONTRACT: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray); BasicTextField(value = data.contractNo, onValueChange = { onUpdate(data.copy(contractNo = it)) }, textStyle = TextStyle(fontSize = 11.sp, color = Color.White), modifier = Modifier.weight(1f).background(Color(0xFF2A3755)).padding(4.dp), singleLine = true) }
                        Text("Sheet ${data.sheetNo} of ${data.totalSheets} | ${data.date}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
            if (data.sections.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("📐", fontSize = 48.sp); Text("No sections added yet", color = Color.Gray, fontSize = 14.sp); Text("Tap '+ Section' to start", color = Color.Gray, fontSize = 12.sp) } } }
            }
            items(data.sections) { section ->
                val totalQty = section.items.filter { !it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 } - section.items.filter { it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedSection = section }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(section.title.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)); Text("${section.items.size} items", fontSize = 11.sp, color = Color.Gray) }
                            if (totalQty > 0) Surface(color = Color(0xFF2196F3).copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) { Text("Total: ${"%.2f".format(totalQty)}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold) }
                        }
                        TextButton({ val nd = data.copy(); nd.sections.removeAll { it.id == section.id }; onUpdate(nd) }) { Text("🗑️ Remove", fontSize = 10.sp, color = Color(0xFFFF453A)) }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (showAddSection) {
        var customName by remember { mutableStateOf("") }
        AlertDialog({ showAddSection = false }, title = { Text("Add Section") }, text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                KENYAN_SECTIONS.forEach { s -> TextButton({ val nd = data.copy(); nd.sections.add(TakingOffSection(title = s)); onUpdate(nd); showAddSection = false }, Modifier.fillMaxWidth()) { Text(s, fontSize = 12.sp, color = Color(0xFF2196F3)) } }
                Divider(Modifier.padding(vertical = 8.dp))
                OutlinedTextField(customName, { customName = it }, label = { Text("Or custom section name") }, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = { if (customName.isNotBlank()) Button({ val nd = data.copy(); nd.sections.add(TakingOffSection(title = customName)); onUpdate(nd); showAddSection = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Add Custom") } }, dismissButton = { Button({ showAddSection = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))) { Text("Cancel") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(section: TakingOffSection, onUpdateSection: (TakingOffSection) -> Unit, onBack: () -> Unit) {
    var showAddItem by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TakingOffItem?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(section.title.take(30), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)) }, navigationIcon = { TextButton(onClick = onBack) { Text("← Back", color = Color(0xFF2196F3)) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A1628))) },
        floatingActionButton = { FloatingActionButton({ showAddItem = true }, containerColor = Color(0xFF2196F3)) { Text("+", fontSize = 24.sp, color = Color.White) } }
    ) { padding ->
        val totalQty = section.items.filter { !it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 } - section.items.filter { it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
        LazyColumn(Modifier.padding(padding).padding(8.dp)) {
            item { Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))) { Column(Modifier.padding(12.dp)) { Text(section.title.uppercase(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)); Row(horizontalArrangement = Arrangement.SpaceBetween) { Text("${section.items.size} items", fontSize = 11.sp, color = Color.Gray); Text("TOTAL: ${"%.2f".format(totalQty)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) } } } }
            item { Row(Modifier.fillMaxWidth().background(Color(0xFF2A3755)).padding(8.dp)) { Text("DIMENSIONS (L×W×H)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.2f)); Text("WASTE/REF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.6f)); Text("DESCRIPTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.5f)) }; Divider(color = Color(0xFF2196F3), thickness = 1.dp) }
            items(section.items) { item ->
                val qty = item.quantity.toDoubleOrNull() ?: 0.0
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { editingItem = item }, colors = CardDefaults.cardColors(containerColor = if (item.isDeduction) Color(0xFF2A1A1A) else Color(0xFF1A2744))) {
                    Row(Modifier.padding(8.dp)) {
                        Column(Modifier.weight(1.2f)) { if (item.length.isNotEmpty()) { Text(item.length, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White); if (item.width.isNotEmpty()) Text(item.width, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White); if (item.height.isNotEmpty()) Text(item.height, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White); Divider(color = Color.Gray, thickness = 0.5.dp) }; Text(if (qty > 0) "%.2f".format(qty) else "", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (item.isDeduction) Color(0xFFFF453A) else Color(0xFF4CAF50), fontFamily = FontFamily.Monospace) }
                        Column(Modifier.weight(0.6f)) { if (item.wasteMultiplier.isNotEmpty()) Text("${item.wasteMultiplier}/", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFF9F0A)); if (item.crossRef.isNotEmpty()) Text(item.crossRef, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray); if (item.isDeduction) Text("(Less)", fontSize = 9.sp, color = Color(0xFFFF453A), fontWeight = FontWeight.Bold) }
                        Column(Modifier.weight(1.5f)) { Text(item.description.ifEmpty { "[No description]" }, fontSize = 10.sp, color = Color.White, maxLines = 4); Text(item.unit, fontSize = 9.sp, color = Color.Gray); if (item.remarks.isNotEmpty()) Text("Note: ${item.remarks}", fontSize = 9.sp, color = Color(0xFFA0A0B8)) }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (showAddItem || editingItem != null) ItemFormDialog(editingItem, { item -> val nd = section.copy(); if (editingItem != null) { val idx = nd.items.indexOfFirst { it.id == editingItem!!.id }; if (idx >= 0) nd.items[idx] = item } else nd.items.add(item); onUpdateSection(nd); editingItem = null; showAddItem = false }, { editingItem = null; showAddItem = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormDialog(existingItem: TakingOffItem?, onSave: (TakingOffItem) -> Unit, onDismiss: () -> Unit) {
    var desc by remember { mutableStateOf(existingItem?.description ?: "") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "m³") }
    var length by remember { mutableStateOf(existingItem?.length ?: "") }
    var width by remember { mutableStateOf(existingItem?.width ?: "") }
    var height by remember { mutableStateOf(existingItem?.height ?: "") }
    var waste by remember { mutableStateOf(existingItem?.wasteMultiplier ?: "") }
    var crossRef by remember { mutableStateOf(existingItem?.crossRef ?: "") }
    var isDeduction by remember { mutableStateOf(existingItem?.isDeduction ?: false) }
    var remarks by remember { mutableStateOf(existingItem?.remarks ?: "") }
    val qty = remember(length, width, height) { val l = length.toDoubleOrNull() ?: 0.0; val w = width.toDoubleOrNull() ?: 1.0; val h = height.toDoubleOrNull() ?: 1.0; val wm = waste.toDoubleOrNull() ?: 1.0; if (waste.isNotEmpty() && wm > 0) l * w * h * wm else l * w * h }
    AlertDialog(onDismiss, title = { Text(if (existingItem != null) "Edit Item" else "Add Item", fontSize = 15.sp, fontWeight = FontWeight.Bold) }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("DIMENSIONS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { OutlinedTextField(length, { length = it }, label = { Text("Length") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)); Text("×", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically)); OutlinedTextField(width, { width = it }, label = { Text("Width") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)); Text("×", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically)); OutlinedTextField(height, { height = it }, label = { Text("Height/Depth") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)) }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E))) { Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("QTY: ${"%.3f".format(qty)}", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace); Text(unit, color = Color(0xFFA0A0B8)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(waste, { waste = it }, label = { Text("Waste ×") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)); OutlinedTextField(crossRef, { crossRef = it }, label = { Text("Cross Ref") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)) }
            Text("Unit:", fontSize = 11.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { KENYAN_UNITS.take(8).forEach { u -> FilterChip(unit == u, { unit = u }, label = { Text(u, fontSize = 9.sp) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { KENYAN_UNITS.drop(8).forEach { u -> FilterChip(unit == u, { unit = u }, label = { Text(u, fontSize = 9.sp) }) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(isDeduction, { isDeduction = it }); Text("Deduction ('Less')", fontSize = 11.sp, color = Color(0xFFFF453A)) }
            OutlinedTextField(desc, { desc = it }, label = { Text("Description *") }, modifier = Modifier.fillMaxWidth(), maxLines = 4, textStyle = TextStyle(fontSize = 11.sp))
            OutlinedTextField(remarks, { remarks = it }, label = { Text("Remarks (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(fontSize = 11.sp))
        }
    }, confirmButton = { Button({ if (desc.isNotBlank()) onSave(TakingOffItem(id = existingItem?.id ?: UUID.randomUUID().toString(), description = desc, unit = unit, length = length, width = width, height = height, wasteMultiplier = waste, crossRef = crossRef, isDeduction = isDeduction, quantity = "%.3f".format(qty), remarks = remarks)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Save") } }, dismissButton = { Button(onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(data: QTOData, ctx: Context, onClose: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("📄 Taking-Off Sheet", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onClose) { Text("← Back", color = Color(0xFF2196F3)) } }, actions = { TextButton({ val file = generateTakingOffPDF(data, ctx); try { val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file); val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; ctx.startActivity(Intent.createChooser(intent, "Share")) } catch (e: Exception) { Toast.makeText(ctx, "PDF saved to Downloads", Toast.LENGTH_LONG).show() } }) { Text("📤 Share", color = Color(0xFF2196F3), fontSize = 11.sp) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A1628))) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            item { Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2744))) { Column(Modifier.padding(16.dp)) { Text("QUANTITY TAKE-OFF SHEET", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), textAlign = TextAlign.Center); Text("Republic of Kenya", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center); Divider(color = Color(0xFF2196F3), modifier = Modifier.padding(vertical = 8.dp)); Text("PROJECT: ${data.projectName.ifEmpty { "_______________________" }}", fontSize = 12.sp, color = Color.White); Text("CONTRACT NO: ${data.contractNo.ifEmpty { "_______________________" }}", fontSize = 11.sp, color = Color.Gray); Text("SHEET ${data.sheetNo} OF ${data.totalSheets} | DATE: ${data.date}", fontSize = 10.sp, color = Color.Gray) } } }
            data.sections.forEach { section ->
                val totalQty = section.items.filter { !it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 } - section.items.filter { it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
                if (section.items.isNotEmpty()) {
                    item { Surface(Modifier.fillMaxWidth(), color = Color(0xFF2A3755)) { Text(section.title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) }; Divider(color = Color(0xFF2196F3), thickness = 1.dp) }
                    item { Row(Modifier.fillMaxWidth().background(Color(0xFF1A2744)).padding(6.dp)) { Text("DIMENSIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.2f)); Text("×/REF", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.5f)); Text("DESCRIPTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.8f)) } }
                    section.items.forEach { item ->
                        item { Row(Modifier.fillMaxWidth().padding(4.dp)) { Column(Modifier.weight(1.2f)) { if (item.length.isNotEmpty()) Text(item.length, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White); if (item.width.isNotEmpty()) Text(item.width, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White); if (item.height.isNotEmpty()) Text(item.height, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White); Divider(color = Color.DarkGray, thickness = 0.5.dp); Text(item.quantity, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (item.isDeduction) Color(0xFFFF453A) else Color(0xFF4CAF50), fontFamily = FontFamily.Monospace) }; Column(Modifier.weight(0.5f)) { if (item.wasteMultiplier.isNotEmpty()) Text("${item.wasteMultiplier}/", fontSize = 9.sp, color = Color(0xFFFF9F0A)); if (item.crossRef.isNotEmpty()) Text(item.crossRef, fontSize = 9.sp, color = Color.Gray); if (item.isDeduction) Text("Less", fontSize = 8.sp, color = Color(0xFFFF453A)) }; Column(Modifier.weight(1.8f)) { Text(item.description, fontSize = 9.sp, color = Color.White); Text("${item.unit}${if (item.remarks.isNotEmpty()) " - ${item.remarks}" else ""}", fontSize = 8.sp, color = Color.Gray) } }; Divider(color = Color(0xFF2A3755), thickness = 0.5.dp) }
                    }
                    item { Row(Modifier.fillMaxWidth().background(Color(0xFF1A2E1A)).padding(6.dp), horizontalArrangement = Arrangement.End) { Text("SECTION TOTAL: ${"%.2f".format(totalQty)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) }; Spacer(Modifier.height(8.dp)) }
                }
            }
            item { val grandTotal = data.sections.sumOf { sec -> sec.items.filter { !it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 } - sec.items.filter { it.isDeduction }.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 } }; Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E))) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("GRAND TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White); Text("${"%.2f".format(grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, fontFamily = FontFamily.Monospace) } } }
            item { Spacer(Modifier.height(24.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Divider(color = Color.Gray, modifier = Modifier.width(120.dp)); Text("Quantity Surveyor", fontSize = 10.sp, color = Color.Gray); Text("Date: _______________", fontSize = 9.sp, color = Color.Gray) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Divider(color = Color.Gray, modifier = Modifier.width(120.dp)); Text("Checked by", fontSize = 10.sp, color = Color.Gray); Text("Date: _______________", fontSize = 9.sp, color = Color.Gray) } } }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

fun generateTakingOffPDF(data: QTOData, ctx: Context): File {
    val pdf = PdfDocument(); val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create(); val page = pdf.startPage(pageInfo); val canvas = page.canvas
    val bold = Paint().apply { color = android.graphics.Color.BLACK; textSize = 16f; typeface = Typeface.DEFAULT_BOLD }; val normal = Paint().apply { color = android.graphics.Color.BLACK; textSize = 10f }; val small = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 8f }; var y = 30f
    canvas.drawText("QUANTITY TAKE-OFF SHEET", 30f, y, bold); y += 20f; canvas.drawText("Republic of Kenya", 30f, y, small); y += 16f
    canvas.drawText("Project: ${data.projectName.ifEmpty { "________________" }}", 30f, y, normal); y += 14f
    canvas.drawText("Sheet ${data.sheetNo} of ${data.totalSheets} | ${data.date}", 30f, y, small); y += 20f
    data.sections.forEach { section -> if (section.items.isNotEmpty()) { canvas.drawText(section.title.uppercase(), 30f, y, bold); y += 16f; section.items.forEach { item -> val dims = "${item.length} × ${item.width} × ${item.height}"; canvas.drawText("$dims = ${item.quantity} ${item.unit}", 30f, y, normal); canvas.drawText((if (item.isDeduction) "(LESS) " else "") + item.description.take(80), 200f, y, small); y += 12f } } }
    pdf.finishPage(page); val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QTO-${SimpleDateFormat("yyyyMMdd").format(Date())}.pdf"); pdf.writeTo(FileOutputStream(file)); pdf.close(); return file
}

@Composable
fun SettingsScreen(data: QTOData, onUpdate: (QTOData) -> Unit) {
    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚙️ Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
            OutlinedTextField(data.projectName, { onUpdate(data.copy(projectName = it)) }, label = { Text("Project Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(data.contractNo, { onUpdate(data.copy(contractNo = it)) }, label = { Text("Contract Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(data.sheetNo, { onUpdate(data.copy(sheetNo = it)) }, label = { Text("Sheet Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(data.totalSheets, { onUpdate(data.copy(totalSheets = it)) }, label = { Text("Total Sheets") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }
}
