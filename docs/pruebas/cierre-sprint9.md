# Cierre técnico del Sprint 9

Estado al 26/11/2026: versión 1.4.2, código 9. El alcance funcional del cronograma F01–F10 está implementado. Este cierre atiende correcciones, integración y validación técnica; las participaciones se excluyen a petición del solicitante.

## Alcance disponible

| Cronograma | Funciones integradas |
| --- | --- |
| Sprints 1–5, F01–F05 | Cuenta y sesión; productos por usuario con foto opcional; registro manual, código de barras y OCR con confirmación; inventario clasificado y alertas con confirmación/archivo. |
| Sprint 6, F06–F07 | Consumos con conversiones kg/g/mg y L/ml, saldo restante, rechazo de cantidades imposibles, confirmación y corrección periódica. |
| Sprint 7, F08/F10 | Umbral de bajo stock, avisos, lista de compras; 28 recetas completas/parciales e ideas sin inventario, faltantes, preparación y YouTube. |
| Sprint 8, F09 | Hora de desayuno configurable, recordatorio aproximado, registro de consumo y permisos opcionales. |
| Sprint 9 | Correcciones 1.4.2, pruebas locales/Android/reglas, diagnóstico de actualización Windows y paquete con APK, fuente, manuales y hashes. |

Se conservan diseño adaptativo, modos claro/oscuro y personalización de nombre/foto de perfil. Compras, preferencias y foto de perfil son locales por cuenta; productos y fotos del producto se guardan en Firestore.

## Correcciones y evidencia

La [ejecución final](https://github.com/Elpipila5/ExpiryAlert/actions/runs/45927237633) aprobó 101 pruebas locales, 37 Android 15 y 14 de reglas, sin fallos ni omisiones, además de lint, APK y diagnóstico Windows. PR #30 integrado; `main` y `v21` sincronizados.

El [informe de 1.4.2](correcciones-142.md) documenta los defectos reproducidos antes de corregir, las soluciones y los flujos completos de Android con Firebase local. El workflow del commit entregado y sus artefactos identifican los resultados reales; no se declara terminado un caso solo por su fecha planeada.

Las reglas del Firebase propio `expiryalert-49b5e` se publicaron el 24/11. El usuario confirmó posteriormente que su cuenta aparece en Authentication y que encontró la vista de datos de Firestore. Este cambio conserva la configuración del servicio y sus reglas.

## Comprobaciones finales en el teléfono

1. Actualizar sin desinstalar y comprobar **1.4.2** en Mi perfil. Usar la cuenta ya creada en el Firebase propio.
2. Guardar un producto con foto y comprobar su persistencia tras cerrar y abrir sesión con la cuenta real.
3. Observar un recordatorio con la app cerrada y las condiciones de ahorro de batería del Xiaomi. Android/WorkManager pueden retrasar el horario.
4. Descargar los artefactos del workflow aprobado y conservar la demostración final. [Entrega y manuales](../entrega/LEEME.md).

Las pruebas de integración no acceden a la cuenta real ni acreditan las restricciones de batería de ese teléfono. Estos límites de aceptación se informan sin inventar resultados.
