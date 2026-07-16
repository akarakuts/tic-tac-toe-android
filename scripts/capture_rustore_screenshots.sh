#!/usr/bin/env bash
# Скриншоты для RuStore: сначала полный кадр эмулятора, затем без status bar; для витрины — строго 9:16 (портрет).
# RuStore режет кадры не 9:16; допустимые примеры: 720×1280, 1080×1920, 1440×2560.
# https://help.rustore.ru/rustore/for_developers/publishing_and_verifying_apps/requirement_apps
# Готовые к загрузке файлы: ./store_${RUSTORE_W}x${RUSTORE_H}/ (по умолчанию ./store_1080x1920/).
# Другой размер 9:16: RUSTORE_W=1440 RUSTORE_H=2560 ./scripts/capture_rustore_screenshots.sh
# После съёма обрезается системная строка состояния (время, сеть, батарея). Высота по умолчанию — 156 px (Pixel 9 Pro / API 36);
# при другом устройстве: STRIP_STATUS_TOP_PX=… ./scripts/capture_rustore_screenshots.sh
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJ_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT="${PROJ_ROOT}/docs/store-screenshots"
# Пиксели, отрезаемые сверху (status bar). Для 1280×2856 на эмуляторе Pixel 9 Pro совпадает с началом TopAppBar (y=156).
# Medium Phone / API 36 @420dpi ≈ 84; Pixel 9 Pro taller cutout ≈ 156.
STRIP_STATUS_TOP_PX="${STRIP_STATUS_TOP_PX:-84}"
# Целевой кадр для консоли RuStore (ширина×высота, соотношение 9:16).
RUSTORE_W="${RUSTORE_W:-1080}"
RUSTORE_H="${RUSTORE_H:-1920}"
# Вертикальное выравнивание при crop до 9:16: bottom — низ экрана (навигация) не обрезается; top|center|bottom.
RUSTORE_CROP_VERTICAL="${RUSTORE_CROP_VERTICAL:-bottom}"
RUSTORE_OUT="${OUT}/store_${RUSTORE_W}x${RUSTORE_H}"

mkdir -p "$OUT" "$RUSTORE_OUT"

dump_ui() {
  adb shell uiautomator dump /sdcard/uid.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/uid.xml 2>/dev/null || true
}

wait_game_ui() {
  local _i
  for _i in $(seq 1 50); do
    if dump_ui | grep -q "Новая игра"; then return 0; fi
    sleep 0.35
  done
  return 1
}

cap() {
  adb exec-out screencap -p >"$1"
}

# Убрать системный status bar (требуется ffmpeg).
strip_status_bar() {
  local f="$1"
  local top="${STRIP_STATUS_TOP_PX}"
  local tmp="${f}.strip.tmp.png"
  local w h nh
  if ! command -v ffmpeg >/dev/null 2>&1; then
    echo "capture_rustore_screenshots: ffmpeg не найден — строка состояния не обрезана: $f" >&2
    return 0
  fi
  w=$(sips -g pixelWidth "$f" 2>/dev/null | awk '/pixelWidth/ {print $2}')
  h=$(sips -g pixelHeight "$f" 2>/dev/null | awk '/pixelHeight/ {print $2}')
  nh=$((h - top))
  if [[ -z "$w" || -z "$h" || "$nh" -lt 32 ]]; then
    echo "capture_rustore_screenshots: не удалось определить размер для обрезки: $f" >&2
    return 0
  fi
  ffmpeg -y -nostdin -hide_banner -loglevel error -i "$f" -vf "crop=${w}:${nh}:0:${top}" -frames:v 1 "$tmp"
  mv "$tmp" "$f"
}

# Портрет 9:16 для витрины: масштаб с сохранением пропорций до покрытия кадра, crop без полей.
# По умолчанию crop прижат к низу (RUSTORE_CROP_VERTICAL=bottom), чтобы не резать нижнюю навигацию.
rustore_portrait_9x16() {
  local src="$1" dst="$2"
  local w="${RUSTORE_W}" h="${RUSTORE_H}"
  local cy_expr
  case "${RUSTORE_CROP_VERTICAL}" in
    top) cy_expr="0" ;;
    center) cy_expr="(ih-${h})/2" ;;
    bottom) cy_expr="ih-${h}" ;;
    *)
      echo "capture_rustore_screenshots: неизвестный RUSTORE_CROP_VERTICAL=${RUSTORE_CROP_VERTICAL}, допустимо top|center|bottom" >&2
      return 1
      ;;
  esac
  if ! command -v ffmpeg >/dev/null 2>&1; then
    echo "capture_rustore_screenshots: ffmpeg не найден — пропуск 9:16: $src" >&2
    return 1
  fi
  ffmpeg -y -nostdin -hide_banner -loglevel error -i "$src" \
    -vf "scale=${w}:${h}:force_original_aspect_ratio=increase,crop=${w}:${h}:(iw-${w})/2:${cy_expr}" \
    -frames:v 1 "$dst"
}

