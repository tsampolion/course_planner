#!/usr/bin/env bash
# Screenshots one screen of the debug app in every setting the phone checks ask for.
#
# Usage: open the screen you want to check in the emulator (or a phone with USB debugging), then run
#   scripts/device-check.sh [label]
# from the repository root. Screenshots and accessibility dumps land in build/device-check/<time>-<label>/.
# Options: --install installs the debug build first; --kill also checks that the app survives being killed.
# Every setting it changes is put back when it finishes, even if it is interrupted.
set -euo pipefail

APP=gr.edusuite.pacer.debug
install=false; kill=false; label=screen
for a in "$@"; do
  case $a in
    --install) install=true ;;
    --kill) kill=true ;;
    -h|--help) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) label=$a ;;
  esac
done

command -v adb >/dev/null || { echo "adb not found: add Android SDK platform-tools to PATH"; exit 1; }
[ "$(adb get-state 2>/dev/null)" = device ] || { echo "No emulator or phone connected (adb devices)"; exit 1; }
$install && ./gradlew -q installDebug

out="build/device-check/$(date +%Y%m%d-%H%M%S)-$label"
mkdir -p "$out"

# Remember the settings this script changes, and restore them on exit.
font=$(adb shell settings get system font_scale | tr -d '\r')
night=$(adb shell cmd uimode night | tr -d '\r' | awk '{print $NF}')
anim=$(adb shell settings get global animator_duration_scale | tr -d '\r')
restore() {
  adb shell settings put system font_scale "${font/null/1.0}" >/dev/null
  adb shell cmd uimode night "${night:-auto}" >/dev/null
  adb shell settings put global animator_duration_scale "${anim/null/1}" >/dev/null
  adb shell wm size reset >/dev/null; adb shell wm density reset >/dev/null
}
trap restore EXIT

shot() { # name
  sleep 1.5
  adb exec-out screencap -p > "$out/$1.png"
  adb shell uiautomator dump /sdcard/pacer-dump.xml >/dev/null 2>&1 &&
    adb pull /sdcard/pacer-dump.xml "$out/$1.xml" >/dev/null 2>&1 || true
  echo "  $1"
}

echo "Saving to $out"
adb shell settings put global animator_duration_scale 0
for mode in no yes; do
  theme=$([ $mode = no ] && echo light || echo dark)
  adb shell cmd uimode night $mode >/dev/null
  for scale in 1.0 1.5 2.0; do
    adb shell settings put system font_scale $scale
    shot "phone-$theme-text$scale"
  done
  adb shell settings put system font_scale 1.0
  # Width checks: 600dp and 840dp are where the panes and the permanent drawer start (docs/PACER_DESIGN.md).
  for w in 599 600 839 840 1024; do
    adb shell wm density 160 >/dev/null; adb shell wm size "${w}x800" >/dev/null
    shot "width$w-$theme"
  done
  adb shell wm size reset >/dev/null; adb shell wm density reset >/dev/null
done

if $kill; then
  adb shell input keyevent KEYCODE_HOME
  sleep 1; adb shell am kill $APP
  adb shell monkey -p $APP -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  shot "after-kill" # opens on Home by design; check no data was lost
fi

echo "Done. The app's palette (Bone or Saffron) is an in-app setting: switch it and run again for the other one."
