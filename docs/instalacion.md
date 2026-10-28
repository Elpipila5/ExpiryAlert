# Instalación y ejecución de ExpiryAlert

> Actualizado para el Sprint 5 (26–30 de octubre de 2026). Los pasos están basados en los archivos actuales del repositorio. Se deben confirmar en el dispositivo de cada integrante.

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
2. Cambiar a la rama que se desea probar. Para cambios de esta semana: `feature/sprint5-integracion-seguridad-pruebas`.
3. Abrir la **carpeta raíz** del repositorio, donde están `settings.gradle.kts`, `gradlew` y `gradle/libs.versions.toml`.
4. Sincronizar Gradle. Usar un Android SDK con API 37 instalada según la configuración del proyecto.
5. Configurar Firebase Authentication con Email/Password y Cloud Firestore en el proyecto Firebase del equipo, respetando las reglas de seguridad. El archivo cliente `app/google-services.json` ya está referenciado; no subir credenciales privadas de administrador.
6. Ejecutar sobre un móvil o emulador compatible. Para el lector automático de código de barras se necesita Google Play Services; si no está disponible, introducir el código manualmente.
7. Probar registro, ingreso, inventario, OCR, código de barras, alertas y confirmaciones.

## Comandos desde la raíz

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

## Casos prioritarios para la entrega semanal

Seguir [Sprint 5 — integración y evidencias](pruebas/sprint5-integracion.md). Registrar para cada prueba dispositivo, versión Android, commit y resultado real. Los fallos se documentan en issues, no se ocultan en la presentación.

## Solución de problemas frecuentes

- **No se ve notificación:** verificar permiso `POST_NOTIFICATIONS` en Android 13+ y el canal de notificaciones. La revisión de WorkManager no garantiza horario exacto; usar `Revisar caducidades ahora` para pruebas.
- **No aparece un producto:** verificar sesión, conectividad, que su `usuarioId` coincide y que no quedó archivado (`activo=false`).
- **Error en Firestore:** comprobar las reglas desplegadas, cuenta autenticada y esquema actual.
- **Falla el lector de códigos:** comprobar Google Play Services; usar el ingreso manual EAN/UPC.
- **El código no aparece en Open Food Facts:** esa base de datos colaborativa puede no contenerlo; registrar manualmente.
- **OCR sin texto:** mejorar enfoque/luz, fotografiar la sección con la fecha y corregir antes de guardar.
