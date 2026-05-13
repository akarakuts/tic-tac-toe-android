# tic-tac-toe-android

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Полная версия на английском: [`README.md`](README.md).

Порт игры **«Крестики-нолики»** (линия из *n* подряд на квадратном поле) для **Android**, выровненный по логике с версией для **macOS / Swift**: те же правила, ИИ, разблокировки тем и идея палитр — на **Kotlin**, **Jetpack Compose** и **Navigation Compose**.

## Возможности (как на macOS)

- **Размер поля** 3×3 … 6×6; **длина линии для победы** 3 … 6 (не больше стороны поля).
- **Два игрока** на одном устройстве или **против компьютера** (минимакс + α–β, ограничения сложности, тактика выигрыша/блока).
- **Сложность ИИ** Легко / Средне / Сложно — сохраняется между запусками.
- **Темы доски** (Классика, Аврора, Роща, Закат), светлая/тёмная системная схема и акцент из **Material** (динамический / primary).
- **Разблокировки** (только в партиях против ИИ): Аврора — первая победа; Роща — 5 побед; Закат — лучшая серия ≥ 3.
- **Статистика** против ИИ — вкладка **«Очки»**.
- **Звук** — короткие синтезированные сигналы; переключатель сохраняется.
- **Интерфейс** — три экрана: **Игра** (поле, статус, «Новая игра»), **Настройки**, **Очки**. Нижняя панель навигации и пункт **«Выход»** в верхней панели (закрывает активность). Доска подстраивается под размер экрана через **`BoxWithConstraints`**.

## Отличия от macOS

| macOS | Android |
|-------|---------|
| SpriteKit | Compose |
| `UserDefaults` | `SharedPreferences` + JSON (`JSONObject`) |
| String Catalog | `res/values/strings.xml`, `values-ru/` |
| Одно окно с HUD | `NavHost` + `Scaffold` |

Зависимости см. в `app/build.gradle.kts` (**Navigation Compose**, **ViewModel**, **Coroutines**, **Material Icons Extended**).

## Требования

- **JDK 11+**
- **Android SDK**, **minSdk 24**, целевой/compile SDK — как в модуле `app`
- **Android Studio** или Gradle в терминале

## CI и автоматизация

| Workflow | Когда | Задача |
|----------|--------|--------|
| [CI](.github/workflows/ci.yml) | push / PR в `main`, вручную | `:app:check` (юнит-тесты, Lint, сборка) |
| [Security](.github/workflows/security.yml) | push / PR в `main`, раз в неделю | OSV, CodeQL |
| [Release](.github/workflows/release.yml) | тег `v*` | APK релиза + GitHub Release |

[Dependabot](.github/dependabot.yml) — еженедельные PR с обновлениями Gradle и GitHub Actions.

## Сборка и запуск

```bash
cd tic-tac-toe-android
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # нужны эмулятор или устройство
adb shell am start -n com.example.tic_tac_toe/.MainActivity
```

Релиз: `./gradlew :app:assembleRelease`. Сборка `release` подписывается **debug-ключом**, чтобы APK (в т.ч. из GitHub Actions) можно было **установить вручную**; для **Google Play** нужен свой upload keystore в `signingConfigs`.

## Релизы на GitHub

Пуш тега `v*` собирает подписанный `assembleRelease` APK и прикладывает его к GitHub Release (тот же debug-сертификат, что и у локальной `assembleRelease`; для загрузки в Play Console нужен свой keystore).

## Локализация

Строки в `app/src/main/res/values/strings.xml` и переводы в каталогах `values-xx` (например `values-ru`). Язык берётся из **системных настроек** устройства.

## Структура проекта

| Путь | Назначение |
|------|------------|
| `app/src/main/java/.../game/` | Модель поля, ИИ, типы |
| `app/src/main/java/.../progress/` | Прогресс и сохранение |
| `app/src/main/java/.../theme/` | Палитры тем |
| `app/src/main/java/.../audio/` | Звук (`AudioTrack`) |
| `app/src/main/java/.../ui/` | ViewModel, навигация, экраны, доска, общие виджеты HUD |

## Тесты

Заготовки лежат в `app/src/test` и `app/src/androidTest`; при необходимости добавьте тесты правил и ИИ по аналогии с macOS-таргетом **tic-tac-toeTests**.

## Лицензия

Программа распространяется на условиях **GNU GPLv3** (или любой более поздней версии на ваш выбор). Полный текст — в [`LICENSE`](LICENSE), тот же тип лицензии, что и у macOS-проекта.
