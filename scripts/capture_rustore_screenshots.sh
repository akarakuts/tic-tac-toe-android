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
STRIP_STATUS_TOP_PX="${STRIP_STATUS_TOP_PX:-156}"
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
  for j in $(seq 1 26); do
    adb shell input swipe 640 2400 640 520 320
    sleep 0.09
  done
  sleep 0.35
}

scroll_settings_align_readable_top() {
  local j
  for j in $(seq 1 10); do
    if dump_ui | grep -q 'text="Классика"'; then return 0; fi
    adb shell input swipe 640 720 640 1680 300
    sleep 0.22
  done
  return 0
}

scroll_settings_to_board_legend() {
  adb shell input tap 640 1400
  sleep 0.15
  local j
  for j in $(seq 1 24); do
    adb shell input swipe 640 2350 640 520 350
    sleep 0.1
  done
  sleep 0.5
}

adb shell am force-stop ru.akarakuts.tictactoe >/dev/null 2>&1 || true
sleep 0.35
adb shell am start -n ru.akarakuts.tictactoe/.MainActivity >/dev/null
wait_game_ui || true
sleep 1

cap "$OUT/01_ru_play_vs_ai.png"

adb shell input tap 289 1245
sleep 0.2
adb shell input tap 639 1245
sleep 0.2
adb shell input tap 289 1595
sleep 0.45
cap "$OUT/02_ru_play_midgame.png"

adb shell input tap 639 2664
sleep 1.1
scroll_settings_abs_top
scroll_settings_align_readable_top
adb shell input swipe 640 920 640 1180 220
sleep 0.35
cap "$OUT/03_ru_settings_sound_themes_modes.png"

scroll_settings_to_board_legend
sleep 0.45
cap "$OUT/04_ru_settings_winlen_board_legend.png"

adb shell input tap 1074 2664
sleep 1
adb shell input swipe 640 1500 640 900 320
sleep 0.3
adb shell input swipe 640 1500 640 900 320
sleep 0.35
cap "$OUT/05_ru_scores.png"

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
