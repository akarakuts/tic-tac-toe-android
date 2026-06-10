package ru.akarakuts.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.akarakuts.tictactoe.ui.GameViewModel
import ru.akarakuts.tictactoe.ui.TicTacToeApp
import ru.akarakuts.tictactoe.ui.theme.TictactoeTheme

/** Single-activity entry: edge-to-edge Compose, [GameViewModel], root [TicTacToeApp]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application
        setContent {
            TictactoeTheme {
                val vm: GameViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            GameViewModel(app, createSavedStateHandle())
                        }
                    },
                )
                Surface(modifier = Modifier.fillMaxSize()) {
                    TicTacToeApp(vm)
                }
            }
        }
    }
}
