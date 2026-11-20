package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppShell
import com.example.ui.theme.DarkGrowthTwinColors
import com.example.ui.theme.GrowthTwinTheme
import com.example.ui.theme.LightGrowthTwinColors
import com.example.ui.viewmodel.GrowthTwinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: GrowthTwinViewModel = viewModel()
            val prefs by vm.prefs.collectAsStateWithLifecycle()

            LaunchedEffect(prefs.darkTheme) {
                val bg = if (prefs.darkTheme) DarkGrowthTwinColors.bg else LightGrowthTwinColors.bg
                val style = if (prefs.darkTheme) {
                    SystemBarStyle.dark(bg.toArgb())
                } else {
                    SystemBarStyle.light(bg.toArgb(), bg.toArgb())
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            GrowthTwinTheme(darkTheme = prefs.darkTheme) {
                AppShell(vm = vm)
            }
        }
    }
}
