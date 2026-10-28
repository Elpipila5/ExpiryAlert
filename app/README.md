# Aplicación Android — ExpiryAlert

Código real del proyecto móvil, escrito en **Kotlin**, con interfaz **Jetpack Compose**.

## Archivos principales

- `src/main/java/com/example/expiryalert/MainActivity.kt`: navegación, autenticación, alta y edición de productos, inventario, confirmación de avisos.
- `NotificationHelper.kt`, `ProgramadorAlertas.kt` y `RevisionCaducidadWorker.kt`: notificaciones, WorkManager y consulta de caducidad.
- `FechasCaducidad.kt`: cálculo de días y validaciones de fechas.
- `CapturaOCRProducto.kt` y `ExtractorDatosOCR.kt`: fotografía y lectura de fecha/cantidad.
- `EscanerCodigoProducto.kt` y `ConsultaProductoCodigo.kt`: lector EAN/UPC y búsqueda de producto en Open Food Facts.
- `build.gradle.kts`: dependencias Firebase, Compose, ML Kit y WorkManager.
- `src/test/`: pruebas unitarias; `src/androidTest/`: pruebas instrumentadas de referencia.

## Abrir, ejecutar y entregar

Abre la **carpeta raíz del repositorio** en Android Studio, sincroniza Gradle y selecciona la rama de trabajo. El procedimiento detallado y los comandos están en [instalación](../docs/instalacion.md).

La prueba integral del **Sprint 5** está en [docs/pruebas/sprint5-integracion.md](../docs/pruebas/sprint5-integracion.md). Las reglas de acceso a productos se encuentran en [firestore.rules](../firestore.rules) y necesitan validación/publicación explícita en Firebase.

No incluir credenciales privadas de Firebase, llaves de servicio, tokens ni bases de datos personales en commits o evidencias.
