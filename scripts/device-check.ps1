# Windows version of device-check.sh: screenshots one screen of the debug app in every setting the phone checks ask for.
#
# Usage: open the screen you want to check in the emulator (or a phone with USB debugging), then run from the
# repository root, in PowerShell or Android Studio's terminal:
#   powershell -ExecutionPolicy Bypass -File scripts\device-check.ps1 group-home [-Install] [-Kill]
# Screenshots and accessibility dumps land in build\device-check\<time>-<label>\.
# -Install installs the debug build first; -Kill also checks that the app survives being killed.
# Every setting it changes is put back when it finishes, even if it is interrupted.
param([string]$Label = "screen", [switch]$Install, [switch]$Kill)
$ErrorActionPreference = "Continue"
$App = "gr.edusuite.pacer.debug"

# Find adb: PATH, then ANDROID_HOME / ANDROID_SDK_ROOT, then local.properties, then Android Studio's default SDK.
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) {
    $sdks = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
    if (Test-Path local.properties) {
        $line = Select-String -Path local.properties -Pattern '^sdk\.dir=(.*)$' | Select-Object -First 1
        if ($line) { $sdks += ($line.Matches[0].Groups[1].Value -replace '\\\\', '\' -replace '\\:', ':') }
    }
    $sdks += "$env:LOCALAPPDATA\Android\Sdk"
    foreach ($s in $sdks) {
        if ($s -and (Test-Path "$s\platform-tools\adb.exe")) { $adb = "$s\platform-tools\adb.exe"; break }
    }
}
if (-not $adb) { Write-Host "adb not found. Install 'Android SDK Platform-Tools' in Android Studio's SDK Manager."; exit 1 }

function Adb { & $adb @args 2>$null }
if ((Adb get-state | Out-String).Trim() -ne "device") { Write-Host "No emulator or phone connected. Start the emulator first."; exit 1 }
if ($Install) { & .\gradlew.bat -q installDebug }

$out = "build\device-check\$(Get-Date -Format yyyyMMdd-HHmmss)-$Label"
New-Item -ItemType Directory -Force -Path $out | Out-Null

# Remember the settings this script changes.
$font = (Adb shell settings get system font_scale | Out-String).Trim(); if ($font -eq "null" -or -not $font) { $font = "1.0" }
$night = ((Adb shell cmd uimode night | Out-String).Trim() -split ' ')[-1]; if (-not $night) { $night = "auto" }
$anim = (Adb shell settings get global animator_duration_scale | Out-String).Trim(); if ($anim -eq "null" -or -not $anim) { $anim = "1" }

function Shot([string]$name) {
    Start-Sleep -Milliseconds 1500
    # Screenshots go through the device and adb pull: PowerShell's > would corrupt the PNG bytes.
    Adb shell screencap -p /sdcard/pacer-shot.png | Out-Null
    Adb pull /sdcard/pacer-shot.png "$out\$name.png" | Out-Null
    Adb shell uiautomator dump /sdcard/pacer-dump.xml | Out-Null
    Adb pull /sdcard/pacer-dump.xml "$out\$name.xml" | Out-Null
    Write-Host "  $name"
}

Write-Host "Saving to $out"
try {
    Adb shell settings put global animator_duration_scale 0 | Out-Null
    foreach ($mode in "no", "yes") {
        $theme = if ($mode -eq "no") { "light" } else { "dark" }
        Adb shell cmd uimode night $mode | Out-Null
        foreach ($scale in "1.0", "1.5", "2.0") {
            Adb shell settings put system font_scale $scale | Out-Null
            Shot "phone-$theme-text$scale"
        }
        Adb shell settings put system font_scale 1.0 | Out-Null
        # Width checks: 600dp and 840dp are where the panes and the permanent drawer start (docs/PACER_DESIGN.md).
        foreach ($w in 599, 600, 839, 840, 1024) {
            Adb shell wm density 160 | Out-Null
            Adb shell wm size "${w}x800" | Out-Null
            Shot "width$w-$theme"
        }
        Adb shell wm size reset | Out-Null
        Adb shell wm density reset | Out-Null
    }
    if ($Kill) {
        Adb shell input keyevent KEYCODE_HOME | Out-Null
        Start-Sleep -Seconds 1
        Adb shell am kill $App | Out-Null
        Adb shell monkey -p $App -c android.intent.category.LAUNCHER 1 | Out-Null
        Shot "after-kill" # opens on Home by design; check no data was lost
    }
} finally {
    Adb shell settings put system font_scale $font | Out-Null
    Adb shell cmd uimode night $night | Out-Null
    Adb shell settings put global animator_duration_scale $anim | Out-Null
    Adb shell wm size reset | Out-Null
    Adb shell wm density reset | Out-Null
}
Write-Host "Done. The app's palette (Bone or Saffron) is an in-app setting: switch it and run again for the other one."
