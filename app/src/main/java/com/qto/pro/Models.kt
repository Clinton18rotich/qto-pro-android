package com.qto.pro

import java.text.SimpleDateFormat
import java.util.*

data class QTOData(
    val projectName: String = "",
    val contractNo: String = "",
    val sheetNo: String = "1",
    val totalSheets: String = "1",
    val date: String = SimpleDateFormat("dd/MM/yyyy").format(Date()),
    val sections: MutableList<TakingOffSection> = mutableListOf()
)

data class TakingOffSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val items: MutableList<TakingOffItem> = mutableListOf()
)

data class TakingOffItem(
    val id: String = UUID.randomUUID().toString(),
    val itemNo: String = "",
    val description: String = "",
    val unit: String = "m³",
    val length: String = "",
    val width: String = "",
    val height: String = "",
    val wasteMultiplier: String = "",
    val crossRef: String = "",
    val isDeduction: Boolean = false,
    val sketchPath: String = "",
    val quantity: String = "",
    val remarks: String = ""
)

val KENYAN_SECTIONS = listOf(
    "PRELIMINARIES & GENERAL",
    "SITE WORKS - DRAINAGE",
    "SITE WORKS - PAVING",
    "SITE WORKS - FENCING",
    "SUBSTRUCTURE - EXCAVATION",
    "SUBSTRUCTURE - CONCRETE",
    "SUPERSTRUCTURE - CONCRETE FRAME",
    "SUPERSTRUCTURE - WALLING",
    "ROOFING & CEILINGS",
    "WATERPROOFING",
    "PLUMBING & DRAINAGE",
    "ELECTRICAL INSTALLATIONS",
    "PLASTERING & SCREEDING",
    "TILING & FINISHES",
    "JOINERY (DOORS & WINDOWS)",
    "PAINTING & DECORATIONS",
    "EXTERNAL WORKS",
    "LANDSCAPING",
    "MECHANICAL SERVICES",
    "FIRE SERVICES"
)

val KENYAN_UNITS = listOf("m³", "m²", "m", "No.", "Tonnes", "kg", "Litres", "Pairs", "Sets", "Rolls", "Lengths", "Hours", "Days")

fun calculateQuantity(item: TakingOffItem): Double {
    val l = item.length.toDoubleOrNull() ?: 0.0
    val w = item.width.toDoubleOrNull() ?: 0.0
    val h = item.height.toDoubleOrNull() ?: 0.0
    val waste = item.wasteMultiplier.toDoubleOrNull() ?: 1.0
    val base = when {
        item.unit in listOf("m³", "Tonnes") -> l * w * h
        item.unit == "m²" -> l * w
        else -> l
    }
    return base * if (waste > 0) waste else 1.0
}

fun formatQuantity(qty: Double): String {
    return String.format("%.3f", qty)
}
