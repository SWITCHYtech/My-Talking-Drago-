package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.firebase.configureAppCheck
import com.example.ui.screens.DragosHomeScreen
import com.example.ui.theme.DragosPetTheme
import com.example.viewmodel.DragosViewModel

class MainActivity : ComponentActivity() {

    private val dragosViewModel: DragosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            DragosPetTheme {
                DragosHomeScreen(
                    viewModel = dragosViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
