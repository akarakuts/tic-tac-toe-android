# tic-tac-toe-android

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Полная версия на английском: [`README.md`](README.md).

Порт игры **«Крестики-нолики»** (линия из *n* подряд на квадратном поле) для **Android**, выровненный по логике с версией для **macOS / Swift**: те же правила, ИИ, разблокировки тем и идея палитр — на **Kotlin**, **Jetpack Compose** и **Navigation Compose**.

## Возможности (как на macOS)

- **Размер поля** 3×3 … 6×6; **длина линии для победы** 3 … 6 (не больше стороны поля).
- **Два игрока** на одном устройстве или **против компьютера** (минимакс + α–β, ограничения сложности, тактика выигрыша/блока).
- **Сложность ИИ** Легко / Средне / Сложно / **Эксперт** (непобедим на 3×3: полный минимакс + таблица транспозиций; на больших полях — глубокий поиск в рамках бюджета времени) — сохраняется между запусками.
- **Темы доски** (Классика, Аврора, Роща, Закат, **Океан**), светлая/тёмная системная схема и акцент из **Material** (динамический / primary).
- **Разблокировки** (только в партиях против ИИ): Аврора — первая победа; Роща — 5 побед; Закат — лучшая серия ≥ 3; **Океан — 10 побед**.
- **Статистика** против ИИ **и для двух игроков** (крестики / нолики / ничьи) — вкладка **«Очки»**.
- **Режим серии** (до 2/3/4/6 побед = best-of 3/5/7/11) — матч идёт, пока одна сторона не возьмёт нужное число партий.
- **Достижения** — 7 локальных достижений (первая победа, серия, 5/10 побед, победа над Экспертом, все темы, чемпион серии) на вкладке **«Очки»**; вычисляются из статистики, без аккаунта.
- **Two-device play (Bluetooth)** — создайте или присоединитесь к партии через классический Bluetooth RFCOMM; рукопожатие config/ack, синхронизация ходов, обработка разрыва. См. [docs/MULTIPLAYER_PROTOCOL.md](docs/MULTIPLAYER_PROTOCOL.md).
- **Поделиться результатом** — отправьте завершённую партию (ASCII-доска + исход) в любое приложение через системный share sheet.
- **Виджет главного экрана** — название приложения, ваши победы/серия против ИИ и кнопка «Новая игра».
- **Отмена хода** (возвращает и ответ ИИ) и **подсказка** (кратко подсвечивает рекомендованный ход; отключается в Настройках).
- **Онбординг при первом запуске** — короткая вступительная лента (правила, режимы, разблокировки тем, статистика); повторяется из Настроек.
- **Язык в приложении** (Системный / English / Русский) через AppCompat per-app locales.
- **Звук** — короткие синтезированные сигналы (ход человека / ИИ / победа / поражение / разблокировка темы / неверный ход); **слайдер громкости** + переключатель, сохраняются.
- **Настройки партии** (поле, линия, режим, сторона) — сохраняются между запусками.
- **Интерфейс** — пять вкладок: **Игра**, **Мультиплеер**, **Настройки**, **Очки** (статистика vs-ИИ, счёт двух игроков, достижения), **О приложении** (версия, поддержка, [политика конфиденциальности](docs/privacy.ru.md)). Нижняя навигация; доска масштабируется через **`BoxWithConstraints`** (на широких экранах — две колонки).
- **Доступность** — подписи TalkBack для навигации и клеток доски; зоны нажатия ≥44dp; векторные фигуры, анимированная линия победы и конфетти при выигрыше.

## Архитектура

- **Состояние**: UI рендерится из единого `StateFlow<GameState>` (один иммутабельный снапшот на рекомпозицию); экраны собирают его через `collectAsStateWithLifecycle()`.
- **Хранилище**: Jetpack **DataStore Preferences** (JSON-полезные данные); при первом чтении мигрирует со старого SharedPreferences. Схема версионирована с автоматическими миграциями (сейчас **v8**).
- **ИИ**: минимакс + α–β на чистом Kotlin с таблицей транспозиций по Zobrist-хешу; Эксперт использует итеративное углубление в рамках бюджета времени.
- **Мультиплеер**: устройственно-независимый интерфейс `Transport` + JSON-протокол (`MultiplayerMessage`); `BluetoothTransport` — офлайн-реализация на RFCOMM.

## Отличия от macOS

| macOS | Android |
|-------|---------|
| SpriteKit | Compose |
| `UserDefaults` | **DataStore Preferences** + JSON (`JSONObject`, миграция со SharedPreferences) |
| String Catalog | `res/values/strings.xml`, `values-ru/` (+ переключатель языка в приложении) |
| Одно окно с HUD | `NavHost` + `Scaffold` |

Зависимости см. в `app/build.gradle.kts` (**Navigation Compose**, **ViewModel**, **Coroutines**, **AppCompat** (локали), **DataStore**). Тесты покрывают правила, ИИ (включая паритет Эксперта), JSON прогресса и инварианты `GameState`.

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

Храните **`upload-keystore.jks`** и пароли надёжно и **сделайте резервную копию** — без файла нельзя выпускать совместимые обновления. Ключ и артефакты RuStore лежат в **`../rustore/tic-tac-toe/`** (не в корне `rustore/` и не в `rustore/checkers/`).

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

### Иконка на витрине RuStore (модерация)

Если иконка в консоли RuStore **не совпадает** с иконкой установленного приложения, модерация отклоняет заявку. Используйте эталон из репозитория: **[`docs/rustore/ic_launcher_store_512.png`](docs/rustore/ic_launcher_store_512.png)** — тот же макет, что в APK (см. [`docs/rustore/README.ru.md`](docs/rustore/README.ru.md)). Общие требования RuStore: [требования к приложениям](https://help.rustore.ru/rustore/for_developers/publishing_and_verifying_apps/requirement_apps).

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

Строки в `app/src/main/res/values/strings.xml` и переводы в каталогах `values-xx` (например `values-ru`). По умолчанию учитывается **системный язык**, но пользователь может переопределить его в **Настройки → Язык** (Системный / English / Русский) через AppCompat per-app locales; выбор сохраняется.

## Структура проекта

| Путь | Назначение |
|------|------------|
| `app/src/main/java/.../game/` | Модель поля, ИИ, типы |
| `app/src/main/java/.../progress/` | Прогресс и сохранение |
| `app/src/main/java/.../theme/` | Палитры тем |
| `app/src/main/java/.../audio/` | Звук (`AudioTrack`) |
| `app/src/main/java/.../ui/` | ViewModel, навигация, экраны, доска, общие виджеты HUD |

## Тесты

Юнит-тесты в `app/src/test` (правила, ИИ, протокол, ViewModel, DataStore). Compose smoke-тест онбординга — в `app/src/androidTest`. Запуск:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

## Лицензия

Программа распространяется на условиях **GNU GPLv3** (или любой более поздней версии на ваш выбор). Полный текст — в [`LICENSE`](LICENSE), тот же тип лицензии, что и у macOS-проекта.
