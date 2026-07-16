# Contributing — tic-tac-toe-android

Thank you for improving the project. This guide covers local setup, automated checks, and manual Bluetooth QA.

## Prerequisites

- **JDK 17** (Gradle toolchains resolve it automatically if missing)
- **Android SDK** with API 36 platform and build-tools
- Android Studio Ladybug or newer (optional but recommended)

Clone the repo and open it in Android Studio, or work from the command line.

## Build and run

```bash
./gradlew :app:assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew :app:installDebug
adb shell am start -n ru.akarakuts.tictactoe/.MainActivity
```

Release build (uses debug signing when `keystore.properties` is absent):

```bash
./gradlew :app:assembleRelease
```

Signed AAB for store upload — see `docs/RELEASE_CHECKLIST.md` and the RuStore keystore path in `.cursor/rules/rustore-artifacts.mdc`.

## Automated quality gate

Run the full check suite before opening a PR or tagging a release:

```bash
./gradlew :app:check
```

This runs:

| Task | What it checks |
|------|----------------|
| `:app:testDebugUnitTest` | Unit tests (Robolectric, coroutines test, MockK) |
| `:app:lintDebug` | Android Lint |
| `:app:ktlintCheck` | Kotlin style (`.editorconfig`) |
| `:app:detekt` | Static analysis (`detekt.yml`) |

Run a single layer when iterating:

```bash
./gradlew :app:testDebugUnitTest          # unit tests only
./gradlew :app:ktlintCheck :app:detekt    # style + detekt
./gradlew :app:lintDebug                  # Android Lint
```

Compose instrumentation tests (device/emulator required):

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Project layout

See [ARCHITECTURE.md](ARCHITECTURE.md) for layers, state flow, and multiplayer overview.  
Bluetooth wire format: [MULTIPLAYER_PROTOCOL.md](MULTIPLAYER_PROTOCOL.md).

## Code style

- Kotlin official style; **4 spaces**, **LF**, **140** char lines — enforced via `.editorconfig` and ktlint.
- New user-visible strings: add to `app/src/main/res/values/strings.xml` **and** `values-ru/strings.xml`.
- Keep game rules pure in `game/`; UI and ViewModels in `ui/`.

## Manual QA — Bluetooth multiplayer (two devices)

Use two physical Android phones with Bluetooth enabled. Emulators do not support real BT pairing for this flow.

### Preparation

1. Install the same debug or release build on **both** devices.
2. On each device: **Settings → Bluetooth** — turn on, make device discoverable if prompted.
3. Grant **Nearby devices / Bluetooth** permissions when the app asks (Android 12+).
4. Optional: pair the phones in system Bluetooth settings first (not strictly required for the in-app lobby).

### Host flow (device A)

1. Open the app → dismiss onboarding if shown.
2. Bottom nav → **Multiplayer**.
3. Tap **Host game** (or equivalent host action).
4. Confirm status shows hosting / waiting for peer.
5. Note the device name shown in the lobby.

### Join flow (device B)

1. Open the app → **Multiplayer**.
2. Tap **Scan** / refresh device list.
3. Select device A from the list → **Join**.
4. Wait until both screens show **Connected** / in-match UI.

### In-match checks

| Step | Expected |
|------|----------|
| Host moves first (or as per protocol) | Move appears on both boards within ~1 s |
| Guest moves | Turn alternates; illegal cells rejected |
| Win a round | Status shows winner on both devices |
| **New game** / rematch | Board resets on both sides |
| Host disconnects (BT off or app kill) | Guest shows disconnect / return to lobby |
| Rotate one device | UI recovers; match state intact |

### Regression smoke (single device)

After BT QA, on one device verify: **Play** (vs AI + two-player), **Settings**, **Scores**, **About**, sound toggle, theme selection.

## Baseline Profile

Not included in this release. The app is small and cold-start cost is dominated by Compose initialization; a `:baselineprofile` module would add CI/device lab overhead without measurable gain for a ~5-screen game. Revisit if startup traces show >300 ms main-thread work on low-end devices.

## Release

Follow [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) before every store upload.
