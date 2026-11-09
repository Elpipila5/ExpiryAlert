# Sprint 7 — PB10/F10: recetas según el inventario

**Cronograma:** 9 al 13 de noviembre de 2026. Este PR entrega PB10. PB08 (bajo stock y lista de compras) se trabaja por separado.

**Estado:** cambios preparados; pruebas de Android Studio y servidor IA real pendientes.

## Pruebas en Android Studio sin servidor IA

| Caso | Acción | Resultado esperado |
| --- | --- | --- |
| R01 | Iniciar sesión, pulsar «🍳 Recetas con mis productos» | Abre pantalla y permite regresar |
| R02 | Registrar tomate y huevo con cantidad positiva | Sugiere «Huevos con tomate» y muestra faltantes |
| R03 | Registrar ingrediente vencido | No aparece entre ingredientes aprovechables |
| R04 | Archivar producto o dejar cantidad 0 | Ya no cuenta para sugerencias |
| R05 | Registrar producto sin fecha | Muestra advertencia para comprobar caducidad antes de usar |
| R06 | Pulsar «Buscar preparación en YouTube» | Abre resultados de búsqueda HTTPS |
| R07 | Iniciar sesión como cuentas distintas | Cada una usa únicamente sus productos |
| R08 | Sin conexión | Se pueden usar datos locales en caché cuando Firebase disponga de ellos; error visible si no hay acceso |
| R09 | IA sin backend HTTPS | Botón desactivado y recetas locales disponibles |
| R10 | Ejecutar pruebas unitarias | `RecetasLocalesTest` termina sin errores |

## Pruebas del servicio IA (necesita claves y despliegue)

| Caso | Acción | Resultado |
| --- | --- | --- |
| IA01 | `GET /salud` | HTTP 200, `estado=ok` |
| IA02 | `POST /recetas/ia` sin token | HTTP 401 |
| IA03 | Token Firebase falso | HTTP 401 |
| IA04 | Token de cuenta A | Solo obtiene productos con `usuarioId=A` |
| IA05 | Productos vencidos, archivados, agotados | Se descartan antes de enviar datos a IA |
| IA06 | Sin ingredientes utilizables | HTTP 400 |
| IA07 | Sin clave del proveedor | HTTP 503 |
| IA08 | Sesión, inventario, clave y cuota válidos | JSON con recetas e ingredientes/faltantes/pasos |
| IA09 | Error de red o servicio | Error legible, sin bloqueo de sugerencias locales |
| IA10 | Peticiones consecutivas | HTTP 429 por límite de desarrollo |

## Evidencias para entregar

Guardar el commit/APK probado, capturas de pantalla de recetas, ingredientes faltantes y YouTube. Ejecutar `:app:testDebugUnitTest` y `python -m unittest test_seleccion.py`; si se activa IA, agregar capturas con HTTP 401 sin token y 200 con cuenta de prueba. No mostrar JWT, correos reales, claves de OpenAI, ni claves de servicio Firebase.

Las recetas son sugerencias; verificar fechas, conservación y cocción real de los alimentos.
