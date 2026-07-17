# What's New — 1.7.0 (versionCode 19)

## RuStore (short, plain text)

What's new in 1.7.0

Play a friend on two phones over Bluetooth: host or join a match — moves stay in sync. Bluetooth is used only for play; no location access, no servers.

Also since the last store version:
• Expert AI difficulty and Ocean board theme
• Best-of-N series mode
• Local achievements on the Scores tab
• Share finished game results
• Home-screen widget
• Undo and hints
• Onboarding, in-app language, sound volume
• Two-player stats and win animation

## GitHub Release

### Tic-Tac-Toe Android 1.7.0 (versionCode 19)

Headline: **Bluetooth multiplayer MVP** — host/join, config/ack handshake, synced moves, and a dedicated game screen. `BLUETOOTH_SCAN` uses `neverForLocation` (no location access).

**Multiplayer**
- Multiplayer tab: host / join / cancel
- RFCOMM transport + JSON wire protocol (config, ack, move, new_game)
- Move sync and disconnect handling

**Gameplay** (accumulated since v1.2.0)
- Expert AI, Ocean theme, best-of-N series
- 7 local achievements, share result, home-screen widget
- Undo, hints, onboarding, language (System/EN/RU), SFX volume
- Two-player stats, vector marks, win-line animation + confetti

**Quality**
- Crash fix: `MultiplayerViewModel` Application-only constructor for default `viewModel()` factory
- Polish: `StatusTextResolver`, `AiCoordinator`, `SoundPlayer`
- ~82 unit tests, detekt, R8 keep rules for multiplayer
- Docs: ARCHITECTURE, MULTIPLAYER_PROTOCOL, CONTRIBUTING, privacy (Bluetooth)

**Full Changelog**: https://github.com/akarakuts/tic-tac-toe-android/compare/v1.2.0...v1.7.0
