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
| [Release](.github/workflows/release.yml) | тег `v*` | Подписанные **APK + AAB** (upload keystore из секретов) + GitHub Release |

[Dependabot](.github/dependabot.yml) — еженедельные PR с обновлениями Gradle и GitHub Actions.

## Подпись релиза (RuStore / GitHub Actions)

RuStore и аналогичные витрины ожидают **релизную сборку с вашим upload-ключом** (часто загружают **AAB**). Официальные шаги RuStore (сертификат, загрузка AAB и т.п.) — в их справке, например [загрузка AAB / подпись](https://www.rustore.ru/help/developers/publishing-and-verifying-apps/app-publication/new-version-app/upload-aab).

### 1. Создать keystore (один раз)

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Храните **`upload-keystore.jks`** и пароли надёжно и **сделайте резервную копию** — без файла нельзя выпускать совместимые обновления.

### 2. Локальная подпись `release`

1. Скопируйте [`keystore.properties.example`](keystore.properties.example) в **`keystore.properties`** в **корне репозитория** (файл в `.gitignore`).
2. Укажите `storeFile`, пароли и `keyAlias` как в вашем keystore.
3. Сборка:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

Артефакты: `app/build/outputs/apk/release/*.apk` и `app/build/outputs/bundle/release/*.aab`.

Если **`keystore.properties` нет**, `release` подписывается **debug-ключом** (удобно для клона без секретов) — **такую сборку нельзя** отдавать в RuStore как финальную.

### 3. Секреты GitHub для тегов `v*`

В настройках репозитория (**Settings → Secrets and variables → Actions**):

| Secret | Значение |
|--------|----------|
| `RELEASE_KEYSTORE_BASE64` | Base64 файла `upload-keystore.jks` (например macOS: `base64 -i upload-keystore.jks \| tr -d '\n'`) |
| `RELEASE_STORE_PASSWORD` | Пароль keystore |
| `RELEASE_KEY_ALIAS` | Alias ключа (например `upload`) |
| `RELEASE_KEY_PASSWORD` | Пароль ключа |

Workflow [Release](.github/workflows/release.yml) создаёт на раннере `keystore.properties` и `upload-keystore.jks`, собирает **`assembleRelease`** и **`bundleRelease`**, прикладывает к GitHub Release **`.apk`** и **`.aab`**. Если секрет не задан — workflow **завершится с ошибкой** (без «тихой» debug-подписи для магазина).

## Сборка и запуск

```bash
cd tic-tac-toe-android
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # нужны эмулятор или устройство
adb shell am start -n ru.akarakuts.tictactoe/.MainActivity
```

Отладочная установка: `./gradlew :app:installDebug`. Для **магазинной** подписи см. [Подпись релиза (RuStore / GitHub Actions)](#подпись-релиза-rustore--github-actions).

## Релизы на GitHub

Пуш тега `v*` запускает Release: **APK + AAB** с **upload-keystore** из секретов GitHub. Без секретов job завершится ошибкой (см. таблицу выше).

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
