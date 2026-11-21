[CmdletBinding()]
param(
    [string]$Dispositivo = '',
    [switch]$SoloDiagnostico
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$registro = Join-Path ([IO.Path]::GetTempPath()) ('ExpiryAlert-actualizacion-{0}.log' -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
$grabando = $false
$codigo = 0

function Invoke-Nativo {
    param([string]$Archivo, [string[]]$Argumentos)
    $anterior = $ErrorActionPreference
    try {
        # Git escribe progreso en stderr incluso cuando termina correctamente.
        $ErrorActionPreference = 'Continue'
        $salida = @(& $Archivo @Argumentos 2>&1 | ForEach-Object { $_.ToString() })
        $resultado = $LASTEXITCODE
    } finally { $ErrorActionPreference = $anterior }
    if ($resultado -ne 0) {
        throw ('Fallo al ejecutar {0} (codigo {1}).{2}{3}' -f (Split-Path -Leaf $Archivo), $resultado, [Environment]::NewLine, ($salida -join [Environment]::NewLine))
    }
    return $salida
}

function Buscar-Git {
    $comando = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($comando) { return $comando.Source }
    $normal = Join-Path $env:ProgramFiles 'Git\cmd\git.exe'
    if (Test-Path -LiteralPath $normal) { return $normal }
    $desktop = Get-ChildItem (Join-Path $env:LOCALAPPDATA 'GitHubDesktop\app-*\resources\app\git\cmd\git.exe') -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($desktop) { return $desktop.FullName }
    throw 'No se encontro Git. Abre la terminal del proyecto con Git disponible.'
}

function Buscar-Adb {
    $candidatos = @()
    $propiedades = Join-Path $raiz 'local.properties'
    if (Test-Path -LiteralPath $propiedades) {
        foreach ($linea in Get-Content -LiteralPath $propiedades) {
            $coincidencia = [regex]::Match($linea, '^\s*sdk\.dir\s*=\s*(.+)$')
            if ($coincidencia.Success) {
                $sdk = $coincidencia.Groups[1].Value.Trim().Replace('\\', '\').Replace('\:', ':')
                $candidatos += Join-Path $sdk 'platform-tools\adb.exe'
            }
        }
    }
    foreach ($sdk in @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME)) {
        if ($sdk) { $candidatos += Join-Path $sdk 'platform-tools\adb.exe' }
    }
    $candidatos += Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    foreach ($ruta in $candidatos) { if (Test-Path -LiteralPath $ruta) { return $ruta } }
    $comando = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($comando) { return $comando.Source }
    return $null
}

try {
    Set-Location -LiteralPath $raiz
    Start-Transcript -Path $registro -Force | Out-Null
    $grabando = $true
    $git = Buscar-Git
    Write-Host ('CARPETA: {0}' -f $raiz)
    $rama = (Invoke-Nativo -Archivo $git -Argumentos @('branch', '--show-current')) -join ''
    Write-Host ('RAMA: {0}' -f $rama)

    if (-not $SoloDiagnostico) {
        $cambios = @(Invoke-Nativo -Archivo $git -Argumentos @('status', '--porcelain'))
        if ($cambios.Count -gt 0) {
            throw ('Hay cambios locales. Guardalos o crea un commit antes de actualizar; no se han borrado.{0}{1}' -f [Environment]::NewLine, ($cambios -join [Environment]::NewLine))
        }
        $origen = (Invoke-Nativo -Archivo $git -Argumentos @('remote', 'get-url', 'origin')) -join ''
        if ($origen -notmatch '(?i)[/:]Elpipila5/ExpiryAlert(?:\.git)?/?$') {
            throw 'El origen de esta carpeta no corresponde a Elpipila5/ExpiryAlert. Revisa la carpeta del proyecto.'
        }
        Write-Host 'Descargando la version publicada en main...'
        Invoke-Nativo -Archivo $git -Argumentos @('fetch', 'origin', 'refs/heads/main:refs/remotes/origin/main')
        $ramas = @(Invoke-Nativo -Archivo $git -Argumentos @('for-each-ref', '--format=%(refname)', 'refs/heads/main'))
        if ($ramas.Count -gt 0) {
            Invoke-Nativo -Archivo $git -Argumentos @('switch', 'main')
        } else {
            Invoke-Nativo -Archivo $git -Argumentos @('switch', '-c', 'main', '--track', 'origin/main')
        }
        Invoke-Nativo -Archivo $git -Argumentos @('merge', '--ff-only', 'origin/main')
    }

    $commit = (Invoke-Nativo -Archivo $git -Argumentos @('rev-parse', '--short=7', 'HEAD')) -join ''
    $configuracion = Get-Content -LiteralPath (Join-Path $raiz 'app\build.gradle.kts') -Raw
    $versionFuente = [regex]::Match($configuracion, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
    $ramaActual = (Invoke-Nativo -Archivo $git -Argumentos @('branch', '--show-current')) -join ''
    Write-Host ('RAMA DEL PROYECTO: {0}' -f $ramaActual)
    Write-Host ('COMMIT: {0}' -f $commit)
    Write-Host ('VERSION DEL PROYECTO: {0}' -f $versionFuente)
    $adb = Buscar-Adb
    if (-not $adb) {
        if ($SoloDiagnostico) {
            Write-Host 'ADB: no encontrado. Revisa el SDK del proyecto en Android Studio.'
        } else { throw 'No se encontro ADB. Configura el Android SDK en Android Studio y sincroniza el proyecto.' }
    } else {
        Write-Host ('ADB: {0}' -f $adb)
        $listado = @(Invoke-Nativo -Archivo $adb -Argumentos @('devices', '-l'))
        $listado | ForEach-Object { Write-Host $_ }
        if (-not $SoloDiagnostico) {
            $conectados = @($listado | ForEach-Object {
                $m = [regex]::Match($_, '^(\S+)\s+device(?:\s|$)')
                if ($m.Success) { $m.Groups[1].Value }
            })
            if ($Dispositivo) {
                if ($Dispositivo -notin $conectados) { throw 'El dispositivo indicado no esta conectado y autorizado. Revisa la lista anterior.' }
                $seleccionado = $Dispositivo
            } elseif ($conectados.Count -eq 1) {
                $seleccionado = $conectados[0]
            } else {
                throw 'Conecta y autoriza un solo telefono, o indica -Dispositivo SERIAL para elegirlo. No se ha instalado nada.'
            }

            Write-Host ('Compilando desde esta carpeta para {0}...' -f $seleccionado)
            Invoke-Nativo -Archivo (Join-Path $raiz 'gradlew.bat') -Argumentos @(':app:clean', ':app:assembleDebug', '--no-daemon')
            $carpetaApk = Join-Path $raiz 'app\build\outputs\apk\debug'
            $metadatos = Get-Content -LiteralPath (Join-Path $carpetaApk 'output-metadata.json') -Raw | ConvertFrom-Json
            if ($metadatos.applicationId -ne 'com.example.expiryalert') { throw 'El APK generado no corresponde a ExpiryAlert.' }
            $elemento = @($metadatos.elements)[0]
            $apk = Join-Path $carpetaApk $elemento.outputFile
            Write-Host ('Instalando {0}, codigo {1}...' -f $elemento.versionName, $elemento.versionCode)
            # -r conserva los datos. Una firma incompatible produce un error; no se desinstala la app.
            Invoke-Nativo -Archivo $adb -Argumentos @('-s', $seleccionado, 'install', '-r', $apk)

            $paquete = (Invoke-Nativo -Archivo $adb -Argumentos @('-s', $seleccionado, 'shell', 'dumpsys', 'package', 'com.example.expiryalert')) -join "`n"
            $versionInstalada = [regex]::Match($paquete, '(?m)^\s*versionName=(\S+)').Groups[1].Value
            $codigoInstalado = [regex]::Match($paquete, '(?m)^\s*versionCode=(\d+)').Groups[1].Value
            if ($versionInstalada -ne [string]$elemento.versionName -or $codigoInstalado -ne [string]$elemento.versionCode) {
                throw ('La version instalada ({0}, codigo {1}) no coincide con el APK generado.' -f $versionInstalada, $codigoInstalado)
            }
            Invoke-Nativo -Archivo $adb -Argumentos @('-s', $seleccionado, 'shell', 'am', 'force-stop', 'com.example.expiryalert')
            Invoke-Nativo -Archivo $adb -Argumentos @('-s', $seleccionado, 'shell', 'am', 'start', '-W', '-n', 'com.example.expiryalert/.MainActivity')
            Write-Host ('VERSION INSTALADA Y ABIERTA: {0}, codigo {1}' -f $versionInstalada, $codigoInstalado) -ForegroundColor Green
        }
    }
} catch {
    $codigo = 1
    Write-Host ('ACTUALIZACION DETENIDA: {0}' -f $_.Exception.Message) -ForegroundColor Red
} finally {
    if ($grabando) { Stop-Transcript | Out-Null }
    Write-Host ('REGISTRO: {0}' -f $registro)
}
exit $codigo
