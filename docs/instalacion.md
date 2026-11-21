# Instalación y ejecución de ExpiryAlert

> Versión candidata 1.3.0. Los pasos corresponden a `main`; la aceptación final en el teléfono se registra por separado de las pruebas automatizadas.

## Tecnología real

- **Android nativo:** Kotlin + Jetpack Compose y Material 3.
- **Gradle:** wrapper del repositorio; `gradle/libs.versions.toml` declara Android Gradle Plugin 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01.
- **SDK:** `compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`.
- **JVM del Gradle daemon:** toolchain 25 según `gradle/gradle-daemon-jvm.properties`. Si Android Studio no localiza/descarga esta JVM, revisar la configuración del proyecto, no cambiar versiones arbitrariamente.
- **Servicios Firebase:** Firebase Authentication (correo/contraseña) y Cloud Firestore.
- **Lectura de empaques:** Google ML Kit OCR `text-recognition:16.0.1`; lector EAN/UPC mediante Google Code Scanner y Google Play Services.
- **Catálogo externo:** Open Food Facts para datos de productos, con acceso a Internet.
- **Recordatorios:** WorkManager, comprobación periódica aproximada y botón manual de revisión.

## Abrir en Android Studio

1. Clonar `https://github.com/Elpipila5/ExpiryAlert.git` (o `Git → Fetch` si ya existe).
2. En GitHub Desktop seleccionar **main → Fetch origin → Pull origin**. En la terminal se puede usar `git switch main` y `git pull --ff-only origin main`. Conservar cualquier cambio local antes de cambiar de rama.
3. Abrir la **carpeta raíz** del repositorio, donde están `settings.gradle.kts`, `gradlew` y `gradle/libs.versions.toml`.
4. Sincronizar Gradle. Usar un Android SDK con API 37 instalada según la configuración del proyecto.
5. Configurar Firebase Authentication con Email/Password y Cloud Firestore en el proyecto Firebase del equipo, respetando las reglas de seguridad. El archivo cliente `app/google-services.json` ya está referenciado; no subir credenciales privadas de administrador.
6. Ejecutar sobre un móvil o emulador compatible. Para el lector automático de código de barras se necesita Google Play Services; si no está disponible, introducir el código manualmente.
7. Comprobar que **Mi perfil** muestra la versión **1.3.0**. Probar inventario, recetas, consumo, compras, desayuno, modo oscuro y foto de perfil según la [lista de cierre](pruebas/cierre-sprint9.md).

## Comandos desde la raíz

### Si la pantalla sigue mostrando una versión anterior

Abre **Terminal dentro de Android Studio**, en la carpeta del proyecto que estás ejecutando. Con el teléfono conectado y autorizado, ejecuta en Windows:

```powershell
git pull --ff-only
.\Actualizar-ExpiryAlert.cmd
```

El actualizador muestra la carpeta y el commit, descarga `main`, limpia únicamente la compilación, genera el APK, lo instala con `adb install -r`, verifica su versión dentro del dispositivo y abre `.MainActivity`. No desinstala la aplicación ni borra sus datos. Se detiene ante cambios locales, una rama divergente, una firma incompatible o varios dispositivos sin seleccionar.

Si hay varios dispositivos, puedes elegir el serial que muestra `adb devices`:

```powershell
.\Actualizar-ExpiryAlert.cmd -Dispositivo SERIAL
```

Para consultar el diagnóstico sin modificar el repositorio ni instalar:

```powershell
.\Actualizar-ExpiryAlert.cmd -SoloDiagnostico
```

La última línea indica dónde se guardó el registro local. El script comprueba la instalación; el aspecto y funcionamiento en el teléfono se confirman al abrirla.

### Compilación manual

En macOS/Linux:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

En Windows (PowerShell):

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Si la compilación funciona, el APK de depuración se genera normalmente en `app/build/outputs/apk/debug/`. No confundir una prueba unitaria correcta con todas las pruebas en teléfono aprobadas.

## Reglas de Firestore

Este repositorio incluye `firestore.rules`, **pero no se aplica al proyecto Firebase al realizar un pull**. Antes de usar las reglas, seguir [Seguridad Firestore](seguridad-firestore.md) y combinar con cualquier configuración preexistente. En caso de `PERMISSION_DENIED`, verificar que se publicaron las reglas compatibles, la sesión actual y los campos enviados.

## Casos prioritarios para la entrega

Seguir la [lista de cierre de la versión candidata](pruebas/cierre-sprint9.md), la [matriz de cantidades, compras y desayuno](pruebas/v21-sprints6-9.md) y las [pruebas de diseño y personalización](pruebas/diseno-personalizacion.md). Registrar dispositivo, versión Android, commit y resultado real.

CI genera un APK de depuración y los informes en [GitHub Actions](https://github.com/Elpipila5/ExpiryAlert/actions). Para actualizar la instalación que ya usas, ejecuta **Run** desde tu Android Studio con la misma clave de depuración. Una compilación de CI puede usar una clave diferente. No borres los datos de la aplicación como paso de actualización: fotos, compras y preferencias son locales.

## Solución de problemas frecuentes

- **No se ve notificación:** verificar permiso `POST_NOTIFICATIONS` en Android 13+ y el canal de notificaciones. La revisión de WorkManager no garantiza horario exacto; usar `Revisar caducidades ahora` para pruebas.
- **No aparece un producto:** verificar sesión, conectividad, que su `usuarioId` coincide y que no quedó archivado (`activo=false`).
- **Error en Firestore:** comprobar las reglas desplegadas, cuenta autenticada y esquema actual.
- **Falla el lector de códigos:** comprobar Google Play Services; usar el ingreso manual EAN/UPC.
- **El código no aparece en Open Food Facts:** esa base de datos colaborativa puede no contenerlo; registrar manualmente.
- **OCR sin texto:** mejorar enfoque/luz, fotografiar la sección con la fecha y corregir antes de guardar.
