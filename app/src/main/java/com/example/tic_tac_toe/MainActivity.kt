package com.example.tic_tac_toe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tic_tac_toe.ui.GameViewModel
import com.example.tic_tac_toe.ui.TicTacToeApp
import com.example.tic_tac_toe.ui.theme.TictactoeTheme

/** Single-activity entry: edge-to-edge Compose, [GameViewModel], root [TicTacToeApp]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TictactoeTheme {
                val vm: GameViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
                )
                Surface(modifier = Modifier.fillMaxSize()) {
                    TicTacToeApp(vm)
                }
            }
        }
    }
}
