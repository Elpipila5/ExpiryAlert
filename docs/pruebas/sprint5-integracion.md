# Sprint 5 — Integración y demostración (26–30 octubre 2026)

**Estado:** código preparado, **pruebas reales por ejecutar y registrar**. La meta del cronograma es un flujo funcional de F01–F05 que demuestre al menos aproximadamente 50 % del alcance; no se declara cumplimiento por el mero hecho de tener código.

## Orden de demostración sugerido (flujo extremo a extremo)

1. Abrir ExpiryAlert e iniciar sesión con una **cuenta de prueba A**. Mostrar la lista de productos vinculada al usuario.
2. Registrar un producto manualmente (por ejemplo `Leche demo`, `Lácteos`, `1 L`) con fecha de mañana; verificar que aparece en inventario.
3. Pulsar **Revisar caducidades ahora** si el trabajo automático aún no se ejecuta. Abrir la notificación, elegir **Sí, todavía lo tengo** y comprobar que se guarda la confirmación.
4. Registrar un producto mediante **código de barras**, revisar datos sugeridos, opcionalmente usar **OCR de fecha impresa**, confirmar y guardar. Si el código no está en el catálogo o la fecha no se reconoce, usar el flujo de corrección manual.
5. Editar nombre/fecha/cantidad del producto y comprobar que se actualiza en tiempo real; mostrar que se ha solicitado revisión de caducidad tras guardarlo.
6. Abrir una notificación de otro producto y elegir **Ya no lo tengo**; debe desaparecer del inventario activo.
7. Eliminar un producto y verificar que desaparece; no debe quedar su alerta visible.
8. Cerrar sesión, iniciar sesión con una **cuenta de prueba B** y comprobar que no puede ver ni cambiar productos de A.
9. Ejecutar pruebas con permiso de notificaciones denegado: el inventario y guardado deben seguir funcionando.
10. Mostrar el resultado de las pruebas unitarias y la matriz de seguridad de Firestore **solo después de activarla y comprobarla en Firebase**.

## Casos a registrar

| ID | Comportamiento | Resultado esperado | Estado |
| --- | --- | --- | --- |
| S5-01 | Registro / login / sesión persistente | Flujo estable | No ejecutado |
| S5-02 | Producto manual con caducidad mañana | Guardado en Firestore y listado | No ejecutado |
| S5-03 | Edición de fecha y cantidad | Actualización visible y comprobación de alertas solicitada | No ejecutado |
| S5-04 | Eliminar producto | Sale de inventario y se cancela aviso visible | No ejecutado |
| S5-05 | Escanear código EAN/UPC | Autocompleta datos disponibles o permite corregir | No ejecutado |
| S5-06 | OCR de fecha | Sugiere una fecha con confirmación obligatoria | No ejecutado |
| S5-07 | Fecha pendiente | Guarda sin inventar fecha y no avisa hasta completar | No ejecutado |
| S5-08 | Notificación, respuesta «todavía lo tengo» | Marca `ultimaConfirmacion` | No ejecutado |
| S5-09 | Notificación, respuesta «ya no lo tengo» | Archiva y no aparece en productos activos | No ejecutado |
| S5-10 | Sin permiso de notificaciones | CRUD sigue funcionando | No ejecutado |
| S5-11 | Dos cuentas diferentes y reglas publicadas | No hay acceso cruzado a productos | No ejecutado |
| S5-12 | Cierre de sesión con avisos visibles | No quedan avisos privados de la cuenta en el panel | No ejecutado |
| S5-13 | Red desactivada durante consulta externa | Error controlado y registro manual disponible | No ejecutado |
| S5-14 | APK / Gradle | Compila e instala en dispositivo de prueba | No ejecutado |

## Registro de evidencias

Rellenar por cada caso: **commit/build**, teléfono/emulador y versión Android, fecha de ejecución, responsable, resultado observado, estado (aprobado/fallido/bloqueado) y captura o enlace a video. No marcar «Aprobado» sin ejecutar.

Usar los casos de [registro inteligente](registro-inteligente-codigos.md), [OCR](sprint5-ocr.md) y [seguridad Firestore](../seguridad-firestore.md) como guías complementarias.

## Limitaciones técnicas que deben explicarse en la exposición

- WorkManager realiza comprobaciones **aproximadas**, no alarmas exactas; para demostrar inmediatamente se usa el botón manual.
- Los EAN/UPC convencionales no contienen necesariamente la caducidad del envase. Puede quedar **pendiente** y no se emitirán alertas hasta conocerla.
- Las sugerencias de OCR deben confirmarse o corregirse.
- Las reglas de Firestore se vuelven efectivas **solo al desplegarlas al proyecto Firebase**.
- F06 y F07 (consumo y verificación periódica) comienzan la semana del 2–6 de noviembre.

## Entrega para el Sprint Review

Conservar build APK de depuración o distribuible, captura de la matriz cumplimentada, notificación funcional, captura OCR/registro, Firestore (sin datos personales), y commits de los tres integrantes. El cronograma pide una demostración ejecutable, no solamente documentación.
