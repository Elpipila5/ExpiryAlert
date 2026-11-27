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

# Un flag mal configurado no debe convertir los flujos de Firebase en falsos éxitos.
python3 - <<'PY'
from pathlib import Path
import json
import xml.etree.ElementTree as ET

informes = list(Path('app/build/outputs/androidTest-results/connected').rglob('TEST-*.xml'))
if not informes:
    raise SystemExit('No hay informes de Android para validar.')
totales = dict(tests=0, failures=0, errors=0, skipped=0)
integracion = []
for informe in informes:
    raiz = ET.parse(informe).getroot()
    for campo in totales:
        totales[campo] += int(raiz.get(campo, 0))
    integracion += [caso.get('name') for caso in raiz.findall('.//testcase')
                    if caso.get('classname') == 'com.example.expiryalert.FlujosFirebaseInstrumentedTest']
if totales['failures'] or totales['errors'] or totales['skipped'] or len(integracion) < 7:
    raise SystemExit(f'Pruebas incompletas: {totales}, flujos Firebase: {len(integracion)}')
resultado = dict(android=totales, proyecto_pruebas='demo-expiryalert-pruebas', flujos_firebase=integracion)
destino = Path('app/build/reports/diseno-android/resultado-flujos.json')
destino.parent.mkdir(parents=True, exist_ok=True)
destino.write_text(json.dumps(resultado, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(resultado, ensure_ascii=False))
PY
