set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

package := "com.copyplay"
activity := "com.copyplay/.MainActivity"
debug-apk := "app/build/outputs/apk/debug/app-debug.apk"
android-serial := env_var_or_default("ANDROID_SERIAL", "emulator-5554")
android-avd := env_var_or_default("COPYPLAY_AVD", "argent")
android-gpu := env_var_or_default("COPYPLAY_EMULATOR_GPU", "swiftshader_indirect")
default-copyparty-url := env_var_or_default("COPYPLAY_COPYPARTY_URL", "http://10.0.2.2:3923/")

default:
    @just --list

# Build the debug APK with a baked-in local copyparty URL for clean installs.
build-debug url=default-copyparty-url:
    @JAVA_HOME="${JAVA_HOME:-$HOME/jdks/temurin-21}" ./gradlew -Pcopyplay.localCopypartyUrl="{{url}}" assembleDebug

# Boot the configured Android emulator if it is not already online.
boot-emulator:
    @serial="{{android-serial}}"; avd="{{android-avd}}"; \
    if adb devices | awk -v serial="$serial" 'NR > 1 && $1 == serial && $2 == "device" { found = 1 } END { exit !found }'; then \
        echo "Emulator already running: $serial"; \
    else \
        emulator_bin="${ANDROID_HOME:-$HOME/Android/Sdk}/emulator/emulator"; \
        if [ ! -x "$emulator_bin" ]; then emulator_bin="$(command -v emulator)"; fi; \
        log="/tmp/copyplay-$avd.log"; \
        echo "Booting AVD $avd as $serial; log: $log"; \
        setsid -f "$emulator_bin" -avd "$avd" -gpu "{{android-gpu}}" -no-snapshot-save >"$log" 2>&1 < /dev/null; \
    fi; \
    adb -s "$serial" wait-for-device; \
    until [ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do \
        sleep 2; \
    done; \
    echo "Android ready: $serial"

# Reinstall the latest debug APK and clear app data.
reinstall-debug:
    @serial="{{android-serial}}"; \
    test -f "{{debug-apk}}"; \
    adb -s "$serial" uninstall "{{package}}" >/dev/null 2>&1 || true; \
    adb -s "$serial" install "{{debug-apk}}"

# Launch Copyplay on the configured emulator/device.
launch-debug:
    @adb -s "{{android-serial}}" shell am start -n "{{activity}}"

# Build, boot, reinstall, and launch the local debug app.
run-debug url=default-copyparty-url:
    @just build-debug "{{url}}"
    @just boot-emulator
    @just reinstall-debug
    @just launch-debug
