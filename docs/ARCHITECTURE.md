# Architecture — tic-tac-toe-android

Concise map of how the app is structured.

## Layers

| Layer | Packages | Responsibility |
|-------|----------|----------------|
| **UI** | `ui/` | Compose screens, `GameViewModel`, `MultiplayerViewModel`, navigation |
| **Game rules** | `game/` | Pure Kotlin: `GameModel`, `TicTacToeAI`, enums |
| **Persistence** | `progress/` | `GameProgress`, DataStore + JSON (`GameProgressStore`) |
| **Multiplayer** | `multiplayer/` | `Transport`, `BluetoothTransport`, `MultiplayerMessage` |
| **Audio** | `audio/` | `SoundPlayer` + `GameSoundFx` synthesis |
| **Theme** | `theme/` | Board palettes per visual style |

## State

- **Single-player**: one `StateFlow<GameState>` from `GameViewModel`; screens collect via `collectAsStateWithLifecycle()`.
- **Bluetooth match**: `MultiplayerViewModel` holds `MultiplayerSessionState` (transport + board); lobby and in-match UI share the same ViewModel instance at app scope.

## AI

`AiCoordinator` schedules minimax moves off the main thread; `GameViewModel` owns epoch cancellation for undo/new-game races.

## Persistence

`GameProgressStore` writes JSON to DataStore Preferences; legacy SharedPreferences migrated once on first read.

## Navigation

`TicTacToeApp` — `Scaffold` + bottom bar (5 tabs) + `NavHost`. Multiplayer in-match view replaces lobby content on the same tab when `MultiplayerPhase.Playing`.

## Testing

Unit tests: game rules, AI, protocol, ViewModels (Robolectric + `FakeTransport`). Instrumentation: Compose smoke tests under `androidTest`.
