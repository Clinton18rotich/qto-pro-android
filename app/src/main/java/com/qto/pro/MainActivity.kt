package com.qto.pro

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = DataStore.load(this)
        setContent {
            var appData by remember { mutableStateOf(data) }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF2196F3),
                background = Color(0xFF0A1628),
                surface = Color(0xFF1A2744)
            )) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(appData, { appData = it; DataStore.save(this@MainActivity, it) }, this)
                }
            }
        }
    }
}

object DataStore {
    private val gson = Gson()
    fun save(ctx: Context, data: QTOData) = ctx.getSharedPreferences("qto", 0).edit().putString("data", gson.toJson(data)).apply()
    fun load(ctx: Context): QTOData = try {
        gson.fromJson(ctx.getSharedPreferences("qto", 0).getString("data", "{}"), QTOData::class.java)
    } catch (e: Exception) {
        QTOData()
    }
}
