package ru.akarakuts.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.akarakuts.tictactoe.ui.GameViewModel
import ru.akarakuts.tictactoe.ui.TicTacToeApp
import ru.akarakuts.tictactoe.ui.theme.TictactoeTheme

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
