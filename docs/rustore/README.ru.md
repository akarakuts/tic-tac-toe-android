# RuStore: иконка витрины = иконка в APK

Модератор RuStore сравнивает **иконку карточки приложения** с **иконкой после установки**. Они должны совпадать.

## Что использовать в консоли

1. Соберите актуальные ресурсы (или возьмите уже сгенерированный файл из репозитория):

   ```bash
   cd tic-tac-toe-android
   .venv-icon/bin/python scripts/generate_launcher_icons.py
   ```

2. В консоли RuStore для **иконки приложения / витрины** загрузите **ровно** файл:

   **`docs/rustore/ic_launcher_store_512.png`**

   Это тот же макет (сетка 3×3, X и O), что и в `mipmap-*` и в adaptive-icon (`ic_launcher_background` + `ic_launcher_foreground`).

3. Не подставляйте другие картинки (логотип «от себя», старый скриншот и т.д.) — иначе снова будет расхождение с установленным приложением.

## Требования RuStore

Общие правила публикации: [Требования к приложениям](https://help.rustore.ru/rustore/for_developers/publishing_and_verifying_apps/requirement_apps).

После смены иконки увеличьте `versionCode` / `versionName` в `app/build.gradle.kts`, соберите новый AAB/APK и отправьте версию на модерацию заново.
