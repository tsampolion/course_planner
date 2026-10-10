package com.coursebook.pacer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.coursebook.pacer.ui.PacerApp
import com.coursebook.pacer.ui.PacerViewModel
import com.coursebook.pacer.ui.theme.PacerTheme

class MainActivity : ComponentActivity() {
    private val vm: PacerViewModel by viewModels { PacerViewModel.Factory((application as PacerApplication).store) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { PacerTheme { PacerApp(vm) } }
    }

    override fun onStop() {
        super.onStop()
        vm.flush()
    }
}
