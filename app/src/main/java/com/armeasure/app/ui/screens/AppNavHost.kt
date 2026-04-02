package com.armeasure.app.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armeasure.app.viewmodel.MeasureViewModel

sealed class AppScreen { object Splash : AppScreen(); object Measure : AppScreen() }

@Composable
fun AppNavHost(vm: MeasureViewModel = viewModel()) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    when (screen) {
        AppScreen.Splash  -> SplashScreen(onComplete = { screen = AppScreen.Measure })
        AppScreen.Measure -> MainMeasureScreen(vm = vm)
    }
}
