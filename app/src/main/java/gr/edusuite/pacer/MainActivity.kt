package gr.edusuite.pacer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import gr.edusuite.pacer.model.ThemeMode
import gr.edusuite.pacer.ui.PacerApp
import gr.edusuite.pacer.ui.PacerViewModel
import gr.edusuite.pacer.ui.theme.PacerTheme

class MainActivity : ComponentActivity() {
    private val vm: PacerViewModel by viewModels {
        val app = application as PacerApplication
        PacerViewModel.Factory(app.store, app.settings, app.updates)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsState()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status bar icons follow the app's theme, not the system's, when the two differ.
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            PacerTheme(dark = dark, palette = settings.palette) { PacerApp(vm) }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.flush()
    }
}
