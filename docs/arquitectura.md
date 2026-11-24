# Arquitectura implementada

ExpiryAlert es Android nativo con Kotlin, Jetpack Compose y Material 3. SDK mínimo 24; compilación/objetivo 37. Gradle utiliza Java 25 y las pruebas locales Robolectric Java 21, según Android CI.

| Área | Código y responsabilidad |
| --- | --- |
| Entrada y navegación | `MainActivity`, pantallas de cuenta/productos y navegación Compose por estado. |
| Presentación | Componentes de diseño e inventario; pantallas de recetas, compras, revisión, desayuno y perfil. |
| Dominio | Fechas, cantidades, recetas locales, extracción OCR y coincidencias de fotos. |
| Datos | Firebase Authentication, Firestore, transacciones de cantidades y preferencias locales por UID. |
| Captura | ML Kit OCR, Google Code Scanner y consultas explícitas a Open Food Facts. |
| Foto de producto | Consulta de sugerencias, confirmación en el editor y miniatura persistente en el documento. |
| Avisos | WorkManager, programadores de alertas/desayuno, workers y canales de notificación. |
| Calidad | JUnit/Robolectric, pruebas instrumentadas Android 15 y reglas con Node/Java en emulador. |

## Flujos

El registro manual/OCR/código termina en un formulario validado antes del guardado. Consumos y ajustes usan transacciones sobre el último saldo del servidor. Las consultas se filtran por UID y los archivados se ocultan conservando su ID.

Las fotos se decodifican y reducen fuera del hilo de interfaz. Las descargas HTTPS tienen tiempo y tamaño limitados; las páginas HTML no se aceptan como imágenes. Una sugerencia reemplaza la foto al confirmarla y se persiste al guardar el producto. Google se abre como búsqueda por nombre y marca; no se extraen automáticamente sus resultados ni se incrusta una clave de búsqueda.

Las recetas son locales; YouTube se utiliza mediante enlaces. El backend de IA de `backend/` es una propuesta anterior opcional y no es necesario para la interfaz final.

## Operación

WorkManager programa avisos aproximados sujetos a batería y conectividad. Los de otra cuenta se cancelan al cerrar sesión. Compras, preferencias y fotografía de perfil son locales; documentos y fotos de producto pertenecen a Firestore. La matriz de permisos en emulador no acredita qué reglas están publicadas en el servidor real.
