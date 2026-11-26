"""Reúne el APK, código, manuales y evidencias disponibles sin inventar resultados."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

RAIZ = Path(__file__).resolve().parents[1]
SALIDA = RAIZ / 'build' / 'entrega'


def preparar():
    configuracion = (RAIZ / 'app/build.gradle.kts').read_text()
    version = re.search(r'versionName\s*=\s*"([^"]+)"', configuracion).group(1)
    codigo = int(re.search(r'versionCode\s*=\s*(\d+)', configuracion).group(1))
    apk = RAIZ / 'app/build/outputs/apk/debug/app-debug.apk'
    if not apk.is_file():
        raise SystemExit('Primero ejecuta :app:assembleDebug y :app:testDebugUnitTest.')
    datos_apk = apk.parent / 'output-metadata.json'
    if not datos_apk.is_file():
        raise SystemExit('Faltan los metadatos generados por Android Gradle Plugin para este APK.')
    elementos = json.loads(datos_apk.read_text()).get('elements', [])
    if not any(e.get('versionName') == version and int(e.get('versionCode', -1)) == codigo and
               e.get('outputFile') == apk.name for e in elementos):
        raise SystemExit('El APK no corresponde a la versión del código. Vuelve a compilar.')
    firebase = json.loads((RAIZ / 'app/google-services.json').read_text())
    paquete = re.search(r'applicationId\s*=\s*"([^"]+)"', configuracion).group(1)
    clientes = [c for c in firebase['client']
                if c['client_info']['android_client_info']['package_name'] == paquete]
    if len(clientes) != 1:
        raise SystemExit('La configuración Firebase no identifica un único cliente para esta app.')
    proyecto_firebase = firebase['project_info']['project_id']
    app_firebase = clientes[0]['client_info']['mobilesdk_app_id']
    # AGP administra la carpeta de salida desde Google Services 4.5.0.
    # Aceptar tanto processDebugGoogleServices como google-services/debug.
    generados = RAIZ / 'app/build/generated/res'
    candidatos = [p for p in generados.rglob('values.xml')
                  if 'google' in p.relative_to(generados).as_posix().lower()
                  and 'debug' in p.relative_to(generados).as_posix().lower()]
    if len(candidatos) != 1:
        raise SystemExit('No se identificaron de forma única los recursos Firebase de debug. Limpia la compilación y vuelve a generar el APK.')
    recursos_firebase = candidatos[0]
    valores = {n.get('name'): n.text for n in ET.parse(recursos_firebase).getroot().findall('string')}
    if valores.get('project_id') != proyecto_firebase or valores.get('google_app_id') != app_firebase:
        raise SystemExit('Los recursos Android no corresponden al proyecto Firebase del código. Vuelve a compilar.')
    informes = list((RAIZ / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
    if not informes:
        raise SystemExit('No hay resultados de pruebas locales para adjuntar.')
    cuentas = dict(tests=0, failures=0, errors=0, skipped=0)
    for informe in informes:
        nodo = ET.parse(informe).getroot()
        for campo in cuentas:
            cuentas[campo] += int(nodo.get(campo, 0))
    if cuentas['failures'] or cuentas['errors'] or cuentas['skipped']:
        raise SystemExit(f'Las pruebas no están completas: {cuentas}')
    commit_build = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=RAIZ, text=True).strip()
    commit_fuente = os.getenv('EXPIRY_COMMIT_FUENTE', commit_build)
    metadata = dict(aplicacion='ExpiryAlert', version=version, versionCode=codigo,
        proyecto_firebase=proyecto_firebase, app_firebase=app_firebase,
        commit_fuente=commit_fuente, commit_build=commit_build,
        creado_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        tipo_apk='debug', pruebas_locales=cuentas,
        apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
        workflow=os.getenv('GITHUB_SERVER_URL', 'https://github.com') + '/' +
            os.getenv('GITHUB_REPOSITORY', 'Elpipila5/ExpiryAlert') + '/actions/runs/' + os.getenv('GITHUB_RUN_ID', ''),
        limites=['El APK de CI usa firma de depuración; para actualizar una instalación de Android Studio se recomienda compilar con su misma clave.',
            'El resultado global del workflow acredita también Android, Windows y reglas; no sustituye reglas publicadas en Firebase.',
            'La revisión del equipo comunicada corresponde a 1.3.0; la conexión al Firebase propio y las fotos deben comprobarse con 1.4.1 en el teléfono.',
            'El proyecto Firebase nuevo comienza sin las cuentas ni el inventario del proyecto anterior; registrarse de nuevo dentro de la app.'])
    archivos = {'ExpiryAlert.apk': apk.read_bytes(),
        'VERSION_Y_PRUEBAS.json': (json.dumps(metadata, ensure_ascii=False, indent=2) + '\n').encode()}
    leeme = ('ExpiryAlert ' + version + '\n\n'
        '1. Abre codigo/docs/entrega/LEEME.md para instalación, manuales y demostración.\n'
        '2. Firebase: ' + proyecto_firebase + '. Registra una cuenta dentro de la app; no se migró el inventario anterior.\n'
        '3. VERSION_Y_PRUEBAS.json identifica el código, Firebase y las pruebas de este APK.\n'
        '4. SHA256SUMS.txt permite verificar la integridad de todos los archivos.\n'
        '5. Revisa los cuatro trabajos del workflow antes de usar este paquete como entrega validada.\n')
    archivos['LEEME.txt'] = leeme.encode()
    rutas = subprocess.check_output(['git', 'ls-files', '-z'], cwd=RAIZ).decode().split('\0')
    for ruta in filter(None, rutas):
        archivo = RAIZ / ruta
        if archivo.is_file():
            archivos['codigo/' + ruta] = archivo.read_bytes()
    for informe in informes:
        archivos['evidencias/pruebas-locales/' + informe.name] = informe.read_bytes()
    for carpeta in [RAIZ / 'app/build/reports/diseno', RAIZ / 'app/build/reports/diseno-android']:
        if carpeta.is_dir():
            for archivo in carpeta.rglob('*.png'):
                archivos['evidencias/' + carpeta.name + '/' + archivo.relative_to(carpeta).as_posix()] = archivo.read_bytes()
    lint = RAIZ / 'app/build/reports/lint-results-debug.html'
    if lint.is_file():
        archivos['evidencias/lint.html'] = lint.read_bytes()
    archivos['SHA256SUMS.txt'] = ''.join(f'{hashlib.sha256(datos).hexdigest()}  {nombre}\n'
        for nombre, datos in sorted(archivos.items())).encode()
    SALIDA.mkdir(parents=True, exist_ok=True)
    destino = SALIDA / f'ExpiryAlert-{version}.zip'
    with zipfile.ZipFile(destino, 'w', zipfile.ZIP_DEFLATED) as paquete:
        for nombre, datos in sorted(archivos.items()):
            paquete.writestr(nombre, datos)
    (SALIDA / (destino.name + '.sha256')).write_text(hashlib.sha256(destino.read_bytes()).hexdigest() + '  ' + destino.name + '\n')
    print(json.dumps(metadata, ensure_ascii=False, indent=2))
    print(destino)


if __name__ == '__main__':
    preparar()
