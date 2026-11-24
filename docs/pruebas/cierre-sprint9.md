# Cierre técnico del Sprint 9

Estado al 24/11/2026: versión 1.4.0, código 7. La presentación del plan está prevista para el 24 y 25; el 26 y 27 se reservan a cierre y retroalimentación.

## Trabajo concluido y preparado

- F01–F10 integradas en la base 1.3.0. José Enrique informó que el equipo de tres integrantes revisó la última versión y que los problemas visuales y de actualización quedaron resueltos.
- F02 ampliada con foto del producto persistida dentro de su documento. Importación desde galería/enlace, sugerencias por código o nombre/marca, búsqueda Google y confirmación de vista previa.
- Reglas ampliadas para fotos, compatibles con productos antiguos y probadas con dos cuentas/cliente anónimo. Suite de 14 casos en emulador, aprobada el 24/11.
- Pruebas compartidas de fotografía y editor para API 27 local y Android 15. Incluyen conservación tras borrar el archivo de origen y restauración de estado.
- Modelo de datos y arquitectura actualizados al código implementado.
- Manual, reporte técnico y guion de demostración en [entrega](../entrega/LEEME.md).
- Paquete reproducible con APK, fuente, hashes y resultados locales. Artefactos de CI con conservación de 90 días.

## Evidencia histórica y nueva

La base 8ddcf02 aprobó [72 pruebas locales y 19 Android](https://github.com/Elpipila5/ExpiryAlert/actions/runs/44942875087), lint y diagnóstico Windows. El workflow de 1.4.0 vuelve a compilar y ampliar las suites; su resultado real y metadatos identifican el APK final. No se declara aprobada una ejecución nueva antes de que termine.

Las fotos de perfil, compras y preferencias son locales por cuenta. La foto del producto viaja con Firestore. Los recordatorios WorkManager son aproximados; una prueba de emulador no acredita entrega bajo ahorro de batería en el Xiaomi.

## Acciones que requieren el servicio o al equipo

1. **Publicar las reglas ampliadas** en Firebase `expiryalert-e9b37`, conservando las reglas de otras colecciones si existen. [Pasos concretos](../entrega/LEEME.md#activar-el-guardado-de-fotos-en-firebase). No hay sesión administrativa disponible aquí para hacerlo o demostrarlo.
2. Instalar 1.4.0 conservando datos y comprobar en el teléfono el nuevo guardado/cambio/eliminación de la foto. La confirmación del equipo recibida corresponde a la versión anterior.
3. Conservar el paquete/artefactos y registrar la demostración final. Documentar los casos específicos de recordatorios y las aportaciones individuales que el equipo vaya a presentar.

El estado del tablero debe reflejar criterios satisfechos y límites pendientes, sin atribuir contribuciones ni aprobaciones de GitHub no comunicadas.
