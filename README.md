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
| [Release](.github/workflows/release.yml) | tag `v*` | Upload-keystore–signed **APK + AAB** + GitHub Release (requires secrets) |

[Dependabot](.github/dependabot.yml) opens weekly PRs for Gradle and GitHub Actions dependencies.

## Release signing (RuStore / GitHub Actions)

RuStore and similar stores expect a **release build signed with your upload key** (AAB is typical for publication). Official RuStore steps (certificate / AAB upload) are in their help centre, e.g. [upload AAB / signing](https://www.rustore.ru/help/developers/publishing-and-verifying-apps/app-publication/new-version-app/upload-aab).

### 1. Create an upload keystore (once)

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Keep **`upload-keystore.jks`** and passwords in a password manager; **back up** the file — without it you cannot ship compatible updates.

### 2. Local signed `release` builds

1. Copy [`keystore.properties.example`](keystore.properties.example) to **`keystore.properties`** in the **repository root** (this file is gitignored).
2. Set `storeFile`, passwords, and `keyAlias` to match your keystore.
3. Run:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

Outputs: `app/build/outputs/apk/release/*.apk` and `app/build/outputs/bundle/release/*.aab`.

If **`keystore.properties` is missing**, `release` still signs with the **debug** keystore so the project builds on fresh clones — **do not** upload that build to RuStore.

### 3. GitHub Actions tag releases (`v*`)

Configure these **repository secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|--------|
| `RELEASE_KEYSTORE_BASE64` | Base64 of `upload-keystore.jks` (e.g. `base64 -i upload-keystore.jks \| tr -d '\n'` on macOS) |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias (e.g. `upload`) |
| `RELEASE_KEY_PASSWORD` | Key password |

The [Release](.github/workflows/release.yml) workflow writes `keystore.properties` and `upload-keystore.jks` on the runner, then runs **`assembleRelease`** and **`bundleRelease`**, and attaches **`.apk`** and **`.aab`** to the GitHub Release. If any secret is missing, the workflow **fails** with an error message (no silent debug-signed store builds).

## Build & run

Open the project root in Android Studio and run the **app** configuration, or from a terminal:

```bash
cd tic-tac-toe-android
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # device or emulator required
```

Start the main activity (if `adb` is on `PATH`):

```bash
adb shell am start -n ru.akarakuts.tictactoe/.MainActivity
```

Debug install: `./gradlew :app:installDebug`. For **store-ready** signed builds, see [Release signing (RuStore / GitHub Actions)](#release-signing-rustore--github-actions).

## GitHub Releases

Tagged pushes (`v*`) run the Release workflow: **APK + AAB** signed with your **upload keystore** from GitHub secrets. Without secrets the workflow fails on purpose (see table above).

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
