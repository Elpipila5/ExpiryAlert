#!/usr/bin/env bash
set -euo pipefail

recuperar_evidencias() {
    mkdir -p app/build/reports/diseno-android
    adb logcat -d > app/build/reports/diseno-android/logcat.txt || true
}

# Se ejecuta antes de que la acción apague el emulador, también si falla una prueba.
trap recuperar_evidencias EXIT
bash gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx2g \
    -Pandroid.testInstrumentationRunnerArguments.expiry.emuladores=true \
    -Pandroid.testInstrumentationRunnerArguments.additionalTestOutputDir=/sdcard/Android/media/com.example.expiryalert/qa_diseno
