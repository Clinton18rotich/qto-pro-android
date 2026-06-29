package com.qto.pro

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(data: QTOData, onUpdate: (QTOData) -> Unit, ctx: Context) {
    val nav = rememberNavController()
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route ?: "takeoff"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentRoute) {
                            "takeoff" -> "📐 Taking Off"
                            "settings" -> "⚙️ Settings"
                            else -> "QTO Pro"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A1628))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xE60A1628)) {
                listOf("takeoff" to "📐", "settings" to "⚙️").forEach { (route, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                nav.navigate(route) {
                                    popUpTo("takeoff") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Text(icon, fontSize = 20.sp) },
                        label = { Text(route.replaceFirstChar { it.uppercase() }, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2196F3),
                            unselectedIconColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "takeoff",
            modifier = Modifier.padding(padding)
        ) {
            composable("takeoff") { TakingOffScreen(data, onUpdate, ctx) }
            composable("settings") { SettingsScreen(data, onUpdate) }
        }
    }
}
