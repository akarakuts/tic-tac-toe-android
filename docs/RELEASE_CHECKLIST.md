# Release checklist — tic-tac-toe-android

Use this list before every store release (RuStore / GitHub Release).

## 1. Version bump

- [x] Update `versionName` and `versionCode` in `app/build.gradle.kts` → **1.7.0 / 19** (2026-07-16)
- [ ] Copy release notes to RuStore console and GitHub Release (if tagging)

## 2. Quality gate

```bash
./gradlew :app:check
```

- [ ] Unit tests pass (`:app:testDebugUnitTest`)
- [ ] Lint passes (`:app:lintDebug`)
- [ ] ktlint passes (`:app:ktlintCheck`)
- [ ] detekt passes (`:app:detekt`)
- [ ] Manual smoke: Play (vs AI + two-player), Settings, Scores, About
- [ ] Manual smoke: Multiplayer tab — host/join on two paired devices (see [CONTRIBUTING.md](CONTRIBUTING.md#manual-qa--bluetooth-multiplayer-two-devices))

For local setup, build commands, and the full BT checklist, see [CONTRIBUTING.md](CONTRIBUTING.md).

## 3. Signed AAB

```bash
./gradlew :app:bundleRelease :app:assembleRelease
```

- [x] `keystore.properties` points to upload keystore (`../rustore/tic-tac-toe/upload-keystore.jks`)
- [x] Output: `app/build/outputs/bundle/release/*.aab`
- [x] Copy AAB to `../rustore/tic-tac-toe/1.7.0/tic-tac-toe-1.7.0-release.aab`

## 4. RuStore upload

- [x] Store icon matches APK: `docs/rustore/ic_launcher_store_512.png` (also copied to rustore handoff)
- [x] Screenshots 1080×1920 → `../rustore/tic-tac-toe/1.7.0/screenshots/`
- [ ] Upload AAB in RuStore console
- [ ] Privacy policy URL / in-app link still valid (`docs/privacy.md`, `docs/privacy.ru.md`)

## 5. Git tag (optional)

```bash
git tag v1.7.0
git push origin v1.7.0
```

Triggers GitHub Release workflow when secrets are configured.

**1.7.0 handoff (outside repo):** `/Users/a.karakuts/Documents/Bars/rustore/tic-tac-toe/1.7.0/` — see `RELEASE_EVIDENCE.txt`.
