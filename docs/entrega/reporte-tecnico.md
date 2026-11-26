# Reporte técnico de ExpiryAlert 1.4.1

Corte: 24 de noviembre de 2026. Equipo según el plan: José Enrique Guajardo Lugo, Martin Abelardo Gutiérrez Alcocer y Carlos Emanuel Calderón Franco. Desarrollo Móvil Integral, IDGS10, Universidad Tecnológica de Montemorelos.

## Resultado y alcance

La aplicación reúne F01–F10: cuenta, registro, OCR, inventario, caducidad, consumo, verificación, bajo stock/compras, desayuno y recetas. La ampliación 1.4.0 cubre la fotografía opcional persistente del producto de F02, con cambio y eliminación. El contenido de la imagen acompaña al documento y no depende de conservar la URI de la galería.

La versión 1.4.1 conecta Android al Firebase propio `expiryalert-49b5e` mediante el archivo oficial proporcionado por el solicitante. Authentication por correo/contraseña, Firestore Standard y reglas privadas se configuraron el 24/11/2026. Tres simulaciones de consola permitieron creación propia y rechazaron creación anónima/ajena. El proyecto nuevo requiere registro dentro de la app y no contiene las cuentas ni el inventario anteriores. [Evidencia y aceptación pendiente](../firebase-proyecto-propio.md).

Las sugerencias automáticas usan Open Food Facts y verifican nombre/marca; un código identifica la presentación del catálogo. La persona confirma la vista previa antes de incorporarla. Google Imágenes se abre con nombre y marca; la foto elegida se importa mediante galería o enlace HTTPS. La API de búsqueda de Google está cerrada a nuevos clientes: no se presenta una búsqueda abierta en el navegador como extracción automática.

Se filtran categorías no alimentarias reconocidas y términos como pasta dental, leche corporal o aceite para motor al calcular recetas. El diseño, los controles de tema y las fotos de perfil se conservan.

## Evolución conforme a los objetivos del cronograma

| Sprint previsto | Desarrollo e integración registrada |
| --- | --- |
| 1, 28 sep.–2 oct. | Planeación, base Android y documentación; incorporación pública de la base el 8 de octubre, PR 14. |
| 2, 5–9 oct. | Formularios, CRUD y consulta de productos por UID. |
| 3, 12–16 oct. | Cámara/galería y OCR con corrección; integración del PR 16 el 28 de octubre. |
| 4, 19–23 oct. | Clasificación y WorkManager; integración de alertas durante el Sprint 5. |
| 5, 26–30 oct. | Unión F01–F05, códigos/Open Food Facts, archivo/ID de documentos y atención de notificaciones. |
| 6, 2–6 nov. | Consumos, unidades y correcciones; consolidación del PR 22 el 19 de noviembre, durante el Sprint 8. |
| 7, 9–13 nov. | Recetas reconstruidas: 28 locales, coincidencias completas/parciales, compras y YouTube; PR 21 el 12 de noviembre. |
| 8, 16–20 nov. | Bajo stock, compras, desayuno, personalización y diseño; PR 22 y 24. |
| 9, 23–27 nov. | Estabilización, nuevas fotos persistentes, reglas automatizadas y paquete de entrega. Presentación prevista el 24 y 25. |

La agrupación sigue los objetivos del plan; no afirma que todas las tareas se integraron dentro de su semana prevista.

José Enrique precisó que él y Martin realizaron programación y Carlos elaboró los documentos. Se registra esta distribución sin atribuir módulos o cuentas individuales no confirmados.

## Pruebas y reproducción

La base 1.3.0 aprobó 72 casos locales y 19 Android 15 en el commit 8ddcf02. El usuario comunicó que los problemas visuales y de actualización quedaron resueltos y que los tres integrantes revisaron esa versión.

La suite 1.4.0 añade coincidencias por marca/nombre, URL de Google escapada, protocolo HTTPS, compresión, orientación/proporción, límite de tamaño, imagen persistente desde content URI, restauración del editor y cambio/eliminación. Las mismas pruebas compartidas de foto/editor se ejecutan en Robolectric API 27 y en Android 15 API 35.

Las 14 pruebas de `seguridad/reglas.test.mjs` se ejecutaron correctamente en el emulador Firestore 1.22.0 con cuentas A/B y cliente anónimo. Comprueban lectura/consulta, escritura, borrado, transferencias, datos inválidos, consumos, archivo y foto: guardar, recuperar, sustituir, eliminar, tamaño y aislamiento. No acceden al proyecto de producción.

```bash
cd seguridad
npm ci --no-audit --no-fund
npm test
```

Requiere Node 22 o superior y Java 21. Android CI también ejecuta las reglas y conserva su salida. La primera ejecución completa de 1.4.0, sobre 30dee32, aprobó 85 pruebas locales y 26 Android 15, además de las 14 de reglas, lint y diagnóstico Windows: https://github.com/Elpipila5/ExpiryAlert/actions/runs/45472834654 . La suite se amplía después con captura de tarjeta/editor y validación de metadatos del APK. El resultado definitivo es el workflow del commit entregado, no un conteo anticipado. `VERSION_Y_PRUEBAS.json` del paquete contiene el total local real. Los informes Android y de seguridad se adjuntan como artefactos separados.

```bash
bash gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
python3 scripts/preparar-entrega.py
```

Para reproducir fuera de CI, revisar los requisitos de JDK/SDK y el uso de Java 21 para Robolectric en la instalación y el workflow. La preparación del paquete falla si faltan APK, pruebas o existen fallos/omisiones locales.

La validación del PR #28 aprobó 86 pruebas locales, 27 Android 15 y 14 de reglas, además de APK, lint y diagnóstico Windows: https://github.com/Elpipila5/ExpiryAlert/actions/runs/45485672418 . Incluye la corrección de una carrera de recomposición al probar la eliminación de foto. Para 1.4.1, consultar el workflow del commit entregado, que vuelve a compilar con el archivo cliente nuevo. El empaquetado valida también `project_id` y `google_app_id` generados por Gradle y registra ambos identificadores en `VERSION_Y_PRUEBAS.json`.

## Entrega y límites pendientes

El paquete contiene APK, fuente, manual, guion, metadatos, hashes e informes locales. Es una compilación de depuración utilizable para demostración; actualizar una instalación existente exige la misma firma.

Las reglas están publicadas en el Firebase propio y Android 1.4.1 incorpora su configuración oficial. Falta registrar la aceptación de cuenta/guardado/fotos desde el teléfono. WorkManager con app cerrada/ahorro de batería requiere evidencia específica de dispositivo. Las contribuciones personales se registran por evidencia, sin atribuir identidades ni trabajo no confirmado. La revisión conjunta comunicada de 1.3.0 se conserva en `docs/equipo.md`.

## Fuentes

- Repositorio y PR: https://github.com/Elpipila5/ExpiryAlert
- Plan del equipo: referencia y secciones en `docs/referencias/README.md`.
- Google Custom Search: https://developers.google.com/custom-search/v1/overview
- API y licencia de fotos Open Food Facts: https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/
- Pruebas de reglas: https://firebase.google.com/docs/rules/unit-tests
