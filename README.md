# tic-tac-toe-android

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Russian / Русский: [README.ru.md](README.ru.md)

Android port of the **tic-tac-toe** game (n-in-a-row on a square grid), aligned with the [macOS / Swift version](https://github.com/akarakuts/tic-tac-toe): same rules, AI behaviour, unlock logic, and theme ideas — implemented with **Kotlin**, **Jetpack Compose**, and **Navigation Compose**.

## Features (parity with macOS)

- **Board size** 3×3 … 6×6; **win line length** 3 … 6 (cannot exceed board edge).
- **Two players** on one device or **vs computer** (minimax + α–β, difficulty caps, win/block tactics).
- **AI difficulty** Easy / Medium / Hard — persisted like on macOS.
- **Board themes** (Classic, Aurora, Grove, Ember) with palettes tuned for light/dark system theme + Material **dynamic / primary** accent.
- **Unlocks** (vs AI only): Aurora — 1st win; Grove — 5 wins; Ember — best streak ≥ 3.
- **Statistics** vs AI (wins / losses / draws, streak, best streak) — see **Scores** tab.
- **Sound** — short synthesized tones (move / win / invalid); toggle persisted.
- **UI** — **three destinations**: **Play** (board + status + New game), **Settings** (rules & appearance), **Scores** (stats + theme legend). Bottom navigation + **Exit** in the top bar (`Activity.finish()`). Layout uses **`BoxWithConstraints`** where needed so the board scales on phones and tablets.

## Android-specific notes

| macOS | Android |
|-------|---------|
| SpriteKit scene | Compose UI |
| `UserDefaults` + JSON | `SharedPreferences` + `JSONObject` |
| `Localizable.xcstrings` | `res/values/strings.xml`, `res/values-ru/strings.xml` |
| Single window HUD | `NavHost` + `Scaffold` + bottom bar |

Third-party libraries include **Navigation Compose**, **Lifecycle + ViewModel**, **Coroutines**, and **Material Icons Extended** (see `app/build.gradle.kts`).

## Requirements

- **JDK 11+** (as in `app/build.gradle.kts`)
- **Android SDK** with API **36** compile SDK (project template); **minSdk 24**
- **Android Studio** Ladybug+ or CLI **Gradle 8+/9+**

## CI & automation

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [CI](.github/workflows/ci.yml) | push / PR to `main`, manual | `:app:check` (unit tests, Lint, compile) |
| [Security](.github/workflows/security.yml) | push / PR to `main`, weekly | OSV dependency scan, CodeQL |
| [Release](.github/workflows/release.yml) | tag `v*` | Signed release APK + GitHub Release |

[Dependabot](.github/dependabot.yml) opens weekly PRs for Gradle and GitHub Actions dependencies.

## Build & run

Open the project root in Android Studio and run the **app** configuration, or from a terminal:

```bash
cd tic-tac-toe-android
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # device or emulator required
```

Start the main activity (if `adb` is on `PATH`):

```bash
adb shell am start -n com.example.tic_tac_toe/.MainActivity
```

Release builds: `./gradlew :app:assembleRelease`. The `release` build type is signed with the **debug keystore** so APKs (including GitHub Actions artifacts) are **sideloadable**; Google Play requires your own **upload keystore** in `signingConfigs` instead.

## GitHub Releases

Tagged pushes (`v*`) build a **signed** `assembleRelease` APK and attach it to the GitHub Release (debug keystore — suitable for sideloading, not for Play Console upload as-is).

## Localization

Add or edit `app/src/main/res/values/strings.xml` (default) and `values-ru/` (or other `values-xx` folders). The app picks strings from resources according to **system locale** (no in-app language switch).

## Project layout

| Path | Role |
|------|------|
| `app/src/main/java/.../game/` | `GameModel`, `TicTacToeAI`, enums — pure Kotlin rules & AI |
| `app/src/main/java/.../progress/` | `GameProgress`, `GameProgressStore` — persistence |
| `app/src/main/java/.../theme/BoardPalette.kt` | Theme palettes (ported from macOS colour intent) |
| `app/src/main/java/.../audio/GameSoundFx.kt` | `AudioTrack` tone synthesis |
| `app/src/main/java/.../ui/GameViewModel.kt` | Game + settings state, AI coroutine, sound hooks |
| `app/src/main/java/.../ui/TicTacToeApp.kt` | Navigation, scaffold, top/bottom bars |
| `app/src/main/java/.../ui/PlayScreen.kt` | Game screen |
| `app/src/main/java/.../ui/SettingsScreen.kt` | Settings screen |
| `app/src/main/java/.../ui/ScoresScreen.kt` | Scores screen |
| `app/src/main/java/.../ui/GameBoard.kt` | Board rendering |
| `app/src/main/java/.../ui/HudComponents.kt` | Shared pills, buttons, grids |

## Testing

Template unit/instrumentation tests live under `app/src/test` and `app/src/androidTest`. Extend them to cover `GameModel` / AI parity with the macOS test suite if you need regression safety.

## License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License** as published by the Free Software Foundation, either **version 3** of the License, or (at your option) any later version.

See the [`LICENSE`](LICENSE) file for the complete GPLv3 text (same license family as the macOS project).