scroll_settings_abs_top() {
  local j
  for j in $(seq 1 8); do
    adb shell input swipe 540 1600 540 500 280
    sleep 0.08
  done
  sleep 0.25
}

scroll_settings_align_readable_top() {
  local j
  for j in $(seq 1 6); do
    if dump_ui | grep -q 'text="Классика"'; then return 0; fi
    adb shell input swipe 540 700 540 1400 280
    sleep 0.18
  done
  return 0
}

scroll_settings_to_board_legend() {
  adb shell input tap 540 1200
  sleep 0.12
  local j
  for j in $(seq 1 10); do
    adb shell input swipe 540 1700 540 500 300
    sleep 0.08
  done
  sleep 0.35
}

# Bottom nav centers for 5 tabs on 1080×2400 (Play / Multiplayer / Settings / Scores / About).
NAV_Y="${NAV_Y:-2232}"
NAV_PLAY_X="${NAV_PLAY_X:-100}"
NAV_MP_X="${NAV_MP_X:-320}"
NAV_SETTINGS_X="${NAV_SETTINGS_X:-540}"
NAV_SCORES_X="${NAV_SCORES_X:-760}"
NAV_ABOUT_X="${NAV_ABOUT_X:-980}"
# Onboarding primary button (Далее / Начать игру) on 1080×2400.
ONBOARD_BTN_X="${ONBOARD_BTN_X:-540}"
ONBOARD_BTN_Y="${ONBOARD_BTN_Y:-2205}"

# Drop stale numbered frames so renamed shots from older nav layouts do not leak into store_*.
rm -f "$OUT"/0*.png "$RUSTORE_OUT"/*.png

adb shell am force-stop ru.akarakuts.tictactoe >/dev/null 2>&1 || true
sleep 0.35
adb shell monkey -p ru.akarakuts.tictactoe -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 \
  || adb shell am start -n ru.akarakuts.tictactoe/.MainActivity >/dev/null
sleep 1.2

# Complete first-launch onboarding if present (4 slides: fixed taps, no dump_ui).
# Safe no-ops if onboarding already done.
for _i in $(seq 1 4); do
  adb shell input tap "$ONBOARD_BTN_X" "$ONBOARD_BTN_Y"
  sleep 0.55
done
wait_game_ui || true
sleep 0.5

cap "$OUT/01_ru_play_vs_ai.png"

# Mid-game: three taps on a 3×3 board (1080×2400 cell centers).
adb shell input tap "${BOARD_X1:-247}" "${BOARD_Y1:-839}"
sleep 0.3
adb shell input tap "${BOARD_X2:-540}" "${BOARD_Y1:-839}"
sleep 0.45
adb shell input tap "${BOARD_X1:-247}" "${BOARD_Y2:-1132}"
sleep 0.7
cap "$OUT/02_ru_play_midgame.png"

adb shell input tap "$NAV_MP_X" "$NAV_Y"
sleep 1.2
cap "$OUT/03_ru_multiplayer_lobby.png"

adb shell input tap "$NAV_SETTINGS_X" "$NAV_Y"
sleep 1.1
scroll_settings_abs_top
scroll_settings_align_readable_top
adb shell input swipe 640 920 640 1180 220
sleep 0.35
cap "$OUT/04_ru_settings_sound_themes_modes.png"

scroll_settings_to_board_legend
sleep 0.45
cap "$OUT/05_ru_settings_winlen_board_legend.png"

adb shell input tap "$NAV_SCORES_X" "$NAV_Y"
sleep 1
adb shell input swipe 640 1500 640 900 320
sleep 0.3
adb shell input swipe 640 1500 640 900 320
sleep 0.35
cap "$OUT/06_ru_scores.png"

adb shell input tap "$NAV_ABOUT_X" "$NAV_Y"
sleep 1
cap "$OUT/07_ru_about.png"

for f in "$OUT"/0*.png; do
  [[ -f "$f" ]] || continue
  strip_status_bar "$f"
done

rm -f "$RUSTORE_OUT"/*.png
for f in "$OUT"/0*.png; do
  [[ -f "$f" ]] || continue
  base=$(basename "$f" .png)
  rustore_portrait_9x16 "$f" "$RUSTORE_OUT/${base}.png"
done

echo "Готово: $OUT"
echo "RuStore (9:16, ${RUSTORE_W}×${RUSTORE_H}): $RUSTORE_OUT"
ls -la "$RUSTORE_OUT"/*.png
ls -la "$OUT"/*.png

# В консоль RuStore — только store_1080x1920 (9:16), не сырые 0*.png и не Screenshot_*.png
RUSTORE_COPY_TO="${RUSTORE_COPY_TO:-}"
if [[ -n "$RUSTORE_COPY_TO" ]]; then
  mkdir -p "$RUSTORE_COPY_TO"
  cp -f "$RUSTORE_OUT"/*.png "$RUSTORE_COPY_TO/"
  echo "Скопировано (9:16): $RUSTORE_COPY_TO"
fi
